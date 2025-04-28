package alp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import alp.analysis.SolutionAnalyzer;
import alp.io.InstanceReader;
import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.solver.ALPSolver;
import alp.solver.Problem1Solver;
import alp.solver.Problem2Solver;
import alp.solver.Problem3Solver;
import alp.visualization.ScheduleVisualizer;
import alp.visualization.VisualizationManager;

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
            System.out.println("Traitement terminé ! Les résultats ont été écrits dans le répertoire 'results'.");

            // Si la visualisation groupée est activée, on affiche toutes les solutions à la
            // fin
            if (enableVisualization && allSolutions != null && !allSolutions.isEmpty()) {
                System.out.println("Lancement de la visualisation groupée des solutions...");
                VisualizationManager.launchVisualizer(allSolutions);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process all the instance files with different runway counts and solvers.
     */
    private static void processInstances(File[] instanceFiles, int[] runwayCounts,
            List<ALPSolver> solvers, PrintWriter summaryWriter) {
        for (File instanceFile : instanceFiles) {
            String instanceName = instanceFile.getName().replace(".txt", "");
            System.out.println("Traitement de l'instance: " + instanceName);

            // Identifier les petites instances compatibles avec CPLEX Community Edition
            boolean isSmallInstance = instanceName.equals("airland1");

            // Si l'instance est grande, réduire sa taille
            int maxAircraft = isSmallInstance ? Integer.MAX_VALUE : 10; // Limiter à 10 avions pour les grandes
                                                                        // instances

            for (int numRunways : runwayCounts) {
                System.out.println("  Avec " + numRunways + " piste(s)");

                try {
                    // Read the instance
                    ALPInstance instance = InstanceReader.readInstance(instanceFile.getPath(), numRunways, maxAircraft);

                    // Si l'instance est trop grande, ajouter un avertissement
                    if (!isSmallInstance) {
                        System.out.println("  ⚠️ Instance tronquée à " + maxAircraft
                                + " avions pour respecter les limites de CPLEX Community Edition");
                    }

                    // Create a specific results file for this instance and runway count
                    String resultsFileName = "results/" + instanceName + "_" + numRunways + "_runways.txt";
                    PrintWriter resultsWriter = new PrintWriter(resultsFileName);

                    // Si l'instance a été tronquée, l'indiquer dans le fichier de résultats
                    if (!isSmallInstance) {
                        resultsWriter.println("⚠️ AVERTISSEMENT: Cette instance a été tronquée à " + maxAircraft +
                                " avions pour respecter les limites de CPLEX Community Edition");
                        resultsWriter.println("Les résultats ne sont pas représentatifs de l'instance complète.\n");
                    }

                    // Process with each solver
                    for (ALPSolver solver : solvers) {
                        System.out.println("    Résolution avec " + solver.getName());

                        try {
                            // Solve the instance
                            ALPSolution solution = solver.solve(instance);

                            // Analyze the solution
                            SolutionAnalyzer.analyzeSolution(solution, resultsWriter);

                            // Store solution for grouped visualization if enabled
                            if (enableVisualization && allSolutions != null) {
                                allSolutions.add(solution);
                            }

                            // Visualize the solution if enabled and individual mode
                            if (enableVisualization && allSolutions == null) {
                                System.out.println("      Affichage de la visualisation...");
                                ScheduleVisualizer.visualizeSchedule(solution);
                            }

                            // Add to summary
                            summaryWriter.println(instanceName + "," + numRunways + "," +
                                    solver.getName() + "," + solution.getObjectiveValue() +
                                    "," + solution.getSolveTime());

                            System.out.println("      Résolu ! Objectif: " + solution.getObjectiveValue() +
                                    ", Temps: " + solution.getSolveTime() + "s");
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
                    System.err
                            .println("Erreur lors du traitement de l'instance " + instanceName + ": " + e.getMessage());
                }
            }
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
                    "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland8.txt"
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