package com.example;

import evolomino.Evolomino;
import evolomino.Sample;
import painter.EvolominoPainter;

import java.io.File;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        int height = 16, width = 16, amount = 10;
        boolean override = false;

        long mStart = System.currentTimeMillis();
        generateNSamples(height, width, amount, override);
        long mFinish = System.currentTimeMillis();
        System.out.println("total time for " + amount + " " + height + "x" + width +" samples (ms): " + (mFinish - mStart));

        // 14x14 50 samples
        // 0 32 9 13 14 14 17 11 18 10 11 14 5 7 7 22 14 24 15 25 9 12 10 16 10 13 10 5 8 10 34 18 19 14 12 10 14 13 8 13 10 9 11 15 14 116 17 17 54 19
        // total time for 50 14x14 samples (ms): 846581

        // 14x14 50 samples unique
        // 211 255 306 170 462 448 250 117 306 336 491 300 726 315 240 295 229 346 267 730 241 381 104 81 229 176 91 120 145 204 158 77 118 196 248 216 63 219 146 165 204 91 35 167 100 93 238 194 215 153
        // total time for 50 14x14 samples (ms): 11692074

        // 15x15 50 samples
        // 0 0 0 0 0 0 0 0 0 0 0 21 19 34 97 30 111 51 20 61 42 67 59 28 22 71 24 56 26 99 32 38 22 48 14 75 52 62 70 69 65 333 45 67 23 23 31 16 38 45
        // total time for 50 15x15 samples (ms): 2124416


    }

    static void checkExactSample() {
        Sample s = new Sample("generatedSamples/5x5/sample1/raw.txt");
        s.name = "id" + -1956855797;

        Evolomino evo = new Evolomino(s);
        showArrowsNReachable(evo);
        EvolominoModel.solve(evo, 0, s.name, s);

        // tested sample id's: -1956855797
    }

    static void generateNSamples(int height, int width, int n, boolean override) {
        File sizesDir = new File(String.format("generatedSamples/%dx%d", height, width));
        if (!sizesDir.exists()) {
            sizesDir.mkdir();
        }
        // puzzle, raw, solution.

        long[] seconds = new long[n];


        for (int i = 0; i < n; ++i) {
            File sampleDir = new File(sizesDir.getPath() + String.format("/sample%d", i + 1));
            if (!sampleDir.exists()) {
                sampleDir.mkdir();
            } else if (!override) {
                continue;
            }

            seconds[i] = System.currentTimeMillis();

            String puzzleFileName = sampleDir.getPath() + "/puzzle.png";
            String rawFileName = sampleDir.getPath() + "/raw.txt";
            String solutionFileName = sampleDir.getPath() + "/solution.png";
            Sample s = EvolominoGenerator.GenerateEvolomino(height, width);
            s.name = "sample" + (i + 1);
            EvolominoPainter.paint(s, puzzleFileName);
            s.saveToFile(rawFileName);

            Evolomino evo = new Evolomino(s);

            EvolominoModel.solve(evo, 0, s.name, s);
            for (int j = 0; j < s.totalCells; ++j) {
                if (EvolominoModel.x[j].solutionValue() == 1 && s.field[j] < 16)
                    s.field[j] += 16;
            }

            EvolominoPainter.paint(s, solutionFileName);

            seconds[i] = System.currentTimeMillis() - seconds[i];
            seconds[i] /= 1000;
        }

        for (int i = 0; i < n; ++i) {
            System.out.print(seconds[i] + " ");
        }
        System.out.println();
    }


    static void showArrowsNReachable(Evolomino evo) {
        // OHH, it's so trilling to launch it for the first time!
        // 23:30, 05/01/2026
        // welcome to the debug epoch!

        System.out.println("Arrows");
        for (int a = 0; a < evo.arrows.size(); ++a) {
            System.out.print("Arrow " + (a + 1) + ": ");
            for (int i: evo.arrows.get(a)) {
                System.out.print((i + 1) + ", ");
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Reachable cells:");
        for (int a = 0; a < evo.arrows.size(); ++a) {
            System.out.print("Arrow " + (a + 1) + ": ");
            for (int i: evo.reachableCells.get(a)) {
                System.out.print((i + 1) + " ");
            }
            System.out.println();
        }
    }


    static void runBaseSample(String sampleName) {
        Sample testSample = new Sample(String.format("samples/input/%s.txt", sampleName));

        Evolomino evo = new Evolomino(testSample);
        showArrowsNReachable(evo);

        EvolominoModel.solve(evo, 0, testSample.name, testSample);
    }

    static void updateAllExports() {
        // TODO(Make a param with a list of samples to update)
        for (int i = 0; i <= 10; ++i) {
            String sampleName = "sample" + i;
            Sample testSample = new Sample(String.format("samples/input/%s.txt", sampleName));

            Evolomino evo = new Evolomino(testSample);

            EvolominoModel.solve(evo, 0, testSample.name, testSample);
        }
    }

    static void getPercentOfArrowInPuzzle() {
        double total = 0;
        for (int i = 0; i <= 10; ++i) {
            String sampleName = "sample" + i;
            Sample testSample = new Sample(String.format("samples/input/%s.txt", sampleName));

            Evolomino evo = new Evolomino(testSample);

            int totalArrowsLength = 0;
            for (ArrayList<Integer> arr: evo.arrows) {
                totalArrowsLength += arr.size();
            }

            System.out.println(totalArrowsLength / (double) evo.totalCells);
            total += totalArrowsLength / (double) evo.totalCells;
        }
        System.out.println("total = " + total / 11);
    }
}
