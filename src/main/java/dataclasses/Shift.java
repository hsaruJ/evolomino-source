package dataclasses;

public class Shift implements Comparable<Shift> {
    public final int a;
    public final int k;
    public final int j;

    // Shifts like this are forbidden
    private Shift() {
        a = -1;
        k = -1;
        j = 0;
    }

    public Shift(
            int o_a,
            int o_k,
            int o_j
    ) {
        a = o_a;
        k = o_k;
        j = o_j;
    }

    @Override
    public int compareTo(Shift other) {
        if (a < other.a) return -1;
        else if (a > other.a) return 1;

        if (k < other.k) return -1;
        else if (k > other.k) return 1;

        if (j < other.j) return -1;
        else if (j > other.j) return 1;

        return 0;
    }
}
