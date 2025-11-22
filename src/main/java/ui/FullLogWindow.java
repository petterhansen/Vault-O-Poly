package ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
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
        styleScrollPane(scrollPane);

        add(scrollPane, BorderLayout.CENTER);
    }

    // For Game Events (adds newline)
    public void appendMessage(String message) {
        appendRaw(message + "\n");
    }

    // --- NEW: For Raw Console Output ---
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