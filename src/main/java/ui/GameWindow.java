package ui;

import javax.swing.*;
import java.awt.*;

/**
 * The main JFrame for the game. Holds the board, info, and control panels.
 */
public class GameWindow extends JFrame {

    private BoardPanel boardPanel;
    private InfoPanel infoPanel;
    private ControlPanel controlPanel;
    private ChatPanel chatPanel;
    // LogPanel is removed, as ChatPanel now handles everything

    // "Pip-Boy" theme colors and font
    public static final Color FALLOUT_GREEN = new Color(27, 255, 128);
    public static final Color DARK_BACKGROUND = new Color(20, 30, 25);
    public static final Font FALLOUT_FONT = new Font("Monospaced", Font.BOLD, 14);

    // "New Vegas" theme colors
    public static final Color NEW_VEGAS_ORANGE = new Color(255, 182, 66);
    public static final Color NEW_VEGAS_BACKGROUND = new Color(36, 32, 28);

    public GameWindow() {
        try {
            java.net.URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                setIconImage(new javax.swing.ImageIcon(iconUrl).getImage());
            } else {
                System.err.println("Icon file not found. Place 'icon.png' in src/main/resources/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Vault-O-Poly");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        boardPanel = new BoardPanel();
        infoPanel = new InfoPanel();
        controlPanel = new ControlPanel();
        chatPanel = new ChatPanel(); // Re-implemented ChatPanel

        // Apply theme
        boardPanel.setBackground(DARK_BACKGROUND);
        infoPanel.setBackground(DARK_BACKGROUND);
        controlPanel.setBackground(DARK_BACKGROUND);
        chatPanel.setBackground(DARK_BACKGROUND);

        // Create a new panel to hold the info and chat panels
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(DARK_BACKGROUND);

        // Info panel goes on top
        rightPanel.add(infoPanel, BorderLayout.NORTH);

        // Just add the chat panel directly to the bottom
        rightPanel.add(chatPanel, BorderLayout.CENTER);
        // -------------------------

        // Add panels to the frame
        add(boardPanel, BorderLayout.CENTER);

        // Add panels to the frame
        add(boardPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        pack();
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
    }

    // Getters for the SwingUI to access panels
    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public ChatPanel getChatPanel() { return  chatPanel; }

    /**
     * Helper utility to convert a Java Color object to a CSS-friendly hex string.
     * @param c The color to convert.
     * @return A string in the format "#RRGGBB".
     */
    public static String getHexColor(Color c) {
        if (c == null) return "#ffffff"; // Default to white if null
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}