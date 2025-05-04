package alp.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import alp.model.ALPInstance;
import alp.model.AircraftData;

/**
 * Class to read ALP instances from files in the OR-Library format.
 */
public class InstanceReader {

    /**
     * Reads an ALP instance from a file with a limit on the number of aircraft.
     * 
     * @param filePath    Path to the instance file
     * @param numRunways  Number of runways to use
     * @param maxAircraft Maximum number of aircraft to read (for CPLEX Community
     *                    Edition limits)
     * @return The parsed ALPInstance
     */
    public static ALPInstance readInstance(String filePath, int numRunways, int maxAircraft) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        
        // Extract file name from path
        String[] pathParts = filePath.split("[/\\\\]");
        String instanceName = pathParts[pathParts.length - 1].split("\\.")[0];

        // Read first line: number of planes and freeze time
        String line = reader.readLine();
        String[] firstLineParts = line.trim().split("\\s+");
        int numAircraft = Integer.parseInt(firstLineParts[0]);
        // Freeze time is in firstLineParts[1] but not used in this implementation

        // Limit the number of aircraft if necessary
        int actualAircraft = Math.min(numAircraft, maxAircraft);
        System.out.println("Nombre d'avions dans l'instance: " + numAircraft +
                (actualAircraft < numAircraft ? " (limité à " + actualAircraft + ")" : ""));

        // Read aircraft data and separation times
        List<AircraftData> aircraftList = new ArrayList<>();
        int[][] separationTimes = new int[actualAircraft][actualAircraft];

        for (int i = 0; i < actualAircraft; i++) {
            // Read aircraft data line
            line = reader.readLine();
            if (line == null) {
                System.err.println("Unexpected end of file while reading aircraft data.");
                break;
            }
            
            String[] parts = line.trim().split("\\s+");
            
            try {
                // Parse aircraft data: appearance time is at index 0,
                // earliest landing time at index 1, target at index 2, latest at index 3
                int appearanceTime = Integer.parseInt(parts[0]);
                int earliestLandingTime = Integer.parseInt(parts[1]);
                int targetLandingTime = Integer.parseInt(parts[2]);
                int latestLandingTime = Integer.parseInt(parts[3]);
                double earlyPenalty = Double.parseDouble(parts[4]);
                double latePenalty = Double.parseDouble(parts[5]);
                
                // Validate the time windows
                if (latestLandingTime < earliestLandingTime) {
                    System.err.println("Data error: latest < earliest for aircraft " + (i + 1));
                    latestLandingTime = earliestLandingTime + 100; // Correction
                }
                
                AircraftData aircraft = new AircraftData(i, earliestLandingTime, targetLandingTime,
                        latestLandingTime, earlyPenalty, latePenalty);
                aircraftList.add(aircraft);
                
                // Print aircraft data for debugging
                System.out.println("Aircraft " + (i + 1) + ": E=" + earliestLandingTime +
                        ", T=" + targetLandingTime + ", L=" + latestLandingTime);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error parsing aircraft " + (i + 1) + " data: " + e.getMessage());
                // Use default values if parsing fails
                AircraftData aircraft = new AircraftData(i, 0, 10, 20, 1.0, 1.0);
                aircraftList.add(aircraft);
            }
            
            // Read separation times for this aircraft with all others
            // This might span multiple lines in the file
            int j = 0;
            while (j < actualAircraft) {
                line = reader.readLine();
                if (line == null) {
                    System.err.println("Unexpected end of file while reading separation times.");
                    break;
                }
                
                String[] sepTimes = line.trim().split("\\s+");
                for (String sepTime : sepTimes) {
                    if (j >= actualAircraft) break;
                    
                    try {
                        separationTimes[i][j] = Integer.parseInt(sepTime);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid separation time format: " + sepTime + 
                                ", using default value");
                        separationTimes[i][j] = (i == j) ? 0 : 3; // Default separation time
                    }
                    j++;
                }
            }
        }
        
        // Skip the rest of the data for aircraft we're not using
        if (actualAircraft < numAircraft) {
            for (int i = actualAircraft; i < numAircraft; i++) {
                reader.readLine(); // Skip aircraft data line
                
                // Skip separation times for this aircraft
                int j = 0;
                while (j < numAircraft) {
                    String line2 = reader.readLine();
                    if (line2 == null) break;
                    
                    String[] sepTimes = line2.trim().split("\\s+");
                    j += sepTimes.length;
                }
            }
        }
        
        reader.close();
        System.out.println("Instance loaded successfully: " + actualAircraft + " aircraft");
        return new ALPInstance(aircraftList, separationTimes, numRunways, instanceName);
    }

    /**
     * Version without aircraft limit.
     * Kept for compatibility.
     */
    public static ALPInstance readInstance(String filePath, int numRunways) throws IOException {
        return readInstance(filePath, numRunways, Integer.MAX_VALUE);
    }
}
