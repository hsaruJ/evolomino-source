package evolomino;

import evolomino.enums.CellType;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class Sample implements Cloneable {
    public int height;
    public int width;
    public int totalCells;
    public int[] field;
    public String name;

    public Sample() {
        height = 0;
        width = 0;
    };

    public Sample(String fileName) {
        Scanner in;

        try {
            in = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Error in reading sample from the file. Stopping");
            return;
        }

        height = in.nextInt();
        width = in.nextInt();
        totalCells = height * width;
        this.field = new int[totalCells];

        for (int i = 0; i < totalCells; ++i) {
            field[i] = in.nextInt();
        }

        int i = fileName.lastIndexOf("/") + 1;
        int j = fileName.indexOf(".");
        name = fileName.substring(i, j);
    }

    public Sample(int height, int width) {
        this.height = height;
        this.width = width;
        totalCells = height * width;

        this.field = new int[totalCells];
        Arrays.fill(this.field, 0);
    }

    public int totalArrowLength() {
        int totalLength = 0;
        for (int x: field) {
            totalLength += (Evolomino.isArrow(CellType.values()[x]) ? 1 : 0);
        }

        return totalLength;
    }

    public int fieldFillPercent() {
        int totalFilled = 0;
        for (int x: field) {
            totalFilled += (Evolomino.cellWithSquare(CellType.values()[x]) ? 1 : 0);
        }

        return totalFilled;
    }

    public void showField() {
        String sp = "";
        System.out.println("Sample " + name + "(" + height + "x" + width + ")");
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                sp = (field[row * width + col] < 10 ? " " : "");
                System.out.print(sp + field[row * width + col] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    @Override
    public Sample clone() {
        try {
            Sample clone = (Sample) super.clone();
            clone.height = this.height;
            clone.width = this.width;
            clone.totalCells = this.totalCells;
            clone.field = this.field.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
