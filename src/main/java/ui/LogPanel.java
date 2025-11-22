package ui;

import game.GameController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogPanel extends JPanel {

    private JTextArea logArea; // <-- Zurück zu JTextArea
    private JTextField inputField;
    private GameController controller;

    public LogPanel() {
        setLayout(new BorderLayout(5, 5));

        setBackground(GameWindow.DARK_BACKGROUND);
        Border greenBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN),
                "WASTELAND LOG",
                0, 0,
                GameWindow.FALLOUT_FONT,
                GameWindow.FALLOUT_GREEN
        );
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                greenBorder
        ));

        logArea = new JTextArea(8, 0); // 8 Zeilen Höhe
        logArea.setEditable(false);
        logArea.setBackground(GameWindow.DARK_BACKGROUND);
        logArea.setForeground(Color.WHITE);
        logArea.setFont(GameWindow.FALLOUT_FONT);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        styleScrollPane(scrollPane);

        inputField = new JTextField();
        inputField.setFont(GameWindow.FALLOUT_FONT);
        inputField.setBackground(GameWindow.DARK_BACKGROUND);
        inputField.setForeground(GameWindow.FALLOUT_GREEN);
        inputField.setCaretColor(GameWindow.FALLOUT_GREEN);
        inputField.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN, 1));

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                if (controller != null && message != null && !message.isBlank()) {
                    controller.sendChatMessage(message);
                    inputField.setText("");
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        // --- MODIFIED: Increased default panel width ---
        setPreferredSize(new Dimension(350, 0));
    }

    public void appendMessage(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void clearLog() {
        logArea.setText("");
    }

    public String getText() {
        return logArea.getText();
    }

    public void setText(String text) {
        logArea.setText(text);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setGameController(GameController controller) {
        this.controller = controller;
    }

    private void styleScrollPane(JScrollPane scrollPane) {
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
}