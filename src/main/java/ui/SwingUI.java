package ui;

import game.GameController;
import mechanics.TradeOffer;
import mechanics.TradeOfferDialog;
import players.Player;
import util.PlayerToken;
import board.fields.BoardField;
import board.fields.*;

import javax.swing.*;
import java.awt.Frame;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.awt.*;
import java.net.URL;

public class SwingUI implements UIInterface {

    private GameController controller;
    private MainMenuWindow mainMenuWindow;
    private GameWindow gameWindow;
    private FullLogWindow fullLogWindow;
    private StringBuffer fullLogText;
    private RadioWindow radioWindow;

    public SwingUI() {
        this.mainMenuWindow = new MainMenuWindow();
        this.fullLogText = new StringBuffer();
        redirectSystemStreams();
    }

    // FIX: Implement the new tunnel details dialog
    @Override
    public String[] askForTunnelDetails(String prompt) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        JTextField ipField = new JTextField(20);
        ipField.setText("147.185.221.180"); // Example placeholder

        JTextField portField = new JTextField(10);
        portField.setText("39761"); // Example placeholder

        panel.add(new JLabel("External Tunnel IP/Domain:"));
        panel.add(ipField);
        panel.add(new JLabel("External Game Port (e.g., 39761):"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(
                gameWindow, panel, prompt,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String ip = ipField.getText().trim();
            String portStr = portField.getText().trim();

            if (ip.isEmpty() || portStr.isEmpty()) {
                JOptionPane.showMessageDialog(gameWindow, "IP and Port cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            try {
                // Validate port is numeric
                Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(gameWindow, "Port must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            return new String[]{ip, portStr};
        }
        return null;
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateLog(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateLog(new String(b, off, len));
            }

            private void updateLog(String text) {
                synchronized (fullLogText) {
                    fullLogText.append(text);

                    // --- FIX: Unbounded Memory Growth ---
                    // Keep only the last ~50,000 characters to prevent OutOfMemoryError
                    if (fullLogText.length() > 50000) {
                        fullLogText.delete(0, fullLogText.length() - 50000);
                    }
                }

                if (fullLogWindow != null && fullLogWindow.isVisible()) {
                    SwingUtilities.invokeLater(() -> fullLogWindow.appendRaw(text));
                }
            }
        };

        PrintStream ps = new PrintStream(out, true);
        System.setOut(ps);
        System.setErr(ps);
    }

    @Override
    public void setGameController(GameController controller) {
        this.controller = controller;
        if (mainMenuWindow != null) {
            mainMenuWindow.setGameController(controller);
        }
        if (gameWindow != null) {
            gameWindow.getControlPanel().setGameController(controller);
            gameWindow.getChatPanel().setGameController(controller);
        }
    }

    @Override
    public void showMainMenu() {
        if (mainMenuWindow == null) {
            mainMenuWindow = new MainMenuWindow();
            mainMenuWindow.setGameController(controller);
        }
        mainMenuWindow.setVisible(true);
    }

    @Override
    public void showGameWindow() {
        if (gameWindow == null) {
            gameWindow = new GameWindow();
            if (controller != null) {
                gameWindow.getControlPanel().setGameController(controller);
                gameWindow.getChatPanel().setGameController(controller);
            }
        }
        gameWindow.setVisible(true);
    }

    @Override
    public void hideGameWindow() {
        if (gameWindow != null) {
            gameWindow.setVisible(false);
        }
    }

    @Override
    public void show() {
        showMainMenu();
    }

    @Override
    public void showNotification(String message) {
        JOptionPane.showMessageDialog(gameWindow, message);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    public void logMessage(String message) {
        // Format game logs for the Chat Panel (Rich Text)
        String formattedLog = "<p class=\"log\">" + escapeHtml(message) + "</p>";

        if (gameWindow != null) {
            gameWindow.getChatPanel().appendMessage(formattedLog);
        }

        // Note: We DON'T append to fullLogText here anymore because
        // logMessage usually comes from GameController which might ALSO print to System.out.
        // However, if GameController calls ui.logMessage() ONLY (and not System.out),
        // we should append it.
        // To be safe and avoid duplicates, we can explicitly append it here:

        System.out.println("[GAME] " + message);
        // ^ By printing to System.out, our Redirector above catches it
        // and adds it to fullLogText/FullLogWindow automatically!
    }

    @Override
    public void displayChatMessage(String htmlMessage) {
        if (gameWindow != null) {
            gameWindow.getChatPanel().appendMessage(htmlMessage);
        }
    }

    @Override
    public void clearLog() {
        if (gameWindow != null) {
            gameWindow.getChatPanel().clearChat();
        }
    }

    @Override
    public void resetFullLog() {
        fullLogText.setLength(0);
        if (fullLogWindow != null) {
            fullLogWindow.clearLog();
        }
    }

    @Override
    public void showFullLog() {
        if (fullLogWindow == null) {
            fullLogWindow = new FullLogWindow(gameWindow);
        }
        fullLogWindow.setText(fullLogText.toString());
        fullLogWindow.setVisible(true);
    }

    @Override
    public void showBoard(List<BoardField> fields) {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().initializeBoard(fields);
        }
    }

    @Override
    public void createPlayerToken(Player player) {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().addPlayerToken(player.getToken());
            gameWindow.getInfoPanel().addPlayer(player);
        }
    }

    @Override
    public void removePlayerToken(PlayerToken token) {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().removePlayerToken(token);
            gameWindow.getInfoPanel().removePlayer(token);
        }
    }

    @Override
    public void movePlayerToken(PlayerToken token, int newPosition) {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().movePlayerToken(token, newPosition);
        }
    }

    @Override
    public void updatePlayerStats(Player player) {
        if (gameWindow != null) {
            gameWindow.getInfoPanel().updatePlayerStats(player);
        }
    }

    @Override
    public void updatePropertyOwner(int position, Player owner) {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().updateFieldOwner(position, owner);
            gameWindow.getInfoPanel().updatePropertyLists();
        }
    }

    @Override
    public void resetBoard() {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().resetBoard();
        }
    }

    @Override
    public void resetStats() {
        if (gameWindow != null) {
            gameWindow.getInfoPanel().resetStats();
        }
    }

    @Override
    public void setPlayerTurn(Player player) {
        if (gameWindow != null) {
            gameWindow.getInfoPanel().setTurn(player);
        }
    }

    @Override
    public void setControlsEnabled(boolean roll, boolean improve, boolean trade) {
        if (gameWindow != null) {
            gameWindow.getControlPanel().setButtonsEnabled(roll, improve, trade);
        }
    }

    @Override
    public String askForString(String prompt) {
        return JOptionPane.showInputDialog(gameWindow, prompt);
    }

    @Override
    public int askForInt(String prompt, int min, int max) {
        while (true) {
            try {
                String result = JOptionPane.showInputDialog(gameWindow, prompt);
                if (result == null) return min;
                int value = Integer.parseInt(result);
                if (value >= min && value <= max) {
                    return value;
                }
                showNotification("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                showNotification("Invalid input. Please enter a number.");
            }
        }
    }

    @Override
    public boolean askForBoolean(String prompt) {
        int result = JOptionPane.showConfirmDialog(
                gameWindow, prompt, "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean askForBoolean(Player player, String prompt) {
        return askForBoolean(prompt);
    }

    @Override
    public String askForSelection(String prompt, String[] options) {
        if (options == null || options.length == 0) {
            return null;
        }
        return (String) JOptionPane.showInputDialog(
                gameWindow, prompt, "Make a Selection",
                JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]
        );
    }

    @Override
    public TradeOffer askForTradeOffer(Player player, String prompt) {
        TradeOfferDialog dialog = new TradeOfferDialog(gameWindow, player, prompt);
        return dialog.getOffer();
    }

    public MainMenuWindow getMainMenuWindow() {
        return mainMenuWindow;
    }

    public Frame getGameWindow() {
        return gameWindow;
    }

    public void handleClientDisconnect() {
        if (controller != null) {
            // Logic handled by controller
        }
        if (mainMenuWindow != null) {
            mainMenuWindow.disconnectFromServer();
        }
        hideGameWindow();
        showMainMenu();
        showNotification("You have been disconnected from the server.");
    }

    @Override
    public void showFullImage(String imageUrl) {
        SwingUtilities.invokeLater(() -> {
            try {
                ImageIcon icon = new ImageIcon(new URL(imageUrl));
                JLabel imageLabel = new JLabel(icon);
                JDialog imageDialog = new JDialog(gameWindow, "Full Image View", true);
                imageDialog.getContentPane().setLayout(new BorderLayout());
                JScrollPane scrollPane = new JScrollPane(imageLabel);
                imageDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int maxWidth = (int) (screenSize.getWidth() * 0.8);
                int maxHeight = (int) (screenSize.getHeight() * 0.8);
                int width = Math.min(icon.getIconWidth() + 20, maxWidth);
                int height = Math.min(icon.getIconHeight() + 20, maxHeight);

                imageDialog.setSize(width, height);
                imageDialog.setLocationRelativeTo(gameWindow);
                imageDialog.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(gameWindow, "Could not load image: " + imageUrl, "Image Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void showCasinoDialog(Player player, GameController controller, mechanics.CasinoConfiguration config) {
        mechanics.CasinoDialog dialog = new mechanics.CasinoDialog(gameWindow, player, config);
        dialog.setVisible(true);
        mechanics.CasinoResult result = dialog.getResult();
        controller.sendCasinoResult(result);
    }

    public void updateBoardState(List<BoardField> fields) {
        if (gameWindow != null) {
            gameWindow.getBoardPanel().updateFieldStates(fields);
        }
    }

    @Override
    public void showRadioWindow() {
        if (gameWindow == null) return;
        if (radioWindow == null) {
            radioWindow = new RadioWindow(gameWindow);
        }
        if (!radioWindow.isVisible()) {
            radioWindow.setVisible(true);
        }
        radioWindow.toFront();
    }

    @Override
    public void stopRadio() {
        if (radioWindow != null) {
            radioWindow.stopRadio();
        }
    }

    @Override
    public void showProgress(String status, int percent) {
        if (gameWindow != null && gameWindow.getChatPanel() != null) {
            gameWindow.getChatPanel().showProgress(status, percent);
        }
    }

    @Override
    public void hideProgress() {
        if (gameWindow != null && gameWindow.getChatPanel() != null) {
            gameWindow.getChatPanel().hideProgress();
        }
    }
}