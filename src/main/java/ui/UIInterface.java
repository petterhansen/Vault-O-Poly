package ui;

import board.fields.BoardField;
import game.GameController;
import mechanics.TradeOffer;
import players.Player;
import util.PlayerToken;
import java.util.List;

public interface UIInterface {
    void showNotification(String message);
    void logMessage(String message);
    void displayChatMessage(String message);
    String askForString(String prompt);
    int askForInt(String prompt, int min, int max);
    boolean askForBoolean(String prompt);
    boolean askForBoolean(Player player, String prompt);
    TradeOffer askForTradeOffer(Player player, String prompt);

    // --- NEW: Ask for Tunnel Details ---
    /**
     * Asks the user for the external Tunnel IP/Domain and the external Game Port.
     * Returns: [0: Tunnel IP/Domain (String), 1: External Game Port (String)] or null if canceled.
     */
    String[] askForTunnelDetails(String prompt);

    void clearLog();
    void resetFullLog();
    void showFullLog();
    void showMainMenu();
    void showGameWindow();
    void hideGameWindow();

    void setGameController(game.GameController controller);
    void show();

    void showBoard(List<BoardField> fields);
    void createPlayerToken(Player player);
    void removePlayerToken(PlayerToken token);
    void movePlayerToken(PlayerToken token, int newPosition);
    void updatePlayerStats(Player player);
    void updatePropertyOwner(int position, Player owner);

    void resetBoard();
    void resetStats();

    void setPlayerTurn(Player player);
    void setControlsEnabled(boolean roll, boolean improve, boolean trade);
    String askForSelection(String prompt, String[] options);
    void showFullImage(String imageUrl);
    void showCasinoDialog(Player player, GameController controller, mechanics.CasinoConfiguration config);
    void updateBoardState(List<BoardField> newFields);
    void showRadioWindow();
    void stopRadio();
    void showProgress(String status, int percent);
    void hideProgress();
}