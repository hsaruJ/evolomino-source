package com.example;

import com.google.ortools.Loader;
import com.google.ortools.init.OrToolsVersion;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import dataclasses.MIPProblem;

public final class ORMIPCall {
    public static void runOrTools(MIPProblem p) {
        Loader.loadNativeLibraries();

        // Show the version of the solver
        System.out.println("Google OR-Tools version: " + OrToolsVersion.getVersionString());

        // Create the linear solver with the GLOP backend.
        MPSolver solver = MPSolver.createSolver("SCIP");
        if (solver == null) {
            System.out.println("Could not create solver SCIP");
            return;
        }

        final double infinity = Double.POSITIVE_INFINITY;

        // Create the variables
        MPVariable[] variables = new MPVariable[p.variableCount];
        for (int i = 0; i < variables.length; ++i) {
            variables[i] = solver.makeIntVar(0.0, infinity, "x" + (i + 1));
        }

        System.out.println("Number of variables = " + solver.numVariables());

        // Create a linear constraints
        MPConstraint[] constraints = new MPConstraint[p.constraintCount];
        for (int i = 0; i < constraints.length; ++i) {
            constraints[i] = solver.makeConstraint(
                    p.constraintMatrix[i][p.variableCount],
                    p.constraintMatrix[i][p.variableCount],
                    "ct" + (i + 1)
            );
            for (int j = 0; j < p.variableCount; ++j) {
                constraints[i].setCoefficient(
                        variables[j],
                        p.constraintMatrix[i][j]
                );
            }
        }

        System.out.println("Number of constraints = " + solver.numConstraints());

        // Create the objective function
        MPObjective objective = solver.objective();
        for (int i = 0; i < p.variableCount; ++i) {
            objective.setCoefficient(
                    variables[i],
                    p.objective[i]
            );
        }
        objective.setOffset(p.objective[p.variableCount]);

        objective.setMaximization();

        System.out.println("Solving with " + solver.solverVersion());
        final MPSolver.ResultStatus resultStatus = solver.solve();

        System.out.println("Status: " + resultStatus);
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("The problem does not have an optimal solution!");
            return;
        }

        System.out.println("Solution:");
        System.out.println("Objective value = " + objective.value());
        for (int i = 0; i < p.variableCount; ++i) {
            System.out.println("x" + (i + 1) + " = " + variables[i].solutionValue());
        }

        System.out.println("Advanced usage:");
        System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");
        System.out.println("Problem solved in " + solver.iterations() + " iterations");
        System.out.println("Problem solved in " + solver.nodes() + " branch-and-bound nodes");
    }

    private ORMIPCall() {}
}