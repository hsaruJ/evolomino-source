package com.example;

import evolomino.Evolomino;
import evolomino.Sample;
import painter.EvolominoPainter;

import java.io.File;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        generateNSamples(10, 10, 50, false);
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

        for (int i = 0; i < n; ++i) {
            File sampleDir = new File(sizesDir.getPath() + String.format("/sample%d", i + 1));
            if (!sampleDir.exists()) {
                sampleDir.mkdir();
            } else if (!override) {
                continue;
            }

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
        }
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
