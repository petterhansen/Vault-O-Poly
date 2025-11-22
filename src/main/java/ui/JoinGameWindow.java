package ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class JoinGameWindow extends JDialog {

    private DefaultListModel<String> listModel;
    private JList<String> serverList;
    private Map<String, String> serverMap;
    private Thread listenerThread;
    private DatagramSocket listenerSocket;
    private String selectedServerIp = null;

    public JoinGameWindow(Frame owner) {
        super(owner, "Join Network Game", true);

        serverMap = new HashMap<>();
        listModel = new DefaultListModel<>();
        serverList = new JList<>(listModel);

        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);
        serverList.setBackground(GameWindow.DARK_BACKGROUND);
        serverList.setForeground(Color.WHITE);
        serverList.setFont(GameWindow.FALLOUT_FONT);
        serverList.setSelectionBackground(GameWindow.FALLOUT_GREEN);
        serverList.setSelectionForeground(GameWindow.DARK_BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(serverList);
        // CHANGED: Use UIHelper
        UIHelper.styleScrollPane(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));

        // CHANGED: Use UIHelper
        JButton joinButton = UIHelper.createFalloutButton("Join Game");
        joinButton.addActionListener(e -> {
            String selected = serverList.getSelectedValue();
            if (selected != null) {
                selectedServerIp = serverMap.get(selected);
                stopListening();
                this.dispose();
            }
        });

        JButton cancelButton = UIHelper.createFalloutButton("Cancel");
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
                listenerSocket = new DatagramSocket(12346);
                listenerSocket.setBroadcast(true);
                byte[] recvBuf = new byte[15000];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    listenerSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.startsWith("VAULT-O-POLY_HOST:")) {
                        String gameName = message.substring("VAULT-O-POLY_HOST:".length());
                        String ipAddress = packet.getAddress().getHostAddress();

                        if (!serverMap.containsKey(gameName)) {
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

    public String getSelectedServerIp() {
        setVisible(true);
        return selectedServerIp;
    }
}