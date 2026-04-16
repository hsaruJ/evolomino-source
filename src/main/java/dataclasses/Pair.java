package dataclasses;

public class Pair extends Object{
    public int first;
    public int second;

    public Pair() {
        first = second = 0;
    }

    public Pair(int f, int s) {
        first = f;
        second = s;
    }

    @Override
    public boolean equals(Object obj) {
        Pair other = (Pair)(obj);
        return (this.first == other.first) && (this.second == other.second);
    }

    // oriented comparator! first is more important than second.
    public boolean less(Pair other) {
        if(equals(other)) return false;

             if (first < other.first) return true ;
        else if (first > other.first) return false;

        return second < other.second;
    }

    public Pair minus(Pair other) {
        return new Pair(this.first - other.first, this.second - other.second);
    }

    public Pair plus(Pair other) {
        return new Pair(this.first + other.first, this.second + other.second);
    }

    public boolean outOfBounds(int height, int width) {
        return first < 0 || first >= height
            || second < 0 || second >= width;

    }
}
