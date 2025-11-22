package game;

import board.Board;
import board.fields.BoardField;
import board.fields.PropertyField;
import com.formdev.flatlaf.FlatDarkLaf;
import mechanics.Dice;
import mechanics.EventDeck;
import mechanics.PropertyDevelopment;
import mechanics.TradeSystem;
import mechanics.TradeOffer;
import players.Player;
import resources.ResourceManager;
import resources.ResourceType;
import ui.RadioWindow;
import ui.UIInterface;
import ui.SwingUI;
import ui.OwnedPropertiesWindow;
import util.PlayerToken;

import com.sun.net.httpserver.HttpServer; // New Import for Media Server
import java.net.InetSocketAddress; // New Import
import java.nio.file.Files; // For File Operations
import java.security.MessageDigest; // For Caching Hash
import java.security.NoSuchAlgorithmException; // For Caching Hash
import javax.swing.*;
import java.awt.Frame;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import game.network.ClientHandler;
import game.network.NetworkMessage;
import util.WebAudioPlayer;

import java.util.concurrent.CopyOnWriteArrayList;

public class GameController {

    private Board board;
    private List<Player> players;
    private Dice dice;
    private int currentPlayerIndex;
    private GameState gameState;
    private UIInterface ui;
    private UIInterface baseUI;

    private EventDeck eventDeck;
    private PropertyDevelopment propertyDevelopment;
    private TradeSystem tradeSystem;
    private ResourceManager resourceManager;
    private mechanics.CasinoConfiguration currentCasinoConfig;

    private OwnedPropertiesWindow propertiesWindow;

    public boolean isNetworkGame = false;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private Thread discoveryAnnouncerThread;
    private ObjectOutputStream networkOut;
    private Player self;

    // --- MEDIA SERVER FIELDS ---
    private HttpServer mediaServer;
    private String hostPublicIp;
    private String hostLocalIp;

    private static final int MEDIA_SERVER_PORT = 10366; // Internal Port
    private int externalMediaPort = MEDIA_SERVER_PORT;

    // --- NEW: TUNNEL DETAILS (Manual Input) ---
    private String externalTunnelIp = null;
    private int externalTunnelGamePort = 0;
    // ------------------------------------------

    private static String getCacheDirectory() {
        return System.getProperty("user.home") + java.io.File.separator + "VaultOPolyCache" + java.io.File.separator + "gifs";
    }
    // ----------------------------

    private Map<Player, PropertyField> pendingPropertyPurchases = new HashMap<>();

    private Player pendingTradeRequester = null;
    private Player pendingTradePartner = null;
    private TradeOffer pendingOffer1 = null;
    private TradeOffer pendingOffer2 = null;
    private boolean running = true;

    public GameController(UIInterface ui) {
        this.players = new CopyOnWriteArrayList<>();
        this.dice = new Dice(2, 6);
        this.baseUI = ui;
        this.ui = new NetworkSafeUI(this, ui);
        this.board = new Board("board.json", this.ui);

        this.eventDeck = new EventDeck();
        this.propertyDevelopment = new PropertyDevelopment();
        this.tradeSystem = new TradeSystem();
        this.resourceManager = new ResourceManager();
    }

    public GameController(UIInterface ui, ObjectOutputStream out) {
        this.ui = ui;
        this.baseUI = ui;
        this.networkOut = out;
        this.isNetworkGame = true;
        this.board = new Board("board.json", ui);
        this.players = new CopyOnWriteArrayList<>();
    }

    public void setSelfPlayer(Player self) {
        this.self = self;
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

    public void startAsHost(String username, int port, java.io.File saveFile) {
        this.isNetworkGame = true;

        // --- FIX 1: CALL askForTunnelDetails HERE ---
        String[] tunnelDetails = ui.askForTunnelDetails("If hosting via a tunnel (e.g., Playit.gg), enter the External IP/Domain and Game Port (e.g., 39761). Leave blank if using LAN only.");

        if (tunnelDetails != null) {
            this.externalTunnelIp = tunnelDetails[0];
            this.externalTunnelGamePort = Integer.parseInt(tunnelDetails[1]);
            ui.logMessage("Tunnel detected. GIF links will use: " + this.externalTunnelIp + ":" + (this.externalTunnelGamePort + 1));
        }
        // ---------------------------------------------

        // --- STEP 2: Start Servers ---
        new Thread(() -> {
            try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
                System.out.println("Server started on port " + port + ". Waiting for clients...");

                startHttpServer();

                while (true) {
                    java.net.Socket clientSocket = serverSocket.accept();

                    // Client connection logic remains to detect LAN/WAN connection types
                    int externalGamePort = clientSocket.getLocalPort();

                    if (externalGamePort != port) {
                        // This happens if traffic arrived via the tunnel (External port != Internal port)
                        int internalGamePort = 10365;
                        int portDifference = MEDIA_SERVER_PORT - internalGamePort; // 10368 - 10365 = 3

                        // Overwrite externalMediaPort with the calculated tunnel port for this session
                        this.externalMediaPort = externalGamePort + portDifference;
                        System.out.println("NETWORK: Client connected via WAN tunnel.");
                        System.out.println("NETWORK: External Game Port detected: " + externalGamePort + ". GIF Port set to: " + this.externalMediaPort);
                    } else {
                        this.externalMediaPort = MEDIA_SERVER_PORT;
                        System.out.println("NETWORK: Client connected via LAN/Localhost. GIF Port set to: " + this.externalMediaPort);
                    }

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    handler.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> ui.showNotification("Server Error: " + e.getMessage()));
            }
        }).start();
        playLocalSound("start.wav");

        startDiscoveryAnnouncer(username + "'s Game", port);
        startHeartbeatLoop();

        if (saveFile != null) {
            try {
                GameSaveState state = SaveManager.loadGame(saveFile);
                if (state != null) {
                    restoreGameState(state);
                    ui.logMessage("Server started on Port " + port + ". Loaded save: " + saveFile.getName());
                    ui.logMessage("Waiting for clients to reconnect...");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ui.showNotification("Failed to load save. Starting new game instead.");
                startAsHost(username, port, null);
            }
        } else {
            resetGame();
            ui.showBoard(board.getFields());

            Player hostPlayer = new Player(username, PlayerToken.VAULT_BOY);
            players.add(hostPlayer);
            this.self = hostPlayer;

            ui.createPlayerToken(hostPlayer);
            this.currentPlayerIndex = 0;
            this.gameState = GameState.PLAYER_TURN;
            startNextTurn();

            ui.logMessage("Server started on Port " + port + ".");
        }
    }

    /**
     * Generates an SHA-256 hash of a string, used for GIF caching.
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 not found. Using simple hash code fallback.");
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Finds the local, non-loopback IP address of the host machine.
     */
    private String getLocalIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> nets = java.net.NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                java.net.NetworkInterface net = nets.nextElement();
                if (net.isLoopback() || !net.isUp()) continue;
                for (java.net.InterfaceAddress addr : net.getInterfaceAddresses()) {
                    java.net.InetAddress ip = addr.getAddress();
                    if (ip instanceof java.net.Inet4Address && !ip.isLoopbackAddress() && !ip.isLinkLocalAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (java.net.SocketException e) {
            return "127.0.0.1";
        }
        return "127.0.0.1";
    }

    /**
     * Starts the embedded HTTP server on the Host machine to serve GIFs from the cache.
     */
    private void startHttpServer() {
        if (mediaServer != null) {
            mediaServer.stop(0);
        }

        try {
            java.io.File cacheDir = new java.io.File(getCacheDirectory());

            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    throw new java.io.IOException("Failed to create cache directory: " + cacheDir.getAbsolutePath());
                }
            }

            // Listen on the MEDIA_SERVER_PORT on all interfaces (0.0.0.0)
            mediaServer = HttpServer.create(new InetSocketAddress(MEDIA_SERVER_PORT), 0);

            mediaServer.createContext("/gifs", exchange -> {
                String path = exchange.getRequestURI().getPath();

                // Map the request URI /gifs/hash.gif to the absolute cache path.
                String relativePath = path.substring("/gifs".length());
                java.io.File file = new java.io.File(cacheDir, relativePath);

                if (file.exists() && file.isFile() && path.toLowerCase().endsWith(".gif")) {
                    exchange.getResponseHeaders().set("Content-Type", "image/gif");
                    exchange.sendResponseHeaders(200, file.length());
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        Files.copy(file.toPath(), os);
                    }
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
                exchange.close();
            });

            mediaServer.setExecutor(null);
            mediaServer.start();

            // Get the external IP (for non-LAN clients)
            this.hostPublicIp = game.network.IpUtil.getPublicIp();

            ui.logMessage("Media Server ready to serve GIFs at " + this.hostPublicIp + ":" + MEDIA_SERVER_PORT);

        } catch (Exception e) {
            ui.logMessage("FATAL: Could not start Media Server on port " + MEDIA_SERVER_PORT + ": " + e.getMessage());
            mediaServer = null;
        }
    }

    private void startHeartbeatLoop() {
        Thread heartbeat = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(2000);
                    if (isNetworkGame && !clients.isEmpty()) {
                        broadcastMessage(new NetworkMessage(
                                NetworkMessage.MessageType.SYNC_BOARD_STATE,
                                board.getFields()
                        ));
                    }
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        if (baseUI instanceof SwingUI) {
                            ((SwingUI) baseUI).updateBoardState(board.getFields());
                        }
                        refreshPropertiesWindow();
                    });
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Heartbeat error: " + e.getMessage());
                }
            }
        });
        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    public void updateBoardState(List<BoardField> newFields) {
        this.board.setFields(newFields);
        ui.updateBoardState(newFields);
        refreshPropertiesWindow();
    }

    public void startAsHost(String username) {
        startAsHost(username, 10365, null);
        startHeartbeatLoop();
    }

    private void startDiscoveryAnnouncer(String gameName, int port) {
        discoveryAnnouncerThread = new Thread(() -> {
            try (DatagramSocket broadcastSocket = new DatagramSocket()) {
                broadcastSocket.setBroadcast(true);
                String message = "VAULT-O-POLY_HOST:" + gameName + ":" + port;
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 12346);
                while (true) {
                    broadcastSocket.send(sendPacket);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.out.println("Discovery announcer stopped.");
            }
        });
        discoveryAnnouncerThread.start();
    }

    public void hostNewGame() {
        ui.showNotification("Hosting game... server is starting.");
        startAsHost("Host");
    }

    public void joinNewGame() {
        ui.showNotification("Please use the 'Join Game' button on the Main Menu.");
    }

    public void startNewGame(String username) {
        if (isNetworkGame) return;
        resetGame();
        this.gameState = GameState.INITIALIZING;
        ui.showGameWindow();
        SwingWorker<Void, Void> gameInitializer = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                initializeGame(username);
                return null;
            }
        };
        gameInitializer.execute();
        playLocalSound("start.wav");
    }

    public void startNewGame() {
        startNewGame("Player 1");
    }

    public void initializeGame(String localUsername) {
        if (isNetworkGame) return;
        ui.showNotification("Welcome to Fallout Monopoly!");
        int numPlayers = ui.askForInt("How many survivors? (2-4)", 2, 4);
        PlayerToken[] tokens = PlayerToken.values();
        Player player1 = new Player(localUsername, tokens[0]);
        players.add(player1);
        ui.createPlayerToken(player1);
        for (int i = 1; i < numPlayers; i++) {
            String name = ui.askForString("Enter name for Player " + (i + 1) + ":");
            if (name == null || name.isBlank()) name = "Survivor " + (i+1);
            Player player = new Player(name, tokens[i]);
            players.add(player);
            ui.createPlayerToken(player);
        }
        ui.showBoard(board.getFields());
        ui.logMessage("Successfully loaded board: board.json");
        this.currentPlayerIndex = 0;
        this.gameState = GameState.PLAYER_TURN;
        startNextTurn();
    }

    public void initializeGame() {
        initializeGame("Player 1");
    }

    private void resetGame() {
        players.clear();
        currentPlayerIndex = 0;
        this.eventDeck = new EventDeck();
        ui.resetBoard();
        ui.resetStats();
        ui.clearLog();
        ui.resetFullLog();
        this.gameState = GameState.INITIALIZING;
    }

    public void startNextTurn() {
        if (players.isEmpty()) return;
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.isInGame()) {
            nextTurn();
            refreshPropertiesWindow();
            startNextTurn();
            return;
        }
        this.currentCasinoConfig = new mechanics.CasinoConfiguration();
        gameState = GameState.PLAYER_TURN;
        ui.setPlayerTurn(currentPlayer);
        resourceManager.calculateResourceProduction(currentPlayer, ui);
        ui.updatePlayerStats(currentPlayer);

        ui.logMessage("--- " + currentPlayer.getName() + "'s Turn ---");
        ui.logMessage("S.P.E.C.I.A.L: " + currentPlayer.getSpecial().toString());

        if (currentPlayer.isInJail()) {
            handleJailTurn(currentPlayer);
        } else {
            ui.logMessage("--- " + currentPlayer.getName() + "'s Turn ---");
            ui.setControlsEnabled(true, true, true);
        }
    }

    public void doRoll() {
        if (isNetworkGame && networkOut != null) {
            sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_ROLL, null));
            return;
        }
        ui.setControlsEnabled(false, false, false);
        Player player = players.get(currentPlayerIndex);
        executeActualRoll(player);
    }

    private void executeActualRoll(Player player) {
        int totalRoll = dice.roll();
        ui.logMessage(player.getName() + " rolled a " + totalRoll + " (" + dice.getLastRollDescription() + ")!");
        boolean turnOver = movePlayerAndLand(player, totalRoll);
        if (turnOver) {
            endTurn();
        }
    }

    public void doImprove() {
        if (isNetworkGame && networkOut != null) {
            sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_IMPROVE, null));
            return;
        }
        Player player = players.get(currentPlayerIndex);
        handleImprovement(player);
    }

    public void doTrade() {
        if (isNetworkGame && networkOut != null) {
            sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_TRADE, null));
            return;
        }
        Player player = players.get(currentPlayerIndex);
        handleTrade(player);
    }

    public void doShowFullLog() {
        ui.showFullLog();
    }

    public void doShowProperties() {
        if (propertiesWindow == null) {
            if (baseUI instanceof SwingUI) {
                Frame mainFrame = ((SwingUI) baseUI).getGameWindow();
                propertiesWindow = new OwnedPropertiesWindow(mainFrame, this);
            } else {
                return;
            }
        }
        Player playerToShow;
        if (isNetworkGame && networkOut != null) {
            playerToShow = this.self;
        } else {
            if (players.isEmpty()) return;
            playerToShow = players.get(currentPlayerIndex);
        }
        if (playerToShow == null) return;
        propertiesWindow.update(board.getFields(), playerToShow);
        propertiesWindow.setVisible(true);
    }

    public void refreshPropertiesWindow() {
        if (propertiesWindow != null && propertiesWindow.isVisible()) {
            Player playerToShow;
            if (isNetworkGame && networkOut != null) {
                playerToShow = this.self;
            } else {
                if (players.isEmpty()) return;
                playerToShow = players.get(currentPlayerIndex);
            }
            if (playerToShow != null) {
                propertiesWindow.update(board.getFields(), playerToShow);
            }
        }
    }

    public void doLeaveGame() {
        if (isNetworkGame && networkOut != null) {
            if (baseUI instanceof SwingUI) {
                this.gameState = GameState.GAME_OVER;
            }
        } else {
            this.gameState = GameState.GAME_OVER;
            endGame();
        }
    }

    public void toggleMortgage(Player player, PropertyField property) {
        if (property.getOwner() != player) return;

        if (property.isMortgaged()) {
            int cost = (int) ((property.getPurchaseCost() / 2) * 1.1);
            if (player.canAfford(cost)) {
                playLocalSound("cash.wav");
                player.payCaps(cost);
                property.setMortgaged(false);
                ui.logMessage(player.getName() + " un-mortgaged " + property.getName() + " for " + cost + " caps.");
            } else {
                ui.showNotification("Not enough caps to un-mortgage!");
            }
        } else {
            playLocalSound("cash.wav");
            int value = property.getPurchaseCost() / 2;
            player.addCaps(value);
            property.setMortgaged(true);
            ui.logMessage(player.getName() + " mortgaged " + property.getName() + " for " + value + " caps.");
        }
        ui.updatePlayerStats(player);
        refreshPropertiesWindow();
    }

    private void handleImprovement(Player player) {
        List<PropertyField> properties = player.getOwnedProperties();
        if (properties.isEmpty()) {
            ui.showNotification("You don't own any properties to improve.");
            return;
        }
        String[] options = properties.stream()
                .map(p -> p.getName() + " (Lvl " + p.getCurrentImprovementLevel() + ")")
                .toArray(String[]::new);
        String choice = ui.askForSelection("Choose property to improve (or cancel):", options);
        if (choice != null) {
            completeImprovement(player, choice);
        }
    }

    private void completeImprovement(Player player, String choice) {
        if (choice == null) return;
        playLocalSound("build.wav");
        PropertyField propToImprove = player.getOwnedProperties().stream()
                .filter(p -> choice.startsWith(p.getName()))
                .findFirst()
                .orElse(null);
        if (propToImprove != null) {
            if (propertyDevelopment.canImprove(player, propToImprove, ui, this.board)) {
                propertyDevelopment.improveProperty(player, propToImprove, ui);
                refreshPropertiesWindow();
            }
        }
        ui.updatePlayerStats(player);
    }

    private void handleTrade(Player player) {
        if (pendingTradeRequester != null) {
            ui.showNotification("A trade is already in progress! Please wait.");
            return;
        }

        List<Player> otherPlayers = players.stream().filter(p -> p != player && p.isInGame()).collect(Collectors.toList());
        if (otherPlayers.isEmpty()) {
            ui.showNotification("There's no one left to trade with, you lone wanderer.");
            return;
        }
        String[] options = otherPlayers.stream().map(Player::getName).toArray(String[]::new);
        String choice = ui.askForSelection("Who do you want to trade with?", options);
        if (choice != null) {
            startTrade(player, choice);
        }
    }

    private void startTrade(Player requester, String partnerName) {
        if (partnerName == null) return;
        Player partner = players.stream()
                .filter(p -> p.getName().equals(partnerName) && p.isInGame())
                .findFirst()
                .orElse(null);
        if (partner == null) {
            ui.logMessage("Could not find trade partner.");
            return;
        }
        pendingTradeRequester = requester;
        pendingTradePartner = partner;
        pendingOffer1 = new TradeOffer();
        pendingOffer2 = new TradeOffer();
        requestOffer(requester, requester.getName() + ", what will you offer " + partner.getName() + "?");
    }

    private void requestOffer(Player playerToAsk, String prompt) {
        TradeOffer offer = ui.askForTradeOffer(playerToAsk, prompt);
        if (offer != null) {
            completeBuildOffer(playerToAsk, offer);
        }
    }

    private void completeBuildOffer(Player player, TradeOffer offer) {
        if (offer == null) {
            ui.logMessage("Trade cancelled.");
            clearPendingTrade();
            return;
        }
        if (player == pendingTradeRequester) {
            pendingOffer1 = offer;
            requestOffer(pendingTradePartner, pendingTradePartner.getName() + ", what will you offer " + pendingTradeRequester.getName() + " in return?");
        } else if (player == pendingTradePartner) {
            pendingOffer2 = offer;
            proposeTrade();
        }
    }

    private void proposeTrade() {
        String p1Name = pendingTradeRequester.getName();
        String p2Name = pendingTradePartner.getName();
        String summary = "--- THE PROPOSED DEAL ---\n\n" +
                p1Name + " offers:\n" +
                pendingOffer1.getSummary() + "\n" +
                p2Name + " offers:\n" +
                pendingOffer2.getSummary() + "\n" +
                "Do you accept this deal?";
        ui.askForBoolean(pendingTradeRequester, summary);
        ui.askForBoolean(pendingTradePartner, summary);
    }

    private void completeTradeAcceptance(Player player, boolean accepted) {
        if (pendingTradeRequester == null || pendingTradePartner == null || pendingOffer1 == null || pendingOffer2 == null) {
            ui.logMessage("[TradeSystem] Ignoring duplicate/late trade response from " + player.getName());
            return;
        }
        if (player == pendingTradeRequester) {
            pendingOffer1.responseReceived = true;
            pendingOffer1.accepted = accepted;
        } else if (player == pendingTradePartner) {
            pendingOffer2.responseReceived = true;
            pendingOffer2.accepted = accepted;
        }
        if (pendingOffer1.responseReceived && pendingOffer2.responseReceived) {
            if (pendingOffer1.accepted && pendingOffer2.accepted) {
                ui.logMessage("Trade accepted by both parties!");
                if (tradeSystem.executeTrade(pendingTradeRequester, pendingOffer1, pendingTradePartner, pendingOffer2, ui, board)) {
                    ui.logMessage("Trade successful!");
                    ui.updatePlayerStats(pendingTradeRequester);
                    ui.updatePlayerStats(pendingTradePartner);

                    refreshPropertiesWindow();
                }
            } else {
                ui.logMessage("Trade rejected.");
            }
            clearPendingTrade();
        }
    }

    private void clearPendingTrade() {
        pendingTradeRequester = null;
        pendingTradePartner = null;
        pendingOffer1 = null;
        pendingOffer2 = null;
    }

    private void handleBankruptcy(Player player) {
        ui.showNotification(player.getName() + " is bankrupt!");
        if (player.getBottleCaps() < 0) {
            player.setInGame(false);
            ui.logMessage("Liquidating " + player.getName() + "'s assets...");
            for (PropertyField prop : player.getOwnedProperties()) {
                prop.setOwner(null);
                prop.setMortgaged(false);
                ui.updatePropertyOwner(prop.getPosition(), null);
            }
            player.getOwnedProperties().clear();
            ui.removePlayerToken(player.getToken());
            ui.showNotification(player.getName() + " has been eliminated. Their properties are now unowned.");
            refreshPropertiesWindow();
            if (isNetworkGame) {
                broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.REMOVE_PLAYER_TOKEN, player));
                broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.SYNC_BOARD_STATE, board.getFields()));
            }
        }
    }

    private void handleJailTurn(Player player) {
        player.incrementJailTurns();
        ui.logMessage("--- " + player.getName() + "'s Turn (In Jail - Turn " + player.getJailTurns() + ") ---");

        if (player.getJailTurns() > 3) {
            ui.showNotification("You've been in jail for 3 turns. You must pay the 50 cap fine.");
            payFineAndFree(player);
            return;
        }

        List<String> options = new ArrayList<>();
        options.add("Try to roll doubles");
        options.add("Pay 50 caps fine");
        if (player.hasGetOutOfJailCard()) {
            options.add("Use 'Get Out of Jail Free' card");
        }

        ClientHandler handler = getHandlerForPlayer(player);
        if (handler != null) {
            ui.askForSelection("You are in jail. What do you do?", options.toArray(new String[0]));
            return;
        }
        String choice = ui.askForSelection("You are in jail. What do you do?", options.toArray(new String[0]));
        if (choice == null) choice = "Try to roll doubles";
        completeJailAction(player, choice);
    }

    private void completeJailAction(Player player, String choice) {
        if (choice == null) choice = "Try to roll doubles";
        switch (choice) {
            case "Pay 50 caps fine":
                payFineAndFree(player);
                break;
            case "Use 'Get Out of Jail Free' card":
                useCardAndFree(player);
                break;
            case "Try to roll doubles":
            default:
                rollForJail(player);
                break;
        }
    }

    private void payFineAndFree(Player player) {
        player.payCaps(50);
        player.setInJail(false);
        ui.updatePlayerStats(player);
        ui.logMessage("You paid the 50 cap fine. You are free!");
        ui.setControlsEnabled(true, true, true);
    }

    private void useCardAndFree(Player player) {
        player.setHasGetOutOfJailCard(false);
        player.setInJail(false);
        ui.logMessage("You used your 'Get Out of Jail Free' card. You are free!");
        ui.setControlsEnabled(true, true, true);
    }

    private void rollForJail(Player player) {
        int totalRoll = dice.roll();
        ui.logMessage("You roll a " + dice.getLastRollDescription() + "...");
        if (dice.didRollDoubles()) {
            ui.logMessage("Doubles! You are free! You move " + totalRoll + " spaces.");
            player.setInJail(false);
            boolean turnOver = movePlayerAndLand(player, totalRoll);
            if (turnOver) {
                endTurn();
            }
        } else {
            ui.logMessage("No luck. You remain in jail for another turn.");
            endTurn();
        }
    }

    public void requestPropertyPurchase(Player player, PropertyField field) {
        String prompt = "Do you want to buy " + field.getName() + " for " + field.getPurchaseCost() + " caps? (y/n)";
        if (isNetworkGame) {
            pendingPropertyPurchases.put(player, field);
            ClientHandler handler = getHandlerForPlayer(player);
            if (handler != null) {
                handler.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SHOW_BOOLEAN_DIALOG, prompt));
            } else {
                boolean wantsToBuy = ui.askForBoolean(prompt);
                completePropertyPurchase(player, wantsToBuy, field);
            }
        } else {
            boolean wantsToBuy = ui.askForBoolean(prompt);
            completePropertyPurchase(player, wantsToBuy, field);
        }
    }

    private void completePropertyPurchase(Player player, boolean wantsToBuy, PropertyField field) {
        if (wantsToBuy) {
            playLocalSound("cash.wav");
            player.payCaps(field.getPurchaseCost());
            field.setOwner(player);
            player.addProperty(field);
            ui.logMessage(player.getName() + " now owns " + field.getName() + "!");
            ui.updatePlayerStats(player);
            ui.updatePropertyOwner(field.getPosition(), player);
            refreshPropertiesWindow();
        } else {
            ui.logMessage(field.getName() + " remains unclaimed.");
        }
        pendingPropertyPurchases.remove(player);
        endTurn();
    }

    private boolean movePlayerAndLand(Player player, int roll) {
        int oldPos = player.getPosition();
        board.movePlayer(player, roll);
        ui.movePlayerToken(player.getToken(), player.getPosition());
        BoardField landedField = board.getField(player.getPosition());
        ui.logMessage(player.getName() + " landed on " + landedField.getName() + ".");
        ui.logMessage("> " + landedField.getDescription());
        boolean turnOver = landedField.onLand(player, this);
        ui.updatePlayerStats(player);
        if (player.getBottleCaps() < 0) {
            handleBankruptcy(player);
        }
        if (player.getPosition() < oldPos) {
            playLocalSound("levelup.wav");
        }
        return turnOver;
    }

    public void nextTurn() {
        if (players.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private void endTurn() {
        checkWinConditions();
        if (gameState != GameState.GAME_OVER) {
            nextTurn();
            startNextTurn();
        } else {
            endGame();
            ui.setControlsEnabled(false, false, false);
            ui.hideGameWindow();
            ui.showMainMenu();
        }
    }

    public void checkWinConditions() {
        long playersInGame = players.stream().filter(Player::isInGame).count();
        if (players.size() > 1 && playersInGame == 1) {
            gameState = GameState.GAME_OVER;
        }
    }

    public void endGame() {
        if (discoveryAnnouncerThread != null) {
            discoveryAnnouncerThread.interrupt();
        }
        ui.stopRadio();
        Player winner = players.stream().filter(Player::isInGame).findFirst().orElse(null);
        String msg = (winner != null) ? "\n--- GAME OVER ---\nThe winner is " + winner.getName() + "!" : "The game ended in a draw.";
        ui.showNotification(msg);
        if (isNetworkGame) {
            broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.GAME_OVER, msg));
        }
        running = false;
    }

    public UIInterface getUi() { return ui; }
    public Board getBoard() { return board; }
    public EventDeck getEventDeck() { return eventDeck; }
    public PropertyDevelopment getPropertyDevelopment() { return propertyDevelopment; }
    public List<Player> getPlayers() { return this.players; }

    public void setPropertyOwner(int position, Player owner) {
        if (board != null) {
            BoardField field = board.getField(position);
            if (field instanceof PropertyField) {
                ((PropertyField) field).setOwner(owner);
            }
        }
    }

    private ClientHandler getHandlerForPlayer(Player player) {
        return clients.stream()
                .filter(c -> c.getPlayer() == player)
                .findFirst()
                .orElse(null);
    }

    // --- NEW IMAGE SANITIZATION HELPERS ---
    private static final String[] UNSUPPORTED_EXTENSIONS = {".avif", ".tiff", ".heic"};
    private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};

    private String sanitizeUrlAggressively(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) return originalUrl;
        String cleanUrl = originalUrl;
        String lowerUrl = cleanUrl.toLowerCase();
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex != -1) cleanUrl = cleanUrl.substring(0, queryIndex);
        int anchorIndex = cleanUrl.indexOf('#');
        if (anchorIndex != -1) cleanUrl = cleanUrl.substring(0, anchorIndex);
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerUrl.contains(ext)) {
                int extIndex = lowerUrl.lastIndexOf(ext);
                return cleanUrl.substring(0, extIndex + ext.length());
            }
        }
        return cleanUrl;
    }

    private boolean isUnsupportedFormat(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        for (String ext : UNSUPPORTED_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        return false;
    }

    private String insertZeroWidthSpace(String text, int maxLength) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.length() > maxLength) {
                result.append('\u200B');
                for (int i = 0; i < word.length(); i += maxLength) {
                    result.append(word.substring(i, Math.min(i + maxLength, word.length())));
                    if (i + maxLength < word.length()) result.append('\u200B');
                }
            } else {
                result.append(word);
            }
            result.append(" ");
        }
        return result.toString().trim();
    }

    // --- HELPER: Parse Time Strings ---
    private double parseTime(String timeStr) {
        if (timeStr == null) return 0;
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 1) return Double.parseDouble(parts[0]);
            if (parts.length == 2) return Double.parseDouble(parts[0]) * 60 + Double.parseDouble(parts[1]);
            if (parts.length == 3) return Double.parseDouble(parts[0]) * 3600 + Double.parseDouble(parts[1]) * 60 + Double.parseDouble(parts[2]);
        } catch (Exception e) {}
        return 0;
    }

    // --- NETWORK HELPER METHODS ---
    public void addClient(ClientHandler client, String username) {
        ui.logMessage("Client connecting: " + username);
        Player playerForClient = null;
        boolean isReconnection = false;
        for (Player p : players) {
            if (p != this.self && p.getNetworkHandler() == null) {
                playerForClient = p;
                isReconnection = true;
                p.setName(username);
                ui.logMessage("Reconnecting " + username + " to existing slot: " + p.getToken());
                break;
            }
        }
        if (playerForClient == null) {
            if (players.size() >= util.PlayerToken.values().length) ui.logMessage("Warning: Max players reached. Adding anyway.");
            PlayerToken token = PlayerToken.values()[players.size() % PlayerToken.values().length];
            playerForClient = new Player(username, token);
            players.add(playerForClient);
        }
        playerForClient.setNetworkHandler(client);
        client.setPlayer(playerForClient);
        this.clients.add(client);
        client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SET_CLIENT_PLAYER, playerForClient));
        client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.INITIALIZE_BOARD, board.getFields()));
        for (Player p : players) {
            client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.ADD_PLAYER_TOKEN, p));
            client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_PLAYER_STATS, p));
            client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.MOVE_PLAYER_TOKEN, new Object[]{p.getToken(), p.getPosition()}));
        }
        for (BoardField field : board.getFields()) {
            if (field instanceof PropertyField) {
                PropertyField prop = (PropertyField) field;
                if (prop.getOwner() != null) {
                    client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_PROPERTY_OWNER, new Object[]{prop.getPosition(), prop.getOwner()}));
                }
            }
        }
        if (!isReconnection) ui.createPlayerToken(playerForClient);
        else {
            ui.updatePlayerStats(playerForClient);
            broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_PLAYER_STATS, playerForClient));
        }
        Player currentPlayer = players.get(currentPlayerIndex);
        client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SET_PLAYER_TURN, currentPlayer));
        ui.displayChatMessage("*** " + username + " has joined the game. ***");
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        Player player = client.getPlayer();
        if (player != null) {
            player.setNetworkHandler(null);
            ui.logMessage("Player " + player.getName() + " disconnected.");
            ui.displayChatMessage("*** " + player.getName() + " has disconnected. ***");
        }
    }

    private static final Map<PlayerToken, String> PLAYER_COLORS = Map.of(
            PlayerToken.VAULT_BOY, "#88B1D8",
            PlayerToken.NUKA_GIRL, "#D88888",
            PlayerToken.DOGMEAT, "#D8B188",
            PlayerToken.GHOUL, "#88D888"
    );
    private static final String DEFAULT_COLOR = "#AAAAAA";

    /**
     * Handles receiving raw chat data (Name + Message).
     * Finds the local Player object (to get the correct Token enum)
     * and generates the HTML using LOCAL resources.
     */
    public void handleIncomingChat(String senderName, String message) {
        Player p = getPlayerByName(senderName);

        // If player not found (e.g. spectator), create a dummy so we don't crash
        if (p == null) {
            p = new Player(senderName, PlayerToken.VAULT_BOY);
        }

        // Generate HTML locally.
        // getPlayerIconTag will now find the image on THIS computer.
        String html = formatPublicMessage(p, message);

        // Display on UI
        baseUI.displayChatMessage(html);
    }

    public void sendChatMessage(String message) {
        if (message == null || message.isBlank()) return;

        // Handle local commands (/casino, etc)
        if (message.trim().equalsIgnoreCase("/casino")) {
            doCasino();
            return;
        }

        Player player = isNetworkGame ? this.self : players.get(currentPlayerIndex);

        // Handle Slash Commands
        if (message.startsWith("/")) {
            String command = message.split(" ")[0].toLowerCase();

            // Client-side commands that don't need network relay immediately
            if (command.equals("/radio") || command.equals("/music") ||
                    command.equals("/log") || command.equals("/history") ||
                    command.equals("/help")) {
                handleChatCommand(player, message);
                return;
            }

            // If it's a command that needs to go to server (like /roll, /pay), send it as a REQUEST
            if (isNetworkGame && networkOut != null) {
                sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_CHAT_MESSAGE, message));
                return;
            }

            handleChatCommand(player, message);
            return;
        }

        // Normal Chat Message Handling
        if (isNetworkGame && networkOut != null) {
            // CLIENT: Send request to server. Do NOT display locally yet (prevents duplicates).
            sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_CHAT_MESSAGE, message));
        } else {
            // HOST / LOCAL:
            if (isNetworkGame) {
                // Host displays locally
                handleIncomingChat(player.getName(), message);
                // Host broadcasts RAW data to clients
                String[] payload = { player.getName(), message };
                broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.PLAYER_CHAT, payload));
            } else {
                // Single player / Local hotseat
                handleIncomingChat(player.getName(), message);
            }
        }
    }

    private String getTimestamp() { return new SimpleDateFormat("HH:mm").format(new Date()); }
    private String getPlayerColor(Player player) { return (player == null || player.getToken() == null) ? DEFAULT_COLOR : PLAYER_COLORS.getOrDefault(player.getToken(), DEFAULT_COLOR); }
    private Player getPlayerByName(String name) { if (name == null) return null; for (Player p : players) if (p.getName().equalsIgnoreCase(name)) return p; return null; }
    // --- FIX: Player Token Pathing for Chat ---
    /**
     * Generates an HTML image tag for the player's token icon.
     */
    private String getPlayerIconTag(Player player) {
        if (player == null || player.getToken() == null) return "";

        // Construct path: e.g., /tokens/vault_boy.png
        String path = "/tokens/" + player.getToken().name().toLowerCase() + ".png";

        // This now runs on the CLIENT's machine, so it finds the image inside the Client's JAR/Resource folder
        java.net.URL url = getClass().getResource(path);

        if (url != null) {
            return "<img src='" + url.toString() + "' width='24' height='24' align='middle'> ";
        }
        return "";
    }
    private String escapeHtml(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private String formatPublicMessage(Player player, String message) {
        String color = getPlayerColor(player);
        String name = escapeHtml(player.getName());
        String processedMessage = insertZeroWidthSpace(message, 20);
        String msg = escapeHtml(processedMessage);
        String iconHtml = getPlayerIconTag(player);
        return String.format("<div class=\"bubble-container\"><p class=\"chat-bubble\"><span class=\"timestamp\">%s</span>%s<span class=\"name\" style=\"color: %s;\">%s:</span> <span class=\"message\">%s</span></p></div>", getTimestamp(), iconHtml, color, name, msg);
    }

    private String formatMeMessage(Player player, String message) {
        String color = getPlayerColor(player);
        String name = escapeHtml(player.getName());
        String processedMessage = insertZeroWidthSpace(message, 20);
        String msg = escapeHtml(processedMessage);
        return String.format("<p class=\"system log\">* <font color='%s'>%s</font> %s *</p>", color, name, msg);
    }

    private String formatGifMessage(Player player, String url) {
        // This method is now used by both the conversion process (to create the URL)
        // AND the chat message handler (to display the final result).
        String color = getPlayerColor(player);
        String name = escapeHtml(player.getName());
        String finalUrl = url;
        if (isUnsupportedFormat(finalUrl)) ui.logMessage("NOTE: Image format (e.g., .webp) is NOT supported by the game client and may not display: " + finalUrl);
        return String.format("<div class=\"bubble-container\"><p class=\"chat-bubble\"><span class=\"timestamp\">%s</span><span class=\"name\" style=\"color: %s;\">%s:</span> <span class=\"message\"><img src='%s' width='150'></span></p></div>", getTimestamp(), color, name, finalUrl);
    }

    private String formatPrivateMessage(String fromName, String toName, String message, String fromColor, String toColor, boolean isSender) {
        String processedMessage = insertZeroWidthSpace(message, 20);
        String msg = escapeHtml(processedMessage);
        if (isSender) return String.format("<p class=\"system private\">%s [To <font color='%s'>%s</font>]: %s</p>", getTimestamp(), toColor, escapeHtml(toName), msg);
        else return String.format("<p class=\"system private\">%s [From <font color='%s'>%s</font>]: %s</p>", getTimestamp(), fromColor, escapeHtml(fromName), msg);
    }

    private void sendPrivateMessage(Player player, String htmlMessage) {
        if (isNetworkGame) {
            ClientHandler handler = getHandlerForPlayer(player);
            if (handler != null) handler.sendMessage(new NetworkMessage(NetworkMessage.MessageType.CHAT_MESSAGE, htmlMessage));
            else baseUI.displayChatMessage(htmlMessage);
        } else {
            baseUI.displayChatMessage(htmlMessage);
        }
    }

    private void handleChatCommand(Player player, String message) {
        String[] parts = message.split(" ");
        String command = parts[0].toLowerCase();

        // --- ADDED /gif-url HANDLER ---
        if (command.equals("/gif-url")) {
            if (parts.length > 1) {
                // The URL is passed as the payload.
                String url = parts[1];
                handleGifUrlMessage(player, url);
                return;
            }
        }
        List<String> debugCmds = Arrays.asList("/setcaps", "/setowner", "/setres", "/setscraps", "/teleport", "/tp", "/addcaps");
        if (debugCmds.contains(command)) {
            if (isNetworkGame && !player.equals(this.self)) {
                sendPrivateMessage(player, "<p class=\"log\">" + getTimestamp() + " <i>[System] Access Denied: Host Only.</i></p>");
                return;
            }
            handleDebugCommand(player, command, parts);
            return;
        }
        switch (command) {
            case "/roll":
                if (player == players.get(currentPlayerIndex) && gameState == GameState.PLAYER_TURN && !player.isInJail()) {
                    ui.logMessage("*** " + player.getName() + " used /roll command! ***"); doRoll();
                } else { sendPrivateMessage(player, "<p class=\"log\">"+getTimestamp()+" <i>[System] You can't use /roll right now.</i></p>"); }
                break;
            case "/casino":
                if (player == players.get(currentPlayerIndex) && gameState == GameState.PLAYER_TURN && !player.isInJail()) {
                    ClientHandler client = getHandlerForPlayer(player);
                    if (client != null) client.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SHOW_CASINO_DIALOG, this.currentCasinoConfig));
                } else { sendPrivateMessage(player, "<p class=\"log\">"+getTimestamp()+" <i>[System] You can't use /casino right now.</i></p>"); }
                break;
            case "/help":
                String helpMsg = "<p class=\"log\">" + getTimestamp() + " <i>[System] Commands:<br>&nbsp; /roll, /casino, /w [name] [msg], /me [action], /gif [url]<br>&nbsp; (Host Only): /setcaps, /setowner, /setres, /teleport</i></p>";
                sendPrivateMessage(player, helpMsg);
                break;
            case "/w": case "/msg":
                if (parts.length < 3) { sendPrivateMessage(player, "<p class=\"log\">"+getTimestamp()+" <i>[System] Usage: /w [name] [message]</i></p>"); return; }
                Player target = getPlayerByName(parts[1]);
                if (target == null) { sendPrivateMessage(player, "<p class=\"log\">"+getTimestamp()+" <i>[System] Player not found.</i></p>"); return; }
                String pm = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                String fromColor = getPlayerColor(player); String toColor = getPlayerColor(target);
                sendPrivateMessage(player, formatPrivateMessage(player.getName(), target.getName(), pm, fromColor, toColor, true));
                sendPrivateMessage(target, formatPrivateMessage(player.getName(), target.getName(), pm, fromColor, toColor, false));
                break;
            case "/me":
                if (parts.length < 2) return;
                String action = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                ui.displayChatMessage(formatMeMessage(player, action));
                break;
            case "/gif":
                if (parts.length < 2) return;

                String rawUrl = parts[1];
                rawUrl = rawUrl.replace("\u200B", "");
                boolean cleanFlag = (parts.length > 2 && parts[2].equalsIgnoreCase("-c"));
                final String url = cleanFlag ? sanitizeUrlAggressively(rawUrl) : rawUrl;

                String start = "0";
                String duration = "10";

                // --- FIX: Apply variable max duration ---
                double maxDurationSeconds = 30.0;
                // If the player issuing the command is the host instance, allow 60s
                if (player.equals(this.self)) {
                    maxDurationSeconds = 60.0;
                }

                int argIdx = cleanFlag ? 3 : 2;

                // Arg 1: Start Time
                if (parts.length > argIdx) {
                    if (parts[argIdx].matches("[0-9:.]+")) {
                        start = parts[argIdx];
                    }
                }

                // Arg 2: Duration (FIXED CLAMP LOGIC)
                if (parts.length > argIdx + 1) {
                    if (parts[argIdx + 1].matches("[0-9.]+")) {
                        double d = Double.parseDouble(parts[argIdx + 1]);

                        if (d > maxDurationSeconds) {
                            d = maxDurationSeconds; // Clamp to allowed maximum
                            sendPrivateMessage(player, String.format("Duration clamped to max allowed: %.0fs.", maxDurationSeconds));
                        }
                        duration = String.valueOf(d);
                    }
                }

                // Calculate dynamic download limit
                double startSec = parseTime(start);
                double durSec = parseTime(duration);
                long calcLimit = (long) (20 * 1024 * 1024 + (startSec + durSec) * 1.5 * 1024 * 1024);

                String cleanPath = url;
                if (cleanPath.contains("?")) cleanPath = cleanPath.substring(0, cleanPath.indexOf("?"));
                String lowerPath = cleanPath.toLowerCase();

                if (util.ImageConverter.isWebM(url) || lowerPath.endsWith(".mkv") || lowerPath.endsWith(".mov") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mp4") || lowerPath.endsWith(".m4v")) {
                    processVideoToGif(url, start, duration, calcLimit);
                    return;
                }
                if (util.ImageConverter.isWebP(url)) {
                    ui.logMessage("Converting WebP image...");
                    ui.showProgress("Converting WebP...", -1);
                    new SwingWorker<String, Void>() {
                        @Override protected String doInBackground() { return util.ImageConverter.convertWebPtoPng(url); }
                        @Override protected void done() {
                            ui.hideProgress();
                            try {
                                String localPngUrl = get();
                                ui.displayChatMessage(formatGifMessage(player, localPngUrl));
                            } catch (Exception e) { ui.logMessage("Failed to load image."); }
                        }
                    }.execute();
                    return;
                }
                ui.displayChatMessage(formatGifMessage(player, url));
                break;
            default:
                sendPrivateMessage(player, "<p class=\"log\">"+getTimestamp()+" <i>[System] Unknown command.</i></p>");
                break;
            case "/radio": case "/music":
                ui.showRadioWindow();
                sendPrivateMessage(player, "<p class=\"log\">" + getTimestamp() + " <i>[System] Opening Radio Tuner...</i></p>");
                break;
            case "/log": case "/history":
                ui.showFullLog();
                sendPrivateMessage(player, "<p class=\"log\">" + getTimestamp() + " <i>[System] Opening Full Log window...</i></p>");
                break;
            case "/flush":
                // 1. Clear the visual chat panel.
                ui.clearLog();

                // 2. Send a system message to confirm the action.
                // Note: The client must receive the broadcasted clearLog() first,
                // and then receive this message.
                sendPrivateMessage(player, "<p class=\"log\">" + getTimestamp() + " <i>[System] Chat history cleared.</i></p>");
                break;
        }
    }

    /**
     * Final stage of video conversion: Handles caching and constructing the public URL.
     */
    private void processVideoToGif(String videoUrl, String startTime, String duration, long maxDownloadBytes) {
        ui.logMessage("Converting Video (" + startTime + "s, len " + duration + "s)...");
        ui.showProgress("Connecting...", -1);

        new Thread(() -> {
            try {
                // --- 1. CACHE SETUP & CHECK ---
                String cacheKey = generateHash(videoUrl + "_" + startTime + "_" + duration);
                java.io.File cacheDir = new java.io.File(getCacheDirectory());

                if (!cacheDir.exists()) cacheDir.mkdirs();

                java.io.File finalGif = new java.io.File(cacheDir, cacheKey + ".gif");

                boolean cacheHit = finalGif.exists();

                if (cacheHit) {
                    ui.logMessage("GIF Cache Hit. Reusing " + finalGif.getName());
                    ui.hideProgress();
                } else {
                    // --- 2. DOWNLOAD (Only if Cache Miss) ---

                    java.net.URL url = new java.net.URL(videoUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    String hostUrl = url.getProtocol() + "://" + url.getHost();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    conn.setRequestProperty("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5");
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                    conn.setRequestProperty("Referer", hostUrl);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(10000);

                    if (conn.getResponseCode() != 200) {
                        ui.logMessage("Download Failed: HTTP " + conn.getResponseCode());
                        ui.hideProgress();
                        return;
                    }

                    long totalSize = conn.getContentLengthLong();
                    ui.showProgress("Downloading...", 0);

                    String path = url.getPath();
                    String ext = ".mp4";
                    if (path.contains(".")) ext = path.substring(path.lastIndexOf('.'));

                    java.io.File tempVideo = java.io.File.createTempFile("vop_raw_", ext);
                    tempVideo.deleteOnExit();

                    long maxBytes = maxDownloadBytes;
                    long totalBytes = 0;

                    // FIX: Tracking thresholds for accurate updates
                    long reportInterval = 1024 * 50; // 50KB
                    long nextReportThreshold = reportInterval;

                    try (java.io.InputStream in = conn.getInputStream();
                         java.io.OutputStream out = new java.io.FileOutputStream(tempVideo)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;

                            // FIX: Check if totalBytes has crossed the next reporting threshold
                            if (totalBytes >= nextReportThreshold) {
                                if (totalSize > 0) {
                                    int percent = (int) ((totalBytes * 100) / totalSize);
                                    ui.showProgress("Downloading... " + (totalBytes / 1024) + "KB", percent);
                                } else {
                                    ui.showProgress("Downloading... " + (totalBytes / 1024) + "KB", -1);
                                }
                                nextReportThreshold = totalBytes + reportInterval; // Advance the threshold
                            }
                            if (totalBytes > maxBytes) break;
                        }
                    }

                    // FIX: Final update to guarantee 100% is shown for small files
                    if (totalSize > 0) {
                        ui.showProgress("Downloading... " + (totalBytes / 1024) + "KB", 100);
                    } else {
                        ui.showProgress("Downloading... " + (totalBytes / 1024) + "KB", -1);
                    }

                    // --- 3. RUN CONVERSION (Conversion Code Omitted) ---
                    java.io.File toolsDir = new java.io.File("tools");
                    java.io.File ffmpegExe = new java.io.File(toolsDir, "ffmpeg.exe");

                    if (!ffmpegExe.exists()) {
                        ui.logMessage("Error: FFmpeg converter tool is missing.");
                        ui.logMessage("Please ensure 'tools/ffmpeg.exe' is installed.");
                        ui.hideProgress();
                        return;
                    }

                    ui.showProgress("Converting (FFmpeg)...", -1);

                    ProcessBuilder pb = new ProcessBuilder(
                            ffmpegExe.getAbsolutePath(),
                            "-y",
                            "-ss", startTime,
                            "-t", duration,
                            "-i", tempVideo.getAbsolutePath(),
                            "-vf", "fps=10,scale=320:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse",
                            finalGif.getAbsolutePath()
                    );

                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    process.getInputStream().transferTo(System.out);

                    ui.hideProgress();

                    if (process.waitFor() != 0) {
                        ui.logMessage("Conversion failed (Check if start time is within downloaded range).");
                        return;
                    }
                }

                // --- 3. URL CONSTRUCTION AND BROADCAST ---
                String finalUrl;

                if (isNetworkGame && networkOut == null) { // Host

                    // FIX: Use manual tunnel details if provided
                    if (this.externalTunnelIp != null && this.externalTunnelGamePort != 0) {
                        // USE TUNNEL IP + CALCULATED EXTERNAL PORT
                        String broadcastIp = this.externalTunnelIp;
                        int broadcastPort = this.externalTunnelGamePort + (MEDIA_SERVER_PORT - 10365); // external port + 3

                        finalUrl = String.format("http://%s:%d/gifs/%s.gif", broadcastIp, broadcastPort, cacheKey);

                    } else if (mediaServer == null || hostPublicIp == null) {
                        ui.logMessage("Error: Media server not running or public IP unknown. Cannot share GIF.");
                        finalUrl = finalGif.toURI().toString();
                    } else {
                        // Fallback to Public IP / Dynamic Tunnel Port calculation (Less reliable than manual input)
                        String broadcastIp = hostPublicIp;
                        finalUrl = String.format("http://%s:%d/gifs/%s.gif", broadcastIp, this.externalMediaPort, cacheKey);
                    }
                } else { // Client or Local Only
                    finalUrl = finalGif.toURI().toString();
                }

                // Construct the dedicated command to carry the public/local URL
                String urlMessage = "/gif " + finalUrl;

                // 2. Network Relay: Send this new command through the chat pipeline
                if (isNetworkGame) {
                    if (networkOut != null) { // Client -> send to Host
                        sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_CHAT_MESSAGE, urlMessage));
                    } else { // Host -> process this command and broadcast the formatted result
                        handleChatCommand(self, urlMessage);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                ui.logMessage("Error: " + e.getMessage());
                ui.hideProgress();
            }
        }).start();
    }

    /**
     * Handles the final step: Client receives a public GIF URL and displays it.
     */
    public void handleGifUrlMessage(Player player, String url) {
        // This is where the client receives the public URL (via the normal chat message path)
        ui.displayChatMessage(formatGifMessage(player, url));
    }

    private void handleDebugCommand(Player admin, String command, String[] parts) {
        try {
            switch (command) {
                case "/setcaps":
                    if (parts.length < 3) throw new IllegalArgumentException("Usage: /setcaps [player] [amount]");
                    Player tCaps = getPlayerByName(parts[1]); if (tCaps == null) throw new IllegalArgumentException("Player not found.");
                    int caps = Integer.parseInt(parts[2]);
                    tCaps.payCaps(tCaps.getBottleCaps()); tCaps.addCaps(caps);
                    ui.updatePlayerStats(tCaps); ui.logMessage("[Admin] Set " + tCaps.getName() + " caps to " + caps);
                    break;
                case "/addcaps":
                    if (parts.length < 3) throw new IllegalArgumentException("Usage: /addcaps [player] [amount]");
                    Player tAdd = getPlayerByName(parts[1]); if (tAdd == null) throw new IllegalArgumentException("Player not found.");
                    int add = Integer.parseInt(parts[2]);
                    tAdd.addCaps(add);
                    ui.updatePlayerStats(tAdd); ui.logMessage("[Admin] Added " + add + " caps to " + tAdd.getName());
                    break;
                case "/setowner":
                    if (parts.length < 3) throw new IllegalArgumentException("Usage: /setowner [pos] [player]");
                    int pos = Integer.parseInt(parts[1]);
                    String pName = parts[2];
                    BoardField field = board.getField(pos);
                    if (!(field instanceof PropertyField)) throw new IllegalArgumentException("Field " + pos + " is not a property.");
                    PropertyField prop = (PropertyField) field;
                    if (pName.equalsIgnoreCase("null") || pName.equalsIgnoreCase("nobody")) {
                        if (prop.getOwner() != null) prop.getOwner().removeProperty(prop);
                        prop.setOwner(null); ui.updatePropertyOwner(pos, null);
                        ui.logMessage("[Admin] Cleared ownership of " + prop.getName());
                    } else {
                        Player newOwner = getPlayerByName(pName); if (newOwner == null) throw new IllegalArgumentException("Player not found.");
                        if (prop.getOwner() != null) prop.getOwner().removeProperty(prop);
                        prop.setOwner(newOwner); newOwner.addProperty(prop); ui.updatePropertyOwner(pos, newOwner);
                        ui.logMessage("[Admin] Set owner of " + prop.getName() + " to " + newOwner.getName());
                    }
                    refreshPropertiesWindow();
                    break;
                case "/setscraps": case "/setres":
                    int argOffset = command.equals("/setscraps") ? 0 : 1;
                    if (parts.length < 3 + argOffset) throw new IllegalArgumentException("Usage: /setres [player] [type] [amount]");
                    Player tRes = getPlayerByName(parts[1]); if (tRes == null) throw new IllegalArgumentException("Player not found.");
                    ResourceType type;
                    int amount;
                    if (command.equals("/setscraps")) { type = ResourceType.SCRAP_MATERIAL; amount = Integer.parseInt(parts[2]); }
                    else { type = ResourceType.valueOf(parts[2].toUpperCase()); amount = Integer.parseInt(parts[3]); }
                    tRes.getResources().put(type, amount);
                    ui.updatePlayerStats(tRes);
                    ui.logMessage("[Admin] Set " + tRes.getName() + " " + type.name() + " to " + amount);
                    break;
                case "/tp": case "/teleport":
                    if (parts.length < 3) throw new IllegalArgumentException("Usage: /teleport [player] [pos]");
                    Player tPort = getPlayerByName(parts[1]); if (tPort == null) throw new IllegalArgumentException("Player not found.");
                    int newPos = Integer.parseInt(parts[2]);
                    if (newPos < 0 || newPos >= 40) throw new IllegalArgumentException("Invalid position (0-39).");
                    board.movePlayer(tPort, 0);
                    tPort.setPosition(newPos);
                    ui.movePlayerToken(tPort.getToken(), newPos);
                    ui.logMessage("[Admin] Teleported " + tPort.getName() + " to " + newPos);
                    break;
            }
        } catch (Exception e) {
            sendPrivateMessage(admin, "<p class=\"log\">" + getTimestamp() + " <i>[Error] " + e.getMessage() + "</i></p>");
        }
    }

    // --- UPDATED: Broadcast with Exclude ---
    public void broadcastMessage(NetworkMessage msg, ClientHandler exclude) {
        if (!isNetworkGame) return;
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(msg);
            }
        }
    }

    public void broadcastMessage(NetworkMessage msg) {
        broadcastMessage(msg, null);
    }

    private synchronized void sendNetworkMessage(NetworkMessage msg) {
        try {
            if (networkOut != null) {
                networkOut.writeObject(msg);
                networkOut.flush();
                networkOut.reset();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- CRITICAL FIX: handleNetworkMessage Threading ---
    public void handleNetworkMessage(NetworkMessage msg, ClientHandler sender) {
        Player player = (sender != null) ? sender.getPlayer() : null;

        // Basic validation
        if (player != null && !players.isEmpty() && player != players.get(currentPlayerIndex)) {
            switch (msg.getType()) {
                case REQUEST_ROLL:
                case REQUEST_IMPROVE:
                case RESPONSE_BUY_PROPERTY:
                case RESPONSE_IMPROVE_SELECTION:
                    sender.sendMessage(new NetworkMessage(NetworkMessage.MessageType.LOG_MESSAGE, "It's not your turn."));
                    return;
            }
        }

        switch (msg.getType()) {
            // --- Non-Blocking Requests ---
            case REQUEST_ROLL: doRoll(); break;
            case REQUEST_IMPROVE: handleImprovement(player); break;
            case REQUEST_TRADE: handleTrade(player); break;
            case RESPONSE_BUY_PROPERTY:
                PropertyField field = pendingPropertyPurchases.get(player);
                if (field != null) completePropertyPurchase(player, (Boolean) msg.getPayload(), field);
                break;
            case RESPONSE_IMPROVE_SELECTION: completeImprovement(player, (String) msg.getPayload()); break;
            case RESPONSE_TRADE_SELECTION: startTrade(player, (String) msg.getPayload()); break;
            case RESPONSE_BUILD_OFFER: completeBuildOffer(player, (TradeOffer) msg.getPayload()); break;
            case RESPONSE_ACCEPT_TRADE: completeTradeAcceptance(player, (Boolean) msg.getPayload()); break;
            case REQUEST_CASINO:
                if (player != null && player == players.get(currentPlayerIndex)) {
                    sender.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SHOW_CASINO_DIALOG, this.currentCasinoConfig));
                }
                break;
            case RESPONSE_CASINO_RESULT:
                if (player != null) applyCasinoResult(player, (mechanics.CasinoResult) msg.getPayload());
                break;

            // --- FIX: Blocking UI Requests Wrapped in invokeLater ---
            // This frees the ClientHandler thread immediately so heartbeats don't timeout.

            case SHOW_CASINO_DIALOG:
                mechanics.CasinoConfiguration config = (mechanics.CasinoConfiguration) msg.getPayload();
                SwingUtilities.invokeLater(() -> ui.showCasinoDialog(this.self, this, config));
                break;

            case SHOW_BOOLEAN_DIALOG:
                String boolPrompt = (String) msg.getPayload();
                SwingUtilities.invokeLater(() -> {
                    boolean result = ui.askForBoolean(boolPrompt);
                    // Send response manually since we are in the controller context
                    if (networkOut != null) {
                        try {
                            networkOut.writeObject(new NetworkMessage(NetworkMessage.MessageType.RESPONSE_BUY_PROPERTY, result));
                            networkOut.flush(); networkOut.reset();
                        } catch(Exception e) { e.printStackTrace(); }
                    }
                });
                break;

            case SHOW_SELECTION_DIALOG:
                Object[] selData = (Object[]) msg.getPayload();
                String selPrompt = (String) selData[0];
                String[] selOptions = (String[]) selData[1];
                SwingUtilities.invokeLater(() -> {
                    String choice = ui.askForSelection(selPrompt, selOptions);
                    NetworkMessage.MessageType type = NetworkMessage.MessageType.LOG_MESSAGE; // Default
                    if (selPrompt.contains("jail")) type = NetworkMessage.MessageType.RESPONSE_JAIL_ACTION;
                    else if (selPrompt.contains("improve")) type = NetworkMessage.MessageType.RESPONSE_IMPROVE_SELECTION;
                    else if (selPrompt.contains("trade")) type = NetworkMessage.MessageType.RESPONSE_TRADE_SELECTION;

                    if (networkOut != null) {
                        try {
                            networkOut.writeObject(new NetworkMessage(type, choice));
                            networkOut.flush(); networkOut.reset();
                        } catch(Exception e) { e.printStackTrace(); }
                    }
                });
                break;

            case REQUEST_BUILD_OFFER:
                String buildPrompt = (String) msg.getPayload();
                SwingUtilities.invokeLater(() -> {
                    TradeOffer offer = ui.askForTradeOffer(this.self, buildPrompt);
                    if (networkOut != null) {
                        try {
                            networkOut.writeObject(new NetworkMessage(NetworkMessage.MessageType.RESPONSE_BUILD_OFFER, offer));
                            networkOut.flush(); networkOut.reset();
                        } catch(Exception e) { e.printStackTrace(); }
                    }
                });
                break;

            case REQUEST_ACCEPT_TRADE:
                String tradeSummary = (String) msg.getPayload();
                SwingUtilities.invokeLater(() -> {
                    boolean accepted = ui.askForBoolean(tradeSummary);
                    if (networkOut != null) {
                        try {
                            networkOut.writeObject(new NetworkMessage(NetworkMessage.MessageType.RESPONSE_ACCEPT_TRADE, accepted));
                            networkOut.flush(); networkOut.reset();
                        } catch(Exception e) { e.printStackTrace(); }
                    }
                });
                break;

            case REQUEST_CHAT_MESSAGE:
                if (player != null) {
                    String message = (String) msg.getPayload();

                    // Check for commands
                    if (message.startsWith("/")) {
                        handleChatCommand(player, message);
                        return;
                    }

                    // It's a standard chat message.
                    // 1. Display on Host UI (Locally)
                    handleIncomingChat(player.getName(), message);

                    // 2. Broadcast RAW DATA to all clients (including sender)
                    // Using PLAYER_CHAT ensures clients generate HTML locally.
                    String[] payload = { player.getName(), message };
                    broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.PLAYER_CHAT, payload));
                }
                break;

            // --- CLIENT RECEIVING RAW CHAT ---
            case PLAYER_CHAT:
                // This message comes from the Server to the Client
                String[] data = (String[]) msg.getPayload();
                String senderName = data[0];
                String text = data[1];

                // Client generates HTML using local assets
                handleIncomingChat(senderName, text);
                break;

            case BROADCAST_IMAGE_DATA:
                // OBSOLETE: Old P2P logic is no longer used.
                ui.logMessage("Old BROADCAST_IMAGE_DATA message received and ignored.");
                break;
        }
    }

    // --- FIX: Security & File Integrity Check ---
    private void saveAndDisplayReceivedImage(String senderName, byte[] imageData) {
        try {
            // 1. Magic Byte Check (Security)
            if (imageData.length < 8) return; // Too small

            boolean isGif = (imageData[0] == 'G' && imageData[1] == 'I' && imageData[2] == 'F') &&
                    (imageData[3] == '8' && (imageData[4] == '7' || imageData[4] == '9') && imageData[5] == 'a');

            boolean isPng = (imageData[0] == (byte)0x89 && imageData[1] == 'P' && imageData[2] == 'N' && imageData[3] == 'G');

            if (!isGif && !isPng) {
                ui.logMessage("Security Warning: Received invalid image file from " + senderName + ". Discarded.");
                return;
            }

            // 2. Save to Temp File
            java.io.File tempFile = java.io.File.createTempFile("vop_p2p_", isGif ? ".gif" : ".png");
            tempFile.deleteOnExit();
            java.nio.file.Files.write(tempFile.toPath(), imageData);

            // 3. Display
            Player sender = getPlayerByName(senderName);
            if (sender == null) sender = new Player(senderName, PlayerToken.VAULT_BOY);

            ui.displayChatMessage(formatGifMessage(sender, tempFile.toURI().toString()));

        } catch (Exception e) {
            e.printStackTrace();
            ui.logMessage("Error displaying streamed image from " + senderName);
        }
    }

    public void doCasino() {
        if (isNetworkGame && networkOut != null) { sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_CASINO, null)); return; }
        Player player = players.get(currentPlayerIndex);
        handleCasino(player);
    }

    public void sendCasinoResult(mechanics.CasinoResult result) {
        if (isNetworkGame && networkOut != null) sendNetworkMessage(new NetworkMessage(NetworkMessage.MessageType.RESPONSE_CASINO_RESULT, result));
        else if (!isNetworkGame) { Player player = players.get(currentPlayerIndex); applyCasinoResult(player, result); }
    }

    private void applyCasinoResult(Player player, mechanics.CasinoResult result) {
        if (result == null) return;
        for (Map.Entry<ResourceType, Integer> entry : result.getDeltas().entrySet()) {
            ResourceType type = entry.getKey();
            int delta = entry.getValue();
            if (type == ResourceType.BOTTLECAPS) { if (delta > 0) player.addCaps(delta); else player.payCaps(-delta); }
            else { if (delta > 0) player.addResources(type, delta); else player.consumeResources(type, -delta); }
        }
        ui.logMessage(player.getName() + " " + result.getSummary());
        ui.updatePlayerStats(player);
    }

    private void handleCasino(Player player) {
        if (this.currentCasinoConfig == null) this.currentCasinoConfig = new mechanics.CasinoConfiguration();
        if (baseUI instanceof SwingUI) {
            mechanics.CasinoDialog dialog = new mechanics.CasinoDialog(((SwingUI) baseUI).getGameWindow(), player, this.currentCasinoConfig);
            dialog.setVisible(true);
            mechanics.CasinoResult result = dialog.getResult();
            applyCasinoResult(player, result);
        }
    }

    public void trySaveGame() {
        java.io.File file = SaveManager.chooseFileToSave(baseUI instanceof SwingUI ? ((SwingUI)baseUI).getGameWindow() : null);
        if (file == null) return;
        try {
            SaveManager.saveGame(this, file);
            ui.showNotification("Game saved successfully to: " + file.getName());
        } catch (Exception e) { e.printStackTrace(); ui.showNotification("Save failed: " + e.getMessage()); }
    }

    public void tryLoadGame() {
        java.io.File file = SaveManager.chooseFileToLoad(baseUI instanceof SwingUI ? ((SwingUI)baseUI).getMainMenuWindow() : null);
        if (file == null) return;
        try {
            ui.showGameWindow();
            GameSaveState state = SaveManager.loadGame(file);
            if (state == null) { ui.showNotification("Could not read save file."); return; }
            restoreGameState(state);
        } catch (Exception e) { e.printStackTrace(); ui.showNotification("Load failed: " + e.getMessage()); ui.hideGameWindow(); ui.showMainMenu(); }
    }

    private void restoreGameState(GameSaveState state) {
        this.players = state.players;
        this.currentPlayerIndex = state.currentPlayerIndex;
        this.currentCasinoConfig = state.casinoConfig;
        this.board.setFields(state.fields);
        ui.resetBoard();
        ui.showBoard(board.getFields());
        for (Player p : players) {
            ui.createPlayerToken(p);
            ui.movePlayerToken(p.getToken(), p.getPosition());
            ui.updatePlayerStats(p);
            for (BoardField f : p.getOwnedProperties()) if (f instanceof PropertyField) ui.updatePropertyOwner(f.getPosition(), p);
        }
        if (!players.isEmpty()) { this.self = players.get(0); for(Player p : players) p.setNetworkHandler(null); }
        ui.logMessage("--- GAME LOADED ---");
        startNextTurn();
    }

    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public mechanics.CasinoConfiguration getCasinoConfig() { return currentCasinoConfig; }

    public void playLocalSound(String filename) {
        try {
            java.net.URL url = getClass().getResource("/sounds/" + filename);
            if (url != null) util.WebAudioPlayer.play(url.toString());
            else System.err.println("Sound not found: " + filename);
        } catch (Exception e) { e.printStackTrace(); }
    }

    class NetworkSafeUI implements UIInterface {
        private GameController controller;
        private UIInterface localUI;

        NetworkSafeUI(GameController controller, UIInterface localUI) {
            this.controller = controller;
            this.localUI = localUI;
        }

        public void updateBoardState(List<BoardField> fields) { if (localUI instanceof SwingUI) ((SwingUI) localUI).updateBoardState(fields); }
        @Override public void showNotification(String message) { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.SHOW_NOTIFICATION, message)); localUI.showNotification(message); }
        @Override public void logMessage(String message) { String timedMessage = getTimestamp() + " " + message; if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.LOG_MESSAGE, timedMessage)); localUI.logMessage(timedMessage); }
        @Override public void displayChatMessage(String message) { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.CHAT_MESSAGE, message)); localUI.displayChatMessage(message); }
        @Override public void clearLog() { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.CLEAR_LOG, null)); localUI.clearLog(); }
        @Override public void resetFullLog() { localUI.resetFullLog(); }
        @Override public void showFullLog() { localUI.showFullLog(); }
        @Override public void showMainMenu() { localUI.showMainMenu(); }
        @Override public void showGameWindow() { localUI.showGameWindow(); }
        @Override public void hideGameWindow() { localUI.hideGameWindow(); }
        @Override public void setGameController(GameController controller) { localUI.setGameController(controller); }
        @Override public void show() { localUI.show(); }
        @Override public void showBoard(List<BoardField> fields) { localUI.showBoard(fields); }
        @Override public void createPlayerToken(Player player) { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.ADD_PLAYER_TOKEN, player)); localUI.createPlayerToken(player); }
        @Override public void removePlayerToken(PlayerToken token) {
            if (controller.isNetworkGame) {
                Player playerToRemove = controller.getPlayers().stream().filter(p -> p.getToken() == token).findFirst().orElse(null);
                if (playerToRemove != null) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.REMOVE_PLAYER_TOKEN, playerToRemove));
            }
            localUI.removePlayerToken(token);
        }
        @Override public void movePlayerToken(PlayerToken token, int newPosition) { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.MOVE_PLAYER_TOKEN, new Object[]{token, newPosition})); localUI.movePlayerToken(token, newPosition); }
        @Override public void updatePlayerStats(Player player) { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_PLAYER_STATS, player)); localUI.updatePlayerStats(player); }
        @Override public void updatePropertyOwner(int position, Player owner) {
            if (controller.isNetworkGame) {
                PlayerToken token = (owner != null) ? owner.getToken() : null;
                controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_PROPERTY_OWNER, new Object[]{position, token}));
            }
            localUI.updatePropertyOwner(position, owner);
        }
        @Override public void resetBoard() { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.RESET_UI, null)); localUI.resetBoard(); }
        @Override public void resetStats() { localUI.resetStats(); }
        @Override public void setPlayerTurn(Player player) { if (controller.isNetworkGame) controller.broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.SET_PLAYER_TURN, player)); localUI.setPlayerTurn(player); }
        @Override public void setControlsEnabled(boolean roll, boolean improve, boolean trade) {
            if (controller.isNetworkGame) {
                if (players.isEmpty()) return;
                Player p = players.get(currentPlayerIndex);
                ClientHandler h = getHandlerForPlayer(p);
                if (h != null) h.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SET_CONTROLS, new boolean[]{roll, improve, trade}));
                else localUI.setControlsEnabled(roll, improve, trade);
            } else localUI.setControlsEnabled(roll, improve, trade);
        }
        @Override public String askForString(String prompt) { return localUI.askForString(prompt); }
        @Override public int askForInt(String prompt, int min, int max) { return localUI.askForInt(prompt, min, max); }
        @Override public boolean askForBoolean(String prompt) { return localUI.askForBoolean(prompt); }
        @Override public boolean askForBoolean(Player player, String prompt) {
            if (isNetworkGame) {
                ClientHandler h = getHandlerForPlayer(player);
                if (h != null) { h.sendMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_ACCEPT_TRADE, prompt)); return false; }
            }
            boolean response = localUI.askForBoolean(prompt);
            if (isNetworkGame) completeTradeAcceptance(player, response);
            return response;
        }
        @Override public String askForSelection(String prompt, String[] options) {
            if (players.isEmpty()) return localUI.askForSelection(prompt, options);
            Player p = players.get(currentPlayerIndex);
            if (isNetworkGame) {
                ClientHandler h = getHandlerForPlayer(p);
                if (h != null) { h.sendMessage(new NetworkMessage(NetworkMessage.MessageType.SHOW_SELECTION_DIALOG, new Object[]{prompt, options})); return null; }
            }
            return localUI.askForSelection(prompt, options);
        }
        @Override public TradeOffer askForTradeOffer(Player player, String prompt) {
            if (isNetworkGame) {
                ClientHandler h = getHandlerForPlayer(player);
                if (h != null) { h.sendMessage(new NetworkMessage(NetworkMessage.MessageType.REQUEST_BUILD_OFFER, prompt)); return null; }
            }
            TradeOffer offer = localUI.askForTradeOffer(player, prompt);
            if (isNetworkGame) completeBuildOffer(player, offer);
            return offer;
        }
        @Override public void showFullImage(String imageUrl) { localUI.showFullImage(imageUrl); }
        @Override public void showCasinoDialog(Player player, GameController controller, mechanics.CasinoConfiguration config) { localUI.showCasinoDialog(player, controller, config); }
        @Override public void showRadioWindow() { localUI.showRadioWindow(); }
        @Override public void stopRadio() { localUI.stopRadio(); }
        @Override public void showProgress(String s, int p) { localUI.showProgress(s, p); }
        @Override public void hideProgress() { localUI.hideProgress(); }
        @Override
        public String[] askForTunnelDetails(String prompt) {
            return localUI.askForTunnelDetails(prompt);
        }
    }
}