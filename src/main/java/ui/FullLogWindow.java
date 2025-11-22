package ui;

import javax.swing.*;
import java.awt.*;

public class FullLogWindow extends JDialog {

    private JTextArea logArea;

    public FullLogWindow(Frame owner) {
        super(owner, "Full Wasteland Log", false);

        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);
        setSize(600, 400);
        setLocationRelativeTo(owner);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(GameWindow.DARK_BACKGROUND);
        logArea.setForeground(Color.WHITE);
        logArea.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));

        // CHANGED: Use UIHelper
        UIHelper.styleScrollPane(scrollPane);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendMessage(String message) {
        appendRaw(message + "\n");
    }

    public void appendRaw(String text) {
        logArea.append(text);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void clearLog() {
        logArea.setText("");
    }

    public void setText(String text) {
        logArea.setText(text);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}