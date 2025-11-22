package ui;

import javax.swing.*;
import java.awt.*;

public class RadioWindow extends JDialog {

    private RadioPanel radioPanel;

    public RadioWindow(Frame owner) {
        super(owner, "Pip-Boy Radio Tuner", false); // Non-modal (can click other windows)

        // Important: HIDE instead of DISPOSE so music keeps playing in background
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        setSize(350, 250);
        setLocationRelativeTo(owner);
        setResizable(false);

        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);
        setLayout(new BorderLayout());

        // Add the existing RadioPanel logic
        radioPanel = new RadioPanel();
        add(radioPanel, BorderLayout.CENTER);

        // Small help tip at bottom
        JLabel help = new JLabel(" (Close to keep playing in background) ");
        help.setFont(GameWindow.FALLOUT_FONT.deriveFont(10f));
        help.setForeground(Color.GRAY);
        help.setHorizontalAlignment(SwingConstants.CENTER);
        add(help, BorderLayout.SOUTH);
    }

    public void stopRadio() {
        if (radioPanel != null) {
            radioPanel.turnOffRadio();
        }
    }
}