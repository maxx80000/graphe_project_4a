package alp.visualization;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer; // Spécifier explicitement javax.swing.Timer pour éviter l'ambiguïté
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;

/**
 * Enhanced class to visualize the aircraft landing schedule with interactive
 * features
 * and animations.
 */
public class ScheduleVisualizer {
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 250);
    private static final Color GRID_COLOR = new Color(220, 220, 230);
    private static final Color AXIS_COLOR = new Color(100, 100, 120);
    private static final Color TEXT_COLOR = new Color(50, 50, 70);
    private static final Color TITLE_COLOR = new Color(25, 25, 112);

    private static final Color[] RUNWAY_COLORS = {
            new Color(68, 138, 255), // Blue
            new Color(246, 139, 30), // Orange
            new Color(97, 206, 112) // Green
    };

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font INFO_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font AIRCRAFT_FONT = new Font("Segoe UI", Font.BOLD, 10);

    /**
     * Displays an enhanced visualization of the aircraft landing schedule.
     */
    public static void visualizeSchedule(ALPSolution solution) {
        try {
            // Use system look and feel for a more modern appearance
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default look and feel if system L&F not available
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        JFrame frame = new JFrame("Aircraft Landing Schedule - " + solution.getProblemVariant());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1200, 700);

        // Create tabbed pane for different visualization options
        JTabbedPane tabbedPane = new JTabbedPane();

        // Main schedule panel with Gantt chart
        EnhancedSchedulePanel schedulePanel = new EnhancedSchedulePanel(solution);
        JPanel ganttPanel = createGanttPanelWithControls(schedulePanel, solution);
        tabbedPane.addTab("Schedule View", new ImageIcon(), ganttPanel, "Gantt chart of aircraft landing schedule");

        // Animation panel showing aircraft approaching and landing
        RunwayAnimationPanel animationPanel = new RunwayAnimationPanel(solution);
        JPanel animationControlPanel = createAnimationControlPanel(animationPanel, solution);
        tabbedPane.addTab("Runway Animation", new ImageIcon(), animationControlPanel,
                "Animation of aircraft landing sequence");

        // Add the tabbed pane to the frame
        frame.add(tabbedPane);

        // Center the frame on screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Wait for the frame to close to allow the program to continue
        final CountDownLatch latch = new CountDownLatch(1);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Displays multiple visualizations in a single window with tabs for better
     * organization.
     */
    public static void visualizeMultipleSolutions(List<ALPSolution> solutions) {
        VisualizationManager.launchVisualizer(solutions);
    }

    /**
     * Creates a panel with the Gantt chart and control elements
     */
    public static JPanel createGanttPanelWithControls(EnhancedSchedulePanel schedulePanel, ALPSolution solution) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Add the schedule panel to a scroll pane
        JScrollPane scrollPane = new JScrollPane(schedulePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Create a toolbar with controls
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(BACKGROUND_COLOR);
        toolBar.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Zoom controls
        JButton zoomInButton = new JButton("Zoom In");
        JButton zoomOutButton = new JButton("Zoom Out");
        JButton resetZoomButton = new JButton("Reset Zoom");

        zoomInButton.addActionListener(e -> schedulePanel.zoomIn());
        zoomOutButton.addActionListener(e -> schedulePanel.zoomOut());
        resetZoomButton.addActionListener(e -> schedulePanel.resetZoom());

        toolBar.add(zoomInButton);
        toolBar.add(zoomOutButton);
        toolBar.add(resetZoomButton);
        toolBar.addSeparator();

        // Save image button
        JButton saveButton = new JButton("Save as Image");
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Schedule as PNG");
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));

            // Set default filename with timestamp
            String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String defaultFilename = solution.getInstance().getInstanceName() + "_" +
                    solution.getProblemVariant() + "_" + dateTime + ".png";
            fileChooser.setSelectedFile(new File(defaultFilename));

            if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // Ensure file has .png extension
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }

                // Create image of the current view
                BufferedImage image = new BufferedImage(
                        schedulePanel.getWidth(), schedulePanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();
                schedulePanel.print(g2d);
                g2d.dispose();

                // Save the image
                try {
                    ImageIO.write(image, "png", file);
                    JOptionPane.showMessageDialog(mainPanel,
                            "Image saved to: " + file.getAbsolutePath(),
                            "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Error saving image: " + ex.getMessage(),
                            "Save Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        toolBar.add(saveButton);

        // Information panel (above the schedule)
        JPanel infoPanel = createInfoPanel(solution);

        // Add everything to the main panel
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * Creates an information panel with solution details
     */
    public static JPanel createInfoPanel(ALPSolution solution) {
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(240, 240, 245));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 210)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        ALPInstance instance = solution.getInstance();
        int numAircraft = instance.getNumAircraft();
        int numRunways = instance.getNumRunways();

        // Create info text
        JLabel instanceLabel = new JLabel("Instance: " + instance.getInstanceName());
        JLabel aircraftLabel = new JLabel("Aircraft: " + numAircraft);
        JLabel runwaysLabel = new JLabel("Runways: " + numRunways);
        JLabel objectiveLabel = new JLabel(String.format("Objective Value: %.2f", solution.getObjectiveValue()));
        JLabel timeLabel = new JLabel(String.format("Solve Time: %.2f seconds", solution.getSolveTime()));

        // Style labels
        for (JLabel label : new JLabel[] { instanceLabel, aircraftLabel, runwaysLabel, objectiveLabel, timeLabel }) {
            label.setFont(INFO_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        infoPanel.add(instanceLabel);
        infoPanel.add(aircraftLabel);
        infoPanel.add(runwaysLabel);
        infoPanel.add(objectiveLabel);
        infoPanel.add(timeLabel);

        return infoPanel;
    }

    /**
     * Creates the animation panel with controls
     */
    public static JPanel createAnimationControlPanel(RunwayAnimationPanel animationPanel, ALPSolution solution) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Create animation controls
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(BACKGROUND_COLOR);
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton resetButton = new JButton("Reset");
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 5);

        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setBackground(BACKGROUND_COLOR);

        JLabel speedLabel = new JLabel("Animation Speed:");
        speedLabel.setFont(INFO_FONT);

        // Add action listeners
        playButton.addActionListener(e -> animationPanel.startAnimation());
        pauseButton.addActionListener(e -> animationPanel.pauseAnimation());
        resetButton.addActionListener(e -> animationPanel.resetAnimation());

        speedSlider.addChangeListener(e -> animationPanel.setAnimationSpeed(speedSlider.getValue()));

        // Add controls to panel
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(speedLabel);
        controlPanel.add(speedSlider);

        // Create info panel
        JPanel infoPanel = createInfoPanel(solution);

        // Add everything to main panel
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(animationPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Enhanced panel for displaying the schedule visualization.
     */
    public static class EnhancedSchedulePanel extends JPanel
            implements MouseWheelListener, MouseMotionListener, MouseListener {
        private ALPSolution solution;
        private int margin = 60;
        private int barHeight = 40;
        private int spacing = 20;
        private double timeScale = 5.0;
        private double zoomFactor = 1.0;
        private Point2D dragStart = null;
        private int translateX = 0;
        private int translateY = 0;
        private String tooltip = null;
        private Point tooltipPoint = null;

        public EnhancedSchedulePanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(BACKGROUND_COLOR);
            setPreferredSize(new Dimension(2000, calculateHeight()));

            // Add interaction listeners
            addMouseWheelListener(this);
            addMouseMotionListener(this);
            addMouseListener(this);

            ToolTipManager.sharedInstance().registerComponent(this);
            ToolTipManager.sharedInstance().setInitialDelay(0);
        }

        private int calculateHeight() {
            ALPInstance instance = solution.getInstance();
            int numRunways = instance.getNumRunways();
            return margin * 2 + numRunways * (barHeight + spacing * 2) + 100; // Extra space for legend
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Save the original transform
            AffineTransform originalTransform = g2d.getTransform();

            // Apply translation for panning
            g2d.translate(translateX, translateY);

            // Apply zoom
            double effectiveTimeScale = timeScale * zoomFactor;

            ALPInstance instance = solution.getInstance();
            int numRunways = instance.getNumRunways();
            int numAircraft = instance.getNumAircraft();

            // Find the latest landing time for scaling
            int maxLandingTime = 0;
            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                if (landingTime > maxLandingTime) {
                    maxLandingTime = landingTime;
                }
            }

            // Draw title and info
            g2d.setFont(TITLE_FONT);
            g2d.setColor(TITLE_COLOR);
            g2d.drawString("Aircraft Landing Schedule - " + solution.getProblemVariant(),
                    margin, 30);

            // Draw time axis with grid
            drawTimeAxis(g2d, maxLandingTime, effectiveTimeScale);

            // Draw runway lanes with backgrounds
            drawRunwayLanes(g2d, numRunways, maxLandingTime, effectiveTimeScale);

            // Prepare colors for each aircraft
            Map<Integer, Color> aircraftColors = generateAircraftColors(numAircraft);

            // Draw aircraft landings
            drawAircraftLandings(g2d, numAircraft, effectiveTimeScale, aircraftColors);

            // Draw legend
            drawLegend(g2d, aircraftColors);

            // Restore original transform
            g2d.setTransform(originalTransform);

            // Draw tooltip if active
            if (tooltip != null && tooltipPoint != null) {
                drawTooltip(g2d, tooltip, tooltipPoint);
            }
        }

        private void drawTimeAxis(Graphics2D g2d, int maxLandingTime, double effectiveTimeScale) {
            int axisY = getHeight() - margin;

            // Draw main axis line
            g2d.setColor(AXIS_COLOR);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(margin, axisY, margin + (int) (maxLandingTime * effectiveTimeScale), axisY);

            // Calculate appropriate tick interval based on zoom level
            int tickInterval = calculateTickInterval(maxLandingTime, effectiveTimeScale);

            // Draw time ticks, grid lines and labels
            g2d.setFont(LABEL_FONT);
            for (int t = 0; t <= maxLandingTime; t += tickInterval) {
                int x = margin + (int) (t * effectiveTimeScale);

                // Vertical grid line
                g2d.setColor(GRID_COLOR);
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10.0f, new float[] { 5.0f }, 0.0f));
                g2d.drawLine(x, margin, x, axisY);

                // Tick mark
                g2d.setColor(AXIS_COLOR);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(x, axisY, x, axisY + 5);

                // Time label
                String timeLabel = String.valueOf(t);
                FontMetrics fm = g2d.getFontMetrics();
                int labelWidth = fm.stringWidth(timeLabel);
                g2d.setColor(TEXT_COLOR);
                g2d.drawString(timeLabel, x - labelWidth / 2, axisY + 20);
            }
        }

        private int calculateTickInterval(int maxTime, double scale) {
            int targetTickCount = 20; // Aim for approximately this many ticks

            int interval = Math.max(1, maxTime / targetTickCount);

            // Round to nice numbers (1, 2, 5, 10, 20, 50, etc.)
            int magnitude = (int) Math.floor(Math.log10(interval));
            int normalized = (int) (interval / Math.pow(10, magnitude));

            if (normalized <= 1)
                normalized = 1;
            else if (normalized <= 2)
                normalized = 2;
            else if (normalized <= 5)
                normalized = 5;
            else
                normalized = 10;

            return normalized * (int) Math.pow(10, magnitude);
        }

        private void drawRunwayLanes(Graphics2D g2d, int numRunways, int maxLandingTime, double effectiveTimeScale) {
            for (int r = 0; r < numRunways; r++) {
                int y = margin + r * (barHeight + spacing * 2) + spacing;
                int laneWidth = (int) (maxLandingTime * effectiveTimeScale);

                // Lane background
                Color runwayColor = RUNWAY_COLORS[r % RUNWAY_COLORS.length];
                Color laneBackground = new Color(runwayColor.getRed(), runwayColor.getGreen(),
                        runwayColor.getBlue(), 40); // Semi-transparent
                g2d.setColor(laneBackground);
                g2d.fillRoundRect(margin, y, laneWidth, barHeight, 10, 10);

                // Lane border
                g2d.setColor(runwayColor);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(margin, y, laneWidth, barHeight, 10, 10);

                // Runway label
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2d.setColor(TITLE_COLOR);
                g2d.drawString("Runway " + (r + 1), margin - 55, y + barHeight / 2 + 5);
            }
        }

        private Map<Integer, Color> generateAircraftColors(int numAircraft) {
            Map<Integer, Color> colors = new HashMap<>();

            for (int i = 0; i < numAircraft; i++) {
                // Generate distinct colors using HSB color model
                float hue = i * (1.0f / numAircraft);
                colors.put(i, Color.getHSBColor(hue, 0.8f, 0.9f));
            }

            return colors;
        }

        private void drawAircraftLandings(Graphics2D g2d, int numAircraft, double effectiveTimeScale,
                Map<Integer, Color> aircraftColors) {
            ALPInstance instance = solution.getInstance();

            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);
                AircraftData aircraft = instance.getAircraft().get(i);

                int x = margin + (int) (landingTime * effectiveTimeScale);
                int y = margin + runway * (barHeight + spacing * 2) + spacing;

                // Aircraft rectangle at landing point
                Color aircraftColor = aircraftColors.get(i);

                // Draw aircraft icon
                drawAircraftIcon(g2d, x, y + barHeight / 2, aircraftColor, 15, i + 1);

                // Draw time window and target lines
                drawAircraftTimeWindows(g2d, aircraft, effectiveTimeScale, y, barHeight);
            }
        }

        private void drawAircraftIcon(Graphics2D g2d, int x, int y, Color color, int size, int id) {
            // Draw a small airplane shape
            int[] xPoints = { x, x - size, x - size / 2, x - size / 2, x, x + size / 2, x + size / 2, x + size };
            int[] yPoints = { y - size / 3, y, y, y + size / 2, y + size / 4, y + size / 2, y, y };

            g2d.setColor(color);
            g2d.fillPolygon(xPoints, yPoints, xPoints.length);

            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawPolygon(xPoints, yPoints, xPoints.length);

            // Draw aircraft ID
            g2d.setFont(AIRCRAFT_FONT);
            String idText = String.valueOf(id);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(idText);
            g2d.setColor(Color.WHITE);
            g2d.drawString(idText, x - textWidth / 2, y + 4);
        }

        private void drawAircraftTimeWindows(Graphics2D g2d, AircraftData aircraft, double effectiveTimeScale,
                int y, int barHeight) {
            int earliestTime = aircraft.getEarliestLandingTime();
            int latestTime = aircraft.getLatestLandingTime();
            int targetTime = aircraft.getTargetLandingTime();

            int earliestX = margin + (int) (earliestTime * effectiveTimeScale);
            int latestX = margin + (int) (latestTime * effectiveTimeScale);
            int targetX = margin + (int) (targetTime * effectiveTimeScale);

            // Time window line
            g2d.setColor(new Color(100, 100, 255, 120));
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[] { 5.0f }, 0.0f));
            g2d.drawLine(earliestX, y + barHeight / 2, latestX, y + barHeight / 2);

            // Target time line
            g2d.setColor(new Color(255, 50, 50, 180));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(targetX, y, targetX, y + barHeight);

            // Earliest and latest time markers
            int markerSize = 5;
            g2d.setColor(new Color(0, 0, 255, 150));
            g2d.fillRect(earliestX - markerSize / 2, y, markerSize, barHeight);
            g2d.fillRect(latestX - markerSize / 2, y, markerSize, barHeight);
        }

        private void drawLegend(Graphics2D g2d, Map<Integer, Color> aircraftColors) {
            int legendY = getHeight() - margin + 40;
            int legendX = margin;
            int boxSize = 15;
            int legendSpacing = 150;

            g2d.setFont(INFO_FONT);

            // Time window
            g2d.setColor(new Color(100, 100, 255, 120));
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[] { 5.0f }, 0.0f));
            g2d.drawLine(legendX, legendY, legendX + boxSize * 2, legendY);
            g2d.setColor(TEXT_COLOR);
            g2d.drawString("Time Window", legendX + boxSize * 2 + 5, legendY + 5);

            // Target time
            legendX += legendSpacing;
            g2d.setColor(new Color(255, 50, 50, 180));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(legendX + boxSize, legendY - boxSize / 2, legendX + boxSize, legendY + boxSize / 2);
            g2d.setColor(TEXT_COLOR);
            g2d.drawString("Target Time", legendX + boxSize * 2 + 5, legendY + 5);

            // Aircraft
            legendX += legendSpacing;
            Color sampleColor = aircraftColors.get(0);
            drawAircraftIcon(g2d, legendX + boxSize, legendY, sampleColor, 15, 1);
            g2d.setColor(TEXT_COLOR);
            g2d.drawString("Aircraft", legendX + boxSize * 2 + 5, legendY + 5);

            // Runway
            legendX += legendSpacing;
            Color runwayColor = RUNWAY_COLORS[0];
            g2d.setColor(runwayColor);
            g2d.fillRoundRect(legendX, legendY - boxSize / 2, boxSize * 2, boxSize, 5, 5);
            g2d.setColor(TEXT_COLOR);
            g2d.drawString("Runway", legendX + boxSize * 2 + 5, legendY + 5);
        }

        private void drawTooltip(Graphics2D g2d, String text, Point location) {
            FontMetrics fm = g2d.getFontMetrics(INFO_FONT);
            Rectangle2D textBounds = fm.getStringBounds(text, g2d);

            int padding = 5;
            int tooltipX = location.x;
            int tooltipY = location.y - 30; // Position above the mouse cursor
            int tooltipWidth = (int) textBounds.getWidth() + padding * 2;
            int tooltipHeight = (int) textBounds.getHeight() + padding * 2;

            // Draw tooltip background
            g2d.setColor(new Color(255, 255, 220));
            g2d.fillRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 6, 6);

            // Draw tooltip border
            g2d.setColor(new Color(150, 150, 150));
            g2d.drawRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 6, 6);

            // Draw tooltip text
            g2d.setColor(Color.BLACK);
            g2d.setFont(INFO_FONT);
            g2d.drawString(text, tooltipX + padding, tooltipY + fm.getAscent() + padding);
        }

        public void zoomIn() {
            zoomFactor *= 1.2;
            repaint();
        }

        public void zoomOut() {
            zoomFactor = Math.max(0.1, zoomFactor / 1.2);
            repaint();
        }

        public void resetZoom() {
            zoomFactor = 1.0;
            translateX = 0;
            translateY = 0;
            repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragStart != null) {
                translateX += e.getX() - dragStart.getX();
                translateY += e.getY() - dragStart.getY();
                dragStart = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            e.getPoint();

            // Check if mouse is over an aircraft
            Point transformedPoint = new Point(
                    e.getX() - translateX,
                    e.getY() - translateY);

            ALPInstance instance = solution.getInstance();
            int numAircraft = instance.getNumAircraft();
            double effectiveTimeScale = timeScale * zoomFactor;

            // Clear tooltip
            boolean foundAircraft = false;

            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);
                AircraftData aircraft = instance.getAircraft().get(i);

                int x = margin + (int) (landingTime * effectiveTimeScale);
                int y = margin + runway * (barHeight + spacing * 2) + spacing + barHeight / 2;

                // Check if mouse is near aircraft
                if (Math.abs(transformedPoint.x - x) < 20 && Math.abs(transformedPoint.y - y) < 20) {
                    // Create tooltip
                    tooltip = "Aircraft " + (i + 1) +
                            "\nLanding Time: " + landingTime +
                            "\nTarget Time: " + aircraft.getTargetLandingTime() +
                            "\nRunway: " + (runway + 1);
                    tooltipPoint = e.getPoint();
                    foundAircraft = true;
                    repaint();
                    break;
                }
            }

            if (!foundAircraft && tooltip != null) {
                tooltip = null;
                repaint();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            dragStart = e.getPoint();
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragStart = null;
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Not used
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Not used
        }

        @Override
        public void mouseExited(MouseEvent e) {
            tooltip = null;
            repaint();
        }
    }

    /**
     * Panel for animated visualization of aircraft landing on runways
     */
    public static class RunwayAnimationPanel extends JPanel implements ActionListener {
        private ALPSolution solution;
        private Timer animationTimer;
        private int currentTime = 0;
        private int maxTime;
        private boolean animationRunning = false;
        private BufferedImage backgroundImage;

        private final Color SKY_COLOR = new Color(135, 206, 250);
        private final Color GROUND_COLOR = new Color(76, 153, 0);
        private final Color RUNWAY_COLOR = new Color(80, 80, 80);
        private final Color RUNWAY_STRIPE_COLOR = Color.WHITE;

        // Aircraft state for animation
        private Map<Integer, AircraftState> aircraftStates = new HashMap<>();

        public RunwayAnimationPanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(SKY_COLOR);
            setPreferredSize(new Dimension(1000, 500));

            // Initialize animation timer
            animationTimer = new Timer(100, this);

            // Determine max time for the animation
            maxTime = 0;
            for (int i = 0; i < solution.getInstance().getNumAircraft(); i++) {
                int landingTime = solution.getLandingTime(i);
                if (landingTime > maxTime) {
                    maxTime = landingTime;
                }
            }

            // Add 20% to max time for animation to complete
            maxTime = (int) (maxTime * 1.2);

            // Initialize aircraft states
            initializeAircraftStates();

            // Create background image
            createBackgroundImage();
        }

        private void initializeAircraftStates() {
            ALPInstance instance = solution.getInstance();
            int numAircraft = instance.getNumAircraft();

            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);

                // Calculate approach start time (20% of total earlier than landing)
                int approachStartTime = (int) (landingTime * 0.8);

                // Generate a random initial position off-screen
                double angle = Math.random() * Math.PI / 2 + Math.PI / 4; // 45-135 degrees
                double distance = 500 + Math.random() * 300; // Varying distances
                int startX = (int) (Math.cos(angle) * distance);
                int startY = (int) (Math.sin(angle) * distance * -1); // Negative to go above

                AircraftState state = new AircraftState(
                        i, runway, landingTime, approachStartTime,
                        startX, startY);

                // Assign a unique color
                float hue = i * (1.0f / numAircraft);
                state.color = Color.getHSBColor(hue, 0.8f, 0.9f);

                aircraftStates.put(i, state);
            }
        }

        private void createBackgroundImage() {
            // Create once for performance
            backgroundImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); // Placeholder

            // Real creation happens when we know the size
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    createSizedBackgroundImage();
                }
            });
        }

        private void createSizedBackgroundImage() {
            if (getWidth() <= 0 || getHeight() <= 0)
                return;

            backgroundImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = backgroundImage.createGraphics();

            // Draw sky
            GradientPaint skyGradient = new GradientPaint(
                    0, 0, new Color(100, 181, 246),
                    0, getHeight() * 0.7f, new Color(197, 225, 252));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw ground
            int groundY = (int) (getHeight() * 0.7);
            g2d.setColor(GROUND_COLOR);
            g2d.fillRect(0, groundY, getWidth(), getHeight() - groundY);

            // Draw runways
            int numRunways = solution.getInstance().getNumRunways();
            drawRunways(g2d, numRunways, groundY);

            g2d.dispose();
        }

        private void drawRunways(Graphics2D g2d, int numRunways, int groundY) {
            int runwayWidth = 40;
            int runwaySpacing = getWidth() / (numRunways + 1);

            for (int r = 0; r < numRunways; r++) {
                int runwayX = (r + 1) * runwaySpacing - runwayWidth / 2;

                // Draw runway
                g2d.setColor(RUNWAY_COLOR);
                g2d.fillRect(runwayX, groundY - 5, runwayWidth, getHeight() - groundY + 5);

                // Draw runway stripes
                g2d.setColor(RUNWAY_STRIPE_COLOR);
                int stripeWidth = 4;
                int stripeLength = 20;
                int stripeSpacing = 40;

                for (int y = groundY; y < getHeight(); y += stripeSpacing) {
                    g2d.fillRect(runwayX + runwayWidth / 2 - stripeWidth / 2, y, stripeWidth, stripeLength);
                }

                // Draw runway number
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2d.setColor(Color.WHITE);
                String runwayLabel = String.valueOf(r + 1);
                FontMetrics fm = g2d.getFontMetrics();
                int labelWidth = fm.stringWidth(runwayLabel);
                g2d.drawString(runwayLabel,
                        runwayX + runwayWidth / 2 - labelWidth / 2,
                        groundY - 10);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the background with runways
            if (backgroundImage.getWidth() != getWidth() || backgroundImage.getHeight() != getHeight()) {
                createSizedBackgroundImage();
            }
            g2d.drawImage(backgroundImage, 0, 0, null);

            // Calculate runway positions
            int numRunways = solution.getInstance().getNumRunways();
            int runwaySpacing = getWidth() / (numRunways + 1);
            int groundY = (int) (getHeight() * 0.7);

            // Draw aircraft
            for (AircraftState aircraft : aircraftStates.values()) {
                if (currentTime < aircraft.approachStartTime)
                    continue;

                // Calculate runway x position
                int runwayX = (aircraft.runway + 1) * runwaySpacing;

                // Calculate aircraft position based on time
                if (currentTime <= aircraft.landingTime) {
                    // Aircraft is approaching
                    double progress = (double) (currentTime - aircraft.approachStartTime) /
                            (aircraft.landingTime - aircraft.approachStartTime);

                    int currentX = (int) (aircraft.startX + (runwayX - aircraft.startX) * progress);
                    int currentY = (int) (aircraft.startY + (groundY - aircraft.startY) * progress);

                    drawAircraft(g2d, currentX, currentY, aircraft.color, aircraft.id + 1);
                } else {
                    // Aircraft has landed, draw on runway
                    // Calculate how far down the runway based on time since landing
                    double runwayProgress = Math.min(1.0, (currentTime - aircraft.landingTime) / 20.0);
                    int landedY = (int) (groundY + (getHeight() - groundY) * runwayProgress);

                    drawAircraftOnGround(g2d, runwayX, landedY, aircraft.color, aircraft.id + 1);
                }
            }

            // Draw time display
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2d.setColor(Color.BLACK);
            g2d.drawString("Time: " + currentTime, 30, 30);
        }

        private void drawAircraft(Graphics2D g2d, int x, int y, Color color, int id) {
            int size = 20;

            // Draw aircraft shape in air
            int[] xPoints = { x, x - size, x - size / 3, x - size / 3, x, x + size / 3, x + size / 3, x + size };
            int[] yPoints = { y - size / 3, y, y, y + size / 2, y + size / 3, y + size / 2, y, y };

            g2d.setColor(color);
            g2d.fillPolygon(xPoints, yPoints, 8);

            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawPolygon(xPoints, yPoints, 8);

            // Draw aircraft ID
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2d.setColor(Color.WHITE);
            String idText = String.valueOf(id);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(idText);
            g2d.drawString(idText, x - textWidth / 2, y + 4);
        }

        private void drawAircraftOnGround(Graphics2D g2d, int x, int y, Color color, int id) {
            int size = 15;

            // Draw a simplified aircraft on ground (top view)
            g2d.setColor(color);
            g2d.fillOval(x - size / 2, y - size, size, size * 2);

            // Draw wings
            g2d.fillRect(x - size, y - size / 3, size * 2, size / 3);

            // Border
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval(x - size / 2, y - size, size, size * 2);
            g2d.drawRect(x - size, y - size / 3, size * 2, size / 3);

            // ID
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2d.setColor(Color.WHITE);
            String idText = String.valueOf(id);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(idText);
            g2d.drawString(idText, x - textWidth / 2, y);
        }

        public void startAnimation() {
            if (!animationRunning) {
                animationTimer.start();
                animationRunning = true;
            }
        }

        public void pauseAnimation() {
            if (animationRunning) {
                animationTimer.stop();
                animationRunning = false;
            }
        }

        public void resetAnimation() {
            currentTime = 0;
            if (animationRunning) {
                animationTimer.stop();
                animationRunning = false;
            }
            repaint();
        }

        public void setAnimationSpeed(int speed) {
            animationTimer.setDelay(1000 / speed);
            if (animationRunning) {
                animationTimer.restart();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Update animation time
            currentTime++;

            // Check if animation has reached the end
            if (currentTime > maxTime) {
                pauseAnimation();
            }

            repaint();
        }

        /**
         * Inner class to represent aircraft state for animation
         */
        public class AircraftState {
            int id;
            int runway;
            int landingTime;
            int approachStartTime;
            int startX;
            int startY;
            Color color;

            public AircraftState(int id, int runway, int landingTime, int approachStartTime,
                    int startX, int startY) {
                this.id = id;
                this.runway = runway;
                this.landingTime = landingTime;
                this.approachStartTime = approachStartTime;
                this.startX = startX;
                this.startY = startY;
            }
        }
    }
}
