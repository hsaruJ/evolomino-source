package evolomino;

public class WeighedCell {
    public double weight;
    public int cellNum;

    public WeighedCell(double w, int c) {
        weight = w;
        cellNum = c;
    }

    public double getWeight() {
        return weight;
    }
}
