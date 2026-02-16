package com.example;

import evolomino.Evolomino;
import evolomino.Sample;
import evolomino.WeighedCell;
import evolomino.enums.CellType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class EvolominoGenerator {
    private static Sample resultSample;
    private static Sample p;

    // int limits
    public static int attemptLimit = 1000;
    public static final int accuracy = 1000;
    // thresholds
    public static double thresholdArrowLengthPercent = 0.4;
    public static double thresholdBlocksSizePercent = 0.6;

    // probabilities
    public static double arrowContinueProbability = 0.8;
    public static double arrowLeftTurnProbability = 0.20;
    public static double arrowRightTurnProbability = 0.20;
    public static double arrowNoTurnProbability = 1.0 - arrowLeftTurnProbability - arrowRightTurnProbability;
    // block prob:
    public static double oneMoreCellProbability = 0.7;
    public static double oneMoreBlockProbability = 1;

    // Random
    private static Random rand = new Random();
    public static int seed = rand.nextInt();

    // Memory for last modified cell (carveToUnique)
    private static boolean hereWasSquare = false;

    public static Sample GenerateEvolomino(
            int m,
            int n
    ) {
        System.out.println("Puzzle ID: " + seed);
        rand.setSeed(seed);


        int tries = 0;
        int generatedArrowsCnt = 0;
        resultSample = new Sample(m, n);
        resultSample.name = String.format("%dx%d sample %s", m, n, "test");
        p = resultSample.clone();
        while (!stopCondition(resultSample, tries)) {
            if (!TryAddArrow()) {
                // reset the rs and try again
                p = resultSample.clone();
                ++tries;
            } else {
                // save the intermediate result and show it
                resultSample = p.clone();
                System.out.println("Arrow " + (generatedArrowsCnt + 1) + " was generated successfully. Result:");
                resultSample.showField();
                ++generatedArrowsCnt;
                // tries = 0;
            }
        }

        defenceBlocks();

        resultSample = p.clone();
        CarveToUnique();
        resultSample = p.clone();

        return resultSample;
    }

    private static void CarveToUnique() {
        // 1. Fill all empty cells.
        for (int i = 0; i < p.totalCells; ++i) {
            if (p.field[i] == CellType.EMPTY.ordinal()) {
                p.field[i] = CellType.FILLED.ordinal();
            }
        }

        // Create and shuffle indexes
        ArrayList<Integer> cellIndices = new ArrayList<>(0);
        for (int i = 0; i < p.totalCells; ++i) {
            if (p.field[i] == CellType.FILLED.ordinal()
            ||  p.field[i] >= CellType.EMPTY_WITHSQUARE.ordinal()) {
                cellIndices.add(i);
            }
        }
        shuffle(cellIndices);

        for (int ind: cellIndices) {
            changeCellAtField(ind);

            if (uniqueSolution()) {
                continue;
            } else {
                changeCellAtField(ind);
            }
        }
    }

    private static boolean uniqueSolution() {
        return !EvolominoModel.solve(new Evolomino(p), 0, p.name, resultSample);
    }

    private static void changeCellAtField(int cellNum) {
        // Case one: if there's square, remove it.
        if (p.field[cellNum] >= CellType.EMPTY_WITHSQUARE.ordinal()) {
            p.field[cellNum] -= CellType.EMPTY_WITHSQUARE.ordinal();
            hereWasSquare = true;
            return;
        }

        // Case two: if there's filled, make it empty.
        if (p.field[cellNum] == CellType.FILLED.ordinal()) {
            p.field[cellNum] = CellType.EMPTY.ordinal();
            hereWasSquare = false;
            return;
        }

        // Case three: redo the square place
        if (p.field[cellNum] < CellType.EMPTY_WITHSQUARE.ordinal() && hereWasSquare) {
            p.field[cellNum] += CellType.EMPTY_WITHSQUARE.ordinal();
            hereWasSquare = false;
            return;
        } else {
            // Case four: it is empty and it was filled
            p.field[cellNum] = CellType.FILLED.ordinal();
        }
    }


    private static boolean TryAddArrow() {
        // here we'll store our arrow's cells
        ArrayList<Integer> arrow = new ArrayList<>(0);
        // choose anchorCell. If it does not exist, come out
        int arrowLastCell = chooseArrowStartCell(p);
        if (arrowLastCell == -1) return false;

        int direction = chooseFreeDirection(p, arrowLastCell);

        if (direction == -1) return false;

        while (arrow.size() < 3 || continueArrow()) {
            // choose left turn or straight or right turn
            // Изменить направление и стрелку
            direction = updateDirection(direction);
            arrowLastCell = nextCell(p, arrowLastCell, direction, arrow);

            // if there's no next cell, then we can't continue arrow.
            if (arrowLastCell == -1) break;

            // all conditions are met, we add cell to arrow
            arrow.add(arrowLastCell);
        }

        // if arrow's length is incorrect, then this arrow can't exist.
        if (arrow.size() < 3) return false;

        layArrow(arrow);

        if (!PlaceBlocks(arrow)) return false;
        else                     return true;
    }

    private static boolean PlaceBlocks(ArrayList<Integer> arrow) {
        // at which cell we'll grow the first block? Decide.
        int anchorCell = chooseFirstBlockAnchorCell(arrow);
        if (anchorCell == -1) return false;

        // if we have chosen an anchor, we need to remember its index
        int anchorCellIndex = arrow.indexOf(anchorCell);

        // we need to know how many blocks are placed;
        int placedBlockCnt = 0;

        // so, then we will grow block!
        ArrayList<Integer> block = growBlock(anchorCell, arrow);

        // place block at the p. We will do like that every time
        layBlock(block);
        // protect one blocks from another using NOTYPE
        fenceBlocks();
        ++placedBlockCnt;

        ArrayList<Integer> newBlock;
        ArrayList<Integer> shuffledArrow;

        int shift;
        boolean blockIsPlaced;
        // main cycle of the block placing part
        while (placedBlockCnt < 2 || (canPlaceNextBlock(arrow.size(), anchorCellIndex) && blockPlacingContinue())) {
            blockIsPlaced = false;
            shuffledArrow = arrangeByWeights(arrow, anchorCellIndex + 2);

            int newAnchorIndex;
            for (newAnchorIndex = 0; newAnchorIndex < shuffledArrow.size() && !blockIsPlaced; ++newAnchorIndex) {
                if (!noSquaresInLocal(shuffledArrow.get(newAnchorIndex))) continue;

                shuffle(block);
                for (int blockCellIndex = 0; blockCellIndex < block.size() && !blockIsPlaced; ++blockCellIndex) {

                    shift = shuffledArrow.get(newAnchorIndex) - block.get(blockCellIndex);
                    newBlock = (ArrayList<Integer>) block.clone();
                    shiftBlock(newBlock, shift);

                    if (isCorrect(newBlock)) {
                        addPossibleNeighbourCell(newBlock);

                        layBlock(newBlock);
                        fenceBlocks();
                        ++placedBlockCnt;

                        block = (ArrayList<Integer>) newBlock.clone();
                        anchorCellIndex += newAnchorIndex + 1;

                        blockIsPlaced = true;
                        break;
                    }
                }
            }
            // we've tried to place every block on every cell. No way, need to go out
            if (newAnchorIndex == shuffledArrow.size() && !blockIsPlaced) {
                return false;
            }
        }

        // if we hadn't placed second block, then the arrow can't exist
        if (placedBlockCnt == 1) return false;

        return true;
    }

    public static boolean canPlaceNextBlock(int arrowSize, int lastAnchoCell) {
        return lastAnchoCell + 2 < arrowSize;
    }

    private static int chooseFirstBlockAnchorCell(ArrayList<Integer> arrow) {
        int anchor = -1;
        for (int x: arrangeByWeights(arrow, 0)) {
            if (noSquaresInLocal(x)) {
                anchor = x;
                break;
            }
        }
        return anchor;
    }

    private static void fenceBlocks() {
        for (int i = 0; i < p.totalCells; ++i) {
            if (p.field[i] < CellType.EMPTY_WITHSQUARE.ordinal()) continue;
            if (p.field[i] == CellType.FILLED.ordinal()) continue;

            for (int n: freeNeighbours(i)) {
                p.field[n] = CellType.FILLED.ordinal();
            }
        }
    }
    private static void defenceBlocks() {
        for (int i = 0; i < p.totalCells; ++i) {
            if (p.field[i] == CellType.FILLED.ordinal()) p.field[i] = 0;
        }
    }


    private static boolean isCorrect(ArrayList<Integer> block) {
        if (block.stream().anyMatch(x -> x < 0 || x >= p.totalCells)) return false;

        if (!isConnected(block)) return false;

        // there may be only one non-zero cell: block's anchor cell on arrow.
        long nonZeroCells = block.stream().filter(x -> p.field[x] != 0).count();
        if (nonZeroCells != 1) return false;

        if (getPossibleAddings(block).isEmpty()) return false;

        return true;
    }

    private static boolean isConnected(ArrayList<Integer> block) {
        // empty block isn't correct for our scope. So we deny it.
        if (block.isEmpty()) return false;

        ArrayList<Integer> group = new ArrayList<>(0);

        group.add(block.getFirst());
        int currentCellNum;
        for (int i = 0; i < group.size(); ++i) {
            currentCellNum = group.get(i);

            for (int neighbourSquareCell: getNeighboursInBlock(currentCellNum,  block)) {
                if (!group.contains(neighbourSquareCell)) group.add(neighbourSquareCell);
            }
        }

        return block.size() == group.size();
    }

    private static ArrayList<Integer> getNeighboursInBlock(int cellNum, ArrayList<Integer> block) {
        ArrayList<Integer> n = new ArrayList<>(
                block.stream().filter(x ->
                        x + 1 == cellNum && (cellNum % p.width != 0)
                    ||  x - 1 == cellNum && (cellNum % p.width != (p.width - 1))
                    ||  x + p.width == cellNum
                    ||  x - p.width == cellNum
                ).toList());
        return n;
    }

    private static void addPossibleNeighbourCell(ArrayList<Integer> block) {
        ArrayList<Integer> addings = getPossibleAddings(block);
        shuffle(addings);

        if (addings.isEmpty()) return;

        block.add(addings.getFirst());
    }

    /** There's Fisher-Ietz shuffle*/
    private static void shuffle(ArrayList<Integer> block) {
        int tmp;
        for (int i = block.size() - 1; i >= 1; --i) {
            int j = rand.nextInt(i + 1);

            tmp = block.get(i);
            block.set(i, block.get(j));
            block.set(j, tmp);
        }
    }

    /** There's Ephramidis-Spirakis */
    private static ArrayList<Integer> arrangeByWeights(
            ArrayList<Integer> arrow,
            int leftIndex
    ) {
        int rightBorder = 0;
        if (leftIndex == 0) rightBorder = 2;

        ArrayList<WeighedCell> toSort = new ArrayList<WeighedCell>(0);
        for (int i = leftIndex; i < arrow.size() - rightBorder; ++i) {
            toSort.add(new WeighedCell(
                    -Math.log(rand.nextInt(accuracy) / (double) accuracy)
                            / (double)(arrow.size() - i + leftIndex), arrow.get(i)));
        }

        Comparator<WeighedCell> cmp = Comparator.comparing(WeighedCell::getWeight);
        toSort.sort(cmp);

        ArrayList<Integer> list = new ArrayList<Integer>(0);
        for (WeighedCell weighedCell : toSort) {
            list.add(weighedCell.cellNum);
        }

        return list;
    }

    private static void shiftBlock(
            ArrayList<Integer> block,
            int shift
    ) {
        block.replaceAll(integer -> integer + shift);
    }

    private static ArrayList<Integer> growBlock(
            int anchorCell,
            ArrayList<Integer> arrow
    ) {
        ArrayList<Integer> block = new ArrayList<>(0);
        block.add(anchorCell);

        while (blockGrowthContinue(block.size())) {
            ArrayList<Integer> addings = getPossibleAddings(block);

            if (addings.isEmpty()) break;

            block.add(addings.get(rand.nextInt(addings.size())));
        }

        return block;
    }

    private static void layBlock(ArrayList<Integer> block) {
        for(int x: block) {
            // TODO(Add a constant to proof the assignment)
            p.field[x] += CellType.EMPTY_WITHSQUARE.ordinal();
        }
    }

    private static ArrayList<Integer> getPossibleAddings(
            ArrayList<Integer> block
            ) {
        ArrayList<Integer> neighbours = new ArrayList<Integer>(0);
        for (int x: block) {
            for (int n: freeNeighbours(x)) {
                if (
                        p.field[n] == 0
                    &&  !neighbours.contains(n)
                    &&  !block.contains(n)
                    &&  noSquaresInLocal(n)
                ) neighbours.add(n);
            }
        }

        return neighbours;
    }

    private static boolean noSquaresInLocal(int cellNum) {
        return getSquaresInLocal(cellNum).isEmpty();
    }

    private static ArrayList<Integer> getSquaresInLocal(int cellNum) {
        ArrayList<Integer> squares = new ArrayList<>(0);

        if (
                cellNum >= p.width
                    && p.field[cellNum - p.width] >= CellType.EMPTY_WITHSQUARE.ordinal()
                    && p.field[cellNum - p.width] != CellType.FILLED.ordinal()
        ) squares.add(cellNum - p.width);

        // east
        if (cellNum % p.width != (p.width - 1)
                && p.field[cellNum + 1] >= CellType.EMPTY_WITHSQUARE.ordinal()
                && p.field[cellNum + 1] != CellType.FILLED.ordinal()
        ) squares.add(cellNum + 1);

        // south
        if (cellNum + p.width < p.totalCells
                && p.field[cellNum + p.width] >= CellType.EMPTY_WITHSQUARE.ordinal()
                && p.field[cellNum + p.width] != CellType.FILLED.ordinal()
        ) squares.add(cellNum + p.width);

        // west
        if (
            cellNum % p.width != 0
                && p.field[cellNum - 1] >= CellType.EMPTY_WITHSQUARE.ordinal()
                && p.field[cellNum - 1] != CellType.FILLED.ordinal()
        ) squares.add(cellNum - 1);

        return squares;
    }

    /**
     *
     * @param cellNum an index of cell in those we seek a position
     * @returns a list of indexes where are empty (0) cells
     */
    private static ArrayList<Integer> freeNeighbours(int cellNum) {
        ArrayList<Integer> n = new ArrayList<Integer>(0);
        for (int ind: getPossibleDirections(p, cellNum)) {
            switch (ind) {
                case 0: {
                    n.add(cellNum - p.width);
                    break;
                }
                case 1: {
                    n.add(cellNum + 1);
                    break;
                }
                case 2: {
                    n.add(cellNum + p.width);
                    break;
                }
                case 3: {
                    n.add(cellNum - 1);
                    break;
                }
            }
        }
        return n;
    }

    private static int chooseArrowStartCell(Sample p) {
        ArrayList<Integer> freeCells = new ArrayList<>(0);
        for (int i = 0; i < p.totalCells; ++i) {
            if (freeNeighbours(i).isEmpty()) continue;
//            if (neighbours(i).size() < 2) continue;

            if (p.field[i] == 0) freeCells.add(i);

        }

        shuffle(freeCells);

        if (freeCells.isEmpty()) return -1;
        else return freeCells.getFirst();
    }

    private static ArrayList<Integer> toArrayList(int[] arr) {
        return new ArrayList<>(Arrays.stream(arr).boxed().toList());
    }

    /**
     * @param p - initial sample field
     * @param source - from this cell we choose a direction
     * @return an index of direction in clockwise order [e.g. NESW (0123)]
     */
    private static int chooseFreeDirection(Sample p, int source) {
        ArrayList<Integer> dirs = getPossibleDirections(p, source);

        if (dirs.isEmpty()) return -1;

        return dirs.get(rand.nextInt(dirs.size()));
    }

    /**
     *
     * @param p - base sample
     * @param cellNum - the cell from whose we'll find directions
     * @return the list of possible directions from [0..3] typeCraft.
     * If the directed cell is not empty, it will not come out from func
     */
    private static ArrayList<Integer> getPossibleDirections(Sample p, int cellNum) {
        ArrayList<Integer> dirs = new ArrayList<Integer>(0);

        // north
        if (
                cellNum - p.width >= 0
                        &&  p.field[cellNum - p.width] == CellType.EMPTY.ordinal()
        ) dirs.add(0);

        // east
        if (
                cellNum % p.width != (p.width - 1)
                        &&  p.field[cellNum + 1] == CellType.EMPTY.ordinal()
        ) dirs.add(1);

        // south
        if (
                cellNum + p.width < p.totalCells
                        &&  p.field[cellNum + p.width] == CellType.EMPTY.ordinal()
        ) dirs.add(2);

        // west
        if (
                cellNum % p.width != 0
                        &&  p.field[cellNum - 1] == CellType.EMPTY.ordinal()
        ) dirs.add(3);

        return dirs;
    }

    private static boolean continueArrow() {
        return rand.nextInt(accuracy) <= accuracy * arrowContinueProbability;
    }

    private static int updateDirection(int oldDirection) {
        int[] diffs = {-1, 0, 1};
        double[] diffProbabilities = {
                arrowLeftTurnProbability,
                arrowNoTurnProbability,
                arrowRightTurnProbability
        };

        int resIndex = -1;
        double dot = rand.nextInt(accuracy + 1) / (double) accuracy;
        for (int i = 0; dot >= 0 && i < diffs.length; ++i) {
            resIndex = i;
            dot -= diffProbabilities[i];
        }

        return (oldDirection + 4 + diffs[resIndex]) % 4;
    }

    private static int nextCell(
            Sample p,
            int cellNum,
            int direction,
            ArrayList<Integer> arrow
    ) {
        // if impossible direction - return -1
        if (!getPossibleDirections(p, cellNum).contains(direction)) return -1;

        int nextCell = switch (direction) {
            case 0 -> cellNum - p.width;
            case 1 -> cellNum + 1;
            case 2 -> cellNum + p.width;
            case 3 -> cellNum - 1;
            default -> -1;
        };

        // TODO(Do we need to use this if?)
//        if (arrow.contains(nextCell)) return -1;

        return nextCell;
    }

    private static void layArrow(
            ArrayList<Integer> arrow
    ) {
        // first cell
        int prevDiff = arrow.get(1) - arrow.get(0);
        int nextDiff = -1;
        p.field[arrow.get(0)] = getFirstArrowSign(p.width, prevDiff);

        for (int i = 1; i < arrow.size() - 1; ++i) {
            prevDiff = arrow.get(i) - arrow.get(i - 1);
            nextDiff = arrow.get(i + 1) - arrow.get(i);

            p.field[arrow.get(i)] = getArrowSign(p.width, prevDiff, nextDiff);
        }

        // last cell
        prevDiff = arrow.getLast() - arrow.get(arrow.size() - 2);
        p.field[arrow.getLast()] = getFirstArrowSign(p.width, prevDiff);
    }

    private static int getFirstArrowSign(int width, int prevDiff) {
             if (prevDiff == +1)     return CellType.LEFTTORIGHT.ordinal();
        else if (prevDiff == -1)     return CellType.RIGHTTOLEFT.ordinal();
        else if (prevDiff == -width) return CellType.DOWNTOUP.ordinal();
        else if (prevDiff ==  width) return CellType.UPTODOWN.ordinal();
        else                         return CellType.FILLED.ordinal();
    }

    /**
     * @param width field parameter. amount of cells in one row
     * @param prevDiff index difference between current and previous cells
     * @param nextDiff index difference between next and current cells
     * @return an index of arrow variety. Returns `CellType.FILLED` if error.
     */
    private static int getArrowSign(
            final int width,
            int prevDiff,
            int nextDiff
    ) {
        if (prevDiff == +1) {
            if (nextDiff == +1)     return CellType.LEFTTORIGHT.ordinal();
            if (nextDiff == -width) return CellType.LEFTTOUP.ordinal();
            if (nextDiff ==  width) return CellType.LEFTTODOWN.ordinal();
        } else if (prevDiff == -1) {
            if (nextDiff == -1)     return CellType.RIGHTTOLEFT.ordinal();
            if (nextDiff == -width) return CellType.RIGHTTOUP.ordinal();
            if (nextDiff ==  width) return CellType.RIGHTTODOWN.ordinal();
        } else if (prevDiff == -width) {
            if (nextDiff == -width) return CellType.DOWNTOUP.ordinal();
            if (nextDiff == -1)     return CellType.DOWNTOLEFT.ordinal();
            if (nextDiff ==  1)     return CellType.DOWNTORIGHT.ordinal();
        } else if (prevDiff == width) {
            if (nextDiff == width)  return CellType.UPTODOWN.ordinal();
            if (nextDiff == -1)     return CellType.UPTOLEFT.ordinal();
            if (nextDiff ==  1)     return CellType.UPTORIGHT.ordinal();
        }
        // TODO(Decide. Maybe we need to put CellType.NOTYPE here?)
        return CellType.FILLED.ordinal();
    }

    private static boolean stopCondition(
            Sample p,
            int attemptsAmount
    ) {
        return p.totalArrowLength() >= thresholdArrowLengthPercent * p.totalCells
            || p.fieldFillPercent() >= thresholdBlocksSizePercent * p.totalCells
            || attemptsAmount > attemptLimit;
    }

    private static boolean blockGrowthContinue(int blockSize) {
//        if (blockSize == 1) return true;

        if (blockSize > p.totalCells / 4) return false;

        return rand.nextInt(accuracy) <= accuracy * oneMoreCellProbability;
    }

    private static boolean blockPlacingContinue() {
        return rand.nextInt(accuracy) <= accuracy * oneMoreBlockProbability;
    }
}
