package mechanics;

import game.GameController;
import players.Player;
import resources.ResourceType;
import ui.UIInterface;
import board.fields.PropertyField;
import board.Board;
import java.util.Map;

/**
 * This class now only contains the final execution logic for a trade.
 * The asynchronous UI logic is handled in GameController.
 */
public class TradeSystem {

    GameController controller;

    public boolean executeTrade(Player p1, TradeOffer offer1, Player p2, TradeOffer offer2, UIInterface ui, Board board) {

        System.out.println("\n[TradeSystem] Attempting to execute trade...");
        System.out.println("[TradeSystem]   > " + p1.getName() + " offers: " + offer1.getSummary().replace("\n", ""));
        System.out.println("[TradeSystem]   > " + p2.getName() + " offers: " + offer2.getSummary().replace("\n", ""));

        // --- CHECK 1: Check Property Eligibility (Not Mortgaged/Improved) ---
        // For simplicity, we enforce no trade of improved/mortgaged properties.

        // P1 Check (properties they GIVE AWAY)
        for (PropertyField prop : offer1.properties) {
            if (prop.getCurrentImprovementLevel() > 0 || prop.isMortgaged()) {
                ui.showNotification(p1.getName() + " attempted to trade an improved or mortgaged property (" + prop.getName() + "). Trade failed.");
                return false;
            }
        }
        // P2 Check (properties they GIVE AWAY)
        for (PropertyField prop : offer2.properties) {
            if (prop.getCurrentImprovementLevel() > 0 || prop.isMortgaged()) {
                ui.showNotification(p2.getName() + " attempted to trade an improved or mortgaged property (" + prop.getName() + "). Trade failed.");
                return false;
            }
        }

        // --- CHECK 2: Check Affordability (Caps/Resources) ---
        // (Existing logic remains here: check canAfford, check Resources)
        // ...

        System.out.println("[TradeSystem] Checking affordability...");
        if (!p1.canAfford(offer1.caps)) { ui.showNotification("A player can no longer afford the caps."); System.out.println("[TradeSystem] FAILED: " + p1.getName() + " cannot afford caps."); return false; }
        if (!p2.canAfford(offer2.caps)) { ui.showNotification("A player can no longer afford the caps."); System.out.println("[TradeSystem] FAILED: " + p2.getName() + " cannot afford caps."); return false; }
        // Check P1 resources
        for (Map.Entry<ResourceType, Integer> entry : offer1.resources.entrySet()) {
            if (p1.getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) { ui.logMessage(p1.getName() + " no longer has enough " + entry.getKey().name()); System.out.println("[TradeSystem] FAILED: " + p1.getName() + " missing " + entry.getKey().name()); return false; }
        }
        // Check P2 resources
        for (Map.Entry<ResourceType, Integer> entry : offer2.resources.entrySet()) {
            if (p2.getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) { ui.logMessage(p2.getName() + " no longer has enough " + entry.getKey().name()); System.out.println("[TradeSystem] FAILED: " + p2.getName() + " missing " + entry.getKey().name()); return false; }
        }

        System.out.println("[TradeSystem] All checks passed. Executing trade...");

        // 1. Execute trade (Caps and Resources)
        p1.payCaps(offer1.caps); p2.addCaps(offer1.caps);
        p2.payCaps(offer2.caps); p1.addCaps(offer2.caps);

        for (Map.Entry<ResourceType, Integer> entry : offer1.resources.entrySet()) { if (entry.getValue() > 0) { p1.consumeResources(entry.getKey(), entry.getValue()); p2.addResources(entry.getKey(), entry.getValue()); } }
        for (Map.Entry<ResourceType, Integer> entry : offer2.resources.entrySet()) { if (entry.getValue() > 0) { p2.consumeResources(entry.getKey(), entry.getValue()); p1.addResources(entry.getKey(), entry.getValue()); } }

        // --- 2. Execute Properties (FIXED) ---
        transferProperties(p1, p2, offer1, board, ui);
        transferProperties(p2, p1, offer2, board, ui);

        System.out.println("[TradeSystem] Trade complete.\n");
        return true;
    }

    private void transferProperties(Player from, Player to, TradeOffer offer, Board board, UIInterface ui) {
        for (PropertyField offerProp : offer.properties) {
            // --- THE FIX ---
            // 1. Find the REAL property on the board using the ID/Position from the offer
            PropertyField realProp = (PropertyField) board.getField(offerProp.getPosition());

            // 2. Update the REAL property
            from.removeProperty(realProp);
            to.addProperty(realProp);
            realProp.setOwner(to);

            // 3. Update Visuals
            ui.updatePropertyOwner(realProp.getPosition(), to);
            ui.logMessage(from.getName() + " traded " + realProp.getName() + " to " + to.getName());
        }
    }
}