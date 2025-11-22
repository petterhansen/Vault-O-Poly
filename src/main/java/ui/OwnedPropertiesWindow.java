package ui;

import board.fields.BoardField;
import board.fields.PropertyField;
import mechanics.PropertyDevelopment;
import players.Player;
import resources.ResourceType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

public class OwnedPropertiesWindow extends JDialog {

    private JList<PropertyField> propertyList;
    private DefaultListModel<PropertyField> listModel;
    private JScrollPane listScroller;

    // Detail panel components
    private JPanel detailPanel;
    private JLabel imageLabel;
    private JLabel nameLabel;
    private JLabel ownerLabel;
    private JLabel groupLabel;
    private JLabel descriptionLabel;
    private JLabel statsLabel;
    private JLabel resourceCostLabel;

    private JButton mortgageButton;

    private Player viewingPlayer;
    private game.GameController controller;

    private String currentImageUrl = null;

    // --- NEW: Icon Cache ---
    private ImageIcon settlementIcon;
    private ImageIcon fortressIcon;

    public OwnedPropertiesWindow(Frame owner, game.GameController controller) {
        super(owner, "All Properties", false);
        this.controller = controller;
        setSize(950, 625);

        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);

        // --- NEW: Load Icons ---
        settlementIcon = loadIcon("/icons/settlement.png", 16, 16); // Small size for text alignment
        fortressIcon = loadIcon("/icons/fortress.png", 16, 16);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clearImage();
            }
        });

        JSplitPane splitPane = new JSplitPane();

        // --- Left Panel (List) ---
        listModel = new DefaultListModel<>();
        propertyList = new JList<>(listModel);

        propertyList.setBackground(GameWindow.DARK_BACKGROUND);
        propertyList.setForeground(Color.WHITE);
        propertyList.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        propertyList.setSelectionBackground(GameWindow.FALLOUT_GREEN);
        propertyList.setSelectionForeground(GameWindow.DARK_BACKGROUND);
        propertyList.setCellRenderer(new PropertyListRenderer());

        listScroller = new JScrollPane(propertyList);
        listScroller.setBorder(BorderFactory.createEmptyBorder());
        styleScrollPane(listScroller);

        splitPane.setLeftComponent(listScroller);

        // --- Right Panel (Details) ---
        detailPanel = new JPanel(new BorderLayout(10, 10));
        detailPanel.setBackground(GameWindow.DARK_BACKGROUND);
        Border greenBorder = BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN);
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(greenBorder, padding));

        imageLabel = new JLabel("Select a property");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setFont(GameWindow.FALLOUT_FONT);
        imageLabel.setForeground(GameWindow.FALLOUT_GREEN);

        nameLabel = new JLabel("");
        nameLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(18f));
        nameLabel.setForeground(GameWindow.FALLOUT_GREEN);
        // Align icon to the left of text if set
        nameLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

        ownerLabel = new JLabel("");
        ownerLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        ownerLabel.setForeground(Color.WHITE);

        groupLabel = new JLabel("");
        groupLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(12f));
        groupLabel.setForeground(Color.GRAY);

        descriptionLabel = new JLabel("");
        descriptionLabel.setFont(GameWindow.FALLOUT_FONT);
        descriptionLabel.setForeground(Color.WHITE);

        statsLabel = new JLabel("");
        statsLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        statsLabel.setForeground(GameWindow.FALLOUT_GREEN);

        resourceCostLabel = new JLabel("");
        resourceCostLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        resourceCostLabel.setForeground(GameWindow.FALLOUT_GREEN);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(GameWindow.DARK_BACKGROUND);

        mortgageButton = new JButton("Mortgage");
        styleButton(mortgageButton);
        mortgageButton.setVisible(false);
        mortgageButton.addActionListener(e -> {
            PropertyField selected = propertyList.getSelectedValue();
            if (selected != null && controller != null) {
                controller.toggleMortgage(viewingPlayer, selected);
                updateDetails(selected);
                propertyList.repaint();
            }
        });
        buttonPanel.add(mortgageButton);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(GameWindow.DARK_BACKGROUND);
        textPanel.add(nameLabel);
        textPanel.add(ownerLabel);
        textPanel.add(groupLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textPanel.add(descriptionLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textPanel.add(statsLabel);
        textPanel.add(resourceCostLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textPanel.add(buttonPanel);

        detailPanel.add(imageLabel, BorderLayout.CENTER);
        detailPanel.add(textPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(detailPanel);

        splitPane.setBackground(GameWindow.DARK_BACKGROUND);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(5);
        splitPane.setDividerLocation(300);

        add(splitPane);

        propertyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                PropertyField selected = propertyList.getSelectedValue();
                if (selected != null) {
                    updateDetails(selected);
                }
            }
        });
    }

    // --- Helper to load icons ---
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

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = GameWindow.FALLOUT_GREEN; this.trackColor = GameWindow.DARK_BACKGROUND; }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { return new JButton() { @Override public Dimension getPreferredSize() { return new Dimension(0, 0); } }; }
        });
    }

    private void styleButton(JButton btn) {
        btn.setFont(GameWindow.FALLOUT_FONT);
        btn.setBackground(new Color(30, 50, 35));
        btn.setForeground(GameWindow.FALLOUT_GREEN);
        btn.setFocusPainted(false);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btn.isVisible())
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/hover.wav").toString());
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (btn.isVisible())
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
            }
        });
    }

    public void update(List<BoardField> allFields, Player currentPlayer) {
        this.viewingPlayer = currentPlayer;

        int selectedIndex = propertyList.getSelectedIndex();
        Point scrollPosition = (listScroller != null && listScroller.getViewport() != null)
                ? listScroller.getViewport().getViewPosition()
                : new Point(0,0);

        listModel.clear();

        if (allFields != null) {
            for (BoardField field : allFields) {
                if (field instanceof PropertyField) {
                    listModel.addElement((PropertyField) field);
                }
            }
        }

        if (!listModel.isEmpty()) {
            if (selectedIndex >= 0 && selectedIndex < listModel.getSize()) {
                propertyList.setSelectedIndex(selectedIndex);
                updateDetails(listModel.get(selectedIndex));
            } else {
                propertyList.setSelectedIndex(0);
            }
        } else {
            clearDetails();
            imageLabel.setText("No properties on board.");
        }

        SwingUtilities.invokeLater(() -> {
            if (listScroller != null && listScroller.getViewport() != null) {
                listScroller.getViewport().setViewPosition(scrollPosition);
            }
        });

        propertyList.repaint();
    }

    private void clearImage() {
        Icon oldIcon = imageLabel.getIcon();
        if (oldIcon instanceof ImageIcon) {
            Image img = ((ImageIcon) oldIcon).getImage();
            if (img != null) img.flush();
        }
        imageLabel.setIcon(null);
    }

    private void clearDetails() {
        nameLabel.setText(""); nameLabel.setIcon(null); // Clear icon
        ownerLabel.setText(""); groupLabel.setText("");
        descriptionLabel.setText(""); statsLabel.setText(""); resourceCostLabel.setText("");
        mortgageButton.setVisible(false);

        clearImage();
        currentImageUrl = null;

        imageLabel.setText("Select a property");
    }

    private Image scaleImage(Image source, int width, int height) {
        BufferedImage scaledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaledImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaledImg;
    }

    private void updateDetails(PropertyField field) {
        if (field == null) {
            clearDetails();
            return;
        }

        nameLabel.setText(field.getName());

        // --- NEW: Update Icon in Details Header ---
        int level = field.getCurrentImprovementLevel();
        if (level >= PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
            nameLabel.setIcon(fortressIcon);
        } else if (level > 0) {
            nameLabel.setIcon(settlementIcon);
        } else {
            nameLabel.setIcon(null);
        }

        descriptionLabel.setText("<html><body style='width: 500px'>" + field.getDescription() + "</body></html>");

        Player owner = field.getOwner();
        if (owner != null) {
            boolean isMine = owner.equals(viewingPlayer);
            ownerLabel.setText("Owner: " + owner.getName());
            ownerLabel.setForeground(isMine ? Color.CYAN : Color.WHITE);

            if (isMine) {
                mortgageButton.setVisible(true);
                if (field.isMortgaged()) {
                    mortgageButton.setText("Un-Mortgage (" + (int)((field.getPurchaseCost()/2)*1.1) + ")");
                    nameLabel.setForeground(Color.RED);
                } else {
                    mortgageButton.setText("Mortgage (+" + (field.getPurchaseCost()/2) + ")");
                    nameLabel.setForeground(GameWindow.FALLOUT_GREEN);
                }
            } else {
                mortgageButton.setVisible(false);
                nameLabel.setForeground(GameWindow.FALLOUT_GREEN);
            }
        } else {
            ownerLabel.setText("Owner: Unowned");
            ownerLabel.setForeground(Color.GRAY);
            mortgageButton.setVisible(false);
            nameLabel.setForeground(GameWindow.FALLOUT_GREEN);
        }

        String groupId = field.getGroupId();
        groupLabel.setText(groupId != null && !groupId.isEmpty() ? "Group: " + groupId.toUpperCase() : "Group: N/A");

        statsLabel.setText(String.format("<html>Rent (Lvl %d): %d Caps<br>Cap Upgrade Cost: %d Caps</html>",
                field.getCurrentImprovementLevel(), field.getRent(), field.getImprovementCost()));

        int currentLevel = field.getCurrentImprovementLevel();
        if (currentLevel >= PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
            resourceCostLabel.setText("Next Upgrade Cost: MAX LEVEL");
            resourceCostLabel.setForeground(Color.ORANGE);
        } else {
            int resourceCost = PropertyDevelopment.RESOURCE_COST_PER_LEVEL * (currentLevel + 1);
            resourceCostLabel.setText(String.format("Next Upgrade Cost: %d %s", resourceCost, ResourceType.SCRAP_MATERIAL.name()));
            resourceCostLabel.setForeground(GameWindow.FALLOUT_GREEN);
        }

        String newUrl = field.getImageUrl();
        if (newUrl == null || newUrl.isEmpty()) {
            if (currentImageUrl != null) {
                clearImage();
                currentImageUrl = null;
                imageLabel.setText("No Image Available");
            }
            return;
        }

        if (newUrl.equals(currentImageUrl)) {
            return;
        }

        clearImage();
        currentImageUrl = newUrl;
        imageLabel.setText("Loading image...");

        SwingWorker<ImageIcon, Void> imageLoader = new SwingWorker<>() {
            @Override protected ImageIcon doInBackground() throws Exception { return new ImageIcon(new URL(newUrl)); }
            @Override protected void done() {
                try {
                    if (!newUrl.equals(currentImageUrl)) return;

                    ImageIcon icon = get();
                    if (icon != null && icon.getIconWidth() > 0) {
                        Image image = scaleImage(icon.getImage(), imageLabel.getWidth(), imageLabel.getHeight());
                        imageLabel.setIcon(new ImageIcon(image));
                        imageLabel.setText(null);
                        icon.getImage().flush();
                    } else {
                        imageLabel.setIcon(null);
                        imageLabel.setText("Image not found");
                    }
                } catch (Exception e) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Image load error");
                }
            }
        };
        imageLoader.execute();
    }

    // --- UPDATED RENDERER TO SHOW ICONS ---
    private class PropertyListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof PropertyField) {
                PropertyField field = (PropertyField) value;
                Player owner = field.getOwner();
                String text;

                String status = field.isMortgaged() ? " [MORTGAGED]" : "";
                String colorHex = field.isMortgaged() ? "#FF6666" : "#FFFFFF";

                if (owner == null) {
                    text = "<html><font color='gray'>" + field.getName() + "</font> <font color='gray'>(Unowned)</font></html>";
                } else if (owner.equals(viewingPlayer)) {
                    text = "<html><font color='"+colorHex+"'>" + field.getName() + status + "</font> <font color='#33FFFF'>(Owned by You)</font></html>";
                } else {
                    text = "<html><font color='"+colorHex+"'>" + field.getName() + status + "</font> <font color='#33FF99'>(Owned)</font></html>";
                }
                setText(text);

                // --- Set Icon Logic ---
                int level = field.getCurrentImprovementLevel();
                if (level >= PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
                    setIcon(fortressIcon);
                } else if (level > 0) {
                    setIcon(settlementIcon);
                } else {
                    setIcon(null);
                }
            }

            c.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
            if (isSelected) {
                c.setBackground(GameWindow.FALLOUT_GREEN);
                c.setForeground(GameWindow.DARK_BACKGROUND);
            } else {
                c.setBackground(GameWindow.DARK_BACKGROUND);
            }
            return c;
        }
    }
}