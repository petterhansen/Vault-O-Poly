package ui;

import game.GameController;
import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private JButton rollButton;
    private JButton improveButton;
    private JButton tradeButton;
    private JButton settingsButton;
    private JButton leaveGameButton;
    private JButton propertiesButton;
    private JButton casinoButton;

    private GameController controller;

    public ControlPanel() {
        setBackground(GameWindow.DARK_BACKGROUND);
        setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));

        // CHANGED: Use UIHelper
        settingsButton = UIHelper.createFalloutButton("Settings");
        settingsButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof Frame) {
                SettingsDialog dialog = new SettingsDialog((Frame) window);
                dialog.setVisible(true);
            }
        });

        rollButton = UIHelper.createFalloutButton("Roll Dice");
        rollButton.addActionListener(e -> {
            if (controller != null) controller.doRoll();
        });

        improveButton = UIHelper.createFalloutButton("Improve Property");
        improveButton.addActionListener(e -> {
            if (controller != null) controller.doImprove();
        });

        tradeButton = UIHelper.createFalloutButton("Trade");
        tradeButton.addActionListener(e -> {
            if (controller != null) controller.doTrade();
        });

        casinoButton = UIHelper.createFalloutButton("Casino");
        casinoButton.addActionListener(e -> {
            if (controller != null) controller.doCasino();
        });

        propertiesButton = UIHelper.createFalloutButton("Properties");
        propertiesButton.addActionListener(e -> {
            if (controller != null) controller.doShowProperties();
        });

        leaveGameButton = UIHelper.createFalloutButton("Leave Game");
        leaveGameButton.addActionListener(e -> {
            if (controller != null) {
                controller.doLeaveGame();
            }
        });

        JButton saveButton = UIHelper.createFalloutButton("Save");
        saveButton.addActionListener(e -> {
            if (controller != null) controller.trySaveGame();
        });

        add(rollButton);
        add(improveButton);
        add(tradeButton);
        add(propertiesButton);
        add(casinoButton);
        add(Box.createRigidArea(new Dimension(30, 0)));
        add(settingsButton);
        add(leaveGameButton);
        add(saveButton);

        setButtonsEnabled(false, false, false);
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