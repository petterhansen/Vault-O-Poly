package board.fields;

import game.GameController;
import players.Player;
import resources.ResourceType;
import ui.UIInterface;
import util.FieldType;

import java.io.Serializable;

public class ResourceField extends BoardField implements Serializable {
    private ResourceType resourceType;
    private int productionRate;

    public ResourceField(String name, String description, int position, ResourceType resourceType, int productionRate) {
        super(name, description, position, FieldType.RESOURCE);
        this.resourceType = resourceType;
        this.productionRate = productionRate;
    }

    @Override
    public boolean onLand(Player player, GameController controller) {
        UIInterface ui = controller.getUi();
        ui.logMessage("You scavenge " + name + "...");

        player.addResources(resourceType, productionRate);
        ui.logMessage("...and find " + productionRate + " " + resourceType.name() + "!");

        return true; // Turn is over
    }
}