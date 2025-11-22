package ui;

import game.GameController;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.text.BadLocationException;

public class ChatPanel extends JPanel {

    private JTextPane chatPane;
    private JTextField inputField;
    private GameController controller;

    // --- NEW: Progress Components ---
    private JPanel progressPanel;
    private JProgressBar progressBar;
    private JLabel progressLabel;

    // ... [Keep existing CSS string 'htmlSkeleton'] ...
    private String htmlSkeleton = "<html><head><style>" +
            "body { background-color: rgb(24, 28, 25); font-family: '" + GameWindow.FALLOUT_FONT.getFontName() + "'; color: white; margin: 0; padding: 0; }" +
            "p { margin: 0; padding: 0; word-wrap: break-word; overflow-wrap: break-word; word-break: break-all; }" +
            "img { max-width: 100%; height: auto; border-radius: 5px; }" +
            ".log { color: #999999; font-style: italic; font-size: 0.9em; padding: 1px 5px; }" +
            ".system { color: " + GameWindow.getHexColor(GameWindow.FALLOUT_GREEN) + "; font-weight: bold; text-align: center; padding: 3px 0; }" +
            ".chat-bubble { background-color: #334438; border: 1px solid #445548; border-radius: 8px; padding: 5px 8px; margin: 4px 0; display: inline-block; max-width: 100%; box-sizing: border-box; }" +
            ".chat-bubble .timestamp { font-size: 0.8em; color: #aaaaaa; display: block; }" +
            ".chat-bubble .name { font-weight: bold; }" +
            ".chat-bubble .message { color: white; }" +
            ".bubble-container { text-align: left; padding: 0; margin: 0; }" +
            "a { color: #55ff55; text-decoration: none; font-weight: bold; }" +
            "</style></head><body id='chat-body'></body></html>";

    public ChatPanel() {
        setLayout(new BorderLayout(5, 5));
        setBackground(GameWindow.DARK_BACKGROUND);
        Border greenBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN), "WASTELAND CHAT", 0, 0, GameWindow.FALLOUT_FONT, GameWindow.FALLOUT_GREEN);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), greenBorder));

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setText(htmlSkeleton);
        chatPane.setBackground(GameWindow.DARK_BACKGROUND);

        chatPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controller == null || controller.getUi() == null) return;
                int pos = chatPane.viewToModel(e.getPoint());
                if (pos < 0) return;
                HTMLDocument doc = (HTMLDocument) chatPane.getDocument();
                Element element = doc.getCharacterElement(pos);
                for (Element el = element; el != null; el = el.getParentElement()) {
                    if (el.getName().equalsIgnoreCase(HTML.Tag.IMG.toString())) {
                        String src = (String) el.getAttributes().getAttribute(HTML.Attribute.SRC);
                        if (src != null && !src.isEmpty()) {
                            controller.getUi().showFullImage(src);
                            break;
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        styleScrollPane(scrollPane);

        // --- INPUT AREA SETUP ---
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(GameWindow.DARK_BACKGROUND);

        // 1. Progress Bar Panel (Initially Hidden)
        progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(GameWindow.DARK_BACKGROUND);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0));
        progressPanel.setVisible(false);

        progressLabel = new JLabel("Processing...");
        progressLabel.setFont(GameWindow.FALLOUT_FONT.deriveFont(10f));
        progressLabel.setForeground(GameWindow.FALLOUT_GREEN);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(GameWindow.FALLOUT_GREEN);
        progressBar.setBackground(Color.BLACK);
        progressBar.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));
        progressBar.setFont(GameWindow.FALLOUT_FONT.deriveFont(10f));

        progressPanel.add(progressLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        bottomContainer.add(progressPanel, BorderLayout.NORTH);

        // 2. Input Field
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
        bottomContainer.add(inputField, BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(300, 350));
    }

    // --- Progress API ---
    public void showProgress(String status, int percent) {
        SwingUtilities.invokeLater(() -> {
            progressPanel.setVisible(true);
            progressLabel.setText(status);
            if (percent < 0) {
                progressBar.setIndeterminate(true);
                progressBar.setString("WORKING...");
            } else {
                progressBar.setIndeterminate(false);
                progressBar.setValue(percent);
                // Text visual: [#######----]
                progressBar.setString(percent + "%");
            }
            revalidate();
        });
    }

    public void hideProgress() {
        SwingUtilities.invokeLater(() -> {
            progressPanel.setVisible(false);
            revalidate();
        });
    }

    // ... [Keep appendMessage, clearChat, setGameController, styleScrollPane] ...
    public void appendMessage(String htmlMessage) {
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) chatPane.getDocument();
                HTMLEditorKit editorKit = (HTMLEditorKit) chatPane.getEditorKit();
                Element body = doc.getElement("chat-body");
                doc.insertBeforeEnd(body, htmlMessage);
                chatPane.setCaretPosition(chatPane.getDocument().getLength());
            } catch (BadLocationException | IOException e) { e.printStackTrace(); }
        });
    }
    public void clearChat() { chatPane.setText(htmlSkeleton); }
    public void setGameController(GameController controller) { this.controller = controller; }
    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = GameWindow.FALLOUT_GREEN; this.trackColor = GameWindow.DARK_BACKGROUND; }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { return new JButton() { @Override public Dimension getPreferredSize() { return new Dimension(0, 0); } }; }
        });
    }
}