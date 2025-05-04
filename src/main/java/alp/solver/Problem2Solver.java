package alp.solver;

import java.util.ArrayList;
import java.util.List;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * CPLEX-based solver for Problem 2: Minimizing Makespan.
 */
public class Problem2Solver implements ALPSolver {
    
    // Big-M constant for logical constraints
    private static final int BIG_M = 100000;
    
    // Maximum time limit for CPLEX in seconds
    private static final int TIME_LIMIT_SECONDS = 60;
    
    // MIP gap tolerance (relative gap between best integer and best bound)
    private static final double MIP_GAP = 0.05; // 5% gap tolerance
    
    @Override
    public ALPSolution solve(ALPInstance instance) {
        try {
            System.out.println("Starting Problem 2 solver: Minimizing Makespan");
            
            // Create the CPLEX model
            IloCplex cplex = new IloCplex();
            
            // Configure CPLEX parameters
            cplex.setParam(IloCplex.Param.MIP.Display, 2);            // Level of display output
            cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT_SECONDS);  // Time limit
            cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, MIP_GAP); // MIP gap tolerance
            
            int n = instance.getNumAircraft();
            int m = instance.getNumRunways();
            
            System.out.println("Problem size: " + n + " aircraft, " + m + " runways");
            
            // Create decision variables
            IloNumVar[] landingTimes = cplex.numVarArray(n, 0, Double.MAX_VALUE);  // x_i: landing time for aircraft i
            IloNumVar[][] runwayAssignment = new IloNumVar[n][m];                  // z_ir: 1 if aircraft i is assigned to runway r, 0 otherwise
            IloNumVar[][] precedence = new IloNumVar[n][n];                        // y_ij: 1 if aircraft i lands before j, 0 otherwise
            IloNumVar makespan = cplex.numVar(0, Double.MAX_VALUE, "makespan");    // Maximum landing time of all aircraft
            
            // Initialize binary variables
            for (int i = 0; i < n; i++) {
                for (int r = 0; r < m; r++) {
                    // z_ir: Aircraft i is assigned to runway r
                    runwayAssignment[i][r] = cplex.boolVar("z_" + i + "_" + r);
                }
                
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        // y_ij: Aircraft i lands before aircraft j
                        precedence[i][j] = cplex.boolVar("y_" + i + "_" + j);
                    }
                }
            }
            
            // OBJECTIVE FUNCTION: Minimize makespan
            cplex.addMinimize(makespan);
            
            // CONSTRAINTS
            
            // 1. Time window constraints: Each aircraft must land within its time window
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                cplex.addGe(landingTimes[i], aircraft.getEarliestLandingTime()); // x_i >= E_i
                cplex.addLe(landingTimes[i], aircraft.getLatestLandingTime());   // x_i <= L_i
            }
            
            // 2. Makespan definition: makespan >= x_i for all i
            for (int i = 0; i < n; i++) {
                cplex.addLe(landingTimes[i], makespan);
            }
            
            // 3. Runway assignment: Each aircraft must be assigned to exactly one runway
            for (int i = 0; i < n; i++) {
                IloLinearNumExpr runwaySum = cplex.linearNumExpr();
                for (int r = 0; r < m; r++) {
                    runwaySum.addTerm(1, runwayAssignment[i][r]);
                }
                cplex.addEq(runwaySum, 1); // Σ_r z_ir = 1
            }
            
            // 4. Precedence constraints (each pair must have an order)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i < j) { // Only need to consider each pair once
                        // Either i lands before j or j lands before i (not both)
                        cplex.addEq(cplex.sum(precedence[i][j], precedence[j][i]), 1);
                    }
                }
            }
            
            // 5. Separation time constraints for aircraft on the same runway
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        for (int r = 0; r < m; r++) {
                            int sepTime = instance.getSeparationTime(i, j);
                            // Build RHS: x_i + sepTime - M*(1 - y_ij) - M*(1 - z_ir) - M*(1 - z_jr)
                            IloNumExpr base = cplex.sum(landingTimes[i], sepTime);
                            IloNumExpr relax1 = cplex.prod(-BIG_M, cplex.diff(1, precedence[i][j]));
                            IloNumExpr relax2 = cplex.prod(-BIG_M, cplex.diff(1, runwayAssignment[i][r]));
                            IloNumExpr relax3 = cplex.prod(-BIG_M, cplex.diff(1, runwayAssignment[j][r]));
                            IloNumExpr rhs = cplex.sum(base, cplex.sum(relax1, cplex.sum(relax2, relax3)));
                            cplex.addGe(landingTimes[j], rhs);
                        }
                    }
                }
            }
            
            // Solve the model
            System.out.println("Starting CPLEX solver...");
            long startTime = System.currentTimeMillis();
            boolean solved = cplex.solve();
            long endTime = System.currentTimeMillis();
            double solveTime = (endTime - startTime) / 1000.0;
            
            if (solved) {
                System.out.println("CPLEX found a solution in " + solveTime + " seconds");
                
                // Extract solution
                int[] finalLandingTimes = new int[n];
                int[] finalRunwayAssignments = new int[n];
                
                for (int i = 0; i < n; i++) {
                    finalLandingTimes[i] = (int) Math.round(cplex.getValue(landingTimes[i]));
                    
                    // Find the assigned runway
                    for (int r = 0; r < m; r++) {
                        if (Math.round(cplex.getValue(runwayAssignment[i][r])) == 1) {
                            finalRunwayAssignments[i] = r;
                            break;
                        }
                    }
                }
                
                double objectiveValue = cplex.getObjValue();
                
                // Validate solution
                if (validateSolution(instance, finalLandingTimes, finalRunwayAssignments)) {
                    System.out.println("Solution validation passed");
                } else {
                    System.out.println("⚠️ Solution validation failed");
                    cplex.end();
                    throw new RuntimeException("CPLEX solution validation failed");
                }
                
                cplex.end();
                
                return new ALPSolution(instance, finalLandingTimes, finalRunwayAssignments, 
                                      objectiveValue, solveTime, "Problem 2: Minimizing Makespan");
                
            } else {
                System.out.println("❌ CPLEX could not find a solution");
                cplex.end();
                throw new RuntimeException("CPLEX could not find a solution");
            }
            
        } catch (IloException e) {
            System.err.println("❌ CPLEX Error: " + e.getMessage());
            throw new RuntimeException("CPLEX error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates that a solution respects all separation time constraints.
     * 
     * @param instance The ALP instance
     * @param landingTimes Landing times for each aircraft
     * @param runwayAssignments Runway assignments for each aircraft
     * @return true if the solution is valid, false otherwise
     */
    private boolean validateSolution(ALPInstance instance, int[] landingTimes, int[] runwayAssignments) {
        int n = instance.getNumAircraft();
        
        // Check time window constraints
        for (int i = 0; i < n; i++) {
            AircraftData aircraft = instance.getAircraft().get(i);
            if (landingTimes[i] < aircraft.getEarliestLandingTime() || 
                landingTimes[i] > aircraft.getLatestLandingTime()) {
                System.err.println("Time window violation for aircraft " + i + ": " + 
                                  landingTimes[i] + " not in [" + 
                                  aircraft.getEarliestLandingTime() + ", " + 
                                  aircraft.getLatestLandingTime() + "]");
                return false;
            }
        }
        
        // Check separation constraints
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && runwayAssignments[i] == runwayAssignments[j]) {
                    // Aircraft on the same runway need separation
                    if (landingTimes[i] < landingTimes[j]) {
                        int sepTime = instance.getSeparationTime(i, j);
                        if (landingTimes[j] < landingTimes[i] + sepTime) {
                            System.err.println("Separation time violation between aircraft " + 
                                              i + " and " + j + " on runway " + runwayAssignments[i] + 
                                              ": " + landingTimes[j] + " < " + landingTimes[i] + 
                                              " + " + sepTime);
                            return false;
                        }
                    } else if (landingTimes[j] < landingTimes[i]) {
                        int sepTime = instance.getSeparationTime(j, i);
                        if (landingTimes[i] < landingTimes[j] + sepTime) {
                            System.err.println("Separation time violation between aircraft " + 
                                              j + " and " + i + " on runway " + runwayAssignments[i] + 
                                              ": " + landingTimes[i] + " < " + landingTimes[j] + 
                                              " + " + sepTime);
                            return false;
                        }
                    } else {
                        // Same landing time is always a violation
                        System.err.println("Aircraft " + i + " and " + j + 
                                          " have same landing time " + landingTimes[i] + 
                                          " on runway " + runwayAssignments[i]);
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    @Override
    public String getName() {
        return "Problem 2: Minimizing Makespan";
    }
}
