package alp.solver;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

/**
 * CPLEX-based solver for Problem 3: Minimizing Total Lateness with Runway Assignment.
 */
public class Problem3Solver implements ALPSolver {
    
    @Override
    public ALPSolution solve(ALPInstance instance) {
        try {
            // Create the model
            IloCplex cplex = new IloCplex();
            
            int n = instance.getNumAircraft();
            int m = instance.getNumRunways();
            
            // Decision variables
            IloNumVar[] x = cplex.numVarArray(n, 0, Integer.MAX_VALUE); // Landing time for each aircraft
            IloNumVar[][] y = new IloNumVar[n][n]; // Binary variables for precedence
            IloNumVar[][] z = new IloNumVar[n][m]; // Binary variables for runway assignment
            IloNumVar[] lateness = cplex.numVarArray(n, 0, Integer.MAX_VALUE); // Lateness for each aircraft
            
            // Initialize y and z variables
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        y[i][j] = cplex.boolVar("y_" + i + "_" + j);
                    }
                }
                
                for (int r = 0; r < m; r++) {
                    z[i][r] = cplex.boolVar("z_" + i + "_" + r);
                }
            }
            
            // Objective function: minimize total lateness
            IloLinearNumExpr objective = cplex.linearNumExpr();
            for (int i = 0; i < n; i++) {
                objective.addTerm(1, lateness[i]);
            }
            cplex.addMinimize(objective);
            
            // Constraints
            
            // Time window constraints
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                cplex.addGe(x[i], aircraft.getEarliestLandingTime());
                cplex.addLe(x[i], aircraft.getLatestLandingTime());
            }
            
            // Lateness definition
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                int arrivalTime = aircraft.getTargetLandingTime(); // Ai = Ti as per requirements
                
                // For each runway
                for (int r = 0; r < m; r++) {
                    // If aircraft i is assigned to runway r
                    // lateness_i >= x_i + t_ir - A_i - M(1 - z_ir)
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(1, x[i]);
                    
                    // Au lieu de: expr.addTerm(aircraft.getTransferTime(r), 1);
                    IloNumVar transferTime = cplex.numVar(aircraft.getTransferTime(r), 
                                                     aircraft.getTransferTime(r), 
                                                     IloNumVarType.Int, 
                                                     "transfer_" + i + "_" + r);
                    expr.addTerm(1, transferTime);
                    
                    // Au lieu de: expr.addTerm(-arrivalTime, 1);
                    IloNumVar arrivalTimeVar = cplex.numVar(arrivalTime, arrivalTime, 
                                                          IloNumVarType.Int, 
                                                          "arrival_" + i);
                    expr.addTerm(-1, arrivalTimeVar);
                    
                    // Définir bigM localement dans cette portée
                    int latenessBigM = 100000;
                    // Au lieu de: expr.addTerm(-bigM, cplex.diff(1, z[i][r]));
                    expr.addTerm(latenessBigM, z[i][r]);
                    IloNumVar bigMVar = cplex.numVar(latenessBigM, latenessBigM, 
                                                   IloNumVarType.Int, 
                                                   "bigM_lateness_" + i + "_" + r);
                    expr.addTerm(-1, bigMVar);
                    
                    cplex.addGe(lateness[i], expr);
                }
                
                // Lateness cannot be negative
                cplex.addGe(lateness[i], 0);
            }
            
            // Each aircraft must be assigned to exactly one runway
            for (int i = 0; i < n; i++) {
                IloLinearNumExpr runwaySum = cplex.linearNumExpr();
                for (int r = 0; r < m; r++) {
                    runwaySum.addTerm(1, z[i][r]);
                }
                cplex.addEq(runwaySum, 1);
            }
            
            // Separation time constraints
            int bigM = 100000; // A large constant
            
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        // For aircraft on the same runway
                        for (int r = 0; r < m; r++) {
                            // If i and j are assigned to the same runway r
                            // and i lands before j, ensure separation constraint is met
                            
                            // x_j >= x_i + s_ij - M(1 - y_ij) - M(1 - z_ir) - M(1 - z_jr)
                            IloLinearNumExpr expr = cplex.linearNumExpr();
                            expr.addTerm(1, x[i]);
                            
                            // Remplacer addConstant par une variable de constante
                            IloNumVar sepTimeVar = cplex.numVar(instance.getSeparationTime(i, j), 
                                                              instance.getSeparationTime(i, j), 
                                                              IloNumVarType.Int, 
                                                              "sep_" + i + "_" + j + "_" + r);
                            expr.addTerm(1, sepTimeVar);
                            
                            // Remplacer la reformulation qui utilisait addConstant
                            expr.addTerm(bigM, y[i][j]);
                            // Utiliser une variable au lieu de addConstant(-bigM)
                            IloNumVar bigMVar1 = cplex.numVar(bigM, bigM, IloNumVarType.Int, "bigM_" + i + "_" + j + "_1_" + r);
                            expr.addTerm(-1, bigMVar1);
                            
                            expr.addTerm(bigM, z[i][r]);
                            // Utiliser une variable au lieu de addConstant(-bigM)
                            IloNumVar bigMVar2 = cplex.numVar(bigM, bigM, IloNumVarType.Int, "bigM_" + i + "_" + j + "_2_" + r);
                            expr.addTerm(-1, bigMVar2);
                            
                            expr.addTerm(bigM, z[j][r]);
                            // Utiliser une variable au lieu de addConstant(-bigM)
                            IloNumVar bigMVar3 = cplex.numVar(bigM, bigM, IloNumVarType.Int, "bigM_" + i + "_" + j + "_3_" + r);
                            expr.addTerm(-1, bigMVar3);
                            
                            cplex.addGe(x[j], expr);
                        }
                        
                        // Either i lands before j or j lands before i
                        cplex.addLe(cplex.sum(y[i][j], y[j][i]), 1);
                    }
                }
            }
            
            // Solve the model
            long startTime = System.currentTimeMillis();
            boolean solved = cplex.solve();
            long endTime = System.currentTimeMillis();
            double solveTime = (endTime - startTime) / 1000.0;
            
            if (solved) {
                // Extract solution
                int[] landingTimes = new int[n];
                int[] runwayAssignments = new int[n];
                
                for (int i = 0; i < n; i++) {
                    landingTimes[i] = (int) Math.round(cplex.getValue(x[i]));
                    
                    // Find the assigned runway
                    for (int r = 0; r < m; r++) {
                        if (Math.round(cplex.getValue(z[i][r])) == 1) {
                            runwayAssignments[i] = r;
                            break;
                        }
                    }
                }
                
                double objectiveValue = cplex.getObjValue();
                
                cplex.end();
                
                return new ALPSolution(instance, landingTimes, runwayAssignments, 
                                     objectiveValue, solveTime, "Problem 3: Minimizing Total Lateness");
            } else {
                cplex.end();
                throw new RuntimeException("No solution found for instance " + instance.getInstanceName());
            }
            
        } catch (IloException e) {
            throw new RuntimeException("CPLEX error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getName() {
        return "Problem 3: Minimizing Total Lateness";
    }
}
