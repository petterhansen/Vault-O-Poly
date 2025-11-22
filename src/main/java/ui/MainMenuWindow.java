package ui;

import game.GameController;
import game.SaveManager;
import game.network.NetworkMessage;
import game.network.SessionCodec;
import game.network.IpUtil;
import mechanics.TradeOffer;
import players.Player;
import util.PlayerToken;
import board.fields.BoardField;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class MainMenuWindow extends JFrame {

    private GameController controller;
    private ObjectOutputStream clientNetworkOut;
    private Player self;
    private JTextField usernameField;

    private Socket clientSocket;
    private Thread networkListenerThread;

    static {
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
    }

    public MainMenuWindow() {
        setTitle("Vault-O-Poly Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 350);
        setLocationRelativeTo(null);

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

        getContentPane().setBackground(GameWindow.DARK_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("VAULT-O-POLY");
        title.setFont(GameWindow.FALLOUT_FONT.deriveFont(24f));
        title.setForeground(GameWindow.FALLOUT_GREEN);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, gbc);

        JPanel userPanel = new JPanel(new BorderLayout(5, 0));
        userPanel.setBackground(GameWindow.DARK_BACKGROUND);
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(GameWindow.FALLOUT_FONT);
        userLabel.setForeground(Color.WHITE);
        userPanel.add(userLabel, BorderLayout.WEST);

        this.usernameField = new JTextField("Wanderer");
        usernameField.setFont(GameWindow.FALLOUT_FONT);
        usernameField.setBackground(GameWindow.DARK_BACKGROUND);
        usernameField.setForeground(GameWindow.FALLOUT_GREEN);
        usernameField.setCaretColor(GameWindow.FALLOUT_GREEN);
        usernameField.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));
        userPanel.add(usernameField, BorderLayout.CENTER);

        gbc.insets = new Insets(0, 10, 10, 10);
        add(userPanel, gbc);

        gbc.insets = new Insets(10, 10, 5, 10);

        // --- 1. NEW GAME BUTTON ---
        JButton localButton = createFalloutButton("Start Local Game");
        localButton.addActionListener(e -> {
            if (controller != null) {
                Object[] options = {"Start Fresh", "Load Save"};
                int choice = JOptionPane.showOptionDialog(this,
                        "Select game mode:",
                        "Local Game",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);

                if (choice == 1) {
                    controller.tryLoadGame();
                    this.dispose();
                } else if (choice == 0) {
                    this.setVisible(false);
                    controller.startNewGame(usernameField.getText());
                }
            }
        });
        add(localButton, gbc);

        // --- 3. HOST BUTTON (UPDATED SMART LOGIC) ---
        JButton hostButton = createFalloutButton("Host Network Game");
        hostButton.addActionListener(e -> {
            String username = usernameField.getText();
            if (username == null || username.isBlank()) username = "Host";

            // Configuration Dialog
            JTextField portField = new JTextField("10365");
            JTextField externalAddrField = new JTextField();

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Port (Default 10365):"));
            panel.add(portField);
            panel.add(new JLabel("External Address (Optional):"));
            panel.add(new JLabel("(Enter if using Playit.gg/Ngrok/VPN)"));
            panel.add(externalAddrField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Host Configuration",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            int port;
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Port Number.");
                return;
            }
            String customAddress = externalAddrField.getText().trim();

            // Game Mode Selection
            Object[] options = {"Start New World", "Load Save File", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Do you want to start a fresh game or load a save?",
                    "Host Game Options",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;

            java.io.File saveFile = null;
            if (choice == 1) {
                saveFile = SaveManager.chooseFileToLoad(this);
                if (saveFile == null) return;
            }

            SwingUI ui = new SwingUI();
            GameController controller = new GameController(ui);
            ui.setGameController(controller);
            ui.getMainMenuWindow().setGameController(controller);

            ui.showGameWindow();

            // Start Server
            controller.startAsHost(username, port, saveFile);
            this.dispose();

            // --- SMART CONNECTION INFO DISPLAY ---
            if (!customAddress.isEmpty()) {
                // Check if the custom address is a numeric IP (can be encoded) or a Domain
                try {
                    // Try to encode it as a Session ID
                    String sessionId = SessionCodec.encodeIp(customAddress);
                    // If successful, show the ID + Port
                    String displayCode = sessionId + ":" + port;

                    ui.logMessage("=====================================");
                    ui.logMessage("GAME HOSTED. SHARE THIS CODE:");
                    ui.logMessage(displayCode);
                    ui.logMessage("(Valid for Join by Session ID)");
                    ui.logMessage("=====================================");

                    showCustomConnectionInfo(ui, "Session Code: " + displayCode + "\n\n(Or direct address: " + customAddress + ":" + port + ")");

                } catch (Exception ex) {
                    // It's a Domain (Text) - Cannot encode. Show Address.
                    showCustomConnectionInfo(ui, customAddress + ":" + port);
                }
            } else {
                // Standard auto-detected IP
                generateAndShowSessionId(ui);
            }
        });
        add(hostButton, gbc);

// --- 4. JOIN BUTTON (UPDATED FOR ID:PORT) ---
        JButton joinButton = createFalloutButton("Join Network Game");
        joinButton.addActionListener(e -> {
            String username = usernameField.getText();
            if (username == null || username.isBlank()) username = "Wanderer";

            String[] joinOptions = {"Find Game on LAN", "Join by Session ID", "Join by Address"};
            String selectedOption = (String) JOptionPane.showInputDialog(
                    this,
                    "How do you want to join?",
                    "Join Network Game",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    joinOptions,
                    joinOptions[0]
            );

            if (selectedOption == null) return;

            String ip = null;
            int port = 10365; // Default

            if (selectedOption.equals("Find Game on LAN")) {
                JoinGameWindow lobby = new JoinGameWindow(this);
                String serverInfo = lobby.getSelectedServerIp(); // This usually returns just IP
                if (serverInfo != null) {
                    connectToServer(serverInfo, 10365, username); // LAN usually defaults to 10365
                }
            }
            else if (selectedOption.equals("Join by Session ID")) {
                String input = JOptionPane.showInputDialog(this, "Enter Session ID (e.g., VAULT-XYZ or VAULT-XYZ:12345):");
                if (input != null && !input.isBlank()) {
                    try {
                        // Check for custom port in ID (format: ID:PORT)
                        String sessionId = input;
                        if (input.contains(":")) {
                            String[] parts = input.split(":");
                            sessionId = parts[0];
                            port = Integer.parseInt(parts[1]);
                        }

                        ip = SessionCodec.decodeSessionId(sessionId);
                        if (ip != null) connectToServer(ip, port, username);

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Invalid Session ID or Port.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            else if (selectedOption.equals("Join by Address")) {
                JTextField hostField = new JTextField();
                JTextField portField = new JTextField();
                Object[] message = {"Host:", hostField, "Port:", portField};
                int option = JOptionPane.showConfirmDialog(this, message, "Join", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    try {
                        connectToServer(hostField.getText(), Integer.parseInt(portField.getText()), username);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Invalid Input.");
                    }
                }
            }
        });
        add(joinButton, gbc);

        JButton settingsButton = createFalloutButton("Settings");
        settingsButton.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this);
            dialog.setVisible(true);
        });
        add(settingsButton, gbc);

        // --- 5. TUTORIAL BUTTON ---
        JButton tutorialButton = createFalloutButton("How to Play");
        tutorialButton.addActionListener(e -> showHowToPlayDialog());
        add(tutorialButton, gbc);
    }

    private void showHowToPlayDialog() {
        String tutorialText =
                "=== HOW TO PLAY VAULT-O-POLY ===\n\n"
                        + "THE GOAL:\n"
                        + "Be the last survivor left! Bankrupt your friends by forcing them to pay rent on your properties.\n\n"
                        + "--- YOUR TURN ---\n"
                        + "On your turn, you have three main actions:\n"
                        + "1. ROLL: Move around the board. If you land on an unowned property, you can buy it. If it's owned, you pay rent!\n\n"
                        + "2. IMPROVE: (Before rolling) If you own all properties of one color, you can 'Improve' them. This costs caps but makes the rent MUCH higher for anyone who lands there.\n\n"
                        + "3. TRADE: (Before rolling) You can offer a trade to any other player for caps, properties, or resources.\n\n"
                        + "--- BOARD SPACES ---\n"
                        + "* PROPERTIES: Buy them! They are your main source of income.\n"
                        + "* RESOURCES: (e.g., Water, Food) These give you resources at the start of your turn. You need resources to improve properties.\n"
                        + "* WASTELAND / VAULT-TEC: Draw an event card. Good or bad, who knows?\n"
                        + "* GO TO JAIL: You are sent to jail. To get out, you must pay a fine, use a 'Get Out of Jail' card, or roll doubles on your turn.\n\n"
                        + "--- NETWORK PLAY ---\n"
                        + "HOSTING:\n"
                        + "(It's highly recommended to use playit.gg's TCP tunnel)\n"
                        + "1. Click 'Host Network Game'.\n"
                        + "2. A Session ID (e.g., 'VAULT-G7P391:12345') will pop up. Give this code to your friends.\n"
                        + "3. The ID is also printed in the game log if you miss it.\n\n"
                        + "JOINING:\n"
                        + "1. Click 'Join Network Game'.\n"
                        + "2. Select 'Join by Session ID'.\n"
                        + "3. Enter the code your host gave you and connect!\n";

        JTextArea textArea = new JTextArea(tutorialText);
        textArea.setFont(GameWindow.FALLOUT_FONT.deriveFont(14f));
        textArea.setBackground(GameWindow.DARK_BACKGROUND);
        textArea.setForeground(Color.WHITE);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(GameWindow.FALLOUT_GREEN));
        scrollPane.setPreferredSize(new Dimension(500, 400));

        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
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

        JOptionPane.showMessageDialog(this,
                scrollPane,
                "How to Play",
                JOptionPane.PLAIN_MESSAGE);
    }

    // --- NEW: Show custom connection info for Playit.gg users ---
    private void showCustomConnectionInfo(UIInterface ui, String customAddress) {
        if (ui != null) {
            ui.logMessage("=====================================");
            ui.logMessage("GAME HOSTED. SHARE THIS ADDRESS:");
            ui.logMessage(customAddress);
            ui.logMessage("=====================================");
        }

        JTextArea textArea = new JTextArea(customAddress);
        textArea.setEditable(false);
        textArea.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
        textArea.setBackground(GameWindow.DARK_BACKGROUND);
        textArea.setForeground(GameWindow.FALLOUT_GREEN);

        JOptionPane.showMessageDialog(
                null,
                new JScrollPane(textArea) {
                    @Override
                    public Dimension getPreferredSize() {
                        Dimension d = super.getPreferredSize();
                        d.width = Math.min(d.width, 400);
                        d.height = Math.min(d.height, 80);
                        return d;
                    }
                },
                "Share Your Server Address",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void generateAndShowSessionId(UIInterface ui) {
        SwingWorker<String, Void> ipWorker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return IpUtil.getPublicIp();
            }

            @Override
            protected void done() {
                try {
                    String publicIp = get();
                    String sessionId = SessionCodec.encodeIp(publicIp);

                    if (ui != null) {
                        ui.logMessage("=====================================");
                        ui.logMessage("GAME HOSTED. SHARE THIS SESSION ID:");
                        ui.logMessage(sessionId);
                        ui.logMessage("=====================================");
                    }

                    JTextArea textArea = new JTextArea(sessionId);
                    textArea.setEditable(false);
                    textArea.setFont(GameWindow.FALLOUT_FONT.deriveFont(16f));
                    textArea.setBackground(GameWindow.DARK_BACKGROUND);
                    textArea.setForeground(GameWindow.FALLOUT_GREEN);

                    JOptionPane.showMessageDialog(
                            null,
                            new JScrollPane(textArea) {
                                @Override
                                public Dimension getPreferredSize() {
                                    Dimension d = super.getPreferredSize();
                                    d.width = Math.min(d.width, 400);
                                    d.height = Math.min(d.height, 80);
                                    return d;
                                }
                            },
                            "Share Your Session ID",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            null,
                            "Could not get public IP: " + e.getMessage() +
                                    "\n\nInternet play is disabled. Friends can still join on your LAN.",
                            "Network Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        ipWorker.execute();
    }

    private void connectToServer(String host, int port, String username) {
        try {
            clientSocket = new Socket(host, port);

            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            this.clientNetworkOut = out;

            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            out.writeObject(new NetworkMessage(NetworkMessage.MessageType.CLIENT_INFO, username));
            out.flush();

            SwingUI ui = new SwingUI();
            GameController controller = new GameController(ui, out);
            ui.setGameController(controller);
            ui.getMainMenuWindow().setGameController(controller);

            networkListenerThread = new Thread(() -> {
                try {
                    while (true) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        NetworkMessage msg = (NetworkMessage) in.readObject();
                        handleServerMessage(msg, ui, controller);
                    }
                } catch (Exception ex) {
                    if (!Thread.currentThread().isInterrupted()) {
                        System.err.println("[Debug] Network listener disconnected.");
                        SwingUtilities.invokeLater(() -> {
                            ui.showNotification("Disconnected from server.");
                            ui.hideGameWindow();
                            ui.showMainMenu();
                        });
                    }
                }
            });
            networkListenerThread.start();

            util.WebAudioPlayer.play(getClass().getResource("/sounds/start.wav").toString());
            ui.showGameWindow();
            this.dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to " + host + ":" + port + "\n" + ex.getMessage());
        }
    }

    private void connectToServer(String ip, String username) {
        connectToServer(ip, 10365, username);
    }

    private void handleServerMessage(NetworkMessage msg, UIInterface ui, GameController controller) {
        SwingUtilities.invokeLater(() -> {
            try {
                switch (msg.getType()) {

                    case LOG_MESSAGE:
                        ui.logMessage((String) msg.getPayload());
                        break;
                    case CHAT_MESSAGE:
                        ui.displayChatMessage((String) msg.getPayload());
                        break;
                    case CLEAR_LOG:
                        ui.clearLog();
                        break;
                    case SHOW_NOTIFICATION:
                        ui.showNotification((String) msg.getPayload());
                        break;
                    case INITIALIZE_BOARD:
                        ui.showBoard((List<BoardField>) msg.getPayload());
                        break;
                    case ADD_PLAYER_TOKEN:
                        ui.createPlayerToken((Player) msg.getPayload());
                        break;
                    case REMOVE_PLAYER_TOKEN:
                        Player playerToRemove = (Player) msg.getPayload();
                        if (playerToRemove != null) {
                            ui.removePlayerToken(playerToRemove.getToken());
                        }
                        break;
                    case MOVE_PLAYER_TOKEN:
                        Object[] moveData = (Object[]) msg.getPayload();
                        ui.movePlayerToken((PlayerToken) moveData[0], (Integer) moveData[1]);
                        break;
                    case UPDATE_PLAYER_STATS:
                        Player updatedPlayer = (Player) msg.getPayload();
                        ui.updatePlayerStats(updatedPlayer);

                        if (this.self != null && this.self.equals(updatedPlayer)) {
                            this.self = updatedPlayer;
                            controller.setSelfPlayer(this.self);
                        }

                        controller.refreshPropertiesWindow();
                        break;

                    case UPDATE_PROPERTY_OWNER:
                        Object[] data = (Object[]) msg.getPayload();
                        Integer pos = (Integer) data[0];

                        // Handle PlayerToken instead of Player object
                        PlayerToken ownerToken = (PlayerToken) data[1];
                        Player owner = null;

                        // Find local player object by token
                        if (ownerToken != null && controller.getPlayers() != null) {
                            for (Player p : controller.getPlayers()) {
                                if (p.getToken() == ownerToken) {
                                    owner = p;
                                    break;
                                }
                            }
                        }

                        // 1. Update the Visual Board (UI)
                        ui.updatePropertyOwner(pos, owner);

                        // 2. Update the Logic Board (Controller Model)
                        controller.setPropertyOwner(pos, owner);

                        // 3. Refresh the Properties Window
                        controller.refreshPropertiesWindow();
                        break;
                    case SET_PLAYER_TURN:
                        ui.setPlayerTurn((Player) msg.getPayload());
                        break;
                    case SET_CONTROLS:
                        boolean[] controls = (boolean[]) msg.getPayload();
                        ui.setControlsEnabled(controls[0], controls[1], controls[2]);
                        break;
                    case RESET_UI:
                        ui.resetBoard();
                        ui.resetStats();
                        ui.clearLog();
                        ui.resetFullLog();
                        break;
                    case SET_CLIENT_PLAYER:
                        this.self = (Player) msg.getPayload();
                        controller.setSelfPlayer(this.self);
                        break;
                    case GAME_OVER:
                        ui.showNotification((String) msg.getPayload());
                        ui.hideGameWindow();
                        ui.showMainMenu();
                        disconnectFromServer();
                        break;
                    // --- DIALOGS ---
                    case SHOW_BOOLEAN_DIALOG:
                        String buyPrompt = (String) msg.getPayload();
                        boolean wantsToBuy = ui.askForBoolean(buyPrompt);
                        sendResponse(clientNetworkOut, new NetworkMessage(NetworkMessage.MessageType.RESPONSE_BUY_PROPERTY, wantsToBuy));
                        break;
                    case SHOW_SELECTION_DIALOG:
                        Object[] dialogData = (Object[]) msg.getPayload();
                        String prompt = (String) dialogData[0];
                        String[] options = (String[]) dialogData[1];
                        String choice = ui.askForSelection(prompt, options);

                        NetworkMessage.MessageType responseType;
                        if (prompt.contains("jail")) {
                            responseType = NetworkMessage.MessageType.RESPONSE_JAIL_ACTION;
                        } else if (prompt.contains("improve")) {
                            responseType = NetworkMessage.MessageType.RESPONSE_IMPROVE_SELECTION;
                        } else if (prompt.contains("trade with")) {
                            responseType = NetworkMessage.MessageType.RESPONSE_TRADE_SELECTION;
                        } else {
                            responseType = NetworkMessage.MessageType.LOG_MESSAGE;
                        }
                        sendResponse(clientNetworkOut, new NetworkMessage(responseType, choice));
                        break;
                    case REQUEST_BUILD_OFFER:
                        String buildPrompt = (String) msg.getPayload();
                        TradeOffer offer = ui.askForTradeOffer(this.self, buildPrompt);
                        sendResponse(clientNetworkOut, new NetworkMessage(NetworkMessage.MessageType.RESPONSE_BUILD_OFFER, offer));
                        break;
                    case REQUEST_ACCEPT_TRADE:
                        String summary = (String) msg.getPayload();
                        boolean accepted = ui.askForBoolean(summary);
                        sendResponse(clientNetworkOut, new NetworkMessage(NetworkMessage.MessageType.RESPONSE_ACCEPT_TRADE, accepted));
                        break;
                    case SHOW_CASINO_DIALOG:
                        mechanics.CasinoConfiguration config = (mechanics.CasinoConfiguration) msg.getPayload();
                        ui.showCasinoDialog(this.self, controller, config);
                        break;
                    case SYNC_BOARD_STATE:
                        List<BoardField> fields = (List<BoardField>) msg.getPayload();
                        // This updates the logic, the board UI, and the properties window
                        controller.updateBoardState(fields);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendResponse(ObjectOutputStream out, NetworkMessage msg) {
        new Thread(() -> {
            try {
                if (out != null) {
                    out.writeObject(msg);
                    out.flush();
                    out.reset();
                }
            } catch (Exception e) {
                System.out.println("Could not send response (may be disconnected).");
            }
        }).start();
    }

    public void disconnectFromServer() {
        if (networkListenerThread != null) {
            networkListenerThread.interrupt();
        }
        try {
            if (clientNetworkOut != null) {
                clientNetworkOut.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (java.io.IOException e) {
            System.out.println("Client disconnected.");
        }
        networkListenerThread = null;
        clientSocket = null;
        clientNetworkOut = null;
    }

    public void setGameController(GameController controller) {
        this.controller = controller;
    }

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
                util.WebAudioPlayer.play(getClass().getResource("/sounds/click.wav").toString());
            }
        });
        return button;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            SwingUI swingUI = new SwingUI();
            GameController controller = new GameController(swingUI);
            swingUI.setGameController(controller);
            swingUI.getMainMenuWindow().setGameController(controller);
            swingUI.show();
        });
    }
}