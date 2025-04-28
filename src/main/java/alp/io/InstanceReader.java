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
        String line;

        // Extract file name from path
        String[] pathParts = filePath.split("[/\\\\]");
        String instanceName = pathParts[pathParts.length - 1].split("\\.")[0];

        // Read number of aircraft
        line = reader.readLine();
        // Parse le premier nombre de la ligne comme nombre d'avions
        String[] firstLineParts = line.trim().split("\\s+");
        int numAircraft = Integer.parseInt(firstLineParts[0]);

        // Limiter le nombre d'avions si nécessaire
        int actualAircraft = Math.min(numAircraft, maxAircraft);

        System.out.println("Nombre d'avions dans l'instance: " + numAircraft +
                (actualAircraft < numAircraft ? " (limité à " + actualAircraft + ")" : ""));

        // Skip the freeze time line (not used in our models)
        reader.readLine();

        // Read aircraft data
        List<AircraftData> aircraftList = new ArrayList<>();

        for (int i = 0; i < actualAircraft; i++) {
            line = reader.readLine();
            if (line == null) {
                System.err.println("Fin de fichier inattendue lors de la lecture des données d'avion.");
                break;
            }

            String[] parts = line.trim().split("\\s+");

            // Vérification que nous avons suffisamment de données
            if (parts.length < 7) {
                System.err.println("Format de ligne incorrect pour l'avion " + (i + 1) + ": " + line);
                System.err.println("Essai d'adapter en utilisant les valeurs disponibles...");

                // Utiliser des valeurs par défaut si nécessaire
                int id = (parts.length > 0) ? Integer.parseInt(parts[0]) - 1 : i;
                int appearanceTime = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
                int earliestLandingTime = appearanceTime;
                int targetLandingTime = (parts.length > 1) ? earliestLandingTime + 10 : 10;
                int latestLandingTime = (parts.length > 1) ? targetLandingTime + 10 : 20;
                double earlyPenalty = 1.0;
                double latePenalty = 1.0;

                AircraftData aircraft = new AircraftData(id, earliestLandingTime, targetLandingTime,
                        latestLandingTime, earlyPenalty, latePenalty);
                aircraftList.add(aircraft);
                continue;
            }

            try {
                int id = Integer.parseInt(parts[0]) - 1; // Convert to 0-indexed
                int earliestLandingTime = Integer.parseInt(parts[2]);
                int targetLandingTime = Integer.parseInt(parts[3]);
                int latestLandingTime = Integer.parseInt(parts[4]);
                double earlyPenalty = Double.parseDouble(parts[5]);
                double latePenalty = Double.parseDouble(parts[6]);

                // Vérification de cohérence des données
                if (latestLandingTime < earliestLandingTime) {
                    System.err.println("Erreur de données: latest < earliest pour l'avion " + (i + 1));
                    latestLandingTime = earliestLandingTime + 100; // Correction
                }

                // S'assurer que la fenêtre de temps est faisable
                latestLandingTime = Math.max(latestLandingTime, earliestLandingTime);
                targetLandingTime = Math.max(Math.min(targetLandingTime, latestLandingTime), earliestLandingTime);

                AircraftData aircraft = new AircraftData(id, earliestLandingTime, targetLandingTime,
                        latestLandingTime, earlyPenalty, latePenalty);
                aircraftList.add(aircraft);

                // Afficher les données pour debug
                System.out.println("Avion " + (i + 1) + ": E=" + earliestLandingTime +
                        ", T=" + targetLandingTime + ", L=" + latestLandingTime);
            } catch (NumberFormatException e) {
                System.err.println("Erreur de format de nombre pour l'avion " + (i + 1) + ": " + e.getMessage());
                System.err.println("Utilisation de valeurs par défaut...");

                AircraftData aircraft = new AircraftData(i, 0, 10, 20, 1.0, 1.0);
                aircraftList.add(aircraft);
            }
        }

        // Lire les lignes restantes pour éviter des problèmes dans le fichier
        if (actualAircraft < numAircraft) {
            for (int i = actualAircraft; i < numAircraft; i++) {
                reader.readLine(); // Sauter les avions supplémentaires
            }
        }

        // Read separation times for the actual aircraft we're using
        int[][] separationTimes = new int[actualAircraft][actualAircraft];
        for (int i = 0; i < actualAircraft; i++) {
            line = reader.readLine();
            if (line == null) {
                System.err.println("Fin de fichier inattendue lors de la lecture des temps de séparation.");
                // Remplir le reste de la matrice avec des valeurs par défaut
                for (int a = i; a < actualAircraft; a++) {
                    for (int b = 0; b < actualAircraft; b++) {
                        separationTimes[a][b] = (a == b) ? 0 : 3; // Valeur de séparation par défaut
                    }
                }
                break;
            }

            String[] parts = line.trim().split("\\s+");

            // S'assurer que nous avons assez de valeurs ou utiliser des valeurs par défaut
            for (int j = 0; j < actualAircraft; j++) {
                if (j < parts.length) {
                    try {
                        separationTimes[i][j] = Integer.parseInt(parts[j]);
                    } catch (NumberFormatException e) {
                        System.err.println("Format de temps de séparation invalide: " + parts[j]
                                + ", utilisation de valeur par défaut");
                        separationTimes[i][j] = (i == j) ? 0 : 3;
                    }
                } else {
                    System.err.println("Pas assez de valeurs de séparation pour l'avion " + (i + 1)
                            + ", utilisation de valeurs par défaut.");
                    separationTimes[i][j] = (i == j) ? 0 : 3; // Valeur de séparation par défaut
                }
            }
        }

        // Garder la compatibilité avec la méthode originale
        reader.close();

        System.out.println("Instance lue avec succès: " + actualAircraft + " avions");

        return new ALPInstance(aircraftList, separationTimes, numRunways, instanceName);
    }

    /**
     * Version simplifiée qui lit tous les avions.
     * Conservée pour la compatibilité.
     */
    public static ALPInstance readInstance(String filePath, int numRunways) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        // Extract file name from path
        String[] pathParts = filePath.split("[/\\\\]");
        String instanceName = pathParts[pathParts.length - 1].split("\\.")[0];

        // Imprimer le contenu du fichier pour debug
        System.out.println("=== Contenu du fichier " + instanceName + " ===");
        BufferedReader debugReader = new BufferedReader(new FileReader(filePath));
        String debugLine;
        int lineCount = 0;
        while ((debugLine = debugReader.readLine()) != null && lineCount < 20) {
            System.out.println("Ligne " + (++lineCount) + ": " + debugLine);
        }
        debugReader.close();
        System.out.println("====================================");

        // Read number of aircraft
        line = reader.readLine();
        // Parse le premier nombre de la ligne comme nombre d'avions
        String[] firstLineParts = line.trim().split("\\s+");
        int numAircraft = Integer.parseInt(firstLineParts[0]);

        System.out.println("Nombre d'avions dans l'instance: " + numAircraft);

        // Skip the freeze time line (not used in our models)
        reader.readLine();

        // Read aircraft data
        List<AircraftData> aircraftList = new ArrayList<>();
        for (int i = 0; i < numAircraft; i++) {
            line = reader.readLine();
            if (line == null) {
                System.err.println("Fin de fichier inattendue lors de la lecture des données d'avion.");
                break;
            }

            String[] parts = line.trim().split("\\s+");

            // Vérification que nous avons suffisamment de données
            if (parts.length < 7) {
                System.err.println("Format de ligne incorrect pour l'avion " + (i + 1) + ": " + line);
                System.err.println("Essai d'adapter en utilisant les valeurs disponibles...");

                // Utiliser des valeurs par défaut si nécessaire
                int id = (parts.length > 0) ? Integer.parseInt(parts[0]) - 1 : i;
                int appearanceTime = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
                int earliestLandingTime = appearanceTime;
                int targetLandingTime = (parts.length > 1) ? earliestLandingTime + 10 : 10;
                int latestLandingTime = (parts.length > 1) ? targetLandingTime + 10 : 20;
                double earlyPenalty = 1.0;
                double latePenalty = 1.0;

                AircraftData aircraft = new AircraftData(id, earliestLandingTime, targetLandingTime,
                        latestLandingTime, earlyPenalty, latePenalty);
                aircraftList.add(aircraft);
                continue;
            }

            try {
                int id = Integer.parseInt(parts[0]) - 1; // Convert to 0-indexed
                int earliestLandingTime = Integer.parseInt(parts[2]);
                int targetLandingTime = Integer.parseInt(parts[3]);
                int latestLandingTime = Integer.parseInt(parts[4]);
                double earlyPenalty = Double.parseDouble(parts[5]);
                double latePenalty = Double.parseDouble(parts[6]);

                // Vérification de cohérence des données
                if (latestLandingTime < earliestLandingTime) {
                    System.err.println("Erreur de données: latest < earliest pour l'avion " + (i + 1));
                    latestLandingTime = earliestLandingTime + 100; // Correction
                }

                // S'assurer que la fenêtre de temps est faisable
                latestLandingTime = Math.max(latestLandingTime, earliestLandingTime);
                targetLandingTime = Math.max(Math.min(targetLandingTime, latestLandingTime), earliestLandingTime);

                AircraftData aircraft = new AircraftData(id, earliestLandingTime, targetLandingTime,
                        latestLandingTime, earlyPenalty, latePenalty);
                aircraftList.add(aircraft);

                // Afficher les données pour debug
                System.out.println("Avion " + (i + 1) + ": E=" + earliestLandingTime +
                        ", T=" + targetLandingTime + ", L=" + latestLandingTime);
            } catch (NumberFormatException e) {
                System.err.println("Erreur de format de nombre pour l'avion " + (i + 1) + ": " + e.getMessage());
                System.err.println("Utilisation de valeurs par défaut...");

                AircraftData aircraft = new AircraftData(i, 0, 10, 20, 1.0, 1.0);
                aircraftList.add(aircraft);
            }
        }

        // Read separation times
        int[][] separationTimes = new int[numAircraft][numAircraft];
        for (int i = 0; i < numAircraft; i++) {
            line = reader.readLine();
            if (line == null) {
                System.err.println("Fin de fichier inattendue lors de la lecture des temps de séparation.");
                // Remplir le reste de la matrice avec des valeurs par défaut
                for (int a = i; a < numAircraft; a++) {
                    for (int b = 0; b < numAircraft; b++) {
                        separationTimes[a][b] = (a == b) ? 0 : 3; // Valeur de séparation par défaut
                    }
                }
                break;
            }

            String[] parts = line.trim().split("\\s+");

            // S'assurer que nous avons assez de valeurs ou utiliser des valeurs par défaut
            for (int j = 0; j < numAircraft; j++) {
                if (j < parts.length) {
                    try {
                        separationTimes[i][j] = Integer.parseInt(parts[j]);
                    } catch (NumberFormatException e) {
                        System.err.println("Format de temps de séparation invalide: " + parts[j]
                                + ", utilisation de valeur par défaut");
                        separationTimes[i][j] = (i == j) ? 0 : 3;
                    }
                } else {
                    System.err.println("Pas assez de valeurs de séparation pour l'avion " + (i + 1)
                            + ", utilisation de valeurs par défaut.");
                    separationTimes[i][j] = (i == j) ? 0 : 3; // Valeur de séparation par défaut
                }
            }
        }

        reader.close();

        System.out.println("Instance lue avec succès: " + numAircraft + " avions");

        return new ALPInstance(aircraftList, separationTimes, numRunways, instanceName);
    }
}
