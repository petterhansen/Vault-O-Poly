package board.fields;

import game.GameController;
import players.Player;
import ui.SwingUI;
import ui.UIInterface;
import util.FieldType;
import util.SpecialEffect;

import java.io.Serializable;

public class SpecialField extends BoardField implements Serializable {
    private SpecialEffect effect;
    private int effectValue; // Generic value for effect (caps, position, etc.)

    public SpecialField(String name, String description, int position, SpecialEffect effect, int effectValue) {
        super(name, description, position, FieldType.SPECIAL);
        this.effect = effect;
        this.effectValue = effectValue;
    }

    @Override
    public boolean onLand(Player player, GameController controller) {
        UIInterface ui = controller.getUi();

        switch (effect) {
            case LOSE_CAPS:
                ui.showNotification("Bad luck! You lose " + effectValue + " caps.");
                player.payCaps(effectValue);
                break;
            case GAIN_CAPS:
                ui.showNotification("Your luck is in! You find " + effectValue + " caps.");
                player.addCaps(effectValue);
                break;
            case GO_TO_FIELD:
                ui.showNotification("You are sent to " + controller.getBoard().getField(effectValue).getName() + "!");
                player.setPosition(effectValue);
                ui.movePlayerToken(player.getToken(), position);

                if (effectValue == 10) { // Position 10 is Jail
                    player.setInJail(true);
                    ui.showNotification("You are now in jail!");
                }
                break;
            case DRAW_EVENT:
                // This is a special case. The event card itself might
                // land the player on another square, which could be an
                // unowned property. We must respect its return value.
                return controller.getEventDeck().drawEvent(player, controller);
            case NOTHING:
                if (this.position == 10) {
                    ui.logMessage("You're just visiting the jail.");
                } else {
                    ui.logMessage("You rest for a moment. Nothing happens.");
                }
                break;
        }
        return true; // Turn is over for all cases except DRAW_EVENT
    }
}
