package ui;

import players.Player;
import resources.ResourceType;
import util.PlayerToken;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class InfoPanel extends JPanel {

    private JLabel turnLabel;
    private JTabbedPane tabbedPane;
    private Map<PlayerToken, PlayerStatsPanel> playerPanels;

    public InfoPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.playerPanels = new HashMap<>();

        // --- THEME ---
        setBackground(GameWindow.DARK_BACKGROUND);
        Border greenBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN),
                "PLAYER INFO",
                0, 0,
                GameWindow.FALLOUT_FONT,
                GameWindow.FALLOUT_GREEN
        );
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                greenBorder
        ));

        turnLabel = new JLabel("Turn: ---");
        turnLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(18f));
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- JTABBEDPANE ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(GameWindow.FALLOUT_FONT);
        tabbedPane.setBackground(GameWindow.DARK_BACKGROUND);
        tabbedPane.setForeground(GameWindow.FALLOUT_GREEN);
        tabbedPane.setFocusable(false);
        tabbedPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleTabbedPane();

        add(turnLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(tabbedPane);

        setPreferredSize(new Dimension(280, 300)); // Slightly wider for icons
    }

    private void styleTabbedPane() {
        UIManager.put("TabbedPane.background", GameWindow.DARK_BACKGROUND);
        UIManager.put("TabbedPane.contentAreaColor", GameWindow.DARK_BACKGROUND);
        UIManager.put("TabbedPane.selected", GameWindow.DARK_BACKGROUND);
        UIManager.put("TabbedPane.unselectedBackground", GameWindow.DARK_BACKGROUND);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.foreground", GameWindow.FALLOUT_GREEN);
        UIManager.put("TabbedPane.borderHightlightColor", GameWindow.FALLOUT_GREEN);
        UIManager.put("TabbedPane.shadow", GameWindow.DARK_BACKGROUND);
        UIManager.put("TabbedPane.darkShadow", GameWindow.DARK_BACKGROUND);
        UIManager.put("TabbedPane.focus", GameWindow.DARK_BACKGROUND);

        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}
            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                g.setColor(GameWindow.FALLOUT_GREEN);
                if (isSelected) g.drawRect(x, y, w - 1, h - 1);
            }
        });
    }

    public void addPlayer(Player player) {
        if (player == null || playerPanels.containsKey(player.getToken())) return;
        PlayerStatsPanel newPanel = new PlayerStatsPanel();
        playerPanels.put(player.getToken(), newPanel);
        tabbedPane.addTab(player.getName(), newPanel);
        tabbedPane.setBackgroundAt(tabbedPane.getTabCount() - 1, GameWindow.DARK_BACKGROUND);
        newPanel.updateDisplay(player);
    }

    public void setTurn(Player player) {
        turnLabel.setText("Turn: " + player.getName());
    }

    public void updatePlayerStats(Player player) {
        if (player == null) return;
        PlayerStatsPanel panel = playerPanels.get(player.getToken());
        if (panel != null) panel.updateDisplay(player);
    }

    public void removePlayer(PlayerToken player) {
        if (player == null) return;
        PlayerStatsPanel panel = playerPanels.remove(player);
        if (panel != null) {
            int tabIndex = tabbedPane.indexOfComponent(panel);
            if (tabIndex != -1) tabbedPane.remove(tabIndex);
        }
    }

    public void resetStats() {
        turnLabel.setText("Turn: ---");
        tabbedPane.removeAll();
        playerPanels.clear();
    }

    public void updatePropertyLists() {
        // Placeholder
    }

    // --- INNER CLASS UPDATED FOR ICONS ---
    private static class PlayerStatsPanel extends JPanel {
        // Map to hold the label for each resource type
        private Map<ResourceType, JLabel> resourceLabels;

        PlayerStatsPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(GameWindow.DARK_BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            resourceLabels = new HashMap<>();

            // 1. Create Bottlecaps Label (Always on top)
            add(createResourceLabel(ResourceType.BOTTLECAPS));
            add(Box.createRigidArea(new Dimension(0, 8))); // Spacer

            // 2. Create Separator
            JSeparator sep = new JSeparator();
            sep.setForeground(GameWindow.FALLOUT_GREEN);
            sep.setBackground(GameWindow.DARK_BACKGROUND);
            add(sep);
            add(Box.createRigidArea(new Dimension(0, 8))); // Spacer

            // 3. Create other resources
            for (ResourceType type : ResourceType.values()) {
                if (type != ResourceType.BOTTLECAPS) {
                    add(createResourceLabel(type));
                    add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }

            // Push content to top
            add(Box.createVerticalGlue());
        }

        private JLabel createResourceLabel(ResourceType type) {
            JLabel label = new JLabel();
            label.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
            label.setForeground(GameWindow.FALLOUT_GREEN);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Load Icon
            String iconName = mapTypeToFilename(type);
            // Try to load from resources
            URL imgUrl = getClass().getResource("/icons/" + iconName);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                // Scale icon to 24x24
                Image img = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(img));
            } else {
                // Fallback if image missing
                label.setText("[" + type.name().substring(0,1) + "] ");
            }

            resourceLabels.put(type, label);
            return label;
        }

        private String mapTypeToFilename(ResourceType type) {
            switch (type) {
                case BOTTLECAPS: return "caps.png";
                case WATER: return "water.png";
                case FOOD: return "food.png";
                case POWER: return "power.png";
                case SCRAP_MATERIAL: return "scrap.png";
                default: return "unknown.png";
            }
        }

        public void updateDisplay(Player player) {
            for (ResourceType type : ResourceType.values()) {
                JLabel label = resourceLabels.get(type);
                if (label != null) {
                    int amount;
                    if (type == ResourceType.BOTTLECAPS) {
                        amount = player.getBottleCaps();
                        // Bold/Highlight Caps
                        label.setText(" Caps: " + amount);
                        label.setForeground(Color.WHITE);
                    } else {
                        amount = player.getResources().getOrDefault(type, 0);
                        label.setText(" " + type.name() + ": " + amount);
                        // Dim resources if 0
                        label.setForeground(amount > 0 ? GameWindow.FALLOUT_GREEN : Color.GRAY);
                    }
                }
            }
        }
    }
}