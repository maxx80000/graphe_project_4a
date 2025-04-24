package alp.model;

import java.util.List;
import java.util.Random;

/**
 * Class representing an instance of the Aircraft Landing Problem.
 */
public class ALPInstance {
    private List<AircraftData> aircraft;
    private int[][] separationTimes; // sij
    private int numRunways;
    private String instanceName;

    public ALPInstance(List<AircraftData> aircraft, int[][] separationTimes, int numRunways, String instanceName) {
        this.aircraft = aircraft;
        this.separationTimes = separationTimes;
        this.numRunways = numRunways;
        this.instanceName = instanceName;
        
        // Generate runway transfer times for problem 3
        generateRunwayTransferTimes();
    }
    
    private void generateRunwayTransferTimes() {
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (AircraftData aircraft : this.aircraft) {
            int[] transferTimes = new int[numRunways];
            int minTime = 1;
            int maxTime = aircraft.getTargetLandingTime() - aircraft.getEarliestLandingTime();
            
            // Ensure maxTime is at least 1
            maxTime = Math.max(1, maxTime);
            
            for (int r = 0; r < numRunways; r++) {
                transferTimes[r] = minTime + random.nextInt(maxTime);
            }
            
            aircraft.setRunwayTransferTimes(transferTimes);
        }
    }

    public List<AircraftData> getAircraft() {
        return aircraft;
    }

    public int[][] getSeparationTimes() {
        return separationTimes;
    }

    public int getNumRunways() {
        return numRunways;
    }
    
    public int getNumAircraft() {
        return aircraft.size();
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public int getSeparationTime(int i, int j) {
        return separationTimes[i][j];
    }
}
