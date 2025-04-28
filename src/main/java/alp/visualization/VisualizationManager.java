package alp.visualization;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.visualization.ScheduleVisualizer.EnhancedSchedulePanel;
import alp.visualization.ScheduleVisualizer.RunwayAnimationPanel;

/**
 * Classe principale de gestion des visualisations qui organise les solutions
 * par problème, par instance ou par nombre de pistes.
 */
public class VisualizationManager {

    private static final String[] ORGANIZATION_MODES = {
            "Par type de problème",
            "Par nombre de pistes",
            "Par instance"
    };

    private Map<String, List<ALPSolution>> solutionsByProblem = new HashMap<>();
    private Map<Integer, List<ALPSolution>> solutionsByRunway = new HashMap<>();
    private Map<String, List<ALPSolution>> solutionsByInstance = new HashMap<>();

    /**
     * Lance le gestionnaire de visualisation avec un ensemble de solutions
     */
    public static void launchVisualizer(List<ALPSolution> solutions) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            VisualizationManager manager = new VisualizationManager();
            manager.organizeSolutions(solutions);
            manager.showOrganizationDialog();
        });
    }

    /**
     * Organise les solutions par catégories
     */
    private void organizeSolutions(List<ALPSolution> solutions) {
        for (ALPSolution solution : solutions) {
            // Organiser par type de problème
            String problemType = solution.getProblemVariant();
            solutionsByProblem.computeIfAbsent(problemType, k -> new ArrayList<>()).add(solution);

            // Organiser par nombre de pistes
            int runways = solution.getInstance().getNumRunways();
            solutionsByRunway.computeIfAbsent(runways, k -> new ArrayList<>()).add(solution);

            // Organiser par instance
            String instanceName = solution.getInstance().getInstanceName();
            solutionsByInstance.computeIfAbsent(instanceName, k -> new ArrayList<>()).add(solution);
        }
    }

    /**
     * Affiche un dialogue permettant de choisir comment organiser les
     * visualisations
     */
    private void showOrganizationDialog() {
        JFrame frame = new JFrame("Gestionnaire de visualisation ALP");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Panel d'en-tête avec des instructions
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Visualisation des solutions ALP");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel instructionsLabel = new JLabel(
                "<html>Sélectionnez comment vous souhaitez organiser les visualisations.<br>" +
                        "Vous pourrez ensuite choisir les solutions spécifiques à visualiser.</html>");
        instructionsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(instructionsLabel, BorderLayout.CENTER);
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Options d'organisation
        JPanel optionsPanel = new JPanel(new GridLayout(ORGANIZATION_MODES.length, 1, 10, 10));
        optionsPanel.setBorder(new TitledBorder("Modes d'organisation"));

        ButtonGroup group = new ButtonGroup();
        JRadioButton[] buttons = new JRadioButton[ORGANIZATION_MODES.length];

        for (int i = 0; i < ORGANIZATION_MODES.length; i++) {
            buttons[i] = new JRadioButton(ORGANIZATION_MODES[i]);
            buttons[i].setActionCommand(String.valueOf(i));
            buttons[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            group.add(buttons[i]);
            optionsPanel.add(buttons[i]);
        }

        // Sélectionner la première option par défaut
        if (buttons.length > 0) {
            buttons[0].setSelected(true);
        }

        // Boutons d'action
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Suivant");
        JButton cancelButton = new JButton("Fermer");

        okButton.addActionListener(e -> {
            int selectedMode = Integer.parseInt(group.getSelection().getActionCommand());
            frame.dispose();
            showSolutionSelectionDialog(selectedMode);
        });

        cancelButton.addActionListener(e -> frame.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(optionsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    /**
     * Affiche un dialogue pour sélectionner les solutions spécifiques à visualiser
     */
    private void showSolutionSelectionDialog(int organizationMode) {
        JFrame frame = new JFrame("Sélection des solutions à visualiser");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Déterminer les catégories de solutions en fonction du mode d'organisation
        String[] categories;
        Map<String, List<ALPSolution>> solutionMap = new HashMap<>();

        switch (organizationMode) {
            case 0: // Par type de problème
                categories = solutionsByProblem.keySet().toArray(new String[0]);
                for (String problem : categories) {
                    solutionMap.put(problem, solutionsByProblem.get(problem));
                }
                break;
            case 1: // Par nombre de pistes
                categories = solutionsByRunway.keySet().stream()
                        .map(r -> r + " piste" + (r > 1 ? "s" : ""))
                        .toArray(String[]::new);
                int i = 0;
                for (Integer runway : solutionsByRunway.keySet()) {
                    solutionMap.put(categories[i++], solutionsByRunway.get(runway));
                }
                break;
            case 2: // Par instance
                categories = solutionsByInstance.keySet().toArray(new String[0]);
                for (String instance : categories) {
                    solutionMap.put(instance, solutionsByInstance.get(instance));
                }
                break;
            default:
                categories = new String[0];
                break;
        }

        // Trier les catégories
        Arrays.sort(categories);

        // Créer un panel avec onglets pour chaque catégorie
        JTabbedPane tabbedPane = new JTabbedPane();
        Map<String, List<JCheckBox>> checkboxesByCategory = new HashMap<>();

        for (String category : categories) {
            JPanel categoryPanel = new JPanel();
            categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
            categoryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // En-tête avec options de sélection
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton selectAllButton = new JButton("Tout sélectionner");
            JButton selectNoneButton = new JButton("Tout désélectionner");

            headerPanel.add(selectAllButton);
            headerPanel.add(selectNoneButton);
            categoryPanel.add(headerPanel);

            // Liste des solutions dans cette catégorie
            JPanel solutionsPanel = new JPanel();
            solutionsPanel.setLayout(new BoxLayout(solutionsPanel, BoxLayout.Y_AXIS));
            List<JCheckBox> checkboxes = new ArrayList<>();

            for (ALPSolution solution : solutionMap.get(category)) {
                String description = formatSolutionDescription(solution);
                JCheckBox checkbox = new JCheckBox(description);
                checkbox.putClientProperty("solution", solution);
                checkboxes.add(checkbox);
                solutionsPanel.add(checkbox);
            }

            checkboxesByCategory.put(category, checkboxes);

            // Configurer les boutons de sélection
            selectAllButton
                    .addActionListener(e -> checkboxesByCategory.get(category).forEach(cb -> cb.setSelected(true)));

            selectNoneButton
                    .addActionListener(e -> checkboxesByCategory.get(category).forEach(cb -> cb.setSelected(false)));

            JScrollPane scrollPane = new JScrollPane(solutionsPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            categoryPanel.add(scrollPane);
            tabbedPane.addTab(category, categoryPanel);
        }

        // Panneau de boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton visualizeButton = new JButton("Visualiser la sélection");
        JButton cancelButton = new JButton("Annuler");

        visualizeButton.addActionListener(e -> {
            List<ALPSolution> selectedSolutions = new ArrayList<>();

            for (List<JCheckBox> checkboxes : checkboxesByCategory.values()) {
                for (JCheckBox checkbox : checkboxes) {
                    if (checkbox.isSelected()) {
                        selectedSolutions.add((ALPSolution) checkbox.getClientProperty("solution"));
                    }
                }
            }

            if (selectedSolutions.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Veuillez sélectionner au moins une solution à visualiser.",
                        "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            } else {
                frame.dispose();
                showMultipleVisualizations(selectedSolutions);
            }
        });

        cancelButton.addActionListener(e -> {
            frame.dispose();
            showOrganizationDialog(); // Retourner à l'écran précédent
        });

        buttonPanel.add(visualizeButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    /**
     * Affiche plusieurs visualisations dans une fenêtre à onglets
     */
    private void showMultipleVisualizations(List<ALPSolution> solutions) {
        JFrame frame = new JFrame("Visualisations ALP");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Créer un onglet pour chaque solution
        for (ALPSolution solution : solutions) {
            String tabTitle = solution.getInstance().getInstanceName() + " - " +
                    solution.getProblemVariant() + " - " +
                    solution.getInstance().getNumRunways() + " piste(s)";

            // Créer le panneau de visualisation
            JPanel solutionPanel = createVisualizationPanel(solution);
            tabbedPane.addTab(tabTitle, solutionPanel);
        }

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    /**
     * Crée un panneau de visualisation pour une solution spécifique
     */
    private JPanel createVisualizationPanel(ALPSolution solution) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Créer des onglets pour les différents types de visualisation
        JTabbedPane viewTabbedPane = new JTabbedPane();

        // Vue planning (Gantt)
        EnhancedSchedulePanel schedulePanel = new EnhancedSchedulePanel(solution);
        JPanel ganttPanel = ScheduleVisualizer.createGanttPanelWithControls(schedulePanel, solution);
        viewTabbedPane.addTab("Planning", ganttPanel);

        // Vue animation
        RunwayAnimationPanel animationPanel = new RunwayAnimationPanel(solution);
        JPanel animationControlPanel = ScheduleVisualizer.createAnimationControlPanel(animationPanel, solution);
        viewTabbedPane.addTab("Animation", animationControlPanel);

        mainPanel.add(viewTabbedPane, BorderLayout.CENTER);

        // Panneau d'information en bas
        JPanel infoPanel = ScheduleVisualizer.createInfoPanel(solution);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Formate une description de solution pour l'affichage
     */
    private String formatSolutionDescription(ALPSolution solution) {
        ALPInstance instance = solution.getInstance();
        return String.format("Instance: %s, Problème: %s, Pistes: %d, Avions: %d, Objectif: %.2f",
                instance.getInstanceName(),
                solution.getProblemVariant(),
                instance.getNumRunways(),
                instance.getNumAircraft(),
                solution.getObjectiveValue());
    }

    /**
     * Méthode principale pour lancer le gestionnaire directement
     */
    public static void main(String[] args) {
        JFileChooser fileChooser = new JFileChooser("results");
        fileChooser.setDialogTitle("Sélectionnez les fichiers solution à visualiser");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers texte", "txt"));

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            List<ALPSolution> solutions = new ArrayList<>();

            for (File file : selectedFiles) {
                try {
                    ALPSolution solution = VisualizationLauncher.parseSolutionFile(file);
                    if (solution != null) {
                        solutions.add(solution);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'analyse du fichier " + file.getName() + ": " + e.getMessage());
                }
            }

            if (solutions.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "Aucune solution valide n'a pu être chargée à partir des fichiers sélectionnés.",
                        "Erreur de chargement", JOptionPane.ERROR_MESSAGE);
            } else {
                launchVisualizer(solutions);
            }
        }
    }
}