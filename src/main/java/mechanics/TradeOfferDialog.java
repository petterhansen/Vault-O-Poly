package mechanics;

import board.fields.PropertyField; // <-- ADDED: Import PropertyField
import mechanics.TradeOffer;
import players.Player;
import resources.ResourceType;
import ui.GameWindow;
import util.PlayerToken; // <-- ADDED: Import PlayerToken

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder; // <-- ADDED: Import LineBorder
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // <-- ADDED: Import stream utility

/**
 * A custom JDialog for building a trade offer.
 */
public class TradeOfferDialog extends JDialog {

    private TradeOffer offer;
    private boolean confirmed = false;

    private Map<ResourceType, JTextField> resourceFields;
    private JTextField capsField;

    // --- ADDED FIELDS ---
    private JList<PropertyField> propertyList;
    private DefaultListModel<PropertyField> listModel;
    // --------------------

    public TradeOfferDialog(Frame owner, Player player, String prompt) {
        super(owner, prompt, true);
        this.offer = new TradeOffer();
        this.resourceFields = new EnumMap<>(ResourceType.class);

        // --- Layout ---
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);

        // --- Title ---
        JLabel titleLabel = new JLabel(prompt);
        titleLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
        titleLabel.setForeground(GameWindow.FALLOUT_GREEN);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(titleLabel, BorderLayout.NORTH);

        // --- Center Panel: Contains Inputs (Resources + Caps) and Properties ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(GameWindow.DARK_BACKGROUND);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 1. Resources and Caps Panel ---
        JPanel resourceInputPanel = new JPanel();
        resourceInputPanel.setLayout(new BoxLayout(resourceInputPanel, BoxLayout.Y_AXIS));
        resourceInputPanel.setBackground(GameWindow.DARK_BACKGROUND);
        resourceInputPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GameWindow.FALLOUT_GREEN), "CAPS & RESOURCES", 0, 0, GameWindow.FALLOUT_FONT, GameWindow.FALLOUT_GREEN));

        int maxCaps = (player != null) ? player.getBottleCaps() : 0;
        resourceInputPanel.add(createOfferRow("Caps:", maxCaps));
        capsField = (JTextField) ((JPanel) resourceInputPanel.getComponent(0)).getComponent(1);

        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.BOTTLECAPS) continue;
            int max = (player != null) ? player.getResources().getOrDefault(type, 0) : 0;
            resourceInputPanel.add(createOfferRow(type.name() + ":", max));
            JTextField field = (JTextField) ((JPanel) resourceInputPanel.getComponent(resourceInputPanel.getComponentCount() - 1)).getComponent(1);
            resourceFields.put(type, field);
        }

        centerPanel.add(resourceInputPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- 2. Properties Panel ---
        JPanel propertyPanel = new JPanel(new BorderLayout());
        propertyPanel.setBackground(GameWindow.DARK_BACKGROUND);
        propertyPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GameWindow.FALLOUT_GREEN), "PROPERTIES (Ctrl-Click to select multiple)", 0, 0, GameWindow.FALLOUT_FONT, GameWindow.FALLOUT_GREEN));

        listModel = new DefaultListModel<>();
        // Only add properties that are NOT improved or mortgaged!
        if (player != null) {
            for (PropertyField prop : player.getOwnedProperties()) {
                if (prop.getCurrentImprovementLevel() == 0 && !prop.isMortgaged()) {
                    listModel.addElement(prop);
                }
            }
        }

        propertyList = new JList<>(listModel);
        propertyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Allow multiple selections
        propertyList.setBackground(GameWindow.DARK_BACKGROUND);
        propertyList.setForeground(Color.WHITE);
        propertyList.setFont(GameWindow.FALLOUT_FONT);
        propertyList.setCellRenderer(new PropertyTradeRenderer(player)); // Use custom renderer

        JScrollPane propertyScroller = new JScrollPane(propertyList);
        propertyScroller.setPreferredSize(new Dimension(200, 150));
        propertyPanel.add(propertyScroller, BorderLayout.CENTER);

        centerPanel.add(propertyPanel);


        add(new JScrollPane(centerPanel), BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(GameWindow.DARK_BACKGROUND);

        JButton cancelButton = createFalloutButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton confirmButton = createFalloutButton("Confirm Offer");
        confirmButton.addActionListener(e -> {
            if (buildOfferFromFields(player)) { // Pass player to check max caps
                confirmed = true;
                dispose();
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createOfferRow(String name, int max) {
        JPanel row = new JPanel(new BorderLayout(5, 5));
        row.setBackground(GameWindow.DARK_BACKGROUND);

        JLabel label = new JLabel(name + " (Max: " + max + ")");
        label.setFont(GameWindow.FALLOUT_FONT);
        label.setForeground(Color.WHITE);

        JTextField field = new JTextField("0", 5);
        field.setFont(GameWindow.FALLOUT_FONT);
        field.setBackground(GameWindow.DARK_BACKGROUND);
        field.setForeground(GameWindow.FALLOUT_GREEN);
        field.setCaretColor(GameWindow.FALLOUT_GREEN);
        field.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN, 1));

        row.add(label, BorderLayout.CENTER);
        row.add(field, BorderLayout.EAST);
        return row;
    }

    private boolean buildOfferFromFields(Player player) {
        try {
            // Parse Caps
            int caps = Integer.parseInt(capsField.getText());
            if (caps < 0 || caps > player.getBottleCaps()) {
                throw new NumberFormatException("Invalid caps amount.");
            }
            offer.caps = caps;

            // Parse Resources
            for (Map.Entry<ResourceType, JTextField> entry : resourceFields.entrySet()) {
                int amount = Integer.parseInt(entry.getValue().getText());
                int max = player.getResources().getOrDefault(entry.getKey(), 0);
                if (amount < 0 || amount > max) {
                    throw new NumberFormatException("Invalid amount for " + entry.getKey().name());
                }
                if (amount > 0) {
                    offer.resources.put(entry.getKey(), amount);
                }
            }

            // Collect Properties
            offer.properties.addAll(propertyList.getSelectedValuesList());

            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public TradeOffer getOffer() {
        setVisible(true);
        return confirmed ? offer : null;
    }

    private JButton createFalloutButton(String text) {
        JButton button = new JButton(text);
        button.setFont(GameWindow.FALLOUT_FONT);
        button.setBackground(new Color(30, 50, 35));
        button.setForeground(GameWindow.FALLOUT_GREEN);
        button.setFocusPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                util.WebAudioPlayer.play(getClass().getResource("/sounds/hover.wav").toString());
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (text.equalsIgnoreCase("Cancel")) {
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/cancel.wav").toString());
                } else {
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
                }
            }
        });
        return button;
    }

    /**
     * Custom ListCellRenderer for the trade dialog list.
     */
    private static class PropertyTradeRenderer extends DefaultListCellRenderer {
        private final Player player;

        public PropertyTradeRenderer(Player player) {
            this.player = player;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof PropertyField) {
                PropertyField field = (PropertyField) value;

                // The field should NEVER be improved or mortgaged, but we add a check for safety
                String status = "";
                if (field.getCurrentImprovementLevel() > 0) {
                    status = " <font color='red'>(Lvl " + field.getCurrentImprovementLevel() + " - BLOCKED)</font>";
                } else if (field.isMortgaged()) {
                    status = " <font color='orange'>(MORTGAGED - BLOCKED)</font>";
                }

                String text = "<html>" + field.getName() + status + "</html>";
                setText(text);
            }

            c.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
            if (isSelected) {
                c.setBackground(GameWindow.FALLOUT_GREEN);
                c.setForeground(GameWindow.DARK_BACKGROUND);
            } else {
                c.setBackground(GameWindow.DARK_BACKGROUND);
                c.setForeground(Color.WHITE);
            }
            return c;
        }
    }
}