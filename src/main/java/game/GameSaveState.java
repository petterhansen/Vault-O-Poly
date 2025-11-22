package game;

import board.fields.BoardField;
import mechanics.CasinoConfiguration;
import players.Player;
import java.io.Serializable;
import java.util.List;

public class GameSaveState implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Player> players;
    public List<BoardField> fields; // Saves ownership, houses, etc.
    public int currentPlayerIndex;
    public CasinoConfiguration casinoConfig;

    public GameSaveState(List<Player> players, List<BoardField> fields, int currentPlayerIndex, CasinoConfiguration casinoConfig) {
        this.players = players;
        this.fields = fields;
        this.currentPlayerIndex = currentPlayerIndex;
        this.casinoConfig = casinoConfig;
    }
}