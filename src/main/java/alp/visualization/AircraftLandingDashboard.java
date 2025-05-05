package alp.visualization;

import alp.io.InstanceReader;
import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.solver.ALPSolver;
import alp.solver.Problem1Solver;
import alp.solver.Problem2Solver;
import alp.solver.Problem3Solver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Interface utilisateur centralisée pour le problème d'atterrissage d'avions.
 * Fournit un tableau de bord complet avec des fonctionnalités de résolution,
 * de visualisation et de comparaison des solutions.
 */
public class AircraftLandingDashboard extends JFrame {

    // Constantes pour l'interface
    private static final String TITLE = "Aircraft Landing Problem - Tableau de Bord";
    private static final Color PRIMARY_COLOR = new Color(60, 90, 180);
    private static final Color PANEL_BACKGROUND = new Color(255, 255, 255);
    private static final Color GRID_COLOR = new Color(230, 230, 240);
    private static final Color TIME_AXIS_COLOR = new Color(100, 100, 120);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    // Composants principaux de l'interface
    private JTabbedPane mainTabbedPane;
    private JPanel controlPanel;
    private JPanel resultsPanel;
    private JPanel statusPanel;
    private JTable solutionsTable;
    private DefaultTableModel solutionsTableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    // Composants pour les paramètres
    private JComboBox<String> instanceSelector;
    private JSpinner runwaySpinner;
    private JCheckBox useAllRunwaysCheckBox;
    private JComboBox<String> solverSelector;
    private JCheckBox useAllSolversCheckBox;
    private JButton solveButton;
    private JButton cancelButton;
    private JButton visualizeButton;
    private JButton compareButton;
    private JButton downloadInstancesButton;

    // Données
    private Map<String, File> instanceFiles = new HashMap<>();
    private List<ALPSolution> solutions = new ArrayList<>();
    private SwingWorker<List<ALPSolution>, String> currentWorker;
    private String instancesDirectory = "instances";

    /**
     * Constructeur - initialise l'interface utilisateur
     */
    public AircraftLandingDashboard() {
        setTitle(TITLE);
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Configuration du look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Création des composants
        initComponents();
        layoutComponents();

        // Chargement des instances disponibles
        loadInstances();

        // Affichage de l'interface
        setVisible(true);
    }

    /**
     * Initialisation des composants de l'interface
     */
    private void initComponents() {
        // Onglets principaux
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(HEADER_FONT);

        // Panneau de contrôle
        controlPanel = createControlPanel();

        // Panneau de résultats
        resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBackground(PANEL_BACKGROUND);

        // Tableau des solutions
        String[] columnNames = { "Instance", "Pistes", "Solveur", "Objectif", "Temps (s)", "Statut" };
        solutionsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        solutionsTable = new JTable(solutionsTableModel);
        solutionsTable.setFont(NORMAL_FONT);
        solutionsTable.setRowHeight(25);
        solutionsTable.getTableHeader().setFont(HEADER_FONT);
        solutionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        solutionsTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(solutionsTable);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // Panneau des boutons d'action
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(PANEL_BACKGROUND);

        visualizeButton = new JButton("Visualiser");
        visualizeButton.setFont(NORMAL_FONT);
        visualizeButton.setEnabled(false);
        visualizeButton.addActionListener(e -> visualizeSelectedSolution());

        compareButton = new JButton("Comparer");
        compareButton.setFont(NORMAL_FONT);
        compareButton.setEnabled(false);
        compareButton.addActionListener(e -> compareSolutions());

        JButton clearButton = new JButton("Effacer");
        clearButton.setFont(NORMAL_FONT);
        clearButton.addActionListener(e -> clearSolutions());

        actionPanel.add(visualizeButton);
        actionPanel.add(compareButton);
        actionPanel.add(clearButton);

        resultsPanel.add(actionPanel, BorderLayout.SOUTH);

        // Panneau de statut
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(PANEL_BACKGROUND);
        statusPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("Prêt");
        statusLabel.setFont(NORMAL_FONT);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setVisible(false);

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        // Ajouter un listener pour le tableau
        solutionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = solutionsTable.getSelectedRow() != -1;
                visualizeButton.setEnabled(hasSelection);
                compareButton.setEnabled(solutionsTable.getRowCount() > 1);
            }
        });
    }

    /**
     * Création du panneau de contrôle avec les options de configuration
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(PANEL_BACKGROUND);

        // Titre
        JLabel titleLabel = new JLabel("Configuration");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 1. Sélection de l'instance
        JPanel instancePanel = new JPanel(new BorderLayout());
        instancePanel.setBackground(PANEL_BACKGROUND);
        instancePanel.setBorder(createGroupBorder("Instances"));
        instancePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 120));
        instancePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        instanceSelector = new JComboBox<>();
        instanceSelector.setFont(NORMAL_FONT);

        JButton browseButton = new JButton("Parcourir...");
        browseButton.setFont(NORMAL_FONT);
        browseButton.addActionListener(e -> browseForInstance());

        downloadInstancesButton = new JButton("Télécharger les instances");
        downloadInstancesButton.setFont(NORMAL_FONT);
        downloadInstancesButton.addActionListener(e -> downloadInstances());

        JPanel instanceButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        instanceButtonPanel.setBackground(PANEL_BACKGROUND);
        instanceButtonPanel.add(browseButton);
        instanceButtonPanel.add(downloadInstancesButton);

        instancePanel.add(instanceSelector, BorderLayout.CENTER);
        instancePanel.add(instanceButtonPanel, BorderLayout.SOUTH);

        // 2. Configuration des pistes
        JPanel runwayPanel = new JPanel(new BorderLayout());
        runwayPanel.setBackground(PANEL_BACKGROUND);
        runwayPanel.setBorder(createGroupBorder("Nombre de pistes"));
        runwayPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        runwayPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        SpinnerNumberModel runwayModel = new SpinnerNumberModel(1, 1, 10, 1);
        runwaySpinner = new JSpinner(runwayModel);
        runwaySpinner.setFont(NORMAL_FONT);

        useAllRunwaysCheckBox = new JCheckBox("Tester plusieurs configurations (1, 2, 3 pistes)");
        useAllRunwaysCheckBox.setFont(NORMAL_FONT);
        useAllRunwaysCheckBox.setBackground(PANEL_BACKGROUND);
        useAllRunwaysCheckBox.addActionListener(e -> runwaySpinner.setEnabled(!useAllRunwaysCheckBox.isSelected()));

        runwayPanel.add(runwaySpinner, BorderLayout.NORTH);
        runwayPanel.add(useAllRunwaysCheckBox, BorderLayout.CENTER);

        // 3. Sélection du solveur
        JPanel solverPanel = new JPanel(new BorderLayout());
        solverPanel.setBackground(PANEL_BACKGROUND);
        solverPanel.setBorder(createGroupBorder("Méthode de résolution"));
        solverPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        solverPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        solverSelector = new JComboBox<>(new String[] {
                "Problem 1: Minimizing Weighted Delay",
                "Problem 2: Minimizing Makespan",
                "Problem 3: Minimizing Total Lateness"
        });
        solverSelector.setFont(NORMAL_FONT);

        useAllSolversCheckBox = new JCheckBox("Utiliser tous les solveurs");
        useAllSolversCheckBox.setFont(NORMAL_FONT);
        useAllSolversCheckBox.setBackground(PANEL_BACKGROUND);
        useAllSolversCheckBox.addActionListener(e -> solverSelector.setEnabled(!useAllSolversCheckBox.isSelected()));

        solverPanel.add(solverSelector, BorderLayout.NORTH);
        solverPanel.add(useAllSolversCheckBox, BorderLayout.CENTER);

        // 4. Boutons d'action
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionPanel.setBackground(PANEL_BACKGROUND);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        solveButton = new JButton("Résoudre");
        solveButton.setFont(HEADER_FONT);
        solveButton.setBackground(PRIMARY_COLOR);
        solveButton.setForeground(Color.WHITE);
        solveButton.addActionListener(e -> solveProblem());

        cancelButton = new JButton("Annuler");
        cancelButton.setFont(NORMAL_FONT);
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelSolving());

        actionPanel.add(solveButton);
        actionPanel.add(cancelButton);

        // Ajouter tous les panneaux au panneau de contrôle
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(instancePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(runwayPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(solverPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(actionPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Organisation des composants dans la fenêtre principale
     */
    private void layoutComponents() {
        // Configuration du layout principal
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Ajout des panneaux aux onglets
        mainTabbedPane.addTab("Configuration", null, controlPanel, "Configurer les paramètres de résolution");
        mainTabbedPane.addTab("Résultats", null, resultsPanel, "Afficher et analyser les résultats des solutions");

        // Ajout des onglets et du panneau de statut
        contentPane.add(mainTabbedPane, BorderLayout.CENTER);
        contentPane.add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Crée une bordure avec un titre pour les groupes de paramètres
     */
    private TitledBorder createGroupBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                title);
        border.setTitleFont(HEADER_FONT);
        border.setTitleColor(PRIMARY_COLOR);
        return border;
    }

    /**
     * Charge la liste des instances disponibles dans le dossier 'instances'
     */
    private void loadInstances() {
        instanceFiles.clear();
        instanceSelector.removeAllItems();

        File folder = new File(instancesDirectory);
        if (!folder.exists() || !folder.isDirectory()) {
            showMessage("Le dossier d'instances n'existe pas.", JOptionPane.WARNING_MESSAGE);
            downloadInstancesButton.setEnabled(true);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (files == null || files.length == 0) {
            showMessage("Aucun fichier d'instance trouvé.", JOptionPane.WARNING_MESSAGE);
            downloadInstancesButton.setEnabled(true);
            return;
        }

        for (File file : files) {
            String name = file.getName().replace(".txt", "");
            instanceFiles.put(name, file);
            instanceSelector.addItem(name);
        }

        downloadInstancesButton.setEnabled(false);
    }

    /**
     * Télécharge les instances depuis l'OR-Library
     */
    private void downloadInstances() {
        setStatus("Téléchargement des instances...");
        downloadInstancesButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Créer le répertoire cible s'il n'existe pas
                    Files.createDirectories(Paths.get(instancesDirectory));

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

                    for (int i = 0; i < urls.length; i++) {
                        String url = urls[i];
                        String fileName = url.substring(url.lastIndexOf("/") + 1);
                        String targetPath = instancesDirectory + File.separator + fileName;

                        publish(i + 1);
                        try {
                            downloadFile(url, targetPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue((progress * 100) / 8);
                    progressBar.setString("Téléchargement: " + progress + "/8");
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    boolean success = get();
                    if (success) {
                        showMessage("Téléchargement des instances réussi!", JOptionPane.INFORMATION_MESSAGE);
                        loadInstances();
                    } else {
                        showMessage("Erreur lors du téléchargement des instances.", JOptionPane.ERROR_MESSAGE);
                        downloadInstancesButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    showMessage("Erreur: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
                    downloadInstancesButton.setEnabled(true);
                }
                setStatus("Prêt");
            }
        };

        worker.execute();
    }

    /**
     * Télécharge un fichier depuis une URL
     */
    private void downloadFile(String urlStr, String destinationPath) throws IOException {
        java.net.URL url = new java.net.URL(urlStr);
        try (
                java.io.InputStream is = url.openStream();
                java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels.newChannel(is);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(destinationPath)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    /**
     * Ouvre une boîte de dialogue pour sélectionner un fichier d'instance
     */
    private void browseForInstance() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sélectionner un fichier d'instance");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers texte", "txt"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String name = selectedFile.getName().replace(".txt", "");

            if (!instanceFiles.containsKey(name)) {
                instanceFiles.put(name, selectedFile);
                instanceSelector.addItem(name);
            }

            instanceSelector.setSelectedItem(name);
        }
    }

    /**
     * Lance la résolution du problème avec les paramètres sélectionnés
     */
    private void solveProblem() {
        if (instanceSelector.getSelectedItem() == null) {
            showMessage("Veuillez sélectionner une instance.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String instanceName = (String) instanceSelector.getSelectedItem();
        File instanceFile = instanceFiles.get(instanceName);

        if (instanceFile == null || !instanceFile.exists()) {
            showMessage("Fichier d'instance introuvable: " + instanceName, JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Récupération des paramètres
        List<Integer> runwayCounts = new ArrayList<>();
        if (useAllRunwaysCheckBox.isSelected()) {
            runwayCounts.add(1);
            runwayCounts.add(2);
            runwayCounts.add(3);
        } else {
            runwayCounts.add((Integer) runwaySpinner.getValue());
        }

        List<ALPSolver> selectedSolvers = new ArrayList<>();
        if (useAllSolversCheckBox.isSelected()) {
            selectedSolvers.add(new Problem1Solver());
            selectedSolvers.add(new Problem2Solver());
            selectedSolvers.add(new Problem3Solver());
        } else {
            int solverIndex = solverSelector.getSelectedIndex();
            switch (solverIndex) {
                case 0:
                    selectedSolvers.add(new Problem1Solver());
                    break;
                case 1:
                    selectedSolvers.add(new Problem2Solver());
                    break;
                case 2:
                    selectedSolvers.add(new Problem3Solver());
                    break;
            }
        }

        // Configuration des contrôles
        solveButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        mainTabbedPane.setSelectedIndex(1); // Afficher l'onglet des résultats

        // Nombre total de tâches à effectuer
        final int totalTasks = runwayCounts.size() * selectedSolvers.size();
        final List<ALPSolution> newSolutions = new ArrayList<>();

        // Lancement de la résolution en arrière-plan
        currentWorker = new SwingWorker<List<ALPSolution>, String>() {
            private int completedTasks = 0;

            @Override
            protected List<ALPSolution> doInBackground() throws Exception {
                for (int numRunways : runwayCounts) {
                    for (ALPSolver solver : selectedSolvers) {
                        if (isCancelled()) {
                            return newSolutions;
                        }

                        try {
                            publish("Résolution avec " + solver.getName() + " pour " + numRunways + " piste(s)...");

                            // Lecture de l'instance
                            ALPInstance instance = InstanceReader.readInstance(instanceFile.getPath(), numRunways);

                            // Résolution
                            ALPSolution solution = solver.solve(instance);
                            newSolutions.add(solution);

                            // Mise à jour de la table des résultats
                            SwingUtilities.invokeLater(() -> {
                                Object[] row = {
                                        instanceName,
                                        numRunways,
                                        solver.getName(),
                                        String.format("%.2f", solution.getObjectiveValue()),
                                        String.format("%.2f", solution.getSolveTime()),
                                        "Réussi"
                                };
                                solutionsTableModel.addRow(row);
                            });

                            completedTasks++;
                            setProgress((completedTasks * 100) / totalTasks);

                        } catch (Exception e) {
                            e.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                Object[] row = {
                                        instanceName,
                                        numRunways,
                                        solver.getName(),
                                        "-",
                                        "-",
                                        "Erreur: " + e.getMessage()
                                };
                                solutionsTableModel.addRow(row);
                            });

                            completedTasks++;
                            setProgress((completedTasks * 100) / totalTasks);
                            publish("Erreur lors de la résolution: " + e.getMessage());
                        }
                    }
                }

                return newSolutions;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String message = chunks.get(chunks.size() - 1);
                    setStatus(message);
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                solveButton.setEnabled(true);
                cancelButton.setEnabled(false);

                try {
                    List<ALPSolution> results = get();
                    solutions.addAll(results);

                    compareButton.setEnabled(solutions.size() > 1);
                    setStatus("Résolution terminée. " + results.size() + " solution(s) générée(s).");

                    if (!results.isEmpty()) {
                        // Sélectionner automatiquement la première ligne du tableau
                        if (solutionsTable.getRowCount() > 0) {
                            solutionsTable.setRowSelectionInterval(0, 0);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    if (!(e.getCause() instanceof InterruptedException)) {
                        showMessage("Erreur lors de la résolution: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                    setStatus("Résolution annulée");
                }
            }
        };

        // Configuration du suivi de la progression
        currentWorker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int progress = (Integer) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");
            }
        });

        // Lancement du traitement
        currentWorker.execute();
    }

    /**
     * Annule la résolution en cours
     */
    private void cancelSolving() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
    }

    /**
     * Visualise la solution actuellement sélectionnée
     */
    private void visualizeSelectedSolution() {
        int selectedRow = solutionsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        // Convertir l'index de la vue vers l'index du modèle
        int modelRow = solutionsTable.convertRowIndexToModel(selectedRow);

        if (modelRow >= 0 && modelRow < solutions.size()) {
            ALPSolution solution = solutions.get(modelRow);
            ScheduleVisualizer.visualizeSchedule(solution);
        }
    }

    /**
     * Affiche une interface de comparaison des solutions
     */
    private void compareSolutions() {
        if (solutions.size() < 2) {
            showMessage("Il faut au moins deux solutions pour faire une comparaison.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Créer une nouvelle fenêtre pour la comparaison
        JFrame compareFrame = new JFrame("Comparaison des solutions");
        compareFrame.setSize(900, 600);
        compareFrame.setLocationRelativeTo(this);
        compareFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Panneau pour les visualisations côte à côte
        JPanel comparisonPanel = new JPanel(new GridLayout(1, 2));
        comparisonPanel.setBackground(PANEL_BACKGROUND);

        // Sélecteurs de solutions
        JComboBox<String> solution1Selector = new JComboBox<>();
        JComboBox<String> solution2Selector = new JComboBox<>();

        for (int i = 0; i < solutions.size(); i++) {
            ALPSolution solution = solutions.get(i);
            String desc = solution.getInstance().getInstanceName() + " - " +
                    solution.getInstance().getNumRunways() + " piste(s) - " +
                    solution.getProblemVariant();

            solution1Selector.addItem(desc);
            solution2Selector.addItem(desc);
        }

        if (solutions.size() > 1) {
            solution2Selector.setSelectedIndex(1);
        }

        // Créer les visualisations initiales
        JPanel viz1 = createMiniVisualization(solutions.get(0));
        JPanel viz2 = createMiniVisualization(solutions.get(solution2Selector.getSelectedIndex()));

        comparisonPanel.add(viz1);
        comparisonPanel.add(viz2);

        // Panneau de contrôle pour la comparaison
        JPanel controlsPanel = new JPanel(new GridLayout(1, 2));

        JPanel leftControls = new JPanel(new FlowLayout());
        leftControls.setBackground(PANEL_BACKGROUND);
        leftControls.add(new JLabel("Solution 1:"));
        leftControls.add(solution1Selector);

        JPanel rightControls = new JPanel(new FlowLayout());
        rightControls.setBackground(PANEL_BACKGROUND);
        rightControls.add(new JLabel("Solution 2:"));
        rightControls.add(solution2Selector);

        controlsPanel.add(leftControls);
        controlsPanel.add(rightControls);

        // Actions lors de la sélection
        solution1Selector.addActionListener(e -> {
            int index = solution1Selector.getSelectedIndex();
            if (index >= 0 && index < solutions.size()) {
                comparisonPanel.remove(0);
                comparisonPanel.add(createMiniVisualization(solutions.get(index)), 0);
                compareFrame.revalidate();
                compareFrame.repaint();
            }
        });

        solution2Selector.addActionListener(e -> {
            int index = solution2Selector.getSelectedIndex();
            if (index >= 0 && index < solutions.size()) {
                comparisonPanel.remove(1);
                comparisonPanel.add(createMiniVisualization(solutions.get(index)));
                compareFrame.revalidate();
                compareFrame.repaint();
            }
        });

        // Assemblage de l'interface
        compareFrame.setLayout(new BorderLayout());
        compareFrame.add(comparisonPanel, BorderLayout.CENTER);
        compareFrame.add(controlsPanel, BorderLayout.SOUTH);

        compareFrame.setVisible(true);
    }

    /**
     * Crée une visualisation compacte d'une solution pour la comparaison
     */
    private JPanel createMiniVisualization(ALPSolution solution) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR),
                solution.getProblemVariant(),
                TitledBorder.CENTER,
                TitledBorder.TOP,
                HEADER_FONT,
                PRIMARY_COLOR));

        // Informations de base sur la solution
        JPanel infoPanel = new JPanel(new GridLayout(0, 2));
        infoPanel.setBackground(PANEL_BACKGROUND);

        addInfoRow(infoPanel, "Instance:", solution.getInstance().getInstanceName());
        addInfoRow(infoPanel, "Pistes:", String.valueOf(solution.getInstance().getNumRunways()));
        addInfoRow(infoPanel, "Avions:", String.valueOf(solution.getInstance().getNumAircraft()));
        addInfoRow(infoPanel, "Objectif:", String.format("%.2f", solution.getObjectiveValue()));
        addInfoRow(infoPanel, "Temps:", String.format("%.2f s", solution.getSolveTime()));

        // Créer une version simplifiée de la timeline
        MiniSchedulePanel schedulePanel = new MiniSchedulePanel(solution);

        // Bouton pour voir la visualisation complète
        JButton viewFullButton = new JButton("Visualisation complète");
        viewFullButton.setFont(NORMAL_FONT);
        viewFullButton.addActionListener(e -> ScheduleVisualizer.visualizeSchedule(solution));

        // Assemblage du panneau
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(schedulePanel), BorderLayout.CENTER);
        panel.add(viewFullButton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Ajoute une ligne d'information à un panneau
     */
    private void addInfoRow(JPanel panel, String label, String value) {
        JLabel lblField = new JLabel(label);
        lblField.setFont(NORMAL_FONT);
        lblField.setForeground(Color.DARK_GRAY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(NORMAL_FONT.deriveFont(Font.BOLD));

        panel.add(lblField);
        panel.add(lblValue);
    }

    /**
     * Efface toutes les solutions et réinitialise la table
     */
    private void clearSolutions() {
        solutions.clear();
        solutionsTableModel.setRowCount(0);
        visualizeButton.setEnabled(false);
        compareButton.setEnabled(false);
        setStatus("Toutes les solutions ont été effacées.");
    }

    /**
     * Affiche un message dans une boîte de dialogue
     */
    private void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, "Aircraft Landing Dashboard", messageType);
    }

    /**
     * Met à jour le message de statut
     */
    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Panneau simplifié pour l'affichage compact d'une timeline
     */
    private class MiniSchedulePanel extends JPanel {
        private ALPSolution solution;
        private int margin = 30;
        private int barHeight = 20;
        private int spacing = 10;
        private int timeScale = 3;

        public MiniSchedulePanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(PANEL_BACKGROUND);

            int numRunways = solution.getInstance().getNumRunways();
            int maxLandingTime = findMaxLandingTime();

            int preferredWidth = margin + maxLandingTime * timeScale + 50;
            int preferredHeight = margin + numRunways * (barHeight + spacing) + margin;

            setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        }

        private int findMaxLandingTime() {
            int max = 0;
            for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
                max = Math.max(max, solution.getLandingTime(i));
            }
            return max;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ALPInstance instance = solution.getInstance();
            int numRunways = instance.getNumRunways();
            int numAircraft = instance.getNumAircraft();
            int maxLandingTime = findMaxLandingTime();

            // Dessiner les pistes
            for (int i = 0; i < numRunways; i++) {
                int y = margin + i * (barHeight + spacing);
                g2d.setColor(GRID_COLOR);
                g2d.fillRect(margin, y, getWidth() - margin * 2, barHeight);

                g2d.setColor(PRIMARY_COLOR);
                g2d.drawString("P" + (i + 1), 5, y + barHeight - 5);
            }

            // Dessiner les avions
            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);

                int x = margin + landingTime * timeScale;
                int y = margin + runway * (barHeight + spacing);

                g2d.setColor(new Color(60, 90, 180, 180));
                g2d.fillOval(x - 5, y + 2, barHeight - 4, barHeight - 4);
            }

            // Dessiner l'axe du temps
            g2d.setColor(TIME_AXIS_COLOR);
            int totalHeight = margin + numRunways * (barHeight + spacing);
            g2d.drawLine(margin, totalHeight, margin + maxLandingTime * timeScale, totalHeight);

            // Quelques marqueurs de temps
            for (int t = 0; t <= maxLandingTime; t += maxLandingTime / 5) {
                int x = margin + t * timeScale;
                g2d.drawLine(x, totalHeight, x, totalHeight + 5);
                g2d.drawString(String.valueOf(t), x - 5, totalHeight + 15);
            }
        }
    }

    /**
     * Point d'entrée du programme
     */
    public static void main(String[] args) {
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