package evolomino;

import evolomino.enums.CellType;

import java.util.ArrayList;
import java.util.Comparator;

import static evolomino.enums.CellType.*;

public class Evolomino {
    public final int height;
    public final int width;
    public final int totalCells;

    public CellType[] area;
    public ArrayList<ArrayList<Integer>> arrows;
    public ArrayList<ArrayList<Integer>> reachableCells;

    public Evolomino(
            Sample s
    ) {
        height = s.height;
        width = s.width;
        totalCells = height * width;

        initArea(s.field);

        initArrows(s.field);

        initReachableCells(s.field);
    }

    private void initArea(int[] problem) {
        area = new CellType[totalCells];
        for (int i = 0; i < totalCells; ++i) {
            area[i] = CellType.values()[problem[i]];
        }
    }

    private void initArrows(int[] problem) {
        // find all beginnings of an arrow
        arrows = new ArrayList<ArrayList<Integer>>(0);
        for (int i = 0; i < totalCells; ++i) {
            if (isStartOfAnArrow(i)) {
                arrows.add(new ArrayList<Integer>());
                arrows.getLast().add(i);
            }
        }

        // fill up each arrow
        int lastCell, nextCell;
        for (ArrayList<Integer> arrow : arrows) {
            lastCell = arrow.getLast();
            nextCell = getNext(lastCell);
            while (
                    nextCell != lastCell &&
                            nextCell != -1
            ) {
                arrow.add(nextCell);
                lastCell = nextCell;
                nextCell = getNext(lastCell);
            }
        }
    }

    private void initReachableCells(int[] problem) {
        reachableCells = new ArrayList<ArrayList<Integer>>(0);

        for (int a = 0; a < arrows.size(); ++a) {
            reachableCells.addLast(new ArrayList<Integer>());

            boolean[] visited = new boolean[totalCells];
            int currentCell = -1;
            for (int c = 0; c < totalCells; ++c) visited[c] = false;

            // beginning of an BFS: first cell
            currentCell = arrows.get(a).getFirst();
            reachableCells.get(a).add(currentCell);
            visited[currentCell] = true;

            for (int i = 0; i < reachableCells.get(a).size(); ++i) {
                currentCell = reachableCells.get(a).get(i);

                if (currentCell + 1 == 12)
                    currentCell = 11;

                for (int neighbourCellNum : getNeighbours(currentCell)) {
                    if  (neighbourCellNum == -1) continue;

                    if (isCellWithSquareOfOtherArrow(neighbourCellNum, a)) {
                        currentCell = -1;
                        break;
                    }
                }

                if (currentCell == -1) {
                    reachableCells.get(a).remove(i);
                    --i;
                    continue;
                }

                for (int neighbourCellNum : getNeighbours(currentCell)) {
                    if (neighbourCellNum == -1) continue;

                    if (visited[neighbourCellNum]) continue;

                    if (
                            cellFromThisArrow(neighbourCellNum, a) ||
                            !isArrow(area[neighbourCellNum]) &&
                            (area[neighbourCellNum] != FILLED)
                    ) {
                       reachableCells.get(a).addLast(neighbourCellNum);
                       visited[neighbourCellNum] = true;
                    }
                }
            }

            reachableCells.get(a).sort(Comparator.naturalOrder());
        }
    }

    private boolean isCellWithSquareOfOtherArrow(int cellNum, int thisArrowNum) {
        // if arrowWithSquare
        if (!isArrow(area[cellNum])) return false;
        if (!cellWithSquare(area[cellNum])) return false;

        return !arrows.get(thisArrowNum).contains(cellNum);
    }

    public static boolean isArrow(CellType t) {
        return t != EMPTY && t != EMPTY_WITHSQUARE &&
               t != FILLED && !t.name().startsWith("NOTYPE");
    }

    public boolean isEmpty(CellType t) {
        return t == EMPTY || t == EMPTY_WITHSQUARE;
    }

    private boolean cellFromThisArrow(int cellNum, int arrowNum) {
        return arrows.get(arrowNum).contains(cellNum);
    }

    public static boolean cellWithSquare(CellType t) {
        return t.ordinal() >= CellType.EMPTY_WITHSQUARE.ordinal() && !t.name().startsWith("NOTYPE");
    }

    public boolean isLastCellOnArrow(int cellNum) {
        return cellNum == getNext(cellNum);
    }

    public boolean isFirstCellOnArrow(int cellNum) {
        return cellNum == getPrevious(cellNum);
    }
    // returns -1 if we're not on an arrow
    // returns cellNum if this is the first cell of an arrow
    public int getPrevious(int cellNum) {
        if (cellNum < 0 || cellNum >= totalCells)
            return -1;

        int prevCellNum = cellNum;
        switch (area[cellNum]) {
            case DOWNTOUP, DOWNTOLEFT, DOWNTORIGHT,
                 DOWNTOUP_WITHSQUARE,
                 DOWNTOLEFT_WITHSQUARE,
                 DOWNTORIGHT_WITHSQUARE -> {
                if (cellNum + width >= totalCells)
                    return cellNum;

                prevCellNum = cellNum + width;
            }
            case UPTODOWN, UPTOLEFT, UPTORIGHT,
                 UPTODOWN_WITHSQUARE,
                 UPTOLEFT_WITHSQUARE,
                 UPTORIGHT_WITHSQUARE -> {
                if (cellNum < width)
                    return cellNum;

                prevCellNum = cellNum - width;
            }
            case LEFTTORIGHT, LEFTTOUP, LEFTTODOWN,
                 LEFTTORIGHT_WITHSQUARE,
                 LEFTTOUP_WITHSQUARE,
                 LEFTTODOWN_WITHSQUARE -> {
                if (cellNum % width == 0)
                    return cellNum;

                prevCellNum = cellNum - 1;
            }
            case RIGHTTOLEFT, RIGHTTOUP, RIGHTTODOWN,
                 RIGHTTOLEFT_WITHSQUARE,
                 RIGHTTOUP_WITHSQUARE,
                 RIGHTTODOWN_WITHSQUARE -> {
                if (cellNum % width == (width - 1))
                    return cellNum;

                prevCellNum = cellNum + 1;
            }
            default -> {
                return -1;
            }
        }

        // if index is out of bounds, then error;
        if (prevCellNum < 0 || prevCellNum >= totalCells)
            return cellNum;
        else if ( // then if we're already not on an arrow;
                area[prevCellNum] == EMPTY ||
                area[prevCellNum] == EMPTY_WITHSQUARE ||
                area[prevCellNum] == FILLED
        )
            return cellNum;

        return prevCellNum;
    }

    // returns -1 if we're not on an arrow
    // returns cellNum if this is the last cell of an arrow
    public int getNext(int cellNum) {
        if (cellNum < 0 || cellNum >= totalCells)
            return -1;

        int nextCellNum = cellNum;
        switch (area[cellNum]) {
            case DOWNTOUP, LEFTTOUP, RIGHTTOUP,
                 DOWNTOUP_WITHSQUARE,
                 LEFTTOUP_WITHSQUARE,
                 RIGHTTOUP_WITHSQUARE -> {
                if (cellNum < width)
                    return cellNum;

                nextCellNum =  cellNum - width;
            }
            case UPTODOWN, LEFTTODOWN, RIGHTTODOWN,
                 UPTODOWN_WITHSQUARE,
                 LEFTTODOWN_WITHSQUARE,
                 RIGHTTODOWN_WITHSQUARE -> {
                if (cellNum + width >= totalCells)
                    return cellNum;

                nextCellNum =  cellNum + width;
            }
            case RIGHTTOLEFT, UPTOLEFT, DOWNTOLEFT,
                 RIGHTTOLEFT_WITHSQUARE,
                 UPTOLEFT_WITHSQUARE,
                 DOWNTOLEFT_WITHSQUARE -> {
                if (cellNum % width == 0)
                    return cellNum;

                nextCellNum = cellNum - 1;
            }
            case LEFTTORIGHT, UPTORIGHT, DOWNTORIGHT,
                 LEFTTORIGHT_WITHSQUARE,
                 UPTORIGHT_WITHSQUARE,
                 DOWNTORIGHT_WITHSQUARE -> {
                if (cellNum % width == (width - 1))
                    return cellNum;

                nextCellNum = cellNum + 1;
            }
            default -> {
                return -1;
            }
        }

        if (nextCellNum < 0 || nextCellNum >= totalCells)
            return cellNum;
        else if ( // then if we're not on an arrow;
                area[nextCellNum] == EMPTY ||
                        area[nextCellNum] == EMPTY_WITHSQUARE ||
                        area[nextCellNum] == FILLED
        )
            return cellNum;

        return nextCellNum;
    }

    public int getArrowOfCell(int cellNum) {
        if (cellNum < 0 || cellNum >= totalCells) {
            return -1;
        }

        int index;
        for (int arrowNum = 0; arrowNum < arrows.size(); ++arrowNum) {
            index = arrows.get(arrowNum).indexOf(cellNum);
            if (index != -1) {
                return arrowNum;
            }
        }

        return -1;
    }

    boolean isStartOfAnArrow(int cellNum) {
        if (
                area[cellNum] != LEFTTORIGHT && area[cellNum] != LEFTTORIGHT_WITHSQUARE &&
                area[cellNum] != RIGHTTOLEFT && area[cellNum] != RIGHTTOLEFT_WITHSQUARE &&
                area[cellNum] != UPTODOWN    && area[cellNum] != UPTODOWN_WITHSQUARE    &&
                area[cellNum] != DOWNTOUP    && area[cellNum] != DOWNTOUP_WITHSQUARE
        )
            return false;

        int prevCellNum = -1; // if this, then there's no previous cell
        switch (area[cellNum]) {
            case LEFTTORIGHT, LEFTTORIGHT_WITHSQUARE -> {
                if (cellNum % width == 0)
                    break;

                prevCellNum = cellNum - 1;
            }
            case RIGHTTOLEFT, RIGHTTOLEFT_WITHSQUARE -> {
                if (cellNum % width == (width - 1))
                    break;

                prevCellNum = cellNum + 1;
            }
            case DOWNTOUP, DOWNTOUP_WITHSQUARE -> {
                if (cellNum + width >= totalCells)
                    break;

                prevCellNum = cellNum + width;
            }
            case UPTODOWN, UPTODOWN_WITHSQUARE -> {
                if (cellNum < width)
                    break;

                prevCellNum = cellNum - width;
            }
        }

        return prevCellNum < 0
                || area[prevCellNum] == EMPTY || area[prevCellNum] == CellType.EMPTY_WITHSQUARE
                || area[prevCellNum] == CellType.FILLED
                || getNext(prevCellNum) != cellNum;
    }

    /**
     if index is out of bounds, then empty array is returned.
     Neighbour order: clockwise (e.g. (N, E, S, W), "12 o'clock", "3 o'clock", "6 o'clock", "9 o'clock").
     If there's no such neighbour, it's number will be replaced with -1.
    */
    public int[] getNeighbours(int cellNum) {
        if (cellNum < 0 || cellNum >= totalCells)
            return new int[0];

        int totalNeighbours = 4;

        ArrayList<Integer> neighbours = new ArrayList<Integer>(0);

        // up neighbour
        if (cellNum - width >= 0) {
            neighbours.add(cellNum - width);
        } else {
            neighbours.add(-1);
        }

        // right neighbour
        if (cellNum % width != (width - 1)) {
            neighbours.add(cellNum + 1);
        } else {
            neighbours.add(-1);
        }

        // down neighbour
        if (cellNum + width < totalCells) {
            neighbours.add(cellNum + width);
        } else {
            neighbours.add(-1);
        }

        // left neighbour
        if (cellNum % width != 0) {
            neighbours.add(cellNum - 1);
        } else {
            neighbours.add(-1);
        }

        int[] a = new int[neighbours.size()];
        for (int i = 0; i < a.length; ++i) {
            a[i] = neighbours.get(i);
        }
        return a;
    }
}
