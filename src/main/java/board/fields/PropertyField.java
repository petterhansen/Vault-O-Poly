package board.fields;

import game.GameController;
import players.Player;
import resources.ResourceType;
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
    private String imageUrl; // <-- ADDED: Field to store the URL

    // The constructor is updated to accept the groupId AND imageUrl
    public PropertyField(String name, String description, int position, int purchaseCost, int[] rentCosts, int improvementCost, PropertyType propertyType, String groupId, String imageUrl) { // <-- MODIFIED
        super(name, description, position, FieldType.PROPERTY);
        this.purchaseCost = purchaseCost;
        this.rentCosts = rentCosts;
        this.improvementCost = improvementCost;
        this.propertyType = propertyType;
        this.groupId = groupId;
        this.imageUrl = imageUrl; // <-- ASSIGN NEW FIELD
        this.owner = null;
        this.currentImprovementLevel = 0; // Index 0 is base rent
    }

    @Override
    public boolean onLand(Player player, GameController controller) {
        UIInterface ui = controller.getUi();

        if (owner == null) {
            // Offer purchase
            if (player.canAfford(purchaseCost)) {
                // REFACTORED:
                // Instead of asking the UI, tell the controller to ask the player.
                controller.requestPropertyPurchase(player, this);
                // --- THIS IS THE FIX ---
                // Tell the game loop to NOT end the turn.
                // The turn will be ended in "completePropertyPurchase"
                return false;
            } else {
                ui.logMessage("You cannot afford to buy " + name + ".");
                return true; // Turn is over.
            }
        } else if (owner != player) {
            int baseRent = getRent();
            int charisma = player.getSpecial().getCharisma();

            // Formula: 2.5% discount per point of Charisma
            // CHA 1: 2.5% discount | CHA 10: 25% discount
            double discountPercent = (charisma * 0.025);
            int discountAmount = (int) (baseRent * discountPercent);
            int rentToPay = Math.max(0, baseRent - discountAmount);

            // Log the interaction
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
            // Landed on own property
            ui.logMessage("You admire your handiwork at " + name + ".");
            return true;
        }
    }

    private boolean isMortgaged = false;

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
        // Return max rent if something is wrong
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

    // --- ADD THIS GETTER ---
    public String getGroupId() {
        return groupId;
    }

    public int getImprovementCost() {
        return improvementCost;
    }

    public int getCurrentImprovementLevel() {
        return currentImprovementLevel;
    }

    // --- ADDED GETTER ---
    public String getImageUrl() {
        return imageUrl;
    }

    public void incrementImprovementLevel() {
        // Use the constant from PropertyDevelopment
        if (currentImprovementLevel < mechanics.PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
            this.currentImprovementLevel++;
        }
    }

    // These methods are stubs for a more complex resource system.
    // They are "complete" as stubs.
    public boolean hasResourceProduction() {
        return false;
    }

    public ResourceType getProducedResource() {
        return null;
    }

    public int getProductionRate() {
        return 0;
    }
}