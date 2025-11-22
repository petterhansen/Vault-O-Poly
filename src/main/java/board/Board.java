package board;

import config.BoardLoader;
import players.Player;
import resources.ResourceType;
import ui.UIInterface;
import board.fields.BoardField;
import board.fields.StartField;

import java.util.List;
import java.util.Map;

public class Board {
    private List<BoardField> fields;
    private int startFieldPosition;
    private Map<ResourceType, Integer> resourcePool; // Not used yet, but in your design
    private UIInterface ui;

    public Board(String boardLayoutFile, UIInterface ui) {
        this.ui = ui;
        try {
            this.fields = BoardLoader.loadBoard(boardLayoutFile);
            // Find the start field
            for (BoardField field : fields) {
                if (field instanceof StartField) {
                    this.startFieldPosition = field.getPosition();
                    break;
                }
            }
            ui.showNotification("Successfully loaded board: " + boardLayoutFile);
        } catch (Exception e) {
            ui.showNotification("FATAL: Could not load board layout file!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public BoardField getField(int position) {
        if (position >= 0 && position < fields.size()) {
            return fields.get(position);
        }
        return null; // Should not happen
    }

    public List<BoardField> getFields() {
        return this.fields;
    }

    public void movePlayer(Player player, int steps) {
        int currentPosition = player.getPosition();
        int newPosition = (currentPosition + steps) % fields.size();

        // Check for passing start
        if (newPosition < currentPosition) {
            BoardField startField = getField(startFieldPosition);
            if (startField instanceof StartField) {
                startField.onPass(player);
                ui.logMessage(player.getName() + " passed " + startField.getName() + " and collected " + ((StartField) startField).getPassReward() + " caps!");
            }
        }

        player.setPosition(newPosition);
    }

    public void setFields(java.util.List<BoardField> fields) {
        this.fields = fields;
    }

    public int getBoardSize() {
        return fields.size();
    }
}