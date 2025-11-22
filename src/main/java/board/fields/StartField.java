package board.fields;

import game.GameController;
import players.Player;
import util.FieldType;

import java.io.Serializable;

public class StartField extends BoardField implements Serializable {
    private int passReward;

    public StartField(String name, String description, int position, int passReward) {
        super(name, description, position, FieldType.START);
        this.passReward = passReward;
    }

    @Override
    public boolean onLand(Player player, GameController controller) {
        // Standard Monopoly: You get nothing for landing on Go
        controller.getUi().logMessage("You're back where you started. Home sweet vault.");
        return true; // Turn is over
    }

    @Override
    public void onPass(Player player) {
        player.addCaps(this.passReward);
    }

    public int getPassReward() {
        return passReward;
    }
}