package alp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import alp.analysis.SolutionAnalyzer;
import alp.io.InstanceReader;
import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.solver.ALPSolver;
import alp.solver.Problem1Solver;
import alp.solver.Problem2Solver;
import alp.solver.Problem3Solver;
import alp.visualization.ScheduleVisualizer;

/**
 * Main class for the Aircraft Landing Problem project.
 */
public class Main {

    // Flag to control visualization
    private static boolean enableVisualization = false;
    // List to store all solutions for visualization at the end
    private static List<ALPSolution> allSolutions = new ArrayList<>();

    public static void main(String[] args) {
        try {
            // Ask user if they want visualizations
            System.out.println("=== Aircraft Landing Problem Solver ===");
            System.out.println("Ce programme résout des problèmes d'ordonnancement d'atterrissage d'avions.");
            System.out.print("Voulez-vous activer la visualisation améliorée? (o/n): ");

            try (Scanner scanner = new Scanner(System.in)) {
                String response = scanner.nextLine().trim().toLowerCase();
                enableVisualization = response.equals("o") || response.equals("oui") ||
                        response.equals("y") || response.equals("yes");

                if (enableVisualization) {
                    System.out.println("Visualisation activée !");
                    System.out.print(
                            "Préférez-vous voir chaque solution individuellement (i) ou toutes à la fin (f)? (i/f): ");
                    String viewMode = scanner.nextLine().trim().toLowerCase();
                    boolean showIndividually = viewMode.equals("i") || viewMode.isEmpty();

                    if (!showIndividually) {
                        System.out.println("Les solutions seront visualisées ensemble à la fin du traitement.");
                    } else {
                        System.out.println("Chaque solution sera visualisée après son calcul.");
                    }

                    enableVisualization = true;
                    // Si on choisit la visualisation groupée, on garde les solutions en mémoire
                    allSolutions = showIndividually ? null : new ArrayList<>();
                }
            }

            // Create results directory if it doesn't exist
            Files.createDirectories(Paths.get("results"));

            // Configuration
            String instancesDir = "instances"; // Directory containing the OR-Library instances
            int[] runwayCounts = { 1, 2, 3 }; // Different runway counts to test

            // Create solvers
            List<ALPSolver> solvers = new ArrayList<>();
            solvers.add(new Problem1Solver());
            solvers.add(new Problem2Solver());
            solvers.add(new Problem3Solver());

            // Results summary file
            PrintWriter summaryWriter = new PrintWriter("results/summary.txt");
            summaryWriter.println("Instance,Runways,Problem,Objective,Time(s)");

            // Process all instance files
            File instancesFolder = new File(instancesDir);
            if (!instancesFolder.exists() || !instancesFolder.isDirectory()) {
                System.err.println("Instances directory not found: " + instancesDir);
                System.err.println("Please create this directory and add the OR-Library ALP instances.");
                printInstancesHelp();

                // Proposer le téléchargement automatique
                System.out.print("Voulez-vous télécharger automatiquement les instances? (o/n): ");
                try (Scanner scanner = new Scanner(System.in)) {
                    String response = scanner.nextLine().trim().toLowerCase();

                    if (response.equals("o") || response.equals("oui") || response.equals("y")
                            || response.equals("yes")) {
                        System.out.println("Téléchargement des instances...");
                        boolean success = downloadInstances(instancesDir);

                        if (success) {
                            System.out.println("Téléchargement réussi! Tentative de reprise du traitement...");
                            // Vérifier à nouveau l'existence des fichiers
                            File[] instanceFiles = instancesFolder.listFiles((dir, name) -> name.endsWith(".txt"));

                            if (instanceFiles != null && instanceFiles.length > 0) {
                                // Continuer l'exécution avec les fichiers téléchargés
                                processInstances(instanceFiles, runwayCounts, solvers, summaryWriter);
                            } else {
                                System.err.println("Échec: toujours aucun fichier trouvé après le téléchargement.");
                            }
                        } else {
                            System.err.println("Échec du téléchargement des instances.");
                        }
                    }
                }
            } else {
                File[] instanceFiles = instancesFolder.listFiles((dir, name) -> name.endsWith(".txt"));

                if (instanceFiles == null || instanceFiles.length == 0) {
                    System.err.println("No instance files found in: " + instancesDir);
                    printInstancesHelp();

                    // Proposer le téléchargement automatique
                    System.out.print("Voulez-vous télécharger automatiquement les instances? (o/n): ");
                    try (Scanner scanner = new Scanner(System.in)) {
                        String response = scanner.nextLine().trim().toLowerCase();

                        if (response.equals("o") || response.equals("oui") || response.equals("y")
                                || response.equals("yes")) {
                            System.out.println("Téléchargement des instances...");
                            boolean success = downloadInstances(instancesDir);

                            if (success) {
                                System.out.println("Téléchargement réussi! Tentative de reprise du traitement...");
                                // Vérifier à nouveau l'existence des fichiers
                                instanceFiles = instancesFolder.listFiles((dir, name) -> name.endsWith(".txt"));

                                if (instanceFiles != null && instanceFiles.length > 0) {
                                    // Continuer l'exécution
                                    processInstances(instanceFiles, runwayCounts, solvers, summaryWriter);
                                } else {
                                    System.err.println("Échec: toujours aucun fichier trouvé après le téléchargement.");
                                }
                            } else {
                                System.err.println("Échec du téléchargement des instances.");
                            }
                        }
                    }
                } else {
                    // Process instances normally
                    processInstances(instanceFiles, runwayCounts, solvers, summaryWriter);
                }
            }

            summaryWriter.close();
            System.out.println("All done! Results written to the 'results' directory.");

            // Proposer la visualisation d'une solution après le traitement
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(null,
                        "Voulez-vous visualiser une solution?",
                        "Visualisation", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    showVisualizationDialog(instancesDir);
                }
            });

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process all the instance files with different runway counts and solvers.
     */
    private static void processInstances(File[] instanceFiles, int[] runwayCounts, List<ALPSolver> solvers,
            PrintWriter summaryWriter) {
        // Liste pour stocker les solutions
        List<ALPSolution> allSolutions = new ArrayList<>();

        for (File instanceFile : instanceFiles) {
            String instanceName = instanceFile.getName().replace(".txt", "");
            System.out.println("Processing instance: " + instanceName);

            for (int numRunways : runwayCounts) {
                System.out.println("  Avec " + numRunways + " piste(s)");

                try {
                    // Read the instance
                    ALPInstance instance = InstanceReader.readInstance(instanceFile.getPath(), numRunways);

                    // Create a specific results file for this instance and runway count
                    String resultsFileName = "results/" + instanceName + "_" + numRunways + "_runways.txt";
                    PrintWriter resultsWriter = new PrintWriter(resultsFileName);

                    // Process with each solver
                    for (ALPSolver solver : solvers) {
                        System.out.println("    Résolution avec " + solver.getName());

                        try {
                            // Solve the instance
                            ALPSolution solution = solver.solve(instance);

                            // Store solution for later visualization
                            allSolutions.add(solution);

                            // Write solution to file for persistence
                            try (PrintWriter solWriter = new PrintWriter(
                                    "results/" + instanceName + "_" + numRunways + "_" +
                                            solver.getClass().getSimpleName() + ".sol")) {
                                solWriter.println("Instance: " + instanceName);
                                solWriter.println("Runways: " + numRunways);
                                solWriter.println("Solver: " + solver.getName());
                                solWriter.println("Objective: " + solution.getObjectiveValue());
                                solWriter.println("Time: " + solution.getSolveTime() + "s");
                            }

                            // Analyze the solution
                            SolutionAnalyzer.analyzeSolution(solution, resultsWriter);

                            // Add to summary
                            summaryWriter.println(instanceName + "," + numRunways + "," +
                                    solver.getName() + "," + solution.getObjectiveValue() +
                                    "," + solution.getSolveTime());

                            System.out.println("      Solved! Objective: " + solution.getObjectiveValue() +
                                    ", Time: " + solution.getSolveTime() + "s");

                            // Proposer de visualiser cette solution
                            final ALPSolution finalSolution = solution;
                            SwingUtilities.invokeLater(() -> {
                                int choice = JOptionPane.showConfirmDialog(null,
                                        "Voulez-vous visualiser la solution pour " + instanceName +
                                                " avec " + numRunways + " piste(s) et " + solver.getName() + "?",
                                        "Visualisation", JOptionPane.YES_NO_OPTION);

                                if (choice == JOptionPane.YES_OPTION) {
                                    ScheduleVisualizer.visualizeSchedule(finalSolution);
                                }
                            });

                        } catch (Exception e) {
                            System.err.println("      Erreur lors de la résolution avec " + solver.getName() + ": "
                                    + e.getMessage());
                            resultsWriter.println(
                                    "Erreur lors de la résolution avec " + solver.getName() + ": " + e.getMessage());
                            e.printStackTrace(resultsWriter);
                        }
                    }

                    resultsWriter.close();
                } catch (IOException e) {
                    System.err.println("Error processing instance " + instanceName + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Affiche une boîte de dialogue permettant de choisir une solution à visualiser
     */
    private static void showVisualizationDialog(String instancesDir) {
        try {
            // Chercher des fichiers de solution
            File resultsDir = new File("results");
            File[] solutionFiles = resultsDir.listFiles((dir, name) -> name.endsWith(".sol"));

            if (solutionFiles == null || solutionFiles.length == 0) {
                JOptionPane.showMessageDialog(null,
                        "Aucune solution sauvegardée trouvée.",
                        "Visualisation impossible", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Créer un tableau des noms de solutions
            String[] solutionNames = new String[solutionFiles.length];
            for (int i = 0; i < solutionFiles.length; i++) {
                String fileName = solutionFiles[i].getName();
                solutionNames[i] = fileName.substring(0, fileName.length() - 4); // Enlever .sol
            }

            // Montrer un dialogue de sélection
            String selected = (String) JOptionPane.showInputDialog(null,
                    "Choisissez une solution à visualiser:",
                    "Visualiser une solution",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    solutionNames,
                    solutionNames[0]);

            if (selected != null) {
                // Parser le nom sélectionné pour obtenir les paramètres
                String[] parts = selected.split("_");
                String instanceName = parts[0];
                int numRunways = Integer.parseInt(parts[1]);
                String solverName = parts[2];

                // Déterminer quelle classe de solver utiliser
                ALPSolver solver;
                if (solverName.equals("Problem1Solver")) {
                    solver = new Problem1Solver();
                } else if (solverName.equals("Problem2Solver")) {
                    solver = new Problem2Solver();
                } else {
                    solver = new Problem3Solver();
                }

                // Charger l'instance
                ALPInstance instance = InstanceReader.readInstance(
                        instancesDir + File.separator + instanceName + ".txt", numRunways);

                // Résoudre à nouveau (puisque nous ne sauvegardons pas le vecteur de solution
                // complet)
                ALPSolution solution = solver.solve(instance);

                // Visualiser
                ScheduleVisualizer.visualizeSchedule(solution);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Erreur lors de la visualisation: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Download instances from OR-Library.
     */
    private static boolean downloadInstances(String targetDir) {
        try {
            // Créer le répertoire cible s'il n'existe pas
            Files.createDirectories(Paths.get(targetDir));

            String[] urls = {
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland1.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland2.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland3.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland4.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland5.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland6.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland7.txt",
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland8.txt",
            };

            boolean allSuccessful = true;

            for (String url : urls) {
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                String targetPath = targetDir + File.separator + fileName;

                System.out.println("Téléchargement de " + fileName + "...");

                try {
                    downloadFile(url, targetPath);
                    System.out.println("  Téléchargement réussi: " + targetPath);
                } catch (IOException e) {
                    System.err.println("  Échec du téléchargement: " + e.getMessage());
                    allSuccessful = false;
                }
            }

            return allSuccessful;

        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire: " + e.getMessage());
            return false;
        }
    }

    /**
     * Download a file from a URL.
     */
    private static void downloadFile(String urlStr, String destinationPath) throws IOException {
        java.net.URL url = new java.net.URL(urlStr);
        try (
                java.io.InputStream is = url.openStream();
                java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels.newChannel(is);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(destinationPath)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    /**
     * Affiche les instructions pour télécharger et configurer les instances.
     */
    private static void printInstancesHelp() {
        System.out.println("\n=== INSTRUCTIONS POUR LES INSTANCES ===");
        System.out.println("1. Créez un dossier 'instances' à la racine du projet");
        System.out.println("2. Téléchargez les instances ALP depuis OR-Library:");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland1.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland2.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland3.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland4.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland5.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland6.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland7.txt");
        System.out.println("   https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland8.txt");
        System.out.println("3. Placez les fichiers téléchargés dans le dossier 'instances'");
        System.out.println("4. Relancez l'application");
        System.out.println("========================================\n");
    }
}