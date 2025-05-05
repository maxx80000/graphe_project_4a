package alp.visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Classe utilitaire pour les composants UI modernes et les icônes SVG
 */
public class UIUtils {

    // Constantes de style
    public static final Color PRIMARY_COLOR = new Color(41, 121, 255);
    public static final Color SECONDARY_COLOR = new Color(67, 99, 216);
    public static final Color ACCENT_COLOR = new Color(255, 94, 87);
    public static final Color BACKGROUND_COLOR = new Color(242, 244, 248);
    public static final Color PANEL_BACKGROUND = new Color(255, 255, 255);
    public static final Color GRID_COLOR = new Color(230, 234, 240);
    public static final Color TEXT_PRIMARY = new Color(50, 60, 70);
    public static final Color TEXT_SECONDARY = new Color(120, 130, 140);
    public static final Color SUCCESS_COLOR = new Color(0, 200, 83);
    public static final Color ERROR_COLOR = new Color(255, 75, 75);
    public static final Color WARNING_COLOR = new Color(255, 171, 0);

    // Polices
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font SUBHEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 11);

    // Constantes de mise en page
    public static final int BUTTON_RADIUS = 8;
    public static final int PANEL_RADIUS = 12;
    public static final int SPACING = 16;

    /**
     * Crée un bouton moderne avec icône SVG et animation au survol
     */
    public static JButton createPrimaryButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(HEADER_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Animation au survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(61, 141, 255));
                    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR);
                    button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        if (action != null) {
            button.addActionListener(e -> action.run());
        }

        return styleAsRoundedButton(button);
    }

    /**
     * Crée un bouton secondaire moderne
     */
    public static JButton createSecondaryButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(NORMAL_FONT);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(BACKGROUND_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        // Animation au survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(230, 234, 240));
                    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(BACKGROUND_COLOR);
                    button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        if (action != null) {
            button.addActionListener(e -> action.run());
        }

        return styleAsRoundedButton(button);
    }

    /**
     * Style un bouton avec des coins arrondis
     */
    private static JButton styleAsRoundedButton(JButton button) {
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                JButton b = (JButton) c;

                if (b.getModel().isPressed()) {
                    g2d.setColor(b.getBackground().darker());
                } else if (b.getModel().isRollover()) {
                    g2d.setColor(b.getBackground().brighter());
                } else {
                    g2d.setColor(b.getBackground());
                }

                g2d.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), BUTTON_RADIUS, BUTTON_RADIUS);

                super.paint(g, c);
                g2d.dispose();
            }
        });

        return button;
    }

    /**
     * Crée un JPanel avec coins arrondis et ombre portée
     */
    public static JPanel createRoundedPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Ombre portée légère
                g2d.setColor(new Color(0, 0, 0, 10));
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, PANEL_RADIUS, PANEL_RADIUS);

                // Arrière-plan du panneau
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, PANEL_RADIUS, PANEL_RADIUS);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);
        panel.setBackground(PANEL_BACKGROUND);
        return panel;
    }

    /**
     * Crée une icône SVG d'avion
     */
    public static Icon createAirplaneIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);

                // Dessin SVG d'un avion
                Path2D path = new Path2D.Float();

                // Corps de l'avion
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
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée une icône SVG de lecture (Play)
     */
    public static Icon createPlayIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);

                // Triangle pour l'icône play
                int[] xPoints = {
                        size / 4,
                        size / 4,
                        size * 3 / 4
                };

                int[] yPoints = {
                        size / 4,
                        size * 3 / 4,
                        size / 2
                };

                g2d.fillPolygon(xPoints, yPoints, 3);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée une icône SVG de pause
     */
    public static Icon createPauseIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);

                // Deux barres pour l'icône pause
                g2d.fillRect(size / 3 - 2, size / 4, size / 6, size / 2);
                g2d.fillRect(size * 2 / 3 - size / 6, size / 4, size / 6, size / 2);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée une icône SVG de téléchargement
     */
    public static Icon createDownloadIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);

                // Flèche vers le bas
                int arrowWidth = size / 3;
                g2d.fillRect(size / 2 - arrowWidth / 2, size / 4, arrowWidth, size / 2);

                // Pointe de la flèche
                int[] xPoints = {
                        size / 4,
                        size / 2,
                        size * 3 / 4
                };

                int[] yPoints = {
                        size / 2,
                        size * 3 / 4,
                        size / 2
                };

                g2d.fillPolygon(xPoints, yPoints, 3);

                // Ligne horizontale en bas
                g2d.fillRect(size / 4, size * 3 / 4, size / 2, size / 12);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée une icône SVG de visualisation (œil)
     */
    public static Icon createVisualizeIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);

                // L'œil
                g2d.fillOval(size / 6, size / 3, size * 2 / 3, size / 3);

                // Pupille
                g2d.setColor(PANEL_BACKGROUND);
                g2d.fillOval(size * 2 / 5, size * 2 / 5, size / 5, size / 5);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée une icône SVG de comparaison (balance)
     */
    public static Icon createCompareIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);

                // Barre centrale verticale
                g2d.fillRect(size / 2 - size / 12, size / 4, size / 6, size / 2);

                // Barre horizontale
                g2d.fillRect(size / 6, size / 3, size * 2 / 3, size / 10);

                // Les deux plateaux
                g2d.fillOval(size / 6, size * 2 / 5, size / 4, size / 10);
                g2d.fillOval(size * 2 / 3 - size / 12, size * 2 / 5, size / 4, size / 10);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée une icône SVG de suppression (X)
     */
    public static Icon createClearIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);

                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(size / 8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Croix
                g2d.drawLine(size / 4, size / 4, size * 3 / 4, size * 3 / 4);
                g2d.drawLine(size * 3 / 4, size / 4, size / 4, size * 3 / 4);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * Crée un tooltip moderne et informatif
     */
    public static JToolTip createModernToolTip(JComponent component, String text) {
        JToolTip toolTip = new JToolTip() {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(50, 50, 50, 230));
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        toolTip.setFont(SMALL_FONT);
        toolTip.setForeground(Color.WHITE);
        toolTip.setBackground(new Color(0, 0, 0, 0));
        toolTip.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        toolTip.setTipText(text);

        component.setToolTipText(text);

        return toolTip;
    }

    /**
     * Crée un effet d'animation de survol pour un composant
     */
    public static void addHoverEffect(JComponent component, Color normalColor, Color hoverColor) {
        component.setBackground(normalColor);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (component.isEnabled()) {
                    component.setBackground(hoverColor);
                    component.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (component.isEnabled()) {
                    component.setBackground(normalColor);
                    component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }

    /**
     * Ajoute une animation de fondu lors du changement d'un composant
     */
    public static void animateComponentChange(Container container, JComponent oldComponent, JComponent newComponent) {
        if (oldComponent != null) {
            container.remove(oldComponent);
        }

        newComponent.setVisible(false);
        container.add(newComponent);
        container.revalidate();

        // Animation de fondu
        Timer timer = new Timer(20, null);
        final float[] alpha = { 0.0f };

        timer.addActionListener(e -> {
            alpha[0] += 0.1f;
            if (alpha[0] >= 1.0f) {
                alpha[0] = 1.0f;
                timer.stop();
                newComponent.setVisible(true);
            }

            newComponent.setVisible(true);
            container.repaint();
        });

        timer.start();
    }
}