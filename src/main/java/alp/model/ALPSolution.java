package alp.model;

/**
 * Class representing a solution to an ALP instance.
 */
public class ALPSolution {
    private ALPInstance instance;
    private int[] landingTimes;
    private int[] runwayAssignments;
    private double objectiveValue;
    private double solveTime;
    private String problemVariant;

    public ALPSolution(ALPInstance instance, int[] landingTimes, int[] runwayAssignments, 
                     double objectiveValue, double solveTime, String problemVariant) {
        this.instance = instance;
        this.landingTimes = landingTimes;
        this.runwayAssignments = runwayAssignments;
        this.objectiveValue = objectiveValue;
        this.solveTime = solveTime;
        this.problemVariant = problemVariant;
    }

    public ALPInstance getInstance() {
        return instance;
    }

    public int[] getLandingTimes() {
        return landingTimes;
    }

    public int[] getRunwayAssignments() {
        return runwayAssignments;
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }
    
    public double getSolveTime() {
        return solveTime;
    }
    
    public String getProblemVariant() {
        return problemVariant;
    }
    
    public int getLandingTime(int aircraftIndex) {
        return landingTimes[aircraftIndex];
    }
    
    public int getRunwayAssignment(int aircraftIndex) {
        return runwayAssignments[aircraftIndex];
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Problem Variant: ").append(problemVariant).append("\n");
        sb.append("Instance: ").append(instance.getInstanceName()).append("\n");
        sb.append("Objective Value: ").append(objectiveValue).append("\n");
        sb.append("Solve Time: ").append(solveTime).append(" seconds\n");
        sb.append("Schedule:\n");
        
        for (int i = 0; i < landingTimes.length; i++) {
            sb.append("Aircraft ").append(i + 1)
              .append(": Landing Time = ").append(landingTimes[i])
              .append(", Runway = ").append(runwayAssignments[i] + 1)
              .append("\n");
        }
        
        return sb.toString();
    }
}
