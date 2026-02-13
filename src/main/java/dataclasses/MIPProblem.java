package dataclasses;

import java.util.ArrayList;

public class MIPProblem {
    public int variableCount;
    public int constraintCount;
    public int[][] constraintMatrix;
    public int[] objective;

    // suppose: all variables are \geq 0
    public MIPProblem(
            int n,
            int m,
            int[][] matrix,
            int[] obj
    ) {
        variableCount = n;
        constraintCount = m;

        constraintMatrix = matrix;
        objective = obj;
    }
}
