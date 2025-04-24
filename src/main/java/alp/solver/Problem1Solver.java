package alp.solver;

import java.util.ArrayList;
import java.util.List;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

/**
 * CPLEX-based solver for Problem 1: Minimizing Weighted Delay with Target Landing Times.
 */
public class Problem1Solver implements ALPSolver {
    
    @Override
    public ALPSolution solve(ALPInstance instance) {
        try {
            // Create the model
            IloCplex cplex = new IloCplex();
            
            // Activer les diagnostics avancés et la résolution avec des paramètres plus souples
            cplex.setParam(IloCplex.Param.MIP.Display, 4);
            cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.1); // 10% d'écart acceptable
            cplex.setParam(IloCplex.Param.TimeLimit, 60); // limite de 60 secondes
            
            int n = instance.getNumAircraft();
            int m = instance.getNumRunways();
            
            // Si le problème semble trop complexe, utiliser une heuristique
            if (n * n * m > 1000) {
                System.out.println("Instance potentiellement trop complexe, utilisation de l'heuristique...");
                return solveWithHeuristic(instance);
            }
            
            // Decision variables
            IloNumVar[] x = cplex.numVarArray(n, 0, Integer.MAX_VALUE); // Landing time for each aircraft
            IloNumVar[][] y = new IloNumVar[n][n]; // Binary variables for precedence
            IloNumVar[][] z = new IloNumVar[n][m]; // Binary variables for runway assignment
            IloNumVar[] alpha = cplex.numVarArray(n, 0, Integer.MAX_VALUE); // Early landing penalty
            IloNumVar[] beta = cplex.numVarArray(n, 0, Integer.MAX_VALUE); // Late landing penalty
            
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
            
            // Objective function: minimize weighted sum of penalties
            IloLinearNumExpr objective = cplex.linearNumExpr();
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                objective.addTerm(aircraft.getEarlyPenalty(), alpha[i]);
                objective.addTerm(aircraft.getLatePenalty(), beta[i]);
            }
            cplex.addMinimize(objective);
            
            // Constraints
            
            // Time window constraints
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                cplex.addGe(x[i], aircraft.getEarliestLandingTime());
                cplex.addLe(x[i], aircraft.getLatestLandingTime());
            }
            
            // Early and late penalty constraints
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                // alpha_i >= T_i - x_i
                cplex.addGe(alpha[i], cplex.diff(aircraft.getTargetLandingTime(), x[i]));
                // beta_i >= x_i - T_i
                cplex.addGe(beta[i], cplex.diff(x[i], aircraft.getTargetLandingTime()));
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
                            
                            // Remplacer addConstant par une autre approche
                            // expr.addConstant(instance.getSeparationTime(i, j));
                            // Créons plutôt une contrainte séparée impliquant cette constante
                            
                            IloNumVar dummy = cplex.numVar(instance.getSeparationTime(i, j), 
                                                          instance.getSeparationTime(i, j), 
                                                          IloNumVarType.Int, 
                                                          "dummy_" + i + "_" + j + "_" + r);
                            expr.addTerm(1, dummy);
                            
                            // Remplacer la reformulation qui utilisait addConstant
                            expr.addTerm(bigM, y[i][j]);
                            // expr.addConstant(-bigM);
                            // Soustrayons bigM directement dans la contrainte
                            IloNumVar bigMVar = cplex.numVar(bigM, bigM, IloNumVarType.Int, "bigM_" + i + "_" + j + "_1_" + r);
                            expr.addTerm(-1, bigMVar);
                            
                            expr.addTerm(bigM, z[i][r]);
                            // expr.addConstant(-bigM);
                            IloNumVar bigMVar2 = cplex.numVar(bigM, bigM, IloNumVarType.Int, "bigM_" + i + "_" + j + "_2_" + r);
                            expr.addTerm(-1, bigMVar2);
                            
                            expr.addTerm(bigM, z[j][r]);
                            // expr.addConstant(-bigM);
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
                                      objectiveValue, solveTime, "Problem 1: Minimizing Weighted Delay");
            } else {
                System.out.println("CPLEX n'a pas trouvé de solution, utilisation de l'heuristique...");
                cplex.end();
                return solveWithHeuristic(instance);
            }
            
        } catch (IloException e) {
            System.err.println("Erreur CPLEX : " + e.getMessage());
            System.out.println("Tentative de résolution avec l'heuristique suite à l'erreur...");
            return solveWithHeuristic(instance);
        }
    }
    
    /**
     * Résout le problème avec une heuristique simple.
     */
    private ALPSolution solveWithHeuristic(ALPInstance instance) {
        long startTime = System.currentTimeMillis();
        int n = instance.getNumAircraft();
        int m = instance.getNumRunways();
        
        // Liste des avions triée par temps cible d'atterrissage
        List<AircraftData> sortedAircraft = new ArrayList<>(instance.getAircraft());
        sortedAircraft.sort((a1, a2) -> Integer.compare(a1.getTargetLandingTime(), a2.getTargetLandingTime()));
        
        // Structure pour stocker le temps d'atterrissage de chaque avion
        int[] landingTimes = new int[n];
        int[] runwayAssignments = new int[n];
        
        // Temps d'occupation de chaque piste
        int[] runwayTimes = new int[m];
        
        // Assigner chaque avion
        double totalPenalty = 0;
        
        for (int i = 0; i < n; i++) {
            AircraftData aircraft = sortedAircraft.get(i);
            int aircraftIndex = instance.getAircraft().indexOf(aircraft);
            
            // Trouver la piste la moins occupée
            int bestRunway = 0;
            for (int r = 1; r < m; r++) {
                if (runwayTimes[r] < runwayTimes[bestRunway]) {
                    bestRunway = r;
                }
            }
            
            // Calculer le temps d'atterrissage possible
            int landingTime = Math.max(aircraft.getEarliestLandingTime(), runwayTimes[bestRunway]);
            
            // Vérifier les contraintes de séparation avec les avions déjà assignés
            for (int j = 0; j < i; j++) {
                int otherAircraftIndex = instance.getAircraft().indexOf(sortedAircraft.get(j));
                int otherRunway = runwayAssignments[otherAircraftIndex];
                int otherLandingTime = landingTimes[otherAircraftIndex];
                
                // Seulement considérer si même piste
                if (bestRunway == otherRunway) {
                    int separationTime = instance.getSeparationTime(otherAircraftIndex, aircraftIndex);
                    int minTime = otherLandingTime + separationTime;
                    landingTime = Math.max(landingTime, minTime);
                }
            }
            
            // S'assurer que le temps d'atterrissage est dans les limites
            landingTime = Math.min(landingTime, aircraft.getLatestLandingTime());
            
            // Stocker le résultat
            landingTimes[aircraftIndex] = landingTime;
            runwayAssignments[aircraftIndex] = bestRunway;
            
            // Mettre à jour le temps d'occupation de la piste
            runwayTimes[bestRunway] = landingTime;
            
            // Calculer la pénalité
            int targetTime = aircraft.getTargetLandingTime();
            if (landingTime < targetTime) {
                totalPenalty += aircraft.getEarlyPenalty() * (targetTime - landingTime);
            } else if (landingTime > targetTime) {
                totalPenalty += aircraft.getLatePenalty() * (landingTime - targetTime);
            }
        }
        
        long endTime = System.currentTimeMillis();
        double solveTime = (endTime - startTime) / 1000.0;
        
        return new ALPSolution(instance, landingTimes, runwayAssignments, 
                             totalPenalty, solveTime, "Problem 1: Minimizing Weighted Delay (Heuristic)");
    }
    
    @Override
    public String getName() {
        return "Problem 1: Minimizing Weighted Delay";
    }
}
