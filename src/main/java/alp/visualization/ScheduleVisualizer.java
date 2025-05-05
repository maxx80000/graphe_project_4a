package alp.visualization;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;

/**
 * Enhanced visualizer for aircraft landing schedule solutions.
 * Provides a comprehensive GUI with landing simulation and optimization
 * details.
 */
public class ScheduleVisualizer {

    // Chronométrage pour les animations
    private static final int ANIMATION_SPEED_MS = 100;
    private static final int TIMELINE_STEP = 1;

    /**
     * Displays an enhanced visualization of the aircraft landing schedule.
     */
    public static void visualizeSchedule(ALPSolution solution) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Aircraft Landing Scheduler - " + solution.getProblemVariant());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);

            // Création correcte de l'icône
            BufferedImage airplaneImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = airplaneImage.createGraphics();
            UIUtils.createAirplaneIcon(32, UIUtils.PRIMARY_COLOR).paintIcon(null, g2d, 0, 0);
            g2d.dispose();
            frame.setIconImage(airplaneImage);

            // Create the main content panel with layout
            JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
            mainPanel.setBackground(UIUtils.BACKGROUND_COLOR);

            // Create header panel
            JPanel headerPanel = createHeaderPanel(solution);
            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Create tabbed pane for different views
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(UIUtils.HEADER_FONT);
            tabbedPane.setBackground(UIUtils.PANEL_BACKGROUND);

            // Create schedule visualization panel
            SchedulePanel schedulePanel = new SchedulePanel(solution);

            // Create statistics panel
            JPanel statsPanel = createStatsPanel(solution);

            // Create simulation panel
            SimulationPanel simulationPanel = new SimulationPanel(solution);

            // Add panels to tabbed pane with icons
            tabbedPane.addTab("Schedule Timeline", UIUtils.createVisualizeIcon(16, UIUtils.PRIMARY_COLOR),
                    new JScrollPane(schedulePanel), "Gantt chart visualization of landing schedule");
            tabbedPane.addTab("Simulation", UIUtils.createAirplaneIcon(16, UIUtils.PRIMARY_COLOR),
                    simulationPanel, "Animated simulation of aircraft landings");
            tabbedPane.addTab("Statistics", UIUtils.createCompareIcon(16, UIUtils.PRIMARY_COLOR),
                    new JScrollPane(statsPanel), "Detailed optimization statistics");

            mainPanel.add(tabbedPane, BorderLayout.CENTER);

            // Add control panel at the bottom
            JPanel controlPanel = createControlPanel(solution, schedulePanel, simulationPanel);
            mainPanel.add(controlPanel, BorderLayout.SOUTH);

            frame.add(mainPanel);
            frame.setVisible(true);
        });
    }

    /**
     * Creates the header panel with solution information
     */
    private static JPanel createHeaderPanel(ALPSolution solution) {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UIUtils.PANEL_BACKGROUND);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.GRID_COLOR),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        // Title with problem variant
        JLabel titleLabel = new JLabel("Aircraft Landing Schedule Optimization");
        titleLabel.setFont(UIUtils.TITLE_FONT);
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subtitle with instance info
        JLabel instanceLabel = new JLabel("Instance: " + solution.getInstance().getInstanceName() +
                " | Problem: " + solution.getProblemVariant());
        instanceLabel.setFont(UIUtils.HEADER_FONT);
        instanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        instanceLabel.setForeground(UIUtils.TEXT_PRIMARY);

        // Result metrics
        JPanel metricsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        metricsPanel.setOpaque(false);
        metricsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create metric components
        addMetricComponent(metricsPanel, "Aircraft", solution.getInstance().getNumAircraft() + "");
        addMetricComponent(metricsPanel, "Runways", solution.getInstance().getNumRunways() + "");
        addMetricComponent(metricsPanel, "Objective Value", String.format("%.2f", solution.getObjectiveValue()));
        addMetricComponent(metricsPanel, "Solve Time", String.format("%.2f sec", solution.getSolveTime()));

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(instanceLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(metricsPanel);

        return headerPanel;
    }

    /**
     * Adds a metric component to the metrics panel
     */
    private static void addMetricComponent(JPanel panel, String label, String value) {
        JPanel metricPanel = new JPanel();
        metricPanel.setLayout(new BoxLayout(metricPanel, BoxLayout.Y_AXIS));
        metricPanel.setOpaque(false);

        JLabel metricLabel = new JLabel(label);
        metricLabel.setFont(UIUtils.SMALL_FONT);
        metricLabel.setForeground(UIUtils.TEXT_SECONDARY);

        JLabel metricValue = new JLabel(value);
        metricValue.setFont(UIUtils.HEADER_FONT);
        metricValue.setForeground(UIUtils.TEXT_PRIMARY);

        metricPanel.add(metricLabel);
        metricPanel.add(metricValue);

        panel.add(metricPanel);
    }

    /**
     * Creates the control panel with timeline slider
     */
    private static JPanel createControlPanel(ALPSolution solution, SchedulePanel schedulePanel,
            SimulationPanel simulationPanel) {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(UIUtils.PANEL_BACKGROUND);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.GRID_COLOR),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        // Find the latest landing time for scaling
        int maxLandingTime = 0;
        for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
            int landingTime = solution.getLandingTime(i);
            if (landingTime > maxLandingTime) {
                maxLandingTime = landingTime;
            }
        }

        // Create time slider
        JLabel sliderLabel = new JLabel("Current Time: 0");
        sliderLabel.setFont(UIUtils.NORMAL_FONT);
        sliderLabel.setForeground(UIUtils.TEXT_PRIMARY);

        JSlider timeSlider = new JSlider(0, maxLandingTime, 0);
        timeSlider.setBackground(UIUtils.PANEL_BACKGROUND);
        timeSlider.setMajorTickSpacing(maxLandingTime / 10);
        timeSlider.setMinorTickSpacing(maxLandingTime / 50);
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintLabels(true);
        timeSlider.setFont(UIUtils.SMALL_FONT);
        timeSlider.setForeground(UIUtils.TEXT_PRIMARY);

        timeSlider.addChangeListener(e -> {
            int time = timeSlider.getValue();
            sliderLabel.setText("Current Time: " + time);
            schedulePanel.setCurrentTime(time);
            simulationPanel.setCurrentTime(time);
        });

        // Create playback control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        JButton playButton = UIUtils.createSecondaryButton("Play", null);
        playButton.setIcon(UIUtils.createPlayIcon(16, UIUtils.PRIMARY_COLOR));

        JButton pauseButton = UIUtils.createSecondaryButton("Pause", null);
        pauseButton.setIcon(UIUtils.createPauseIcon(16, UIUtils.PRIMARY_COLOR));

        JButton resetButton = UIUtils.createSecondaryButton("Reset", null);

        // Animation timer
        final Timer[] timer = { null };
        final int[] animationSpeed = { 1 }; // seconds per frame

        playButton.addActionListener(e -> {
            if (timer[0] != null) {
                timer[0].cancel();
            }

            timer[0] = new Timer();
            timer[0].scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (timeSlider.getValue() < timeSlider.getMaximum()) {
                        SwingUtilities.invokeLater(() -> {
                            timeSlider.setValue(timeSlider.getValue() + TIMELINE_STEP);
                        });
                    } else {
                        timer[0].cancel();
                        timer[0] = null;
                    }
                }
            }, 0, ANIMATION_SPEED_MS * animationSpeed[0]);
        });

        pauseButton.addActionListener(e -> {
            if (timer[0] != null) {
                timer[0].cancel();
                timer[0] = null;
            }
        });

        resetButton.addActionListener(e -> {
            if (timer[0] != null) {
                timer[0].cancel();
                timer[0] = null;
            }
            timeSlider.setValue(0);
        });

        // Speed control
        JLabel speedLabel = new JLabel("Speed: ");
        speedLabel.setFont(UIUtils.NORMAL_FONT);
        speedLabel.setForeground(UIUtils.TEXT_PRIMARY);

        JComboBox<String> speedCombo = new JComboBox<>(new String[] { "0.5x", "1x", "2x", "5x", "10x" });
        speedCombo.setSelectedIndex(1); // Default 1x
        speedCombo.setFont(UIUtils.NORMAL_FONT);
        speedCombo.setBackground(UIUtils.PANEL_BACKGROUND);
        speedCombo.setForeground(UIUtils.TEXT_PRIMARY);

        speedCombo.addActionListener(e -> {
            String selected = (String) speedCombo.getSelectedItem();
            if (selected == null)
                return;

            switch (selected) {
                case "0.5x":
                    animationSpeed[0] = 2;
                    break;
                case "1x":
                    animationSpeed[0] = 1;
                    break;
                case "2x":
                    animationSpeed[0] = 1;
                    break;
                case "5x":
                    animationSpeed[0] = 1;
                    break;
                case "10x":
                    animationSpeed[0] = 1;
                    break;
            }
        });

        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(speedLabel);
        buttonPanel.add(speedCombo);

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setOpaque(false);
        sliderPanel.add(sliderLabel, BorderLayout.WEST);
        sliderPanel.add(timeSlider, BorderLayout.CENTER);

        controlPanel.add(sliderPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        return controlPanel;
    }

    /**
     * Creates the statistics panel with detailed solution information
     */
    private static JPanel createStatsPanel(ALPSolution solution) {
        ALPInstance instance = solution.getInstance();
        int[] landingTimes = solution.getLandingTimes();
        int[] runwayAssignments = solution.getRunwayAssignments();

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(UIUtils.PANEL_BACKGROUND);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add summary panel
        JPanel summaryPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        summaryPanel.setBackground(UIUtils.PANEL_BACKGROUND);
        summaryPanel.setBorder(createSectionBorder("Optimization Summary"));
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Problem-specific statistics
        String problemType = solution.getProblemVariant();
        if (problemType.contains("Problem 1")) {
            // Weighted delay stats
            double totalDelay = 0;
            int earlyAircraft = 0;
            int lateAircraft = 0;

            for (int i = 0; i < instance.getNumAircraft(); i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                int targetTime = aircraft.getTargetLandingTime();
                int actualTime = landingTimes[i];

                if (actualTime < targetTime) {
                    earlyAircraft++;
                    totalDelay += aircraft.getEarlyPenalty() * (targetTime - actualTime);
                } else if (actualTime > targetTime) {
                    lateAircraft++;
                    totalDelay += aircraft.getLatePenalty() * (actualTime - targetTime);
                }
            }

            addStatField(summaryPanel, "Total Weighted Delay", String.format("%.2f", totalDelay));
            addStatField(summaryPanel, "Early Aircraft", String.valueOf(earlyAircraft));
            addStatField(summaryPanel, "Late Aircraft", String.valueOf(lateAircraft));
            addStatField(summaryPanel, "On-Time Aircraft",
                    String.valueOf(instance.getNumAircraft() - earlyAircraft - lateAircraft));

        } else if (problemType.contains("Problem 2")) {
            // Makespan stats
            int makespan = 0;
            for (int time : landingTimes) {
                makespan = Math.max(makespan, time);
            }

            addStatField(summaryPanel, "Makespan (Last Landing)", String.valueOf(makespan));

            // Runway utilization
            Map<Integer, Integer> runwayLandings = new HashMap<>();
            for (int runway : runwayAssignments) {
                runwayLandings.put(runway, runwayLandings.getOrDefault(runway, 0) + 1);
            }

            for (int r = 0; r < instance.getNumRunways(); r++) {
                addStatField(summaryPanel, "Runway " + (r + 1) + " Landings",
                        String.valueOf(runwayLandings.getOrDefault(r, 0)));
            }

        } else if (problemType.contains("Problem 3")) {
            // Total lateness stats
            double totalLateness = 0;
            int lateAircraft = 0;

            for (int i = 0; i < instance.getNumAircraft(); i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                int targetTime = aircraft.getTargetLandingTime();
                int transferTime = aircraft.getTransferTime(runwayAssignments[i]);
                int arrivalTime = landingTimes[i] + transferTime;

                int lateness = Math.max(0, arrivalTime - targetTime);
                totalLateness += lateness;

                if (lateness > 0)
                    lateAircraft++;
            }

            addStatField(summaryPanel, "Total Lateness", String.format("%.2f", totalLateness));
            addStatField(summaryPanel, "Late Aircraft", String.valueOf(lateAircraft));
            addStatField(summaryPanel, "On-Time Aircraft", String.valueOf(instance.getNumAircraft() - lateAircraft));

            // Average transfer time
            double avgTransferTime = 0;
            for (int i = 0; i < instance.getNumAircraft(); i++) {
                AircraftData aircraft = instance.getAircraft().get(i);
                avgTransferTime += aircraft.getTransferTime(runwayAssignments[i]);
            }
            avgTransferTime /= instance.getNumAircraft();

            addStatField(summaryPanel, "Avg Transfer Time", String.format("%.2f", avgTransferTime));
        }

        // General statistics
        addStatField(summaryPanel, "Solve Time", String.format("%.2f seconds", solution.getSolveTime()));

        // Add detailed aircraft data table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(UIUtils.PANEL_BACKGROUND);
        tablePanel.setBorder(createSectionBorder("Aircraft Landing Details"));
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create table model
        String[] columnNames;
        if (problemType.contains("Problem 3")) {
            columnNames = new String[] {
                    "Aircraft", "Runway", "Landing Time", "Target Time", "Transfer Time",
                    "Arrival Time", "Lateness", "Time Window"
            };
        } else {
            columnNames = new String[] {
                    "Aircraft", "Runway", "Landing Time", "Target Time",
                    "Deviation", "Penalty", "Time Window"
            };
        }

        Object[][] data = new Object[instance.getNumAircraft()][columnNames.length];

        // Populate table data
        for (int i = 0; i < instance.getNumAircraft(); i++) {
            AircraftData aircraft = instance.getAircraft().get(i);
            int landingTime = landingTimes[i];
            int runway = runwayAssignments[i];
            int targetTime = aircraft.getTargetLandingTime();
            String timeWindow = aircraft.getEarliestLandingTime() + " - " + aircraft.getLatestLandingTime();

            if (problemType.contains("Problem 3")) {
                int transferTime = aircraft.getTransferTime(runway);
                int arrivalTime = landingTime + transferTime;
                int lateness = Math.max(0, arrivalTime - targetTime);

                data[i] = new Object[] {
                        "A" + (i + 1), runway + 1, landingTime, targetTime,
                        transferTime, arrivalTime, lateness, timeWindow
                };
            } else {
                int deviation = landingTime - targetTime;
                double penalty = 0;

                if (deviation < 0) {
                    penalty = aircraft.getEarlyPenalty() * Math.abs(deviation);
                } else if (deviation > 0) {
                    penalty = aircraft.getLatePenalty() * deviation;
                }

                data[i] = new Object[] {
                        "A" + (i + 1), runway + 1, landingTime, targetTime,
                        deviation, String.format("%.2f", penalty), timeWindow
                };
            }
        }

        // Create and configure table
        JTable table = new JTable(data, columnNames);
        table.setFont(UIUtils.NORMAL_FONT);
        table.setRowHeight(25);
        table.getTableHeader().setFont(UIUtils.HEADER_FONT);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Cell renderer for coloring early/late
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                int modelRow = table.convertRowIndexToModel(row);

                // Add padding
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                if (!isSelected) {
                    int deviation = 0;
                    if (columnNames[column].equals("Deviation")) {
                        deviation = (int) table.getModel().getValueAt(modelRow, 4);
                    } else if (columnNames[column].equals("Lateness")) {
                        deviation = (int) table.getModel().getValueAt(modelRow, 6);
                    }

                    if (deviation < 0) {
                        c.setBackground(new Color(220, 255, 220));
                        ((JLabel) c).setForeground(UIUtils.SUCCESS_COLOR);
                    } else if (deviation > 0) {
                        c.setBackground(new Color(255, 220, 220));
                        ((JLabel) c).setForeground(UIUtils.ERROR_COLOR);
                    } else {
                        c.setBackground(row % 2 == 0 ? UIUtils.PANEL_BACKGROUND : new Color(248, 250, 253));
                        ((JLabel) c).setForeground(UIUtils.TEXT_PRIMARY);
                    }
                }

                return c;
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(UIUtils.PANEL_BACKGROUND);

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add both panels to statistics panel
        statsPanel.add(summaryPanel);
        statsPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        statsPanel.add(tablePanel);

        return statsPanel;
    }

    /**
     * Helper to add a field to the statistics panel
     */
    private static void addStatField(JPanel panel, String label, String value) {
        JLabel lblField = new JLabel(label + ":");
        lblField.setFont(UIUtils.NORMAL_FONT);
        lblField.setForeground(UIUtils.TEXT_SECONDARY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(UIUtils.NORMAL_FONT.deriveFont(Font.BOLD));
        lblValue.setForeground(UIUtils.TEXT_PRIMARY);

        panel.add(lblField);
        panel.add(lblValue);
    }

    /**
     * Creates a titled border for sections
     */
    private static Border createSectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIUtils.GRID_COLOR),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        UIUtils.HEADER_FONT,
                        UIUtils.PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Panel for displaying the enhanced schedule visualization.
     */
    private static class SchedulePanel extends JPanel {
        private ALPSolution solution;
        private int currentTime = 0;
        private int margin = 60;
        private int barHeight = 35;
        private int spacing = 15;
        private int timeScale = 5;
        private int padding = 20;

        public SchedulePanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(UIUtils.PANEL_BACKGROUND);

            // Make the panel scrollable with proper sizing
            int numRunways = solution.getInstance().getNumRunways();
            int maxLandingTime = findMaxLandingTime();

            int preferredWidth = margin + maxLandingTime * timeScale + 100;
            int preferredHeight = margin + numRunways * (barHeight + spacing) + margin;

            setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        }

        public void setCurrentTime(int time) {
            this.currentTime = time;
            repaint();
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
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            ALPInstance instance = solution.getInstance();
            int numRunways = instance.getNumRunways();
            int numAircraft = instance.getNumAircraft();
            int maxLandingTime = findMaxLandingTime();

            // Draw grid background
            int totalHeight = margin + numRunways * (barHeight + spacing);
            for (int i = 0; i < numRunways; i++) {
                int y = margin + i * (barHeight + spacing);
                g2d.setColor(UIUtils.GRID_COLOR);
                g2d.fillRect(margin, y, getWidth() - margin * 2, barHeight);
            }

            // Draw vertical grid lines
            g2d.setColor(UIUtils.GRID_COLOR);
            int tickInterval = Math.max(1, maxLandingTime / 20); // Aim for roughly 20 ticks
            for (int t = 0; t <= maxLandingTime; t += tickInterval) {
                int x = margin + t * timeScale;
                g2d.drawLine(x, margin - padding / 2, x, totalHeight);
            }

            // Draw time axis
            g2d.setColor(UIUtils.TEXT_SECONDARY);
            g2d.drawLine(margin, totalHeight, margin + maxLandingTime * timeScale, totalHeight);

            // Draw time ticks and labels
            g2d.setFont(UIUtils.SMALL_FONT);
            for (int t = 0; t <= maxLandingTime; t += tickInterval) {
                int x = margin + t * timeScale;
                g2d.drawLine(x, totalHeight, x, totalHeight + 5);
                String timeLabel = String.valueOf(t);
                FontMetrics fm = g2d.getFontMetrics();
                int labelWidth = fm.stringWidth(timeLabel);
                g2d.drawString(timeLabel, x - labelWidth / 2, totalHeight + 18);
            }

            // Draw runway labels
            g2d.setFont(UIUtils.NORMAL_FONT);
            for (int r = 0; r < numRunways; r++) {
                int y = margin + r * (barHeight + spacing) + barHeight / 2;
                g2d.setColor(UIUtils.PRIMARY_COLOR);
                g2d.drawString("Runway " + (r + 1), 5, y + 5);
            }

            // Prepare colors for each aircraft
            List<Color> colors = new ArrayList<>();
            for (int i = 0; i < numAircraft; i++) {
                float hue = (float) ((i * 0.618033988749895) % 1.0); // Golden ratio
                colors.add(Color.getHSBColor(hue, 0.8f, 0.9f));
            }

            // Draw aircraft time windows as background bars
            for (int i = 0; i < numAircraft; i++) {
                int runway = solution.getRunwayAssignment(i);
                AircraftData aircraft = instance.getAircraft().get(i);

                int earliestTime = aircraft.getEarliestLandingTime();
                int latestTime = aircraft.getLatestLandingTime();
                int targetTime = aircraft.getTargetLandingTime();

                int x1 = margin + earliestTime * timeScale;
                int x2 = margin + latestTime * timeScale;
                int y = margin + runway * (barHeight + spacing);

                // Draw time window
                g2d.setColor(new Color(colors.get(i).getRed(), colors.get(i).getGreen(), colors.get(i).getBlue(), 40));
                g2d.fillRect(x1, y, x2 - x1, barHeight);

                // Draw target time line
                int targetX = margin + targetTime * timeScale;
                g2d.setColor(new Color(colors.get(i).getRed(), colors.get(i).getGreen(), colors.get(i).getBlue(), 80));
                g2d.drawLine(targetX, y, targetX, y + barHeight);
            }

            // Draw aircraft landings
            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);
                AircraftData aircraft = instance.getAircraft().get(i);

                int x = margin + landingTime * timeScale;
                int y = margin + runway * (barHeight + spacing);

                Color aircraftColor = colors.get(i);

                // Highlight if landing matches current time
                if (Math.abs(landingTime - currentTime) <= 1) {
                    // Draw highlighted aircraft
                    g2d.setColor(aircraftColor.brighter());
                    g2d.fillOval(x - 15, y - 5, barHeight + 10, barHeight + 10);
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(x - 10, y, barHeight, barHeight);
                    g2d.setColor(aircraftColor);
                    g2d.fillOval(x - 8, y + 2, barHeight - 4, barHeight - 4);

                    // Draw info tooltip
                    String infoText = "A" + (i + 1) + " | Time: " + landingTime + " | Target: "
                            + aircraft.getTargetLandingTime();
                    drawTooltip(g2d, infoText, x, y - 20);
                } else {
                    // Draw normal aircraft
                    g2d.setColor(aircraftColor);
                    g2d.fillOval(x - 10, y + 2, barHeight - 4, barHeight - 4);
                }

                // Draw aircraft ID
                g2d.setColor(Color.WHITE);
                g2d.setFont(UIUtils.SMALL_FONT);
                String aircraftId = "A" + (i + 1);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(aircraftId);
                int textHeight = fm.getHeight();
                g2d.drawString(aircraftId, x - textWidth / 2, y + barHeight / 2 + textHeight / 4);
            }

            // Draw current time indicator
            int timeX = margin + currentTime * timeScale;
            g2d.setColor(UIUtils.ACCENT_COLOR);
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    0, new float[] { 8, 4 }, 0));
            g2d.drawLine(timeX, margin - padding, timeX, totalHeight);
            g2d.setStroke(new BasicStroke(1f));

            // Display current time
            g2d.setFont(UIUtils.HEADER_FONT);
            g2d.setColor(UIUtils.ACCENT_COLOR);
            g2d.drawString("t = " + currentTime, timeX + 5, margin - padding / 2);
        }

        private void drawTooltip(Graphics2D g2d, String text, int x, int y) {
            FontMetrics fm = g2d.getFontMetrics(UIUtils.NORMAL_FONT);
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int padding = 6;
            int boxX = x - textWidth / 2 - padding;
            int boxY = y - textHeight - padding;
            int boxWidth = textWidth + padding * 2;
            int boxHeight = textHeight + padding * 2;

            // Draw tooltip background
            g2d.setColor(new Color(50, 50, 50, 220));
            g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

            // Draw tooltip text
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.NORMAL_FONT);
            g2d.drawString(text, boxX + padding, boxY + textHeight);
        }
    }

    /**
     * Panel for displaying an animated simulation of aircraft landings.
     */
    private static class SimulationPanel extends JPanel {
        private ALPSolution solution;
        private int currentTime = 0;
        private Map<Integer, Point2D.Double> aircraftPositions = new HashMap<>();
        private Map<Integer, Double> aircraftAltitudes = new HashMap<>();
        private static final double MAX_ALTITUDE = 300.0;

        public SimulationPanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(UIUtils.PANEL_BACKGROUND);

            // Initialize aircraft positions (off-screen)
            for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
                aircraftPositions.put(i, new Point2D.Double(-100, -100));
                aircraftAltitudes.put(i, MAX_ALTITUDE);
            }

            // Enable mouse interaction for aircraft info
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    repaint(); // Refresh for hover effects
                }
            });
        }

        public void setCurrentTime(int time) {
            this.currentTime = time;
            updateAircraftPositions();
            repaint();
        }

        private void updateAircraftPositions() {
            ALPInstance instance = solution.getInstance();
            int numAircraft = instance.getNumAircraft();
            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);
                int timeToLanding = landingTime - currentTime;

                double x, y, altitude;

                if (timeToLanding > 20) {
                    // Aircraft not visible yet (far away)
                    x = -100;
                    y = -100;
                    altitude = MAX_ALTITUDE;
                } else if (timeToLanding <= 0) {
                    // Aircraft has landed
                    double runwayX = getWidth() / 2;
                    double runwayY = getHeight() * 0.7 + runway * 30;

                    if (timeToLanding >= -10) {
                        // Show aircraft on the runway for a short time
                        x = runwayX + (-timeToLanding * 10);
                        y = runwayY;
                        altitude = 0;
                    } else {
                        // Aircraft has left the scene
                        x = -100;
                        y = -100;
                        altitude = 0;
                    }
                } else {
                    // Aircraft is approaching
                    double runwayX = getWidth() / 2;
                    double runwayY = getHeight() * 0.7 + runway * 30;

                    // Calculate approach path
                    double progress = 1.0 - timeToLanding / 20.0;

                    // Start from a random position based on aircraft ID
                    double startX = (i % 2 == 0) ? 0 : getWidth();
                    double startY = 100 + ((i * 137) % (getHeight() * 0.3));

                    // Interpolate position
                    x = startX + progress * (runwayX - startX);
                    y = startY + progress * (runwayY - startY);

                    // Calculate altitude (descending)
                    altitude = MAX_ALTITUDE * (1.0 - progress);
                }

                aircraftPositions.put(i, new Point2D.Double(x, y));
                aircraftAltitudes.put(i, altitude);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ALPInstance instance = solution.getInstance();
            int numRunways = instance.getNumRunways();
            int numAircraft = instance.getNumAircraft();

            // Draw sky
            GradientPaint skyGradient = new GradientPaint(
                    0, 0, new Color(170, 210, 240),
                    0, getHeight(), new Color(220, 240, 255));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw ground
            GradientPaint groundGradient = new GradientPaint(
                    0, (int) (getHeight() * 0.6), new Color(120, 180, 80),
                    0, getHeight(), new Color(80, 140, 40));
            g2d.setPaint(groundGradient);
            g2d.fillRect(0, (int) (getHeight() * 0.6), getWidth(), getHeight() - (int) (getHeight() * 0.6));

            // Draw clouds
            drawClouds(g2d);

            // Draw runways
            int runwayWidth = 300;
            int runwayHeight = 20;
            for (int r = 0; r < numRunways; r++) {
                int x = getWidth() / 2 - runwayWidth / 2;
                int y = (int) (getHeight() * 0.7) + r * 30 - runwayHeight / 2;

                // Runway asphalt
                g2d.setColor(new Color(60, 60, 60));
                g2d.fillRect(x, y, runwayWidth, runwayHeight);

                // Runway markings
                g2d.setColor(Color.WHITE);
                for (int i = 0; i < runwayWidth; i += 30) {
                    g2d.fillRect(x + i, y + runwayHeight / 2 - 1, 15, 2);
                }

                // Runway number
                g2d.setFont(UIUtils.NORMAL_FONT.deriveFont(Font.BOLD));
                g2d.setColor(Color.WHITE);
                g2d.drawString("R" + (r + 1), x - 25, y + runwayHeight / 2 + 5);
            }

            // Prepare colors for each aircraft
            List<Color> colors = new ArrayList<>();
            for (int i = 0; i < numAircraft; i++) {
                float hue = (float) ((i * 0.618033988749895) % 1.0); // Golden ratio
                colors.add(Color.getHSBColor(hue, 0.8f, 0.9f));
            }

            // Draw aircraft
            for (int i = 0; i < numAircraft; i++) {
                Point2D.Double pos = aircraftPositions.get(i);
                double altitude = aircraftAltitudes.get(i);

                if (pos.x < 0 || pos.y < 0)
                    continue; // Skip if off-screen

                Color aircraftColor = colors.get(i);
                drawAircraft(g2d, pos.x, pos.y, aircraftColor, i, altitude);
            }

            // Draw time display
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(10, 10, 150, 30, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.HEADER_FONT);
            g2d.drawString("Time: " + currentTime, 20, 30);

            // Draw aircraft info if hovering
            Point mousePos = getMousePosition();
            if (mousePos != null) {
                for (int i = 0; i < numAircraft; i++) {
                    Point2D.Double pos = aircraftPositions.get(i);
                    if (pos.x >= 0 && pos.y >= 0) {
                        double distance = mousePos.distance(pos);
                        if (distance < 30) {
                            drawAircraftInfo(g2d, i, pos.x, pos.y);
                            break;
                        }
                    }
                }
            }
        }

        private void drawClouds(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, 180));

            // Generate some random clouds based on seed to keep them consistent
            Random random = new Random(42);
            for (int i = 0; i < 10; i++) {
                int x = random.nextInt(getWidth());
                int y = random.nextInt((int) (getHeight() * 0.4));
                int size = 20 + random.nextInt(40);

                // Draw a fluffy cloud
                for (int j = 0; j < 5; j++) {
                    int offsetX = random.nextInt(size) - size / 2;
                    int offsetY = random.nextInt(size / 2) - size / 4;
                    g2d.fillOval(x + offsetX, y + offsetY, size, size);
                }
            }
        }

        private void drawAircraft(Graphics2D g2d, double x, double y, Color color, int aircraftId, double altitude) {
            int size = 15 + (int) (altitude / 30);

            // Shadow on ground
            if (altitude < 100) {
                g2d.setColor(new Color(0, 0, 0, 50));
                double groundY = getHeight() * 0.7 + solution.getRunwayAssignment(aircraftId) * 30;
                int shadowSize = Math.max(5, size - (int) (altitude / 10));
                g2d.fillOval((int) x - shadowSize / 2, (int) groundY - shadowSize / 2, shadowSize, shadowSize);
            }

            // Aircraft body
            g2d.setColor(color);

            // Draw aircraft as a stylized plane shape
            int[] xPoints = new int[] {
                    (int) x, (int) (x + size / 2), (int) (x + size / 3), (int) (x - size / 3), (int) (x - size / 2)
            };
            int[] yPoints = new int[] {
                    (int) (y - size / 2), (int) y, (int) (y + size / 2), (int) (y + size / 2), (int) y
            };
            g2d.fillPolygon(xPoints, yPoints, 5);

            // Wings
            int wingSize = size * 2 / 3;
            g2d.fillRect((int) (x - wingSize / 2), (int) (y - size / 6), wingSize, size / 3);

            // Tail
            int tailSize = size / 3;
            int[] tailX = new int[] {
                    (int) (x - size / 3), (int) (x - size / 3), (int) (x - size / 2 - tailSize / 2)
            };
            int[] tailY = new int[] {
                    (int) (y - size / 6), (int) (y - size / 2), (int) (y - size / 2)
            };
            g2d.fillPolygon(tailX, tailY, 3);

            // Add aircraft ID
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.SMALL_FONT);
            String aircraftIdStr = "A" + (aircraftId + 1);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(aircraftIdStr, (int) (x - fm.stringWidth(aircraftIdStr) / 2), (int) y + fm.getHeight() / 4);
        }

        private void drawAircraftInfo(Graphics2D g2d, int aircraftId, double x, double y) {
            AircraftData aircraft = solution.getInstance().getAircraft().get(aircraftId);
            int landingTime = solution.getLandingTime(aircraftId);
            int runway = solution.getRunwayAssignment(aircraftId);
            int targetTime = aircraft.getTargetLandingTime();
            int timeToLanding = landingTime - currentTime;

            String[] infoLines = {
                    "Aircraft: A" + (aircraftId + 1),
                    "Runway: R" + (runway + 1),
                    "Landing Time: " + landingTime,
                    "Target Time: " + targetTime,
                    "Status: " + (timeToLanding > 0 ? "Approaching (" + timeToLanding + " time units)"
                            : "Landed (" + (-timeToLanding) + " time units ago)")
            };

            // Calculate box size
            g2d.setFont(UIUtils.NORMAL_FONT);
            FontMetrics fm = g2d.getFontMetrics();
            int lineHeight = fm.getHeight();
            int maxWidth = 0;
            for (String line : infoLines) {
                maxWidth = Math.max(maxWidth, fm.stringWidth(line));
            }

            int padding = 10;
            int boxWidth = maxWidth + padding * 2;
            int boxHeight = lineHeight * infoLines.length + padding * 2;

            // Adjust position to keep within bounds
            int boxX = (int) x + 20;
            if (boxX + boxWidth > getWidth())
                boxX = (int) x - boxWidth - 20;

            int boxY = (int) y - boxHeight / 2;
            if (boxY < 0)
                boxY = 0;
            if (boxY + boxHeight > getHeight())
                boxY = getHeight() - boxHeight;

            // Draw info box
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

            // Draw text
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < infoLines.length; i++) {
                g2d.drawString(infoLines[i], boxX + padding, boxY + padding + lineHeight * (i + 1) - lineHeight / 4);
            }

            // Draw pointer line
            g2d.drawLine((int) x, (int) y, boxX + (x > boxX + boxWidth ? boxWidth : 0), boxY + boxHeight / 2);
        }
    }
}
