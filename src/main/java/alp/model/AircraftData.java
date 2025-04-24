package alp.model;

/**
 * Class representing the data for a single aircraft in the Aircraft Landing Problem.
 */
public class AircraftData {
    private int id;
    private int earliestLandingTime; // Ei
    private int targetLandingTime;   // Ti
    private int latestLandingTime;   // Li
    private double earlyPenalty;     // c-i
    private double latePenalty;      // c+i
    private int[] runwayTransferTimes; // tir

    public AircraftData(int id, int earliestLandingTime, int targetLandingTime, int latestLandingTime,
                      double earlyPenalty, double latePenalty) {
        this.id = id;
        this.earliestLandingTime = earliestLandingTime;
        this.targetLandingTime = targetLandingTime;
        this.latestLandingTime = latestLandingTime;
        this.earlyPenalty = earlyPenalty;
        this.latePenalty = latePenalty;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public int getEarliestLandingTime() {
        return earliestLandingTime;
    }

    public int getTargetLandingTime() {
        return targetLandingTime;
    }

    public int getLatestLandingTime() {
        return latestLandingTime;
    }

    public double getEarlyPenalty() {
        return earlyPenalty;
    }

    public double getLatePenalty() {
        return latePenalty;
    }
    
    public void setRunwayTransferTimes(int[] runwayTransferTimes) {
        this.runwayTransferTimes = runwayTransferTimes;
    }
    
    public int[] getRunwayTransferTimes() {
        return runwayTransferTimes;
    }
    
    public int getTransferTime(int runway) {
        return runwayTransferTimes[runway];
    }
}
