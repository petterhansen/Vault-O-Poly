package board.fields;

import game.GameController;
import players.Player;
import util.FieldType;
import java.io.Serializable;

public abstract class BoardField implements Serializable {
    protected String name;
    protected String description;
    protected int position;
    protected FieldType type;

    public BoardField(String name, String description, int position, FieldType type) {
        this.name = name;
        this.description = description;
        this.position = position;
        this.type = type;
    }

    // Player lands on this field
    // UPDATED: Now returns 'true' if the turn is over,
    // 'false' if we are waiting for player input.
    public abstract boolean onLand(Player player, GameController controller);

    // Player passes this field (default: do nothing)
    public void onPass(Player player) {
        // Default implementation does nothing
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPosition() {
        return position;
    }

    public FieldType getType() {
        return type;
    }
}