package board.fields;

import game.GameController;
import players.Player;
import ui.UIInterface;
import util.FieldType;
import util.PropertyType;

import java.io.Serializable;

public class PropertyField extends BoardField implements Serializable {
    private Player owner;
    private int purchaseCost;
    private int[] rentCosts;
    private int currentImprovementLevel;
    private int improvementCost;
    private PropertyType propertyType;
    private String groupId;
    private String imageUrl;
    private boolean isMortgaged = false;

    public PropertyField(String name, String description, int position, int purchaseCost, int[] rentCosts, int improvementCost, PropertyType propertyType, String groupId, String imageUrl) {
        super(name, description, position, FieldType.PROPERTY);
        this.purchaseCost = purchaseCost;
        this.rentCosts = rentCosts;
        this.improvementCost = improvementCost;
        this.propertyType = propertyType;
        this.groupId = groupId;
        this.imageUrl = imageUrl;
        this.owner = null;
        this.currentImprovementLevel = 0;
    }

    @Override
    public boolean onLand(Player player, GameController controller) {
        UIInterface ui = controller.getUi();

        if (owner == null) {
            if (player.canAfford(purchaseCost)) {
                controller.requestPropertyPurchase(player, this);
                return false; // Wait for purchase decision
            } else {
                ui.logMessage("You cannot afford to buy " + name + ".");
                return true;
            }
        } else if (owner != player) {
            int baseRent = getRent();
            int charisma = player.getSpecial().getCharisma();

            // Formula: 2.5% discount per point of Charisma
            double discountPercent = (charisma * 0.025);
            int discountAmount = (int) (baseRent * discountPercent);
            int rentToPay = Math.max(0, baseRent - discountAmount);

            if (discountAmount > 0) {
                ui.logMessage(player.getName() + " uses Charisma (" + charisma + ") to haggle rent down by " + discountAmount + " caps.");
            }
            ui.logMessage(player.getName() + " pays " + rentToPay + " caps in rent to " + owner.getName() + ".");

            player.payCaps(rentToPay);
            owner.addCaps(rentToPay);

            if (controller.isNetworkGame) {
                controller.getUi().updatePlayerStats(owner);
            }
            return true;
        } else {
            ui.logMessage("You admire your handiwork at " + name + ".");
            return true;
        }
    }

    public boolean isMortgaged() { return isMortgaged; }

    public void setMortgaged(boolean mortgaged) {
        this.isMortgaged = mortgaged;
    }

    public int getRent() {
        if (isMortgaged) {
            return 0;
        }
        if (currentImprovementLevel < rentCosts.length) {
            return rentCosts[currentImprovementLevel];
        }
        return rentCosts[rentCosts.length - 1];
    }

    // --- Getters and Setters ---
    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    public int getPurchaseCost() {
        return purchaseCost;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public String getGroupId() {
        return groupId;
    }

    public int getImprovementCost() {
        return improvementCost;
    }

    public int getCurrentImprovementLevel() {
        return currentImprovementLevel;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void incrementImprovementLevel() {
        if (currentImprovementLevel < mechanics.PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
            this.currentImprovementLevel++;
        }
    }
}