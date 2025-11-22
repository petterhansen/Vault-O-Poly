package ui;

import game.GameController;
import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private JButton rollButton;
    private JButton improveButton;
    private JButton tradeButton;
    private JButton settingsButton; // <--- REPLACED fullLogButton
    private JButton leaveGameButton;
    private JButton propertiesButton;
    private JButton casinoButton;

    private GameController controller;

    public ControlPanel() {
        setBackground(GameWindow.DARK_BACKGROUND);
        setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));

        // --- NEW: Settings Button ---
        settingsButton = createFalloutButton("Settings");
        settingsButton.addActionListener(e -> {
            // Find parent frame to use as owner for the dialog
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof Frame) {
                SettingsDialog dialog = new SettingsDialog((Frame) window);
                dialog.setVisible(true);
            }
        });

        rollButton = createFalloutButton("Roll Dice");
        rollButton.addActionListener(e -> {
            if (controller != null) controller.doRoll();
        });

        improveButton = createFalloutButton("Improve Property");
        improveButton.addActionListener(e -> {
            if (controller != null) controller.doImprove();
        });

        tradeButton = createFalloutButton("Trade");
        tradeButton.addActionListener(e -> {
            if (controller != null) controller.doTrade();
        });

        casinoButton = createFalloutButton("Casino");
        casinoButton.addActionListener(e -> {
            if (controller != null) controller.doCasino();
        });

        propertiesButton = createFalloutButton("Properties");
        propertiesButton.addActionListener(e -> {
            if (controller != null) controller.doShowProperties();
        });

        leaveGameButton = createFalloutButton("Leave Game");
        leaveGameButton.addActionListener(e -> {
            if (controller != null) {
                controller.doLeaveGame();
            }
        });

        JButton saveButton = createFalloutButton("Save");
        saveButton.addActionListener(e -> {
            if (controller != null) controller.trySaveGame();
        });

        // --- Layout ---
        add(rollButton);
        add(improveButton);
        add(tradeButton);
        add(propertiesButton);
        add(casinoButton);

        add(Box.createRigidArea(new Dimension(30, 0))); // Spacer

        add(settingsButton); // <--- Added here instead of Full Log
        add(leaveGameButton);
        add(saveButton);

        setButtonsEnabled(false, false, false);
    }

    // Helper method to style buttons
    private JButton createFalloutButton(String text) {
        JButton button = new JButton(text);
        button.setFont(GameWindow.FALLOUT_FONT);
        button.setBackground(new Color(30, 50, 35));
        button.setForeground(GameWindow.FALLOUT_GREEN);
        button.setFocusPainted(false);

        // --- SOUND: Hover and Click ---
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/hover.wav").toString());
                }
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
                }
            }
        });
        return button;
    }

    public void setGameController(GameController controller) {
        this.controller = controller;
    }

    public void setButtonsEnabled(boolean roll, boolean improve, boolean trade) {
        rollButton.setEnabled(roll);
        improveButton.setEnabled(improve);
        tradeButton.setEnabled(trade);
        casinoButton.setEnabled(roll);
    }
}