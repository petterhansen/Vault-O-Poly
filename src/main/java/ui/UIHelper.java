package ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Utility class for common UI component styling to eliminate code duplication.
 */
public class UIHelper {

    /**
     * Creates a button styled with the Fallout theme.
     * Includes hover and click sound effects.
     */
    public static JButton createFalloutButton(String text) {
        JButton button = new JButton(text);
        button.setFont(GameWindow.FALLOUT_FONT);
        button.setBackground(new Color(30, 50, 35));
        button.setForeground(GameWindow.FALLOUT_GREEN);
        button.setFocusPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    util.WebAudioPlayer.play(UIHelper.class.getResource("/sounds/hover.wav").toString());
                }
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    // Special handling for cancel buttons
                    if (text.equalsIgnoreCase("Cancel")) {
                        util.WebAudioPlayer.play(UIHelper.class.getResource("/sounds/cancel.wav").toString());
                    } else {
                        util.WebAudioPlayer.play(UIHelper.class.getResource("/sounds/click.wav").toString());
                    }
                }
            }
        });
        return button;
    }

    /**
     * Creates a button styled for the New Vegas Casino theme.
     */
    public static JButton createVegasButton(String text) {
        JButton button = new JButton(text);
        button.setFont(GameWindow.FALLOUT_FONT);
        button.setBackground(new Color(80, 40, 10)); // Dark Orange/Brown BG
        button.setForeground(GameWindow.NEW_VEGAS_ORANGE);
        button.setFocusPainted(false);
        button.setBorder(new javax.swing.border.LineBorder(GameWindow.NEW_VEGAS_ORANGE, 1));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled())
                    util.WebAudioPlayer.play(UIHelper.class.getResource("/sounds/hover.wav").toString());
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    util.WebAudioPlayer.play(UIHelper.class.getResource("/sounds/click.wav").toString());
                }
            }
        });
        return button;
    }

    /**
     * Applies Fallout-themed styling to a JScrollPane.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = GameWindow.FALLOUT_GREEN;
                this.trackColor = GameWindow.DARK_BACKGROUND;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    /**
     * Creates a text field with Fallout theme styling.
     */
    public static JTextField createFalloutTextField(String defaultText, int columns) {
        JTextField field = new JTextField(defaultText, columns);
        field.setFont(GameWindow.FALLOUT_FONT);
        field.setBackground(GameWindow.DARK_BACKGROUND);
        field.setForeground(GameWindow.FALLOUT_GREEN);
        field.setCaretColor(GameWindow.FALLOUT_GREEN);
        field.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN, 1));
        return field;
    }

    /**
     * Creates a text area with Fallout theme styling.
     */
    public static JTextArea createFalloutTextArea(String defaultText) {
        JTextArea area = new JTextArea(defaultText);
        area.setFont(GameWindow.FALLOUT_FONT);
        area.setBackground(GameWindow.DARK_BACKGROUND);
        area.setForeground(GameWindow.FALLOUT_GREEN);
        area.setEditable(false);
        return area;
    }
}