package alp.visualization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import alp.model.AircraftData;
import alp.model.ALPInstance;
import alp.model.ALPSolution;

/**
 * VisualizationLauncher is a standalone utility class that allows users to
 * visualize existing ALP solution files without having to solve the problems
 * again.
 */
public class VisualizationLauncher {

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        System.out.println("=== Aircraft Landing Schedule Visualization Launcher ===");
        System.out.println("This utility allows you to visualize existing ALP solution files.");

        // Choose a solution file to visualize
        JFileChooser fileChooser = new JFileChooser("results");
        fileChooser.setDialogTitle("Select a solution file to visualize");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            try {
                ALPSolution solution = parseSolutionFile(selectedFile);
                if (solution != null) {
                    System.out.println("Launching visualization...");
                    ScheduleVisualizer.visualizeSchedule(solution);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Could not parse the solution file. Make sure it's a valid ALP solution file.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                System.err.println("Error parsing solution file: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error parsing solution file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("No file selected.");
        }
    }

    /**
     * Parse a solution file and extract the necessary information to create an
     * ALPSolution object.
     */
    static ALPSolution parseSolutionFile(File file) throws IOException {
        // Patterns to match relevant information in the file
        Pattern instancePattern = Pattern.compile("Instance:\\s+(\\S+)");
        Pattern problemPattern = Pattern.compile("Problem Variant:\\s+(\\S+)");
        Pattern objectivePattern = Pattern.compile("Objective Value:\\s+(\\S+)");
        Pattern solveTimePattern = Pattern.compile("Solve Time:\\s+(\\S+)\\s+seconds");
        Pattern aircraftPattern = Pattern.compile("Aircraft (\\d+): Landing Time = (\\d+), Runway = (\\d+)");
        Pattern runwaysPattern = Pattern.compile("Number of Runways:\\s+(\\d+)");
        Pattern earliestTimePattern = Pattern.compile("Earliest Landing Time:\\s+(\\d+)");
        Pattern latestTimePattern = Pattern.compile("Latest Landing Time:\\s+(\\d+)");
        Pattern targetTimePattern = Pattern.compile("Target Landing Time:\\s+(\\d+)");

        String instanceName = "";
        String problemVariant = "";
        double objectiveValue = 0;
        double solveTime = 0;
        int numRunways = 1;
        List<Integer> landingTimes = new ArrayList<>();
        List<Integer> runwayAssignments = new ArrayList<>();
        List<AircraftData> aircraftData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Match instance name
                Matcher instanceMatcher = instancePattern.matcher(line);
                if (instanceMatcher.find()) {
                    instanceName = instanceMatcher.group(1);
                }

                // Match problem variant
                Matcher problemMatcher = problemPattern.matcher(line);
                if (problemMatcher.find()) {
                    problemVariant = problemMatcher.group(1);
                }

                // Match objective value
                Matcher objectiveMatcher = objectivePattern.matcher(line);
                if (objectiveMatcher.find()) {
                    objectiveValue = Double.parseDouble(objectiveMatcher.group(1));
                }

                // Match solve time
                Matcher solveTimeMatcher = solveTimePattern.matcher(line);
                if (solveTimeMatcher.find()) {
                    solveTime = Double.parseDouble(solveTimeMatcher.group(1));
                }

                // Match runway count
                Matcher runwaysMatcher = runwaysPattern.matcher(line);
                if (runwaysMatcher.find()) {
                    numRunways = Integer.parseInt(runwaysMatcher.group(1));
                }

                // Match aircraft schedule
                Matcher aircraftMatcher = aircraftPattern.matcher(line);
                if (aircraftMatcher.find()) {
                    int aircraft = Integer.parseInt(aircraftMatcher.group(1));
                    int landingTime = Integer.parseInt(aircraftMatcher.group(2));
                    int runway = Integer.parseInt(aircraftMatcher.group(3)) - 1; // Runway is 1-indexed in output

                    // Ensure we have the right index
                    while (landingTimes.size() < aircraft) {
                        landingTimes.add(0);
                        runwayAssignments.add(0);
                    }

                    landingTimes.set(aircraft - 1, landingTime);
                    runwayAssignments.set(aircraft - 1, runway);
                }

                // Look for aircraft data details
                if (line.trim().startsWith("Aircraft")) {
                    // This might be the start of an aircraft data block
                    // Try to parse earliest, latest, and target times
                    int aircraftId = -1;
                    int earliestTime = 0;
                    int latestTime = 0;
                    int targetTime = 0;

                    try {
                        aircraftId = Integer.parseInt(line.trim().split("\\s+")[1].replace(":", ""));
                    } catch (Exception e) {
                        continue; // Not a valid aircraft data line
                    }

                    // Read the next few lines to find the time window data
                    for (int i = 0; i < 5 && (line = reader.readLine()) != null; i++) {
                        Matcher earliestMatcher = earliestTimePattern.matcher(line);
                        if (earliestMatcher.find()) {
                            earliestTime = Integer.parseInt(earliestMatcher.group(1));
                        }

                        Matcher latestMatcher = latestTimePattern.matcher(line);
                        if (latestMatcher.find()) {
                            latestTime = Integer.parseInt(latestMatcher.group(1));
                        }

                        Matcher targetMatcher = targetTimePattern.matcher(line);
                        if (targetMatcher.find()) {
                            targetTime = Integer.parseInt(targetMatcher.group(1));
                        }
                    }

                    // If we found valid data, create an AircraftData object
                    if (earliestTime > 0 && latestTime > 0) {
                        // Ensure we have the right index
                        while (aircraftData.size() < aircraftId) {
                            aircraftData.add(null);
                        }

                        AircraftData data = new AircraftData(
                                aircraftId, // ID
                                0, 0, // Early and late penalties (not critical for visualization)
                                earliestTime,
                                latestTime,
                                targetTime);

                        aircraftData.set(aircraftId - 1, data);
                    }
                }
            }
        }

        // If we couldn't find enough data, try a simpler parsing approach
        if (landingTimes.isEmpty() || aircraftData.isEmpty()) {
            System.out.println("Using simplified parsing approach...");
            return parseSimplifiedSolutionFile(file);
        }

        // Convert lists to arrays
        int[] landingTimesArray = landingTimes.stream().mapToInt(Integer::intValue).toArray();
        int[] runwayAssignmentsArray = runwayAssignments.stream().mapToInt(Integer::intValue).toArray();

        // Create separation times matrix
        int numAircraft = aircraftData.size();
        int[][] separationTimes = createDefaultSeparationTimes(numAircraft);

        // Create an instance object with the correct constructor
        ALPInstance instance = new ALPInstance(aircraftData, separationTimes, numRunways, instanceName);

        // Create and return the solution object
        return new ALPSolution(
                instance,
                landingTimesArray,
                runwayAssignmentsArray,
                objectiveValue,
                solveTime,
                problemVariant);
    }

    /**
     * Create a default separation times matrix (all values set to 1)
     */
    private static int[][] createDefaultSeparationTimes(int numAircraft) {
        int[][] separationTimes = new int[numAircraft][numAircraft];
        for (int i = 0; i < numAircraft; i++) {
            for (int j = 0; j < numAircraft; j++) {
                separationTimes[i][j] = (i != j) ? 1 : 0; // Default value of 1 for separation time
            }
        }
        return separationTimes;
    }

    /**
     * Simplified parsing approach for solution files that don't have full aircraft
     * data.
     */
    private static ALPSolution parseSimplifiedSolutionFile(File file) throws IOException {
        // This is a simpler approach that extracts just the basic schedule
        String instanceName = file.getName().replaceAll("_\\d+_runways\\.txt$", "");
        String problemVariant = "Unknown";
        double objectiveValue = 0;
        double solveTime = 0;

        List<Integer> landingTimes = new ArrayList<>();
        List<Integer> runwayAssignments = new ArrayList<>();
        int numRunways = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            // Try to get some basic info from the first few lines
            for (int i = 0; i < 10 && (line = reader.readLine()) != null; i++) {
                if (line.contains("Problem Variant:")) {
                    problemVariant = line.split(":")[1].trim();
                }
                if (line.contains("Objective Value:")) {
                    try {
                        objectiveValue = Double.parseDouble(line.split(":")[1].trim());
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
                if (line.contains("Solve Time:")) {
                    try {
                        String timeStr = line.split(":")[1].trim().replace("seconds", "").trim();
                        solveTime = Double.parseDouble(timeStr);
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
                if (line.contains("runways") || line.contains("Runways")) {
                    Pattern p = Pattern.compile("\\d+");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        numRunways = Integer.parseInt(m.group());
                    }
                }
            }

            // Reset to start of file
            reader.close();
            BufferedReader reReader = new BufferedReader(new FileReader(file));

            // Try to extract aircraft schedules from the file
            Pattern schedulePattern = Pattern
                    .compile("Aircraft\\s+(\\d+):\\s+Landing\\s+Time\\s+=\\s+(\\d+),\\s+Runway\\s+=\\s+(\\d+)");

            while ((line = reReader.readLine()) != null) {
                Matcher scheduleMatcher = schedulePattern.matcher(line);
                if (scheduleMatcher.find()) {
                    int aircraftId = Integer.parseInt(scheduleMatcher.group(1));
                    int landingTime = Integer.parseInt(scheduleMatcher.group(2));
                    int runway = Integer.parseInt(scheduleMatcher.group(3)) - 1; // Convert from 1-indexed to 0-indexed

                    // Ensure we have the right index
                    while (landingTimes.size() < aircraftId) {
                        landingTimes.add(0);
                        runwayAssignments.add(0);
                    }

                    landingTimes.set(aircraftId - 1, landingTime);
                    runwayAssignments.set(aircraftId - 1, runway);
                }
            }

            reReader.close();
        }

        if (landingTimes.isEmpty()) {
            System.err.println("Could not find any aircraft landing schedules in the file.");
            return null;
        }

        // Convert lists to arrays
        int[] landingTimesArray = landingTimes.stream().mapToInt(Integer::intValue).toArray();
        int[] runwayAssignmentsArray = runwayAssignments.stream().mapToInt(Integer::intValue).toArray();

        // Create synthetic aircraft data with reasonable defaults
        List<AircraftData> aircraftData = new ArrayList<>();
        for (int i = 0; i < landingTimes.size(); i++) {
            // Use landing time as a basis for time window
            int landingTime = landingTimes.get(i);
            int earliestTime = Math.max(0, landingTime - 20);
            int latestTime = landingTime + 20;
            int targetTime = landingTime;

            AircraftData data = new AircraftData(
                    i + 1, // ID
                    1, 1, // Generic penalty weights
                    earliestTime,
                    latestTime,
                    targetTime);

            aircraftData.add(data);
        }

        // Create separation times matrix
        int numAircraft = aircraftData.size();
        int[][] separationTimes = createDefaultSeparationTimes(numAircraft);

        // Create an instance object with the correct constructor
        ALPInstance instance = new ALPInstance(aircraftData, separationTimes, numRunways, instanceName);

        // Create and return the solution object
        return new ALPSolution(
                instance,
                landingTimesArray,
                runwayAssignmentsArray,
                objectiveValue,
                solveTime,
                problemVariant);
    }
}