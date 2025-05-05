package alp;

import alp.visualization.AircraftLandingDashboard;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main class for the Aircraft Landing Problem project.
 */
public class Main {

    public static void main(String[] args) {
        // Lancer le tableau de bord centralisé
        SwingUtilities.invokeLater(() -> {
            try {
                // S'assurer que les répertoires nécessaires existent
                Files.createDirectories(Paths.get("instances"));
                Files.createDirectories(Paths.get("results"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            new AircraftLandingDashboard();
        });
    }
}