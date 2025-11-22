package game.network;

import java.io.Serializable;
import java.util.List;
import board.fields.BoardField;
import players.Player;
import util.PlayerToken;

public class NetworkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum MessageType {
        // Client-to-Server Requests
        REQUEST_ROLL,
        REQUEST_IMPROVE,
        REQUEST_TRADE,
        REQUEST_CHAT_MESSAGE,
        CLIENT_INFO,
        REQUEST_CASINO,

        // Client-to-Server RESPONSES (from UI dialogs)
        RESPONSE_BUY_PROPERTY,
        RESPONSE_JAIL_ACTION,
        RESPONSE_IMPROVE_SELECTION,
        RESPONSE_TRADE_SELECTION,
        RESPONSE_BUILD_OFFER,
        RESPONSE_ACCEPT_TRADE,
        RESPONSE_CASINO_RESULT,

        // Server-to-Client Updates (UI Commands)
        LOG_MESSAGE,
        CHAT_MESSAGE,
        SHOW_NOTIFICATION,
        SET_CONTROLS,
        SET_PLAYER_TURN,
        UPDATE_PLAYER_STATS,
        MOVE_PLAYER_TOKEN,
        INITIALIZE_BOARD,
        ADD_PLAYER_TOKEN,
        REMOVE_PLAYER_TOKEN,
        UPDATE_PROPERTY_OWNER,
        RESET_UI,
        CLEAR_LOG,
        SHOW_CASINO_DIALOG,

        // Server-to-Client REQUESTS (UI dialogs)
        SHOW_BOOLEAN_DIALOG,
        SHOW_SELECTION_DIALOG,
        REQUEST_BUILD_OFFER,
        REQUEST_ACCEPT_TRADE,

        // State & Connection
        CLIENT_CONNECTED_INFO,
        START_GAME,
        GAME_OVER,
        SET_CLIENT_PLAYER,
        SYNC_BOARD_STATE,

        // --- NEW: P2P Streaming ---
        BROADCAST_IMAGE_DATA,

        // --- NEW: Raw Chat Broadcast (Fixes Token Images) ---
        PLAYER_CHAT
    }

    private final MessageType type;
    private final Object payload;

    public NetworkMessage(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
}