package alp.analysis;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;

/**
 * Utility class for analyzing solutions to the ALP.
 */
public class SolutionAnalyzer {
    
    /**
     * Analyzes and prints statistics for a given solution.
     */
    public static void analyzeSolution(ALPSolution solution, PrintWriter writer) {
        ALPInstance instance = solution.getInstance();
        int n = instance.getNumAircraft();
        int m = instance.getNumRunways();
        
        writer.println("=== Solution Analysis ===");
        writer.println("Problem: " + solution.getProblemVariant());
        writer.println("Instance: " + instance.getInstanceName());
        writer.println("Number of aircraft: " + n);
        writer.println("Number of runways: " + m);
        writer.println("Objective value: " + solution.getObjectiveValue());
        writer.println("Solve time: " + solution.getSolveTime() + " seconds");
        
        // Analyze runway utilization
        int[] runwayCount = new int[m];
        Map<Integer, Integer> runwayMakespan = new HashMap<>();
        
        for (int i = 0; i < n; i++) {
            int runway = solution.getRunwayAssignment(i);
            runwayCount[runway]++;
            
            int landingTime = solution.getLandingTime(i);
            int currentMakespan = runwayMakespan.getOrDefault(runway, 0);
            runwayMakespan.put(runway, Math.max(currentMakespan, landingTime));
        }
        
        writer.println("\nRunway utilization:");
        for (int r = 0; r < m; r++) {
            writer.println("Runway " + (r + 1) + ": " + runwayCount[r] + " aircraft (" + 
                         (100.0 * runwayCount[r] / n) + "%)");
        }
        
        // Analyze makespan
        int overallMakespan = 0;
        for (int makespan : runwayMakespan.values()) {
            overallMakespan = Math.max(overallMakespan, makespan);
        }
        
        writer.println("\nMakespan (completion time of last aircraft): " + overallMakespan);
        
        // Analyze deviation from target times
        int earlyCount = 0;
        int lateCount = 0;
        int onTimeCount = 0;
        double totalEarlyPenalty = 0;
        double totalLatePenalty = 0;
        
        writer.println("\nDeviations from target times:");
        
        for (int i = 0; i < n; i++) {
            AircraftData aircraft = instance.getAircraft().get(i);
            int landingTime = solution.getLandingTime(i);
            int targetTime = aircraft.getTargetLandingTime();
            
            if (landingTime < targetTime) {
                earlyCount++;
                double earlyPenalty = aircraft.getEarlyPenalty() * (targetTime - landingTime);
                totalEarlyPenalty += earlyPenalty;
                writer.println("Aircraft " + (i + 1) + ": Early by " + (targetTime - landingTime) + 
                             " units (penalty: " + earlyPenalty + ")");
            } else if (landingTime > targetTime) {
                lateCount++;
                double latePenalty = aircraft.getLatePenalty() * (landingTime - targetTime);
                totalLatePenalty += latePenalty;
                writer.println("Aircraft " + (i + 1) + ": Late by " + (landingTime - targetTime) + 
                             " units (penalty: " + latePenalty + ")");
            } else {
                onTimeCount++;
                writer.println("Aircraft " + (i + 1) + ": Landed on time");
            }
        }
        
        writer.println("\nSummary:");
        writer.println("Early landings: " + earlyCount + " (" + (100.0 * earlyCount / n) + "%)");
        writer.println("On-time landings: " + onTimeCount + " (" + (100.0 * onTimeCount / n) + "%)");
        writer.println("Late landings: " + lateCount + " (" + (100.0 * lateCount / n) + "%)");
        writer.println("Total early penalty: " + totalEarlyPenalty);
        writer.println("Total late penalty: " + totalLatePenalty);
        writer.println("Total penalty: " + (totalEarlyPenalty + totalLatePenalty));
        
        // Analyze separation time compliance
        writer.println("\nSeparation time check:");
        boolean separationViolation = false;
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    int runwayI = solution.getRunwayAssignment(i);
                    int runwayJ = solution.getRunwayAssignment(j);
                    
                    // Only check if on the same runway
                    if (runwayI == runwayJ) {
                        int landingTimeI = solution.getLandingTime(i);
                        int landingTimeJ = solution.getLandingTime(j);
                        
                        if (landingTimeI < landingTimeJ) {
                            int requiredSeparation = instance.getSeparationTime(i, j);
                            int actualSeparation = landingTimeJ - landingTimeI;
                            
                            if (actualSeparation < requiredSeparation) {
                                writer.println("Violation: Separation between aircraft " + (i + 1) + 
                                             " and " + (j + 1) + " is " + actualSeparation + 
                                             " but should be at least " + requiredSeparation);
                                separationViolation = true;
                            }
                        }
                    }
                }
            }
        }
        
        if (!separationViolation) {
            writer.println("All separation times are respected.");
        }
        
        // For Problem 3, calculate actual lateness
        if (solution.getProblemVariant().contains("Lateness")) {
            writer.println("\nLateness calculation (for Problem 3):");
            double totalLateness = 0;
            
            for (int i = 0; i < n; i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                int runway = solution.getRunwayAssignment(i);
                int landingTime = solution.getLandingTime(i);
                int transferTime = aircraft.getTransferTime(runway);
                int arrivalTime = aircraft.getTargetLandingTime(); // Ai = Ti as per requirements
                
                int parkingArrivalTime = landingTime + transferTime;
                int lateness = Math.max(0, parkingArrivalTime - arrivalTime);
                totalLateness += lateness;
                
                writer.println("Aircraft " + (i + 1) + ": Landing time = " + landingTime + 
                             ", Transfer time = " + transferTime + ", Arrival time at parking = " + 
                             parkingArrivalTime + ", Target = " + arrivalTime + 
                             ", Lateness = " + lateness);
            }
            
            writer.println("Total lateness: " + totalLateness + 
                         " (should match objective value: " + solution.getObjectiveValue() + ")");
        }
        
        writer.println("\n===========================\n");
    }
}
