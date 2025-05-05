package alp.visualization;

import alp.io.InstanceReader;
import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.solver.ALPSolver;
import alp.solver.Problem1Solver;
import alp.solver.Problem2Solver;
import alp.solver.Problem3Solver;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    // Constantes pour l'interface - Thème moderne
    private static final String TITLE = "Aircraft Landing Problem - Tableau de Bord";

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
    private JButton clearButton;
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
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Création d'une ImageIcon à partir de l'icône personnalisée
        ImageIcon icon = new ImageIcon(createAirplaneIconImage(32, UIUtils.PRIMARY_COLOR));
        setIconImage(icon.getImage());

        // Configuration du look and feel moderne
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            applyModernLookAndFeel();
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
     * Crée une image d'icône d'avion compatible avec ImageIcon
     */
    private Image createAirplaneIconImage(int size, Color color) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(color);

        // Corps de l'avion
        Path2D path = new Path2D.Float();
        path.moveTo(size / 2f, size * 0.2f);
        path.lineTo(size * 0.8f, size * 0.5f);
        path.lineTo(size / 2f, size * 0.8f);
        path.lineTo(size * 0.2f, size * 0.5f);
        path.closePath();
        g2d.fill(path);

        // Ailes
        Path2D wings = new Path2D.Float();
        wings.moveTo(size * 0.3f, size * 0.45f);
        wings.lineTo(size * 0.7f, size * 0.45f);
        wings.lineTo(size * 0.7f, size * 0.55f);
        wings.lineTo(size * 0.3f, size * 0.55f);
        wings.closePath();
        g2d.fill(wings);

        // Queue
        Path2D tail = new Path2D.Float();
        tail.moveTo(size * 0.45f, size * 0.25f);
        tail.lineTo(size * 0.55f, size * 0.25f);
        tail.lineTo(size * 0.5f, size * 0.15f);
        tail.closePath();
        g2d.fill(tail);

        g2d.dispose();
        return image;
    }

    /**
     * Applique un style moderne aux composants Swing
     */
    private void applyModernLookAndFeel() {
        // Customisation des composants UI pour un look plus moderne
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ProgressBar.selectionBackground", Color.WHITE);
        UIManager.put("ProgressBar.foreground", UIUtils.PRIMARY_COLOR);
        UIManager.put("ProgressBar.background", UIUtils.BACKGROUND_COLOR);

        // Tables avec des lignes alternées
        UIManager.put("Table.alternateRowColor", new Color(245, 247, 250));
        UIManager.put("Table.selectionBackground", new Color(UIUtils.PRIMARY_COLOR.getRed(),
                UIUtils.PRIMARY_COLOR.getGreen(),
                UIUtils.PRIMARY_COLOR.getBlue(), 80));
        UIManager.put("Table.selectionForeground", UIUtils.TEXT_PRIMARY);

        // Onglets plus modernes
        UIManager.put("TabbedPane.selected", UIUtils.PANEL_BACKGROUND);
        UIManager.put("TabbedPane.background", UIUtils.BACKGROUND_COLOR);
        UIManager.put("TabbedPane.contentAreaColor", UIUtils.PANEL_BACKGROUND);

        // Boutons et contrôles
        UIManager.put("Button.select", UIUtils.PRIMARY_COLOR.darker());
        UIManager.put("ComboBox.selectionBackground", UIUtils.PRIMARY_COLOR);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        // ScrollBars plus fines et modernes
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.track", UIUtils.BACKGROUND_COLOR);
        UIManager.put("ScrollBar.thumb", new ColorUIResource(UIUtils.TEXT_SECONDARY));
    }

    /**
     * Initialisation des composants de l'interface
     */
    private void initComponents() {
        // Onglets principaux
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(UIUtils.HEADER_FONT);
        mainTabbedPane.setBackground(UIUtils.PANEL_BACKGROUND);
        mainTabbedPane.setBorder(BorderFactory.createEmptyBorder());

        // Panneau de contrôle
        controlPanel = createControlPanel();

        // Panneau de résultats
        resultsPanel = new JPanel(new BorderLayout(UIUtils.SPACING, UIUtils.SPACING));
        resultsPanel.setBackground(UIUtils.PANEL_BACKGROUND);
        resultsPanel.setBorder(
                BorderFactory.createEmptyBorder(UIUtils.SPACING, UIUtils.SPACING, UIUtils.SPACING, UIUtils.SPACING));

        // En-tête du panneau de résultats
        JPanel resultsHeaderPanel = new JPanel(new BorderLayout(UIUtils.SPACING, 0));
        resultsHeaderPanel.setOpaque(false);

        JLabel resultsTitle = new JLabel("Résultats et Solutions");
        resultsTitle.setFont(UIUtils.SUBHEADER_FONT);
        resultsTitle.setForeground(UIUtils.PRIMARY_COLOR);
        resultsHeaderPanel.add(resultsTitle, BorderLayout.WEST);

        JPanel resultActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        resultActionsPanel.setOpaque(false);

        visualizeButton = UIUtils.createSecondaryButton("Visualiser", () -> visualizeSelectedSolution());
        visualizeButton.setIcon(UIUtils.createVisualizeIcon(16, UIUtils.PRIMARY_COLOR));
        visualizeButton.setEnabled(false);

        compareButton = UIUtils.createSecondaryButton("Comparer", () -> compareSolutions());
        compareButton.setIcon(UIUtils.createCompareIcon(16, UIUtils.PRIMARY_COLOR));
        compareButton.setEnabled(false);

        clearButton = UIUtils.createSecondaryButton("Effacer", () -> clearSolutions());
        clearButton.setIcon(UIUtils.createClearIcon(16, UIUtils.PRIMARY_COLOR));

        resultActionsPanel.add(visualizeButton);
        resultActionsPanel.add(compareButton);
        resultActionsPanel.add(clearButton);

        resultsHeaderPanel.add(resultActionsPanel, BorderLayout.EAST);
        resultsPanel.add(resultsHeaderPanel, BorderLayout.NORTH);

        // Tableau des solutions
        String[] columnNames = { "Instance", "Pistes", "Solveur", "Objectif", "Temps (s)", "Statut" };
        solutionsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3 || columnIndex == 4) {
                    return Double.class;
                }
                return String.class;
            }
        };

        solutionsTable = new JTable(solutionsTableModel);
        solutionsTable.setFont(UIUtils.NORMAL_FONT);
        solutionsTable.setRowHeight(35);
        solutionsTable.setShowGrid(false);
        solutionsTable.setIntercellSpacing(new Dimension(0, 0));
        solutionsTable.setFillsViewportHeight(true);
        solutionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        solutionsTable.setAutoCreateRowSorter(true);

        // En-tête du tableau avec style
        JTableHeader header = solutionsTable.getTableHeader();
        header.setFont(UIUtils.SUBHEADER_FONT);
        header.setBackground(UIUtils.PANEL_BACKGROUND);
        header.setForeground(UIUtils.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.GRID_COLOR));

        // Rendu personnalisé pour chaque colonne
        solutionsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);

                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                if (col == 5) { // Status column
                    String status = (String) value;
                    if (status.contains("Optimal")) {
                        label.setForeground(UIUtils.SUCCESS_COLOR);
                    } else if (status.contains("Error") || status.contains("Failed")) {
                        label.setForeground(UIUtils.ERROR_COLOR);
                    } else if (status.contains("Running")) {
                        label.setForeground(UIUtils.WARNING_COLOR);
                    } else {
                        label.setForeground(UIUtils.TEXT_PRIMARY);
                    }
                } else {
                    label.setForeground(UIUtils.TEXT_PRIMARY);
                }

                // Alternate row colors
                if (!isSelected) {
                    label.setBackground(row % 2 == 0 ? UIUtils.PANEL_BACKGROUND : new Color(248, 250, 253));
                }

                return label;
            }
        });

        // Style pour les colonnes numériques
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        solutionsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Objectif
        solutionsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Temps

        // ScrollPane avec coins arrondis
        JScrollPane scrollPane = new JScrollPane(solutionsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIUtils.PANEL_BACKGROUND);

        JPanel tablePanel = new RoundedPanel(UIUtils.PANEL_RADIUS);
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(scrollPane);
        resultsPanel.add(tablePanel, BorderLayout.CENTER);

        // Panneau de statut
        statusPanel = new JPanel(new BorderLayout(UIUtils.SPACING, 0));
        statusPanel.setBackground(UIUtils.PANEL_BACKGROUND);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        statusLabel = new JLabel("Prêt");
        statusLabel.setFont(UIUtils.NORMAL_FONT);
        statusLabel.setForeground(UIUtils.TEXT_PRIMARY);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 20));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);

        // Ajouter un effet de hover sur les lignes du tableau
        solutionsTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int row = solutionsTable.rowAtPoint(p);

                if (row >= 0) {
                    if (solutionsTable.getSelectedRow() != row) {
                        solutionsTable.setRowSelectionInterval(row, row);
                    }
                } else {
                    solutionsTable.clearSelection();
                }
            }
        });

        // Ajouter un écouteur de sélection pour activer/désactiver les boutons
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
        JPanel panel = UIUtils.createRoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(
                BorderFactory.createEmptyBorder(UIUtils.SPACING, UIUtils.SPACING, UIUtils.SPACING, UIUtils.SPACING));

        // En-tête avec titre
        JLabel titleLabel = new JLabel("Configuration du problème");
        titleLabel.setFont(UIUtils.HEADER_FONT);
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Carte pour la sélection d'instance
        JPanel instanceCard = createCardPanel("Instance d'avions");

        // Panel pour la sélection et téléchargement
        JPanel instanceSelectionPanel = new JPanel(new BorderLayout(10, 0));
        instanceSelectionPanel.setOpaque(false);

        instanceSelector = new JComboBox<>();
        instanceSelector.setFont(UIUtils.NORMAL_FONT);
        instanceSelector.setPreferredSize(new Dimension(200, 30));
        instanceSelectionPanel.add(instanceSelector, BorderLayout.CENTER);

        JPanel instanceButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        instanceButtonsPanel.setOpaque(false);

        JButton browseButton = UIUtils.createSecondaryButton("Parcourir", () -> browseForInstance());

        downloadInstancesButton = UIUtils.createSecondaryButton("Télécharger", () -> downloadInstances());
        downloadInstancesButton.setIcon(UIUtils.createDownloadIcon(16, UIUtils.PRIMARY_COLOR));

        instanceButtonsPanel.add(browseButton);
        instanceButtonsPanel.add(downloadInstancesButton);
        instanceSelectionPanel.add(instanceButtonsPanel, BorderLayout.EAST);

        instanceCard.add(instanceSelectionPanel);
        panel.add(instanceCard);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Carte pour les pistes d'atterrissage
        JPanel runwayCard = createCardPanel("Pistes d'atterrissage");

        JPanel runwayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        runwayPanel.setOpaque(false);

        JLabel runwayLabel = new JLabel("Nombre de pistes:");
        runwayLabel.setFont(UIUtils.NORMAL_FONT);
        runwayPanel.add(runwayLabel);

        SpinnerNumberModel runwayModel = new SpinnerNumberModel(3, 1, 10, 1);
        runwaySpinner = new JSpinner(runwayModel);
        runwaySpinner.setFont(UIUtils.NORMAL_FONT);
        JComponent editor = runwaySpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setColumns(2);
        }
        runwayPanel.add(runwaySpinner);

        useAllRunwaysCheckBox = new JCheckBox("Tester plusieurs configurations de pistes");
        useAllRunwaysCheckBox.setFont(UIUtils.NORMAL_FONT);
        useAllRunwaysCheckBox.setOpaque(false);
        runwayPanel.add(useAllRunwaysCheckBox);

        runwayCard.add(runwayPanel);
        panel.add(runwayCard);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Carte pour la sélection de solveur
        JPanel solverCard = createCardPanel("Méthode de résolution");

        JPanel solverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        solverPanel.setOpaque(false);

        JLabel solverLabel = new JLabel("Solveur:");
        solverLabel.setFont(UIUtils.NORMAL_FONT);
        solverPanel.add(solverLabel);

        solverSelector = new JComboBox<>(new String[] {
                "Problem 1 (Délai pondéré)",
                "Problem 2 (Makespan)",
                "Problem 3 (Temps d'arrivée)"
        });
        solverSelector.setFont(UIUtils.NORMAL_FONT);
        solverSelector.setPreferredSize(new Dimension(200, 30));
        solverPanel.add(solverSelector);

        useAllSolversCheckBox = new JCheckBox("Tester tous les solveurs");
        useAllSolversCheckBox.setFont(UIUtils.NORMAL_FONT);
        useAllSolversCheckBox.setOpaque(false);
        solverPanel.add(useAllSolversCheckBox);

        solverCard.add(solverPanel);
        panel.add(solverCard);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Boutons d'action
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        solveButton = UIUtils.createPrimaryButton("Résoudre", () -> solveProblem());
        solveButton.setIcon(UIUtils.createPlayIcon(16, Color.WHITE));

        cancelButton = UIUtils.createSecondaryButton("Annuler", () -> cancelSolving());
        cancelButton.setIcon(UIUtils.createPauseIcon(16, UIUtils.TEXT_PRIMARY));
        cancelButton.setEnabled(false);

        actionPanel.add(solveButton);
        actionPanel.add(cancelButton);

        panel.add(actionPanel);

        return panel;
    }

    /**
     * Crée un panneau avec un style de carte
     */
    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(UIUtils.PANEL_BACKGROUND);
        panel.setBorder(createGroupBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 10));

        panel.add(contentPanel, BorderLayout.CENTER);

        return contentPanel;
    }

    /**
     * Organisation des composants dans la fenêtre principale
     */
    private void layoutComponents() {
        // Créer le panneau principal avec un BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIUtils.BACKGROUND_COLOR);

        // Ajouter un panneau pour les contrôles à gauche
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(UIUtils.SPACING, UIUtils.SPACING, UIUtils.SPACING,
                UIUtils.SPACING / 2));
        leftPanel.add(controlPanel, BorderLayout.NORTH);

        // Ajouter un panneau pour les résultats à droite
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(UIUtils.SPACING, UIUtils.SPACING / 2, UIUtils.SPACING,
                UIUtils.SPACING));
        rightPanel.add(resultsPanel, BorderLayout.CENTER);

        // Ajouter les panneaux au SplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(350);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setBackground(UIUtils.BACKGROUND_COLOR);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Ajouter le panneau principal à la fenêtre
        setContentPane(mainPanel);
    }

    /**
     * Crée une bordure avec un titre pour les groupes de paramètres
     */
    private TitledBorder createGroupBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.GRID_COLOR, 1, true),
                title);
        border.setTitleFont(UIUtils.SUBHEADER_FONT);
        border.setTitleColor(UIUtils.PRIMARY_COLOR);
        border.setTitleJustification(TitledBorder.LEFT);
        border.setTitlePosition(TitledBorder.TOP);
        return border;
    }

    /**
     * Charge la liste des instances disponibles dans le dossier 'instances'
     */
    private void loadInstances() {
        instanceFiles.clear();
        instanceSelector.removeAllItems();

        File instanceDir = new File(instancesDirectory);
        if (!instanceDir.exists()) {
            instanceDir.mkdirs();
        }

        File[] files = instanceDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (files != null && files.length > 0) {
            for (File file : files) {
                String name = file.getName().replace(".txt", "");
                instanceFiles.put(name, file);
                instanceSelector.addItem(name);
            }
            instanceSelector.setSelectedIndex(0);
            solveButton.setEnabled(true);
        } else {
            instanceSelector.addItem("Aucune instance disponible");
            solveButton.setEnabled(false);
        }
    }

    /**
     * Télécharge les instances depuis l'OR-Library
     */
    private void downloadInstances() {
        setStatus("Téléchargement des instances...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        // Désactiver le bouton pendant le téléchargement
        downloadInstancesButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // S'assurer que le répertoire existe
                Files.createDirectories(Paths.get(instancesDirectory));

                // URL de base pour les instances
                String baseUrl = "http://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/";

                // Télécharger les 8 instances
                for (int i = 1; i <= 8; i++) {
                    String filename = "airland" + i + ".txt";
                    downloadFile(baseUrl + filename, instancesDirectory + "/" + filename);
                }

                return null;
            }

            @Override
            protected void done() {
                downloadInstancesButton.setEnabled(true);
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);

                try {
                    get(); // Check for exceptions
                    setStatus("Instances téléchargées avec succès.");
                    loadInstances(); // Rafraîchir la liste
                } catch (Exception e) {
                    setStatus("Erreur lors du téléchargement: " + e.getMessage());
                    showMessage("Erreur lors du téléchargement des instances.\n" + e.getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * Télécharge un fichier depuis une URL
     */
    private void downloadFile(String urlStr, String destinationPath) throws IOException {
        URL url = new URL(urlStr);
        Path destination = Paths.get(destinationPath);

        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
            Files.copy(
                    Channels.newInputStream(rbc),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);
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

            // Copier le fichier dans le dossier instances
            try {
                Files.createDirectories(Paths.get(instancesDirectory));
                Files.copy(
                        selectedFile.toPath(),
                        Paths.get(instancesDirectory, selectedFile.getName()),
                        StandardCopyOption.REPLACE_EXISTING);

                loadInstances(); // Rafraîchir la liste

                // Sélectionner l'instance ajoutée
                String name = selectedFile.getName().replace(".txt", "");
                instanceSelector.setSelectedItem(name);

            } catch (IOException e) {
                showMessage("Erreur lors de la copie du fichier:\n" + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Lance la résolution du problème avec les paramètres sélectionnés
     */
    private void solveProblem() {
        // Vérifier qu'une instance est sélectionnée
        if (instanceSelector.getSelectedIndex() < 0 ||
                instanceSelector.getSelectedItem().toString().contains("Aucune")) {
            showMessage("Veuillez sélectionner une instance valide.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Configurer l'interface pour la résolution
        solveButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        setStatus("Résolution en cours...");

        // Créer et lancer le worker
        currentWorker = new SwingWorker<List<ALPSolution>, String>() {
            @Override
            protected List<ALPSolution> doInBackground() throws Exception {
                List<ALPSolution> results = new ArrayList<>();

                // Charger l'instance
                String instanceName = instanceSelector.getSelectedItem().toString();
                File instanceFile = instanceFiles.get(instanceName);

                // Déterminer les configurations de pistes à tester
                int numRunways = ((Number) runwaySpinner.getValue()).intValue();
                List<Integer> runwayConfigs = new ArrayList<>();
                if (useAllRunwaysCheckBox.isSelected()) {
                    // Tester de 1 à numRunways pistes
                    for (int r = 1; r <= numRunways; r++) {
                        runwayConfigs.add(r);
                    }
                } else {
                    // Utiliser juste le nombre spécifié
                    runwayConfigs.add(numRunways);
                }

                // Déterminer les solveurs à utiliser
                List<Integer> solverIndices = new ArrayList<>();
                if (useAllSolversCheckBox.isSelected()) {
                    solverIndices.add(0); // Problem 1
                    solverIndices.add(1); // Problem 2
                    solverIndices.add(2); // Problem 3
                } else {
                    solverIndices.add(solverSelector.getSelectedIndex());
                }

                for (int r : runwayConfigs) {
                    // Charger l'instance avec le bon nombre de pistes
                    ALPInstance instance = InstanceReader.readInstance(instanceFile.getAbsolutePath(), r);

                    publish("Configuration avec " + r + " pistes...");

                    // Pour chaque solveur...
                    for (int solverIdx : solverIndices) {
                        if (isCancelled()) {
                            return results;
                        }

                        ALPSolver solver;
                        switch (solverIdx) {
                            case 0:
                                publish("Résolution avec Problem1Solver (délai pondéré)...");
                                solver = new Problem1Solver();
                                break;
                            case 1:
                                publish("Résolution avec Problem2Solver (makespan)...");
                                solver = new Problem2Solver();
                                break;
                            case 2:
                                publish("Résolution avec Problem3Solver (temps d'arrivée)...");
                                solver = new Problem3Solver();
                                break;
                            default:
                                solver = new Problem1Solver();
                        }

                        try {
                            ALPSolution solution = solver.solve(instance);
                            results.add(solution);
                            publish("Solution trouvée avec objectif: " + solution.getObjectiveValue());
                        } catch (Exception e) {
                            publish("Erreur: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                return results;
            }

            @Override
            protected void process(List<String> chunks) {
                // Mettre à jour le statut avec les messages
                for (String msg : chunks) {
                    setStatus(msg);
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                solveButton.setEnabled(true);
                cancelButton.setEnabled(false);

                try {
                    List<ALPSolution> results = get();
                    setStatus("Résolution terminée. " + results.size() + " solutions trouvées.");

                    // Ajouter les solutions à la liste et à la table
                    for (ALPSolution solution : results) {
                        addSolutionToTable(solution);
                    }

                    solutions.addAll(results);

                } catch (InterruptedException | ExecutionException e) {
                    if (!(e.getCause() instanceof InterruptedException)) {
                        setStatus("Erreur lors de la résolution: " + e.getMessage());
                        showMessage("Erreur lors de la résolution:\n" + e.getMessage(), JOptionPane.ERROR_MESSAGE);
                    } else {
                        setStatus("Résolution annulée.");
                    }
                }

                // Activer le bouton de comparaison si on a plusieurs solutions
                compareButton.setEnabled(solutionsTable.getRowCount() > 1);
            }

        };

        currentWorker.execute();
    }

    /**
     * Ajoute une solution à la table des résultats
     */
    private void addSolutionToTable(ALPSolution solution) {
        String instanceName = solution.getInstance().getInstanceName();
        int numRunways = solution.getInstance().getNumRunways();
        String solverName = solution.getProblemVariant();
        double objective = solution.getObjectiveValue();
        double time = solution.getSolveTime();
        String status = "Optimal";

        solutionsTableModel.addRow(new Object[] {
                instanceName, numRunways, solverName, objective, time, status
        });
    }

    /**
     * Annule la résolution en cours
     */
    private void cancelSolving() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            setStatus("Annulation de la résolution...");
        }
    }

    /**
     * Visualise la solution actuellement sélectionnée
     */
    private void visualizeSelectedSolution() {
        int selectedRow = solutionsTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Convertir l'index de la vue à celui du modèle (pour le tri)
            int modelRow = solutionsTable.convertRowIndexToModel(selectedRow);

            // Récupérer les données de la ligne sélectionnée pour identifier la solution
            String instanceName = solutionsTableModel.getValueAt(modelRow, 0).toString();
            int numRunways = Integer.parseInt(solutionsTableModel.getValueAt(modelRow, 1).toString());
            String solverName = solutionsTableModel.getValueAt(modelRow, 2).toString();

            // Trouver la solution correspondante dans la liste
            for (ALPSolution solution : solutions) {
                if (solution.getInstance().getInstanceName().equals(instanceName) &&
                        solution.getInstance().getNumRunways() == numRunways &&
                        solution.getProblemVariant().equals(solverName)) {

                    // Visualiser la solution trouvée
                    ScheduleVisualizer.visualizeSchedule(solution);
                    return;
                }
            }

            // Si aucune solution correspondante n'est trouvée
            showMessage("Impossible de trouver la solution correspondante.", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Affiche une interface de comparaison des solutions
     */
    private void compareSolutions() {
        if (solutions.size() < 2) {
            showMessage("Il faut au moins deux solutions pour effectuer une comparaison.",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Créer une fenêtre de dialogue pour la comparaison
        JDialog compareDialog = new JDialog(this, "Comparaison de Solutions", true);
        compareDialog.setSize(950, 650);
        compareDialog.setLocationRelativeTo(this);

        // Créer un panneau principal avec une grille de solutions
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // En-tête
        JLabel titleLabel = new JLabel("Comparaison des solutions");
        titleLabel.setFont(UIUtils.TITLE_FONT);
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Liste des solutions
        JPanel solutionsPanel = new JPanel(new GridLayout(0, 2, 15, 15));
        solutionsPanel.setBackground(UIUtils.BACKGROUND_COLOR);

        for (ALPSolution solution : solutions) {
            JPanel solutionPanel = createMiniVisualization(solution);
            solutionsPanel.add(solutionPanel);
        }

        JScrollPane scrollPane = new JScrollPane(solutionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bouton de fermeture
        JButton closeButton = UIUtils.createPrimaryButton("Fermer", null);
        closeButton.addActionListener(e -> compareDialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        compareDialog.add(mainPanel);
        compareDialog.setVisible(true);
    }

    /**
     * Crée une visualisation compacte d'une solution pour la comparaison
     */
    private JPanel createMiniVisualization(ALPSolution solution) {
        JPanel panel = UIUtils.createRoundedPanel();
        panel.setLayout(new BorderLayout(5, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // En-tête
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(solution.getInstance().getInstanceName() +
                " - " + solution.getProblemVariant());
        titleLabel.setFont(UIUtils.SUBHEADER_FONT);
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel objectiveLabel = new JLabel("Objectif: " + String.format("%.2f", solution.getObjectiveValue()));
        objectiveLabel.setFont(UIUtils.NORMAL_FONT.deriveFont(Font.BOLD));
        headerPanel.add(objectiveLabel, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Visualisation simplifiée
        MiniSchedulePanel schedulePanel = new MiniSchedulePanel(solution);
        schedulePanel.setPreferredSize(new Dimension(400, 150));
        panel.add(schedulePanel, BorderLayout.CENTER);

        // Informations détaillées
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        addInfoRow(infoPanel, "Avions", String.valueOf(solution.getInstance().getNumAircraft()));
        addInfoRow(infoPanel, "Pistes", String.valueOf(solution.getInstance().getNumRunways()));
        addInfoRow(infoPanel, "Temps de calcul", String.format("%.2f s", solution.getSolveTime()));

        // Stats spécifiques au type de problème
        if (solution.getProblemVariant().contains("Problem 1")) {
            // Calcul du délai total
            double totalDelay = 0;
            for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
                int landingTime = solution.getLandingTime(i);
                int targetTime = solution.getInstance().getAircraft().get(i).getTargetLandingTime();
                int deviation = landingTime - targetTime;

                if (deviation < 0) {
                    totalDelay += solution.getInstance().getAircraft().get(i).getEarlyPenalty() * Math.abs(deviation);
                } else if (deviation > 0) {
                    totalDelay += solution.getInstance().getAircraft().get(i).getLatePenalty() * deviation;
                }
            }

            addInfoRow(infoPanel, "Délai pondéré", String.format("%.2f", totalDelay));
        } else if (solution.getProblemVariant().contains("Problem 2")) {
            // Calcul du makespan
            int makespan = 0;
            for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
                makespan = Math.max(makespan, solution.getLandingTime(i));
            }

            addInfoRow(infoPanel, "Makespan", String.valueOf(makespan));
        } else if (solution.getProblemVariant().contains("Problem 3")) {
            // Calcul du temps d'arrivée moyen
            double totalArrivalTime = 0;
            for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
                int landingTime = solution.getLandingTime(i);
                int transferTime = solution.getInstance().getAircraft().get(i)
                        .getTransferTime(solution.getRunwayAssignment(i));

                totalArrivalTime += landingTime + transferTime;
            }

            double avgArrival = totalArrivalTime / solution.getInstance().getNumAircraft();
            addInfoRow(infoPanel, "Temps d'arrivée moyen", String.format("%.2f", avgArrival));
        }

        // Ajouter les infos au panneau
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(infoPanel, BorderLayout.CENTER);

        // Bouton pour visualiser cette solution spécifique
        JButton visualizeButton = UIUtils.createSecondaryButton("Visualiser", () -> {
            ScheduleVisualizer.visualizeSchedule(solution);
        });
        visualizeButton.setIcon(UIUtils.createVisualizeIcon(16, UIUtils.PRIMARY_COLOR));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(visualizeButton);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Ajoute une ligne d'information à un panneau
     */
    private void addInfoRow(JPanel panel, String label, String value) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setOpaque(false);

        JLabel labelComponent = new JLabel(label + ":");
        labelComponent.setFont(UIUtils.NORMAL_FONT);
        labelComponent.setForeground(UIUtils.TEXT_SECONDARY);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(UIUtils.NORMAL_FONT);
        valueComponent.setForeground(UIUtils.TEXT_PRIMARY);

        rowPanel.add(labelComponent, BorderLayout.WEST);
        rowPanel.add(valueComponent, BorderLayout.EAST);

        panel.add(rowPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
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
        JOptionPane.showMessageDialog(this, message, "Aircraft Landing Problem", messageType);
    }

    /**
     * Met à jour le message de statut
     */
    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Panneau avec coins arrondis pour un style moderne
     */
    private class RoundedPanel extends JPanel {
        private int cornerRadius;

        public RoundedPanel(int radius) {
            super();
            this.cornerRadius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dessiner l'arrière-plan avec ombre légère
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, cornerRadius, cornerRadius);

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, cornerRadius, cornerRadius);

            g2.dispose();
        }
    }

    /**
     * Panneau simplifié pour l'affichage compact d'une timeline
     */
    private class MiniSchedulePanel extends JPanel {
        private ALPSolution solution;
        private int margin = 30;
        private int barHeight = 20;
        private int spacing = 5;
        private int timeScale = 3;

        public MiniSchedulePanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(UIUtils.PANEL_BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ALPInstance instance = solution.getInstance();
            int numRunways = instance.getNumRunways();
            int numAircraft = instance.getNumAircraft();

            // Trouver le temps maximal
            int maxTime = 0;
            for (int i = 0; i < numAircraft; i++) {
                maxTime = Math.max(maxTime, solution.getLandingTime(i));
            }

            // Calculer l'échelle de temps pour s'adapter à la largeur
            timeScale = Math.max(1, (getWidth() - 2 * margin) / maxTime);

            // Dessiner les pistes
            for (int r = 0; r < numRunways; r++) {
                int y = margin + r * (barHeight + spacing);

                // Fond de la piste
                g2d.setColor(UIUtils.GRID_COLOR);
                g2d.fillRect(margin, y, getWidth() - 2 * margin, barHeight);

                // Étiquette de la piste
                g2d.setColor(UIUtils.PRIMARY_COLOR);
                g2d.setFont(UIUtils.SMALL_FONT);
                g2d.drawString("R" + (r + 1), 5, y + barHeight / 2 + 5);
            }

            // Dessiner une échelle de temps
            g2d.setColor(UIUtils.TEXT_SECONDARY);
            g2d.drawLine(margin, margin + numRunways * (barHeight + spacing) + 5,
                    getWidth() - margin, margin + numRunways * (barHeight + spacing) + 5);

            int timeStep = Math.max(1, maxTime / 10);
            for (int t = 0; t <= maxTime; t += timeStep) {
                int x = margin + t * timeScale;
                g2d.drawLine(x, margin + numRunways * (barHeight + spacing) + 3,
                        x, margin + numRunways * (barHeight + spacing) + 7);
                g2d.drawString(Integer.toString(t), x - 3, margin + numRunways * (barHeight + spacing) + 20);
            }

            // Dessiner les atterrissages
            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);

                int x = margin + landingTime * timeScale;
                int y = margin + runway * (barHeight + spacing);

                // Calculer une couleur basée sur l'ID de l'avion
                float hue = (float) ((i * 0.618033988749895) % 1.0); // Golden ratio
                Color color = Color.getHSBColor(hue, 0.8f, 0.9f);

                // Dessiner l'avion
                g2d.setColor(color);
                g2d.fillOval(x - 5, y + 2, barHeight - 4, barHeight - 4);

                // ID de l'avion
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Dialog", Font.BOLD, 9));
                g2d.drawString(Integer.toString(i + 1), x - 2, y + barHeight / 2 + 4);
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