package com.example;

import com.google.ortools.Loader;
import com.google.ortools.init.OrToolsVersion;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.google.ortools.sat.SolutionCallback;
import dataclasses.Shift;
import evolomino.Evolomino;
import evolomino.Sample;
import evolomino.enums.CellType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.lang.Math.abs;

public final class EvolominoModel {
    private static MPSolver solver;
    private static Evolomino solution;

    public static MPVariable[] x;
    public static MPVariable[][][] y;
    public static MPVariable[][][] F;
    public static MPVariable[][][][] f;
    public static MPVariable[][][] t;

    private static ArrayList<Shift> allowedShifts;


    public static void prepareModel(
            Evolomino evo,
            String sampleName,
            Sample baseSol
    ) {
        Loader.loadNativeLibraries();

        // Show the version of the solver
//        System.out.println("Google OR-Tools version: " + OrToolsVersion.getVersionString());

        // Create the linear solver with the GLOP backend.
//        solver = MPSolver.createSolver("SCIP");
//        solver = MPSolver.createSolver("BOP");
        solver = MPSolver.createSolver("SAT");
        if (solver == null) {
            System.out.println("Could not create solver SCIP");
            return ;
        }

        // init parameters
        // TODO(Add a way to setup filePath for saving)
        exportFilePath = "generatedSamples/5x5/sample1/model.txt";
//        exportFilePath = exportFilePath.replace("[]", sampleName);

        // field sizes
        final int rowCount = evo.height;
        final int colCount = evo.width;
        final int cellCount = rowCount * colCount;

        // amount of arrows:
        final int arrowCount = evo.arrows.size();

        // maximum amount of blocks on each arrow
        final int[] kOfArrow = new int[evo.arrows.size()];
        for (int a = 0; a < arrowCount; ++a) {
            kOfArrow[a] = (evo.arrows.get(a).size() + 1) / 2;
        }
        // maximum among all arrows:
        final int kMax = Arrays.stream(kOfArrow).max().getAsInt();
        final int maxArrowSize = evo.arrows.stream().max(Comparator.comparing(ArrayList::size)).get().size();

        // some big constant, to provide a maximum
        final long M = cellCount;

        // Variable to store neighbour numbers. Analogue of N(i) from the model
        int[] neighbours;

        // ---- ---- ---- V A R I A B L E S  P A R T ---- ---- --- //

        // x_i:
        x = new MPVariable[cellCount];
        for (int i = 0; i < cellCount; ++i) {
            x[i] = solver.makeBoolVar("x" + (i + 1));
        }

        // Preprocessing
        MPConstraint uniqueness = solver.makeConstraint(
                "There is at least one difference from this solution"
        );
        int filledCellsCounter = 0;
        for (int i = 0; i < baseSol.totalCells; ++i) {
            if (baseSol.field[i] >= CellType.EMPTY_WITHSQUARE.ordinal()) {
                uniqueness.setCoefficient(
                        x[i],
                        -1.0
                );
                ++filledCellsCounter;
            } else {
                uniqueness.setCoefficient(
                        x[i],
                        1.0
                );
            }
        }
        uniqueness.setLb(1 - filledCellsCounter);

        // Preprocessing
        for (int i = 0; i < cellCount; ++i) {
            if (evo.cellWithSquare(evo.area[i]))
                x[i].setBounds(1.0, 1.0);
            else if (evo.area[i] == CellType.FILLED)
                x[i].setBounds(0.0, 0.0);
        }


        // b_ak
        MPVariable[][] b = new MPVariable[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                b[a][k] = solver.makeBoolVar(
                        "b"
                                + "_" + (a + 1)
                                + "_" + (k + 1)
                );
            }

            // First and second blocks always exist
            b[a][0].setBounds(1.0, 1.0);
            b[a][1].setBounds(1.0, 1.0);
        }

        // y_a_k_i
        y = new MPVariable[arrowCount][kMax][cellCount];
        for (int a = 0; a < arrowCount; ++a) {
            for  (int k = 0; k < kOfArrow[a]; ++k) {
                for (int i: evo.reachableCells.get(a)) {
                    y[a][k][i] = solver.makeBoolVar(
                            "y"
                                    + "_" + (a + 1)
                                    + "_" + (k + 1)
                                    + "_" + (i + 1)
                    );
                }
            }
        }

        // N_ak
        MPVariable[][] N = new MPVariable[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                N[a][k] = solver.makeIntVar(
                        0.0,
                        cellCount / 2.0,
                        "N"
                                + "_" + (a + 1)
                                + "_" + (k + 1)
                );
            }

            // Necessary:  we don't know the size of first block on the arrow.
            // Size may be not equal to 1, it may be more. That's why we don't init it here
        }

        // f_akij
        f = new MPVariable[arrowCount][kMax][cellCount][4];
        // for each arrow
        for (int a = 0; a < arrowCount; ++a) {
            // for each block on each arrow
            for (int k = 0; k < kOfArrow[a]; ++k) {
                // for each cell from each block of each arrow
                for (int i: evo.reachableCells.get(a)) {
                    // for each neighbour of each cell from each block of each arrow
                    // steps order: clockwise
                    // (e.g. "12 o'clock", "3 o'clock", "6 o'clock", "9 o'clock")

                    neighbours = evo.getNeighbours(i);
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;
                        if (!evo.reachableCells.get(a).contains(neighbours[j])) continue;
                        if (evo.isArrow(evo.area[i]) && evo.isArrow(evo.area[neighbours[j]])) continue;

                        f[a][k][i][j] =
                                solver.makeIntVar(
                                        0.0,
                                        cellCount,
                                        "f"
                                                + "_" + (a + 1)
                                                + "_" + (k + 1)
                                                + "_" + (i + 1)
                                                + "_" + (neighbours[j] + 1)
                                );
                    }
                }
            }
        }

        // F_aki
        F = new MPVariable[arrowCount][kMax][maxArrowSize];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                // index that shows order of a cell in an arrow
                for (int iFromArrow = 0; iFromArrow < evo.arrows.get(a).size(); ++iFromArrow) {
                    F[a][k][iFromArrow] = solver.makeIntVar(
                            0.0,
                            M,
                            "F"
                                    + "_" + (a + 1)
                                    + "_" + (k + 1)
                                    + "_" + (evo.arrows.get(a).get(iFromArrow) + 1)
                    );
                }
            }
        }

        // All existing shifts: [-cellCount + 1, -cellCount + 2, ..., -3, -2, 2, 3, .., cellCount - 2, cellCount - 1]
        // TotalShifts: cellCount * 2 - 4
        // But to store them at the indexes we need to store cellCount * 2 - 1
        // And if cellCount == 5 then t[1][2][0] is equal to t_2_3_-4
        // T part (shifts prepare)
        ArrayList<Integer> shifts = new ArrayList<Integer>();
        for (int j = -cellCount + 1; j <= (cellCount - 1); ++j) {
            if (
                    j == -colCount || j == colCount ||
                            j == -1 || j == 1 ||
                            j == 0
            ) continue;

            shifts.addLast(j);
        }
        int[] T = new int[shifts.size()];
        int last = 0;
        for (int j: shifts) {
            T[last] = j; ++last;
        }

        // \t_akj
        t = new MPVariable[arrowCount][kMax][cellCount * 2];
        // Idea: init shift only if you see is in the constraint!
        // Indexing: use (cellCount - 1) + j. Also use existing_shifts (T)

        // ---- ---- ---- C O N S T R A I N T S  P A R T ---- ---- --- //

        // Constraints stage 2.3

        // Each cell must be bound exactly to one block
        // \sum_a \sum_{k_a}y_aki - x_i = 0
        MPConstraint[] cellBoundConstraint = new MPConstraint[cellCount];
        for (int i = 0; i < cellCount; ++i) {
            if (evo.area[i] == CellType.FILLED) continue;


            cellBoundConstraint[i] = solver.makeConstraint(
                    0.0,
                    0.0,
                    "Сell " + (i + 1) + " can be only in one block"
            );

            for (int a = 0; a < arrowCount; ++a) {
                for (int k = 0; k < kOfArrow[a]; ++k) {
                    cellBoundConstraint[i].setCoefficient(
                            y[a][k][i],
                            1.0
                    );
                }
            }

            cellBoundConstraint[i].setCoefficient(
                    x[i],
                    -1.0
            );
        }

        // Each two cells on arrow can't contain squares simultaneously
        // x_i + x_{\next{i}} \leq 1
        MPConstraint[][] squaresOnBothNeighboursBanConstraint = new MPConstraint[arrowCount][maxArrowSize];
        int cellNum;
        int next;
        for (int a = 0; a < arrowCount; ++a) {
            for (int j = 0; j < evo.arrows.get(a).size() - 1; ++j) {
                cellNum = evo.arrows.get(a).get(j);
                next = evo.arrows.get(a).get(j + 1);

                squaresOnBothNeighboursBanConstraint[a][j] = solver.makeConstraint(
                        "Can't set squares on two neighbour cells at the same time "
                                + (cellNum + 1) + " and " + (next + 1)
                );
                squaresOnBothNeighboursBanConstraint[a][j].setUb(1.0);

                squaresOnBothNeighboursBanConstraint[a][j].setCoefficient(
                        x[cellNum],
                        1.0
                );
                squaresOnBothNeighboursBanConstraint[a][j].setCoefficient(
                        x[next],
                        1.0
                );

            }
        }

        // Block is activated only if contains at least one squareCell
        // \sum_i y_aki - b_ak \geq 0
        MPConstraint[][] blockActivationConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                blockActivationConstraint[a][k] = solver.makeConstraint(
                        "Block " + (k + 1)
                                + " from arrow " + (a + 1)
                                + " is active if contains square"
                );
                blockActivationConstraint[a][k].setLb(0.0);

                for (int i = 0; i < cellCount; ++i) {
                    blockActivationConstraint[a][k].setCoefficient(
                            y[a][k][i],
                            1.0
                    );
                }

                blockActivationConstraint[a][k].setCoefficient(
                        b[a][k],
                        -1.0
                );
            }

        }

        // Block is deactivated if it is empty (if it doesn't contain squares)
        // \sum_i{y^{ak}_i} - M \cdot b_{ak} \leq 0
        MPConstraint[][] blockDeactivationConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                blockDeactivationConstraint[a][k] = solver.makeConstraint(
                        "Block " + (k + 1)
                                + " from arrow " + (a + 1)
                                + " is inactive if empty"
                );
                blockDeactivationConstraint[a][k].setUb(0.0);

                for (int i = 0; i < cellCount; ++i) {
                    blockDeactivationConstraint[a][k].setCoefficient(
                            y[a][k][i],
                            1.0
                    );
                }

                blockDeactivationConstraint[a][k].setCoefficient(
                        b[a][k],
                        -M
                );
            }
        }

        // Activated block must contain exactly one square on the arrow
        // \sum_{i \in P_{a}}{y_i^{ak}} - b_{ak} = 0
        MPConstraint[][] blockBindConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                blockBindConstraint[a][k] = solver.makeConstraint(
                        0.0,
                        0.0,
                        "Block " + (k + 1)
                                + " from arrow " + (a + 1)
                                + " must contain only one square on arrow"
                );

                for (int i: evo.arrows.get(a)) {
                    blockBindConstraint[a][k].setCoefficient(
                            y[a][k][i],
                            1.0
                    );
                }

                blockBindConstraint[a][k].setCoefficient(
                        b[a][k],
                        -1.0
                );
            }
        }

        // Blocks must be activated in natural order
        // b_{ak} - b_{a, k-1} \leq 0
        MPConstraint[][] activationSequenceConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 1; k < kOfArrow[a]; ++k) {
                activationSequenceConstraint[a][k] = solver.makeConstraint(
                        "Block " + (k + 1)
                                + " from arrow " + (a + 1)
                                + " must be earlier than block " + (k)

                );
                activationSequenceConstraint[a][k].setUb(0.0);

                activationSequenceConstraint[a][k].setCoefficient(
                        b[a][k],
                        1.0
                );
                activationSequenceConstraint[a][k].setCoefficient(
                        b[a][k - 1],
                        -1.0
                );
            }
        }

        // Arrow and block numbering must be co-directed
        // \sum_{j \in P^{<i}_a}{y^{ak}_j} + y^{a, k - 1}_i \leq 1
        MPConstraint[][][] codirectionConstraint = new MPConstraint[arrowCount][kMax][maxArrowSize];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 1; k < kOfArrow[a]; ++k) {
                for (int i = 0; i < evo.arrows.get(a).size(); ++i) {
                    cellNum = evo.arrows.get(a).get(i);

                    codirectionConstraint[a][k][i] = solver.makeConstraint(
                            "Order of blocks " + (k + 1) + "," + (k + 1 + 1)
                                    + " of arrow " + (a + 1)
                                    + " of cell " + (cellNum + 1)
                                    + " must be co-directed with arrow"
                    );
                    codirectionConstraint[a][k][i].setUb(1.0);

                    for (int j = 0; j < i; ++j) {
                        codirectionConstraint[a][k][i].setCoefficient(
                                y[a][k][evo.arrows.get(a).get(j)],
                                1.0
                        );
                    }

                    codirectionConstraint[a][k][i].setCoefficient(
                            y[a][k - 1][cellNum],
                            1.0
                    );


                }
            }
        }

        // Two neighbour cells can't belong to the different blocks simultaneously (2 parts)
        // y^{ak}_i + y^{a'k'}_{i+1} \leq 1 (constraints[0])
        // y^{ak}_i + y^{a'k'}_{i+c} \leq 1 (constraints[1])
        MPConstraint[][][][][][] blocksTouchBanConstraint = new MPConstraint[2][arrowCount][arrowCount][kMax][kMax][cellCount];
        for (int a = 0; a < arrowCount; ++a) {
            for (int aStrix = 0; aStrix < arrowCount; ++aStrix) {
                for (int k = 0; k < kOfArrow[a]; ++k) {
                    for (int kStrix = 0; kStrix < kOfArrow[aStrix]; ++kStrix) {
                        if (aStrix == a && kStrix == k) continue;

                        for (int i: evo.reachableCells.get(a)) {
                            if (i % evo.width == (evo.width - 1)) continue;

                            if (!evo.reachableCells.get(aStrix).contains(i + 1)) continue;

                            blocksTouchBanConstraint[0][a][aStrix][k][kStrix][i] =
                                    solver.makeConstraint(
                                            "Blocks "
                                                    + (k + 1) + "," + (kStrix + 1)
                                                    + " (arrows " + (a + 1) + "," + (aStrix + 1) + ")"
                                                    + " can't contain cells "
                                                    + (i + 1) + ", " + (i + 1 + 1)
                                                    + " simultaneously (pt.1)"
                                    );
                            blocksTouchBanConstraint[0][a][aStrix][k][kStrix][i].setUb(1.0);

                            blocksTouchBanConstraint[0][a][aStrix][k][kStrix][i].setCoefficient(
                                    y[a][k][i],
                                    1.0
                            );
                            blocksTouchBanConstraint[0][a][aStrix][k][kStrix][i].setCoefficient(
                                    y[aStrix][kStrix][i + 1],
                                    1.0
                            );

                        }

                        for (int i: evo.reachableCells.get(a)) {
                            if (i + evo.width > evo.totalCells) break;
                            if (!evo.reachableCells.get(aStrix).contains(i + evo.width)) continue;

                            blocksTouchBanConstraint[1][a][aStrix][k][kStrix][i] =
                                    solver.makeConstraint(
                                            "Blocks "
                                                    + (k + 1) + "," + (kStrix + 1)
                                                    + " (arrows " + (a + 1) + "," + (aStrix + 1) + ")"
                                                    + " can't contain cells "
                                                    + (i + 1) + ", " + (i + evo.width + 1)
                                                    + " simultaneously (pt.2)"
                                    );
                            blocksTouchBanConstraint[1][a][aStrix][k][kStrix][i].setUb(1.0);

                            blocksTouchBanConstraint[1][a][aStrix][k][kStrix][i].setCoefficient(
                                    y[a][k][i],
                                    1.0
                            );
                            blocksTouchBanConstraint[1][a][aStrix][k][kStrix][i].setCoefficient(
                                    y[aStrix][kStrix][i + evo.width],
                                    1.0
                            );
                        }
                    }
                }
            }
        }

        // Size of block must be equal to exact variable
        // N_{ak} - \sum_{i \in C}{y^{ak}_i} = 0
        MPConstraint[][] blockSizesConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                blockSizesConstraint[a][k] = solver.makeConstraint(
                        0.0,
                        0.0,
                        "Size"
                                + " of block " + (k + 1)
                                + " of arrow " + (a + 1)
                );

                blockSizesConstraint[a][k].setCoefficient(
                        N[a][k],
                        1.0
                );

                for (int i = 0; i < cellCount; ++i) {
                    blockSizesConstraint[a][k].setCoefficient(
                            y[a][k][i],
                            -1.0
                    );
                }
            }
        }

        // Each next block grows by 1 cell in comparison with previous
        // N_{a, k-1} - N_{ak} + M \cdot b_{ak} \geq M - 1 (constraints[0])
        // N_{a, k-1} - N_{ak} - M \cdot b_{ak} \leq -M -1 (constraints[1])
        MPConstraint[][][] blockGrowthConstraint = new MPConstraint[2][arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            // part 1
            for (int k = 1; k < kOfArrow[a]; ++k) {
                blockGrowthConstraint[0][a][k] = solver.makeConstraint(
                        "Block " + (k + 1)
                                + " must be +1 size than block " + (k - 1 + 1)
                                + " (pt.1)"
                );
                blockGrowthConstraint[0][a][k].setUb(M - 1);

                blockGrowthConstraint[0][a][k].setCoefficient(
                        N[a][k - 1],
                        1.0
                );
                blockGrowthConstraint[0][a][k].setCoefficient(
                        N[a][k],
                        -1.0
                );
                blockGrowthConstraint[0][a][k].setCoefficient(
                        b[a][k],
                        M
                );
            }

            // part 2
            for (int k = 1; k < kOfArrow[a]; ++k) {
                blockGrowthConstraint[1][a][k] = solver.makeConstraint(
                        "Block " + (k + 1)
                                + " must be +1 size than block " + (k - 1 + 1)
                                + " (pt.2)"
                );
                blockGrowthConstraint[1][a][k].setLb(-M - 1);

                blockGrowthConstraint[1][a][k].setCoefficient(
                        N[a][k - 1],
                        1.0
                );
                blockGrowthConstraint[1][a][k].setCoefficient(
                        N[a][k],
                        -1.0
                );
                blockGrowthConstraint[1][a][k].setCoefficient(
                        b[a][k],
                        -M
                );
            }
        }

        // Constraints Stage 2.4

        // Flow must be saved for non-source cells of block
        //  \sum_{j \in N(i)}{f^{ak}_{ji}} - \sum_{j \in N(i)}{f^{ak}_{ij}} - y^{ak}_i = 0
        MPConstraint[][][] consumerFlowSaveConstraint = new MPConstraint[arrowCount][kMax][cellCount];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                for (int i: evo.reachableCells.get(a)) {
                    if (evo.arrows.get(a).contains(i)) continue;

                    neighbours = evo.getNeighbours(i);
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        if (evo.area[neighbours[j]] == CellType.FILLED)
                            neighbours[j] = -1;
                    }

                    consumerFlowSaveConstraint[a][k][i] = solver.makeConstraint(
                            0.0,
                            0.0,
                            "Flow save for consumers outside the arrow " + (i + 1)
                                    + " of block " + (k + 1)
                                    + " of arrow " + (a + 1)
                    );

                    // first sum
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;
                        consumerFlowSaveConstraint[a][k][i].setCoefficient(
                                f[a][k][neighbours[j]][(j + 2) % 4],
                                1.0
                        );
                    }

                    // second sum
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        consumerFlowSaveConstraint[a][k][i].setCoefficient(
                                f[a][k][i][j],
                                -1.0
                        );

                    }

                    consumerFlowSaveConstraint[a][k][i].setCoefficient(
                            y[a][k][i],
                            -1.0
                    );
                }
            }
        }

        // Flow must be saved also for potential source: for cell on arrow
        //  \sum_{j \in N(i)}{f^{ak}_{ji}} - \sum_{j \in N(i)}{f^{ak}_{ij}} - y^{ak}_i + F^{ak}_i = 0
        MPConstraint[][][] sourcesFlowSaveConstraint = new MPConstraint[arrowCount][kMax][maxArrowSize];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                for (int i = 0; i < evo.arrows.get(a).size(); ++i) {
                    int absoluteCellNum = evo.arrows.get(a).get(i);

                    neighbours = evo.getNeighbours(absoluteCellNum);
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        if (
                                evo.area[neighbours[j]] == CellType.FILLED ||
                                        evo.isArrow(evo.area[neighbours[j]])
                        )
                            neighbours[j] = -1;
                    }

                    sourcesFlowSaveConstraint[a][k][i] = solver.makeConstraint(
                            0.0,
                            0.0,
                            "Flow save for potential mainSources"
                                    + " for cell " + (absoluteCellNum + 1)
                                    + " of block " + (k + 1)
                                    + " of arrow " + (a + 1)
                    );

                    // first sum
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;
                        if (!evo.isEmpty(evo.area[neighbours[j]])) continue;

                        sourcesFlowSaveConstraint[a][k][i].setCoefficient(
                                f[a][k][neighbours[j]][(j + 2) % 4],
                                1.0
                        );
                    }

                    // second sum
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;
                        if (!evo.isEmpty(evo.area[neighbours[j]])) continue;

                        sourcesFlowSaveConstraint[a][k][i].setCoefficient(
                                f[a][k][absoluteCellNum][j],
                                -1.0
                        );

                    }

                    sourcesFlowSaveConstraint[a][k][i].setCoefficient(
                            y[a][k][absoluteCellNum],
                            -1.0
                    );

                    sourcesFlowSaveConstraint[a][k][i].setCoefficient(
                            F[a][k][i],
                            1.0
                    );
                }
            }
        }

        // Total generated flow for each potential source cells must be equal to size of a block
        // \sum_{i \in P_a}{F^{ak}_i} - N_{ak} = 0
        MPConstraint[][] totalBlockFlowConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                totalBlockFlowConstraint[a][k] = solver.makeConstraint(
                        0.0,
                        0.0,
                        "Total flow equals the size of a block"
                                + " of block " + (k + 1)
                                + " of arrow " + (a + 1)
                );

                for (int i = 0; i < evo.arrows.get(a).size(); ++i) {
                    totalBlockFlowConstraint[a][k].setCoefficient(
                            F[a][k][i],
                            1.0
                    );
                }

                totalBlockFlowConstraint[a][k].setCoefficient(
                        N[a][k],
                        -1.0
                );
            }
        }

        // Flow can be generated only in source that belongs to block
        // Caution! There's only one part of an unequality, cause unequality F \geq 0 is already at the F init process
        // F^{ak}_{i} - M \cdot y^{ak}_i \leq 0
        MPConstraint[][][] sourceBelongConstraint = new MPConstraint[arrowCount][kMax][maxArrowSize];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                for (int i = 0; i < evo.arrows.get(a).size(); ++i) {
                    sourceBelongConstraint[a][k][i] = solver.makeConstraint(
                            "Flow mainSource must be exactly at source cell"
                                    + " for cell " + (i + 1)
                                    + " of block " + (k + 1)
                                    + " of arrow " + (a + 1)
                    );
                    sourceBelongConstraint[a][k][i].setUb(0.0);

                    sourceBelongConstraint[a][k][i].setCoefficient(
                            F[a][k][i],
                            1.0
                    );

                    sourceBelongConstraint[a][k][i].setCoefficient(
                            y[a][k][evo.arrows.get(a).get(i)],
                            -M
                    );

                }
            }
        }

        // Flow comes out from cell only if cell belongs to a block
        // f^{ak}_{ij} - M \cdot y^{ak}_i \leq 0
        MPConstraint[][][][] flowSourceConstraint = new MPConstraint[arrowCount][kMax][cellCount][4];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                for (int i: evo.reachableCells.get(a)) {
                    neighbours = evo.getNeighbours(i);
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        if (evo.area[neighbours[j]] == CellType.FILLED)
                            neighbours[j] = -1;
                    }

                    if (evo.isArrow(evo.area[i])) {
                        for (int j = 0; j < neighbours.length; ++j) {
                            if (neighbours[j] == -1) continue;

                            if (evo.isArrow(evo.area[neighbours[j]]))
                                neighbours[j] = -1;
                        }
                        // TODO(Do we need to skip iteration within this condition?)
                        if (allNeighboursAreArrows(evo, i)) continue;
                    }
                    if (Arrays.stream(neighbours).allMatch((int o)-> o == -1)) continue;

                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        flowSourceConstraint[a][k][i][j] = solver.makeConstraint(
                                "Flow comes only from cells in a block"
                                        + " for cells " + (i + 1) + ", " + (neighbours[j] + 1)
                                        + " of block "  + (k + 1)
                                        + " of arrow "  + (a + 1)
                        );
                        flowSourceConstraint[a][k][i][j].setUb(0.0);

                        flowSourceConstraint[a][k][i][j].setCoefficient(
                                f[a][k][i][j],
                                1.0
                        );
                        flowSourceConstraint[a][k][i][j].setCoefficient(
                                y[a][k][i],
                                -M
                        );
                    }
                }
            }
        }

        // Flow enters only block's cells
        // f^{ak}_{ij} - M \cdot y^{ak}_j \leq 0
        MPConstraint[][][][] flowDestinationConstraint = new MPConstraint[arrowCount][kMax][cellCount][4];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 0; k < kOfArrow[a]; ++k) {
                for (int i: evo.reachableCells.get(a)) {
                    neighbours = evo.getNeighbours(i);
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        if (
                                evo.area[neighbours[j]] == CellType.FILLED ||
                                        !evo.reachableCells.get(a).contains(neighbours[j])
                        )
                            neighbours[j] = -1;
                    }

                    if (evo.isArrow(evo.area[i])) {
                        for (int j = 0; j < neighbours.length; ++j) {
                            if (neighbours[j] == -1) continue;

                            if (evo.isArrow(evo.area[neighbours[j]]))
                                neighbours[j] = -1;
                        }
                        if (allNeighboursAreArrows(evo, i)) continue;
                    }


                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;

                        flowDestinationConstraint[a][k][i][j] = solver.makeConstraint(
                                "Flow destination is only block cells"
                                        + " for cells " + (i + 1) + ", " + (neighbours[j] + 1)
                                        + " of block "  + (k + 1)
                                        + " of arrow "  + (a + 1)
                        );
                        flowDestinationConstraint[a][k][i][j].setUb(0.0);

                        flowDestinationConstraint[a][k][i][j].setCoefficient(
                                f[a][k][i][j],
                                1.0
                        );

                        // the only difference from 2.4.4 is j in y_akj
                        flowDestinationConstraint[a][k][i][j].setCoefficient(
                                y[a][k][neighbours[j]],
                                -M
                        );
                    }
                }
            }
        }

        // Constraints stage 2.5

        // All cells from previous block must be placed at next block
        // y^{ak}_{i + j} - y^{a,k - 1} - t^{ak}_j \geq -1
        MPConstraint[][][][] completeBlockMoveConstraint = new MPConstraint[arrowCount][kMax][cellCount][cellCount * 2];

        allowedShifts = new ArrayList<Shift>(0);
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 1; k < kOfArrow[a]; ++k) {
                for (int i: evo.reachableCells.get(a)) {
                    for (int i2: evo.reachableCells.get(a)) {

                        final int shift = i2 - i;
                        if (Arrays.stream(T).noneMatch(u -> u == shift)) continue;

                        Shift sh = new Shift(a, k, shift);

                        if (
                                allowedShifts.isEmpty()
                                        || allowedShifts.stream().noneMatch(u -> sh.compareTo(u) == 0)
                        ) {
                            // define this shift because it is allowed
                            t[a][k][(cellCount - 1) + shift] =
                                    solver.makeBoolVar(
                                            "t"
                                                    + "_" + (a + 1)
                                                    + "_" + (k + 1)
                                                    + (shift < 0 ? "_m" : "_") + abs(shift)
                                    );
                            // add the index to allowed indexes. Without conditional shifts
                            allowedShifts.addLast(sh);
                        }

                        completeBlockMoveConstraint[a][k][i][(cellCount - 1) + shift] = solver.makeConstraint(
                                "We can move cell " + (i + 1) + " to cell " + (i2 + 1)
                                        + " of block " + (k + 1)
                                        + " of arrow " + (a + 1)
                        );
                        completeBlockMoveConstraint[a][k][i][(cellCount - 1) + shift].setLb(-1.0);

                        completeBlockMoveConstraint[a][k][i][(cellCount - 1) + shift].setCoefficient(
                                y[a][k][i2],
                                1.0
                        );
                        completeBlockMoveConstraint[a][k][i][(cellCount - 1) + shift].setCoefficient(
                                y[a][k - 1][i],
                                -1.0
                        );

                        completeBlockMoveConstraint[a][k][i][(cellCount - 1) + shift].setCoefficient(
                                t[a][k][(cellCount - 1) + shift],
                                -1.0
                        );
                    }
                }
            }
        }

        allowedShifts.sort((Shift::compareTo));

        // Each cell of block must be moved using selected shift
        // \sum_{j \in T^a_i}t^{ak}_j - y^{a, k-1}_i \geq 0
        MPConstraint[][][] mustMoveAllConstraint = new MPConstraint[arrowCount][kMax][cellCount];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 1; k < kOfArrow[a]; ++k) {
                for (int i: evo.reachableCells.get(a)) {
                    mustMoveAllConstraint[a][k][i] = solver.makeConstraint(
                            "Cell " + (i + 1)
                                    + " of block " + (k + 1)
                                    + " of arrow " + (a + 1)
                                    + " must be moved from previous block"
                    );
                    mustMoveAllConstraint[a][k][i].setLb(-1.0);

                    for (Shift sh: allowedShifts) {
                        if (sh.a != a || sh.k != k) continue;
                        if (!evo.reachableCells.get(a).contains(i + sh.j)) continue;

                        mustMoveAllConstraint[a][k][i].setCoefficient(
                                t[a][k][(cellCount - 1) + sh.j],
                                1.0
                        );
                    }

                    mustMoveAllConstraint[a][k][i].setCoefficient(
                            y[a][k - 1][i],
                            -1.0
                    );

                    mustMoveAllConstraint[a][k][i].setCoefficient(
                            b[a][k],
                            -1.0
                    );
                }
            }
        }


        // There's exactly one shift for each block
        // \sum_{j \in T}{t^{ak}_j} - b_{ak} = 0
        int shiftIndex = 0;
        MPConstraint[][] uniqueShiftConstraint = new MPConstraint[arrowCount][kMax];
        for (int a = 0; a < arrowCount; ++a) {
            for (int k = 1; k < kOfArrow[a]; ++k) {
                uniqueShiftConstraint[a][k] = solver.makeConstraint(
                        0.0,
                        0.0,
                        "Shift must be unique"
                                + " of block " + (k + 1)
                                + " on arrow " + (a + 1)
                );

                for (;
                     shiftIndex < allowedShifts.size()
                             && (allowedShifts.get(shiftIndex).a == a)
                             && (allowedShifts.get(shiftIndex).k == k); ++shiftIndex) {
                    uniqueShiftConstraint[a][k].setCoefficient(
                            t[a][k][(cellCount - 1) + allowedShifts.get(shiftIndex).j],
                            1.0
                    );
                }

                uniqueShiftConstraint[a][k].setCoefficient(
                        b[a][k],
                        -1.0
                );
            }
        }

        System.out.println("Number of variables = " + solver.numVariables());
        System.out.println("Number of constraints = " + solver.numConstraints());

    }

    public static boolean uniqueSolution(
            Evolomino evo,
            String sampleName
    ) {
//        prepareModel(evo, sampleName, );
        final MPSolver.ResultStatus firstStatus = solver.solve();

        if(firstStatus == MPSolver.ResultStatus.INFEASIBLE) {
            System.out.println("Error! Got infeasible model when tried to check the solution uniqueness");
            return false;
        }

        for (int i = 0; i < evo.totalCells; ++i) {
            if (x[i].solutionValue() == 1 && !Evolomino.cellWithSquare(evo.area[i])) {
                evo.area[i] = CellType.values()[evo.area[i].ordinal() + CellType.EMPTY_WITHSQUARE.ordinal()];
            }
        }

        solver.delete();
//        prepareModel(evo, sampleName, );
        MPConstraint uniqueness = solver.makeConstraint(
                "There is at least one difference from this solution"
        );
        int filledCellsCounter = 0;
        for (int i = 0; i < evo.totalCells; ++i) {
            if (x[i].solutionValue() == 1) {
                uniqueness.setCoefficient(
                        x[i],
                        -1.0
                );
                ++filledCellsCounter;
            } else {
                uniqueness.setCoefficient(
                        x[i],
                        1.0
                );
            }
        }
        uniqueness.setLb(1 - filledCellsCounter);

        final MPSolver.ResultStatus secondStatus = solver.solve();


        System.out.println("We was here");
        // so then if there's no another solutions, it will become infeasible
        if (secondStatus == MPSolver.ResultStatus.INFEASIBLE) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean solve(
            Evolomino evo,
            int solutionVariablesOutputDepth,
            String sampleName,
            Sample baseSol
    ) {
        prepareModel(evo, sampleName, baseSol);
//        exportModelToFile(solver.exportModelAsLpFormat());

        // central part: here we call .solve() method
        System.out.println("Solving with " + solver.solverVersion());
        final MPSolver.ResultStatus resultStatus = solver.solve();

        System.out.println("Status: " + resultStatus);
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("The problem does not have an optimal solution!");
//            System.out.println("Problem solved in " + solver.iterations() + " iterations");
            return false;
        }

        // SHOW X
        System.out.println("(Cells on field) x_i:");
        for (int row = 0; row < evo.height; ++row) {
            for (int col = 0; col < evo.width; ++col) {
                System.out.print(x[row * evo.width + col].solutionValue() + " ");
            }
            System.out.println();
        }
        System.out.println();

        return true;

//        try {
//            showSolutionVariables(
//                    solutionVariablesOutputDepth,
//                    x, y, F, f, t, allowedShifts,
//                    evo
//            );
//
//            exportSolutionExtra(
//                    solver,
//                    true
//            );
//        } catch (IOException e) {
//            System.out.println("IO error in an output of solution variables or extra.");
//        }

    }

    private EvolominoModel() {}

    static boolean allNeighboursAreArrows(Evolomino evo, int cellNum) {
        return Arrays.stream(evo.getNeighbours(cellNum)).allMatch((int i) -> i == -1 || evo.isArrow(evo.area[i]));
    }

    /**
     *
     * @param stage:
     *             0 if only needs x
     *             1 if need x and y (what cells belong to what blocks)
     *             2 if need x and y and F, f (how the flow works)
     *             3 if need x and y and F, f and t (how the shifts work)
     */
    static void showSolutionVariables(
            int stage,
            MPVariable[] x,
            MPVariable[][][] y,
            MPVariable[][][] F,
            MPVariable[][][][] f,
            MPVariable[][][] t,
            ArrayList<Shift> allowedShifts,
            Evolomino evo
    ) throws IOException {
        FileWriter writer;

        writer = new FileWriter(exportFilePath, true);

        writer.write("\n" + "Solution:" + "\n");

        writer.write("(Cells on field) x_i:" + "\n");
        for (int row = 0; row < evo.height; ++row) {
            for (int col = 0; col < evo.width; ++col) {
                writer.write(x[row * evo.width + col].solutionValue() + " ");
            }
            writer.write("\n");
        }
        writer.write("\n");


        if (stage == 0) {
            writer.close();
            return;
        }

        writer.write("(Belong to blocks) y_aki" + "\n");
        for (int a = 0; a < evo.arrows.size(); ++a) {
            writer.write("---- ---- Arrow " + (a + 1) + ":" + "\n");
            for (int k = 0; k < (evo.arrows.get(a).size() + 1) / 2; ++k) {
                writer.write("Block " + (k + 1) + ": ");
                for (int ri: evo.reachableCells.get(a)) {
                    writer.write(y[a][k][ri].name() + ": " + y[a][k][ri].solutionValue() + "  ");
                }
                writer.write("\n");
            }
        }
        writer.write("\n");

        if (stage == 1) {
            writer.close();
            return;
        }

        writer.write("(mainSources of flow) F_aki" + "\n");
        for (int a = 0; a < evo.arrows.size(); ++a) {
            writer.write("---- ---- Arrow " + (a + 1) + ":" + "\n");
            for (int k = 0; k < (evo.arrows.get(a).size() + 1) / 2; ++k) {
                writer.write("Block " + (k + 1) + ": ");
                for (int ar = 0; ar < evo.arrows.get(a).size(); ++ar) {
                    writer.write(F[a][k][ar].name() + ": " + F[a][k][ar].solutionValue() + "  ");
                }
                writer.write("\n");

            }
        }
        writer.write("\n");

        int[] neighbours;
        int counter = 0;
        writer.write("(flow madness) f_akij" + "\n");
        for (int a = 0; a < evo.arrows.size(); ++a) {
            writer.write("---- ---- Arrow " + (a + 1) + ":" + "\n");
            for (int k = 0; k < (evo.arrows.get(a).size() + 1) / 2; ++k) {
                writer.write("Block " + (k + 1) + ": " + "\n");
                for (int i: evo.reachableCells.get(a)) {

                    neighbours = evo.getNeighbours(i);
                    for (int j = 0; j < neighbours.length; ++j) {
                        if (neighbours[j] == -1) continue;
                        if (!evo.reachableCells.get(a).contains(neighbours[j])) continue;
                        if (evo.isArrow(evo.area[i]) && evo.isArrow(evo.area[neighbours[j]])) continue;

                        writer.write(f[a][k][i][j].name() + ": " + f[a][k][i][j].solutionValue() + "  ");

                        ++counter;
                        if (counter == 10) {
                            System.out.println();
                            counter = 0;
                        }
                    }

                }
                writer.write("\n");
            }
        }
        writer.write("\n");

        if (stage == 2) {
            writer.close();
            return;
        }

        writer.write("(shifts madness) t_akj" + "\n");
        counter = 0;
        int shiftIndex = 0;
        for (int a = 0; a < evo.arrows.size(); ++a) {
            writer.write("--- ---- Arrow " + (a + 1) + ":" + "\n");
            for (int k = 1; k < (evo.arrows.get(a).size() + 1) / 2; ++k) {
                writer.write("Block " + (k + 1) + ": " + "\n");
                // Cycle among all shifts
                for (;
                     shiftIndex < allowedShifts.size()
                             && (allowedShifts.get(shiftIndex).a == a)
                             && (allowedShifts.get(shiftIndex).k == k); ++shiftIndex) {
                    int j = allowedShifts.get(shiftIndex).j;
                    writer.write(
                            t[a][k][(evo.area.length - 1) + j].name() + ": "
                            + t[a][k][(evo.area.length - 1) + j].solutionValue() + "  ");

                    ++counter;
                    if (counter == 10) {
                        writer.write("\n");
                        counter = 0;
                    }
                }
                writer.write("\n");
                counter = 0;
            }
        }

        System.out.println("Solution variables output was successfully done.");
        writer.close();

    }

    static void exportSolutionExtra(
            MPSolver solver,
            boolean echo
    ) throws IOException {
        if (echo) {
            System.out.println("Advanced usage:");
            System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");
//            System.out.println("Problem solved in " + solver.iterations() + " iterations");
            System.out.println("Problem solved in " + solver.nodes() + " branch-and-bound nodes");
        }

        FileWriter writer;

        writer = new FileWriter(exportFilePath, true);
        writer.write("Advanced usage:" + "\n");
        writer.write("Problem solved in " + solver.wallTime() + " milliseconds" + "\n");
//        writer.write("Problem solved in " + solver.iterations() + " iterations" + "\n");
        writer.write("Problem solved in " + solver.nodes() + " branch-and-bound nodes" + "\n");
        writer.close();
    }

    static void exportModelToFile(String output) {

        FileWriter writer;

        try {
            writer = new FileWriter(exportFilePath);

            writer.write(output);
            System.out.println("exported to '" + exportFilePath + "'.");
            writer.close();
        } catch (IOException e) {
            System.out.println("Can't open a file for export. End.");
        }
    }

    static private String exportFilePath;
}