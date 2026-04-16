package dataclasses;

public class Shift implements Comparable<Shift> {
    public final int a; // arrow index
    public final int k; // block index
    public final Pair sh;

    // Shifts like this are forbidden
    private Shift() {
        a = -1;
        k = -1;
        sh = new Pair();
    }

    public Shift(
            int o_a,
            int o_k,
            Pair o_sh
    ) {
        a = o_a;
        k = o_k;
        sh = o_sh;
    }

    @Override
    public int compareTo(Shift other) {
        if (a < other.a) return -1;
        else if (a > other.a) return 1;

        if (k < other.k) return -1;
        else if (k > other.k) return 1;

        if (sh.less(other.sh)) return -1;
        else if (!sh.equals(other.sh)) return 1;

        return 0;
    }
}
