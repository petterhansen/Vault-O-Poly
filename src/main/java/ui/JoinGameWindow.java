package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * A modal JDialog that scans the local network for game broadcasts
 * and allows the user to select one to join.
 */
public class JoinGameWindow extends JDialog {

    private DefaultListModel<String> listModel;
    private JList<String> serverList;
    private Map<String, String> serverMap; // <Game Name, IP Address>
    private Thread listenerThread;
    private DatagramSocket listenerSocket;
    private String selectedServerIp = null;

    public JoinGameWindow(Frame owner) {
        super(owner, "Join Network Game", true); // true = modal

        serverMap = new HashMap<>();
        listModel = new DefaultListModel<>();
        serverList = new JList<>(listModel);

        // --- THEME ---
        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);
        serverList.setBackground(GameWindow.DARK_BACKGROUND);
        serverList.setForeground(Color.WHITE);
        serverList.setFont(GameWindow.FALLOUT_FONT);
        serverList.setSelectionBackground(GameWindow.FALLOUT_GREEN);
        serverList.setSelectionForeground(GameWindow.DARK_BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(serverList);
        styleScrollPane(scrollPane); // Apply theme to scrollbar
        scrollPane.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));

        JButton joinButton = createFalloutButton("Join Game");
        joinButton.addActionListener(e -> {
            String selected = serverList.getSelectedValue();
            if (selected != null) {
                selectedServerIp = serverMap.get(selected);
                stopListening();
                this.dispose(); // Close the window
            }
        });

        JButton cancelButton = createFalloutButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedServerIp = null;
            stopListening();
            this.dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(GameWindow.DARK_BACKGROUND);
        buttonPanel.add(cancelButton);
        buttonPanel.add(joinButton);

        JLabel title = new JLabel("Searching for games on network...");
        title.setFont(GameWindow.FALLOUT_FONT);
        title.setForeground(GameWindow.FALLOUT_GREEN);
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(title, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(400, 300);
        setLocationRelativeTo(owner);

        startListening();
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                // Listen on the broadcast port
                listenerSocket = new DatagramSocket(12346);
                listenerSocket.setBroadcast(true);
                byte[] recvBuf = new byte[15000];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    listenerSocket.receive(packet); // This blocks

                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.startsWith("VAULT-O-POLY_HOST:")) {
                        String gameName = message.substring("VAULT-O-POLY_HOST:".length());
                        String ipAddress = packet.getAddress().getHostAddress();

                        // Use serverMap to prevent duplicates
                        if (!serverMap.containsKey(gameName)) {
                            // Add to UI
                            SwingUtilities.invokeLater(() -> {
                                listModel.addElement(gameName);
                                serverMap.put(gameName, ipAddress);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                if (listenerSocket == null || !listenerSocket.isClosed()) {
                    System.out.println("Listener stopped.");
                }
            }
        });
        listenerThread.start();
    }

    private void stopListening() {
        if (listenerSocket != null) {
            listenerSocket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    /**
     * Public method to show the dialog and get the IP
     */
    public String getSelectedServerIp() {
        setVisible(true); // This blocks until the window is disposed
        return selectedServerIp; // Will be null if canceled, or an IP if "Join" was clicked
    }

    // Helper method from ControlPanel to style buttons
    private JButton createFalloutButton(String text) {
        JButton button = new JButton(text);
        button.setFont(GameWindow.FALLOUT_FONT);
        button.setBackground(new Color(30, 50, 35));
        button.setForeground(GameWindow.FALLOUT_GREEN);
        button.setFocusPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                util.WebAudioPlayer.play(getClass().getResource("/sounds/hover.wav").toString());
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                // If it's "Cancel", play cancel sound instead?
                if (text.equalsIgnoreCase("Cancel")) {
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/cancel.wav").toString());
                } else {
                    util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
                }
            }
        });
        return button;
    }

    // Helper method to style the JScrollPane
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