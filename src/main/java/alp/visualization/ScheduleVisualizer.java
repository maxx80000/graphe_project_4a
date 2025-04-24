package alp.visualization;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.JPanel;

import alp.model.ALPInstance;
import alp.model.ALPSolution;
import alp.model.AircraftData;

/**
 * Class to visualize the aircraft landing schedule.
 */
public class ScheduleVisualizer {

    /**
     * Displays a Gantt chart visualization of the aircraft landing schedule.
     */
    public static void visualizeSchedule(ALPSolution solution) {
        JFrame frame = new JFrame("Aircraft Landing Schedule - " + solution.getProblemVariant());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1000, 600);
        
        SchedulePanel panel = new SchedulePanel(solution);
        frame.add(panel);
        
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
     * Panel for displaying the schedule visualization.
     */
    private static class SchedulePanel extends JPanel {
        private ALPSolution solution;
        private int margin = 50;
        private int barHeight = 30;
        private int spacing = 10;
        private int timeScale = 5;
        
        public SchedulePanel(ALPSolution solution) {
            this.solution = solution;
            setBackground(Color.WHITE);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
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
            
            // Adjust time scale if necessary
            if (maxLandingTime * timeScale > getWidth() - 2 * margin) {
                timeScale = Math.max(1, (getWidth() - 2 * margin) / maxLandingTime);
            }
            
            // Draw time axis
            g2d.setColor(Color.BLACK);
            g2d.drawLine(margin, getHeight() - margin, margin + maxLandingTime * timeScale, getHeight() - margin);
            
            // Draw time ticks and labels
            int tickInterval = Math.max(1, maxLandingTime / 20); // Aim for roughly 20 ticks
            for (int t = 0; t <= maxLandingTime; t += tickInterval) {
                int x = margin + t * timeScale;
                g2d.drawLine(x, getHeight() - margin, x, getHeight() - margin + 5);
                g2d.drawString(String.valueOf(t), x - 5, getHeight() - margin + 20);
            }
            
            // Draw runway labels
            for (int r = 0; r < numRunways; r++) {
                int y = margin + r * (barHeight + spacing * 2) + barHeight / 2;
                g2d.drawString("Runway " + (r + 1), margin - 45, y + 5);
            }
            
            // Prepare colors for each aircraft
            List<Color> colors = new ArrayList<>();
            for (int i = 0; i < numAircraft; i++) {
                int hue = (i * 360 / numAircraft) % 360;
                colors.add(Color.getHSBColor(hue / 360.0f, 0.8f, 0.9f));
            }
            
            // Draw aircraft landings
            for (int i = 0; i < numAircraft; i++) {
                int landingTime = solution.getLandingTime(i);
                int runway = solution.getRunwayAssignment(i);
                AircraftData aircraft = instance.getAircraft().get(i);
                
                int x = margin + landingTime * timeScale;
                int y = margin + runway * (barHeight + spacing * 2);
                
                // Draw aircraft rectangle
                g2d.setColor(colors.get(i));
                g2d.fillRect(x - 5, y, 10, barHeight);
                
                // Draw target time line if applicable
                int targetTime = aircraft.getTargetLandingTime();
                int targetX = margin + targetTime * timeScale;
                g2d.setColor(Color.RED);
                g2d.drawLine(targetX, y, targetX, y + barHeight);
                
                // Draw earliest and latest time lines
                int earliestTime = aircraft.getEarliestLandingTime();
                int latestTime = aircraft.getLatestLandingTime();
                int earliestX = margin + earliestTime * timeScale;
                int latestX = margin + latestTime * timeScale;
                g2d.setColor(Color.BLUE);
                g2d.drawLine(earliestX, y, earliestX, y + barHeight);
                g2d.drawLine(latestX, y, latestX, y + barHeight);
                
                // Draw aircraft ID
                g2d.setColor(Color.BLACK);
                g2d.drawString("A" + (i + 1), x + 12, y + barHeight / 2 + 5);
                
                // Draw landing time
                g2d.drawString(String.valueOf(landingTime), x - 5, y - 5);
            }
            
            // Draw legend
            drawLegend(g2d);
            
            // Draw title
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Aircraft Landing Schedule - " + solution.getProblemVariant(), 
                         margin, margin - 20);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Instance: " + instance.getInstanceName() + " | Objective Value: " + 
                         String.format("%.2f", solution.getObjectiveValue()) + " | Solve Time: " + 
                         String.format("%.2f", solution.getSolveTime()) + " seconds", 
                         margin, margin - 5);
        }
        
        private void drawLegend(Graphics2D g2d) {
            int legendX = margin;
            int legendY = getHeight() - margin + 40;
            int boxSize = 10;
            int textOffset = 15;
            
            // Earliest time
            g2d.setColor(Color.BLUE);
            g2d.drawLine(legendX, legendY, legendX, legendY + boxSize);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Earliest Landing Time", legendX + textOffset, legendY + boxSize);
            
            // Latest time
            legendX += 150;
            g2d.setColor(Color.BLUE);
            g2d.drawLine(legendX, legendY, legendX, legendY + boxSize);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Latest Landing Time", legendX + textOffset, legendY + boxSize);
            
            // Target time
            legendX += 150;
            g2d.setColor(Color.RED);
            g2d.drawLine(legendX, legendY, legendX, legendY + boxSize);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Target Landing Time", legendX + textOffset, legendY + boxSize);
            
            // Aircraft
            legendX += 150;
            g2d.setColor(Color.getHSBColor(0.3f, 0.8f, 0.9f));
            g2d.fillRect(legendX, legendY, boxSize, boxSize);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Aircraft", legendX + textOffset, legendY + boxSize);
        }
    }
}

