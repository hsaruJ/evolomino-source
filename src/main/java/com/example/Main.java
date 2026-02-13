package com.example;

import evolomino.Evolomino;
import evolomino.Sample;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Sample s = EvolominoGenerator.GenerateEvolomino(10, 10);

        System.out.println("\n\n---- ---- ---- ---- Generated result: ---- ---- ---- ----");
        s.showField();
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

        EvolominoSolverCall.runOrTools(evo, 0, testSample.name);
    }

    static void updateAllExports() {
        // TODO(Make a param with a list of samples to update)
        for (int i = 0; i <= 10; ++i) {
            String sampleName = "sample" + i;
            Sample testSample = new Sample(String.format("samples/input/%s.txt", sampleName));

            Evolomino evo = new Evolomino(testSample);

            EvolominoSolverCall.runOrTools(evo, 0, testSample.name);
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
