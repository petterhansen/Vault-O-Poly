package ui;

import board.fields.BoardField;
import board.fields.PropertyField;
import board.fields.ResourceField;
import players.Player;
import util.PlayerToken;
import util.PropertyType;
import mechanics.PropertyDevelopment;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

public class BoardPanel extends JPanel {

    private FieldPanel[] fieldPanels;
    private Map<PlayerToken, JLabel> playerLabels;
    private Map<PlayerToken, Color> playerColors;
    private Map<PlayerToken, Integer> playerVisualPositions;

    private JPanel southPanel;
    private JPanel westPanel;
    private JPanel northPanel;
    private JPanel eastPanel;
    private JPanel centerPanel;

    // Icon Cache
    private ImageIcon settlementIcon;
    private ImageIcon fortressIcon;

    public BoardPanel() {
        this.fieldPanels = new FieldPanel[40];
        this.playerLabels = new HashMap<>();
        this.playerColors = new HashMap<>();
        this.playerVisualPositions = new HashMap<>();

        setLayout(new BorderLayout(5, 5));
        setBackground(GameWindow.DARK_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        southPanel = new JPanel(new GridLayout(1, 11, 2, 2));
        westPanel = new JPanel(new GridLayout(9, 1, 2, 2));
        northPanel = new JPanel(new GridLayout(1, 11, 2, 2));
        eastPanel = new JPanel(new GridLayout(9, 1, 2, 2));
        centerPanel = new JPanel(new GridBagLayout());

        southPanel.setBackground(GameWindow.DARK_BACKGROUND);
        westPanel.setBackground(GameWindow.DARK_BACKGROUND);
        northPanel.setBackground(GameWindow.DARK_BACKGROUND);
        eastPanel.setBackground(GameWindow.DARK_BACKGROUND);
        centerPanel.setBackground(GameWindow.DARK_BACKGROUND);

        add(southPanel, BorderLayout.SOUTH);
        add(westPanel, BorderLayout.WEST);
        add(northPanel, BorderLayout.NORTH);
        add(eastPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        JLabel title = new JLabel("Vault-O-Poly");
        title.setFont(GameWindow.FALLOUT_FONT.deriveFont(40f));
        title.setForeground(GameWindow.FALLOUT_GREEN);
        centerPanel.add(title);

        // Load Icons
        settlementIcon = loadIcon("/icons/settlement.png", 12, 12);
        fortressIcon = loadIcon("/icons/fortress.png", 14, 14);
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path);
        }
        return null;
    }

    public void initializeBoard(List<BoardField> fields) {
        for (int i = 0; i < 40; i++) {
            if (i < fields.size()) {
                fieldPanels[i] = new FieldPanel(fields.get(i));
            } else {
                fieldPanels[i] = new FieldPanel(null);
            }
        }

        for (int i = 10; i >= 0; i--) southPanel.add(fieldPanels[i]);
        for (int i = 19; i >= 11; i--) westPanel.add(fieldPanels[i]);
        for (int i = 20; i <= 30; i++) northPanel.add(fieldPanels[i]);
        for (int i = 31; i <= 39; i++) eastPanel.add(fieldPanels[i]);
    }

    public void addPlayerToken(PlayerToken token) {
        if (playerLabels.containsKey(token)) return;

        JLabel label = new JLabel();
        String imagePath = "/tokens/" + token.name().toLowerCase() + ".png";
        URL imgUrl = getClass().getResource(imagePath);

        if (imgUrl != null) {
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));
        } else {
            label.setText(token.name().substring(0, 2));
            label.setOpaque(true);
        }

        Color labelColor = Color.WHITE;
        switch (token) {
            case VAULT_BOY: labelColor = Color.CYAN; break;
            case NUKA_GIRL: labelColor = Color.RED; break;
            case DOGMEAT:   labelColor = Color.ORANGE; break;
            case GHOUL:     labelColor = Color.GREEN; break;
        }

        if (label.getIcon() == null) label.setBackground(labelColor);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        playerLabels.put(token, label);
        playerColors.put(token, labelColor);
        playerVisualPositions.put(token, 0);

        if (fieldPanels[0] != null) {
            fieldPanels[0].addPlayerToken(label);
        }
        revalidate();
        repaint();
    }

    public void movePlayerToken(PlayerToken token, int targetPosition) {
        JLabel label = playerLabels.get(token);
        if (label == null) return;

        int currentPos = playerVisualPositions.getOrDefault(token, 0);
        if (currentPos == targetPosition) return;

        Timer animationTimer = new Timer(150, null);
        animationTimer.addActionListener(e -> {
            int nextPos = (playerVisualPositions.get(token) + 1) % 40;

            if (label.getParent() != null) {
                label.getParent().remove(label);
            }
            if (fieldPanels[nextPos] != null) {
                fieldPanels[nextPos].addPlayerToken(label);
            }

            playerVisualPositions.put(token, nextPos);
            revalidate();
            repaint();

            if (nextPos == targetPosition) {
                animationTimer.stop();
            }
        });
        animationTimer.start();
    }

    public void removePlayerToken(PlayerToken token) {
        JLabel label = playerLabels.get(token);
        if (label != null) {
            if (label.getParent() != null) label.getParent().remove(label);
            playerLabels.remove(token);
            playerColors.remove(token);
            playerVisualPositions.remove(token);
            revalidate();
            repaint();
        }
    }

    public void resetBoard() {
        southPanel.removeAll();
        westPanel.removeAll();
        northPanel.removeAll();
        eastPanel.removeAll();
        playerLabels.clear();
        playerColors.clear();
        playerVisualPositions.clear();
        this.fieldPanels = new FieldPanel[40];
        revalidate();
        repaint();
    }

    /**
     * Updates visuals for a specific field when an action (Buy/Mortgage) occurs immediately.
     */
    public void updateFieldOwner(int position, Player owner) {
        if (position < 0 || position >= fieldPanels.length || fieldPanels[position] == null) return;

        Color ownerColor = (owner != null) ? playerColors.get(owner.getToken()) : null;
        fieldPanels[position].setOwnerBorder(ownerColor);

        // Force a tooltip refresh with the new owner immediately
        fieldPanels[position].refreshTooltip(owner);
    }

    /**
     * Called by the Heartbeat Loop to update ALL field data from the server.
     */
    public void updateFieldStates(List<BoardField> newFields) {
        for (int i = 0; i < fieldPanels.length; i++) {
            if (i < newFields.size() && fieldPanels[i] != null) {
                fieldPanels[i].updateData(newFields.get(i));
            }
        }
        repaint();
    }


    // --- Inner Class: FieldPanel ---
    private class FieldPanel extends JPanel {
        private JPanel colorBar;
        private JLabel nameLabel;
        private JPanel tokenPanel;
        private BoardField field;

        public FieldPanel(BoardField field) {
            this.field = field;
            setLayout(new BorderLayout(1, 1));
            setBackground(Color.DARK_GRAY);
            setBorder(new LineBorder(GameWindow.FALLOUT_GREEN, 1));

            // Color/Icon Bar
            colorBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            colorBar.setPreferredSize(new Dimension(0, 16));
            colorBar.setOpaque(true);

            nameLabel = new JLabel("<html><center>" + (field != null ? field.getName() : "???") + "</center></html>", SwingConstants.CENTER);
            nameLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(10f));
            nameLabel.setForeground(Color.WHITE);

            tokenPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            tokenPanel.setOpaque(false);

            // Initial Color
            if (field instanceof PropertyField) {
                colorBar.setBackground(getPropertyColor(((PropertyField) field).getPropertyType()));
            } else if (field instanceof ResourceField) {
                colorBar.setBackground(Color.GRAY);
            } else {
                colorBar.setBackground(Color.DARK_GRAY);
            }

            add(colorBar, BorderLayout.NORTH);
            add(nameLabel, BorderLayout.CENTER);
            add(tokenPanel, BorderLayout.SOUTH);

            setPreferredSize(new Dimension(80, 80));

            // Initial Visuals Draw
            refreshVisuals();
        }

        public void addPlayerToken(JLabel tokenLabel) {
            tokenPanel.add(tokenLabel);
        }

        public void setOwnerBorder(Color ownerColor) {
            if (ownerColor != null) {
                setBorder(new LineBorder(ownerColor, 2));
            } else {
                setBorder(new LineBorder(GameWindow.FALLOUT_GREEN, 1));
            }
        }

        // Called by Heartbeat
        public void updateData(BoardField newField) {
            this.field = newField;
            refreshVisuals();
        }

        private void refreshVisuals() {
            Player owner = null;
            Color ownerColor = null;
            int level = 0;

            if (field instanceof PropertyField) {
                PropertyField prop = (PropertyField) field;
                owner = prop.getOwner();
                level = prop.getCurrentImprovementLevel();

                if (owner != null) {
                    ownerColor = playerColors.get(owner.getToken());
                }
            }

            setOwnerBorder(ownerColor);
            refreshTooltip(owner); // Regenerate tooltip text
            updateImprovementIcons(level);
        }

        private void updateImprovementIcons(int level) {
            colorBar.removeAll();

            if (level > 0) {
                if (level >= PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
                    // Max Level -> Fortress
                    if (fortressIcon != null) {
                        colorBar.add(new JLabel(fortressIcon));
                    } else {
                        JLabel star = new JLabel("★");
                        star.setForeground(Color.ORANGE);
                        colorBar.add(star);
                    }
                } else {
                    // Levels 1-2 -> Settlements
                    for (int i = 0; i < level; i++) {
                        if (settlementIcon != null) {
                            colorBar.add(new JLabel(settlementIcon));
                        } else {
                            JLabel star = new JLabel("*");
                            star.setForeground(Color.WHITE);
                            colorBar.add(star);
                        }
                    }
                }
            }
            colorBar.revalidate();
            colorBar.repaint();
        }

        // --- FIXED TOOLTIP GENERATION ---
        public void refreshTooltip(Player owner) {
            if (field == null) return;

            StringBuilder tip = new StringBuilder();
            tip.append("<html><b>").append(field.getName()).append("</b><br>");

            if (field instanceof PropertyField) {
                PropertyField prop = (PropertyField) field;

                // 1. Mortgage Status
                if (prop.isMortgaged()) {
                    tip.append("<font color='red'>[MORTGAGED]</font><br>");
                }

                // 2. Cost
                tip.append("Cost: ").append(prop.getPurchaseCost()).append(" Caps<br>");

                // 3. Owner & Rent
                if (owner != null) {
                    tip.append("Owner: ").append(owner.getName()).append("<br>");

                    // If Mortgaged, rent is 0
                    if (prop.isMortgaged()) {
                        tip.append("Rent: 0 (Mortgaged)<br>");
                    } else {
                        tip.append("Rent: ").append(prop.getRent()).append("<br>");
                    }

                    if (prop.getCurrentImprovementLevel() > 0) {
                        tip.append("Level: ").append(prop.getCurrentImprovementLevel());
                    }
                } else {
                    tip.append("<i>Unowned</i>");
                }
            } else {
                tip.append(field.getDescription());
            }
            tip.append("</html>");
            this.setToolTipText(tip.toString());
        }

        private Color getPropertyColor(PropertyType type) {
            if (type == null) return Color.LIGHT_GRAY;
            switch (type) {
                case SETTLEMENT: return new Color(139, 69, 19);
                case OUTPOST: return new Color(173, 216, 230);
                case RADIO_STATION: return new Color(255, 105, 180);
                case ENCLAVE_OUTPOST: return new Color(255, 165, 0);
                default: return Color.LIGHT_GRAY;
            }
        }
    }
}