package mechanics;

import board.Board;
import board.fields.BoardField;
import board.fields.PropertyField;
import players.Player;
import resources.ResourceType;
import ui.UIInterface;
import util.PropertyType;

public class PropertyDevelopment {

    public static final int MAX_IMPROVEMENT_LEVEL = 3; // Corresponds to rentCosts[3]
    public static final int RESOURCE_COST_PER_LEVEL = 2; // Example: 2 SCRAP_MATERIAL

    /**
     * Checks if a player *can* improve a given property.
     * MODIFIED: Now takes the 'board' as a parameter to check for group ownership.
     */
    public boolean canImprove(Player player, PropertyField property, UIInterface ui, Board board) {
        // Check 1: Is it their property?
        if (property.getOwner() != player) {
            ui.logMessage("You don't own " + property.getName() + ".");
            return false;
        }

        // We can use the groupId for this. "radio" and "utility" are not improvable.
        String groupId = property.getGroupId();
        if (groupId == null || groupId.equals("utility")) {
            ui.logMessage(property.getName() + " cannot be improved.");
            return false;
        }// Check if the player owns all properties in this group (matching groupId)
        for (BoardField field : board.getFields()) {
            if (field instanceof PropertyField) {
                PropertyField propInGroup = (PropertyField) field;
                // Check if it's in the same group but not owned by the player
                if (groupId.equals(propInGroup.getGroupId()) && propInGroup.getOwner() != player) {
                    ui.logMessage("You must own all properties in the " + groupId + " group to improve this.");
                    return false;
                }
            }
        }

        // Check 2: Is it already maxed out?
        if (property.getCurrentImprovementLevel() >= MAX_IMPROVEMENT_LEVEL) {
            ui.logMessage(property.getName() + " is already a max-level fortress!");
            return false;
        }

        // Check 3: Can they afford the caps?
        if (!player.canAfford(property.getImprovementCost())) {
            ui.logMessage("You don't have enough caps to improve " + property.getName() + ".");
            return false;
        }

        // Check 4: Can they afford the resources?
        int requiredScrap = RESOURCE_COST_PER_LEVEL * (property.getCurrentImprovementLevel() + 1);
        if (player.getResources().getOrDefault(ResourceType.SCRAP_MATERIAL, 0) < requiredScrap) {
            ui.logMessage("You need " + requiredScrap + " SCRAP_MATERIAL to improve " + property.getName() + ".");
            return false;
        }

        return true;
    }

    /**
     * Executes the improvement, consuming resources and caps.
     * Assumes canImprove() has already been checked.
     */
    public void improveProperty(Player player, PropertyField property, UIInterface ui) {
        int requiredScrap = RESOURCE_COST_PER_LEVEL * (property.getCurrentImprovementLevel() + 1);

        // Consume resources
        player.payCaps(property.getImprovementCost());
        player.consumeResources(ResourceType.SCRAP_MATERIAL, requiredScrap);

        // Improve the property
        property.incrementImprovementLevel();

        ui.logMessage(player.getName() + " improved " + property.getName() + "!");
        ui.logMessage("It's now at improvement level " + property.getCurrentImprovementLevel() + ".");
    }
}