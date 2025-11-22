package mechanics;

import board.fields.BoardField;
import board.fields.PropertyField;
import game.GameController;
import players.Player;
import ui.UIInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EventDeck {

    // Defines the two types of cards
    private enum CardType {
        STANDARD,  // Used and immediately discarded
        KEEPABLE   // Player holds this card (e.g., Get Out of Jail)
    }

    // Inner class to define a card
    private static class EventCard {
        final String description;
        final CardType type;
        final CardEffect effect; // Use a functional interface for the effect

        EventCard(String description, CardType type, CardEffect effect) {
            this.description = description;
            this.type = type;
            this.effect = effect;
        }
    }

    private List<EventCard> deck;
    private List<EventCard> discardPile;

    public EventDeck() {
        this.deck = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        initializeDeck();
        shuffleDeck();
    }

    /**
     * Fills the deck with all the event cards.
     */
    private void initializeDeck() {
        deck.clear();
        discardPile.clear();

        // --- GAIN CAPS ---
        deck.add(new EventCard("You find a pre-war stash! Gain 100 caps.", CardType.STANDARD,
                (p, c) -> { p.addCaps(100); return true; } // Return true
        ));
        // ... (Add 'return true;' to all simple lambda effects) ...
        deck.add(new EventCard("Brahmin caravan pays you for protection. Collect 50 caps.", CardType.STANDARD,
                (p, c) -> { p.addCaps(50); return true; }
        ));
        deck.add(new EventCard("You win the Megaton lottery! Collect 200 caps.", CardType.STANDARD,
                (p, c) -> { p.addCaps(200); return true; }
        ));

        // --- LOSE CAPS ---
        deck.add(new EventCard("A Deathclaw raids your supplies. Lose 150 caps.", CardType.STANDARD,
                (p, c) -> { p.payCaps(150); return true; }
        ));
        deck.add(new EventCard("You're bitten by a Radroach. Pay 25 caps for a Stimpak.", CardType.STANDARD,
                (p, c) -> { p.payCaps(25); return true; }
        ));

        // --- MOVE TO FIELD ---
        deck.add(new EventCard("Advance to GO! (Collect 200 Caps)", CardType.STANDARD,
                (p, c) -> {
                    p.setPosition(0);

                    c.getBoard().getField(0).onPass(p); // Manually trigger pass reward
                    // Now call onLand for the GO square
                    return c.getBoard().getField(0).onLand(p, c);
                }
        ));
        deck.add(new EventCard("Go to Jail! Do not pass GO, do not collect 200 caps.", CardType.STANDARD,
                (p, c) -> {
                    p.setPosition(10); // Position 10 is Jail

                    p.setInJail(true); // SET JAIL STATE
                    // Do NOT call onLand for jail, just end the turn
                    return true;
                }
        ));
        deck.add(new EventCard("A friendly caravan takes you to The Strip. Advance to The Strip.", CardType.STANDARD,
                (p, c) -> {
                    int newPos = 16; // Position 16 is The Strip
                    if (p.getPosition() > newPos) {
                        c.getUi().logMessage("You pass GO and collect 200 caps.");
                        c.getBoard().getField(0).onPass(p); // Manually trigger pass reward
                    }
                    p.setPosition(newPos);
                    
                    // --- THIS IS THE FIX ---
                    // Return the result of the new field's onLand method
                    return c.getBoard().getField(newPos).onLand(p, c);
                }
        ));
        deck.add(new EventCard("You find an NCR map. Advance to Shady Sands.", CardType.STANDARD,
                (p, c) -> {
                    int newPos = 11; // Position 11 is Shady Sands
                    if (p.getPosition() > newPos) {
                        c.getUi().logMessage("You pass GO and collect 200 caps.");
                        c.getBoard().getField(0).onPass(p); // Manually trigger pass reward
                    }
                    p.setPosition(newPos);
                    
                    return c.getBoard().getField(newPos).onLand(p, c); // Return onLand result
                }
        ));
        deck.add(new EventCard("A radstorm pushes you back 3 spaces.", CardType.STANDARD,
                (p, c) -> {
                    int newPos = (p.getPosition() - 3 + c.getBoard().getBoardSize()) % c.getBoard().getBoardSize();
                    p.setPosition(newPos);
                    
                    return c.getBoard().getField(newPos).onLand(p, c); // Return onLand result
                }
        ));

        // --- INTERACT WITH PLAYERS ---
        deck.add(new EventCard("It's your birthday! Collect 25 caps from each survivor.", CardType.STANDARD,
                (p, c) -> {
                    for (Player other : c.getPlayers()) {
                        if (other != p && other.isInGame()) {
                            other.payCaps(25);
                            p.addCaps(25);
                        }
                    }
                    return true; // Turn ends
                }
        ));
        deck.add(new EventCard("You host a party at The Tops. Pay 50 caps to each survivor.", CardType.STANDARD,
                (p, c) -> {
                    for (Player other : c.getPlayers()) {
                        if (other != p && other.isInGame()) {
                            p.payCaps(50);
                            other.addCaps(50);
                        }
                    }
                    return true; // Turn ends
                }
        ));

        // --- PROPERTY REPAIRS ---
        deck.add(new EventCard("Your settlements need repairs. Pay 25 caps for each property you own.", CardType.STANDARD,
                (p, c) -> {
                    int totalCost = p.getOwnedProperties().size() * 25;
                    c.getUi().logMessage("You own " + p.getOwnedProperties().size() + " properties. Total cost: " + totalCost);
                    p.payCaps(totalCost);
                    return true; // Turn ends
                }
        ));
        deck.add(new EventCard("Your fortresses are attacked! Pay 100 caps for each fully upgraded property (Level 3).", CardType.STANDARD,
                (p, c) -> {
                    int totalCost = 0;
                    for (PropertyField prop : p.getOwnedProperties()) {
                        if (prop.getCurrentImprovementLevel() == PropertyDevelopment.MAX_IMPROVEMENT_LEVEL) {
                            totalCost += 100;
                        }
                    }
                    if (totalCost > 0) {
                        c.getUi().logMessage("Your upgraded properties cost you " + totalCost + " in repairs.");
                        p.payCaps(totalCost);
                    }
                    return true; // Turn ends
                }
        ));

        // --- GET OUT OF JAIL ---
        deck.add(new EventCard("You find a 'Get Out of Jail Free' holotape. Keep this card.", CardType.KEEPABLE,
                (p, c) -> {
                    p.setHasGetOutOfJailCard(true);
                    return true; // Turn ends
                }
        ));

        // --- GAIN RESOURCES ---
        deck.add(new EventCard("You find a hidden cache of scrap metal. Gain 2 SCRAP_MATERIAL.", CardType.STANDARD,
                (p, c) -> {
                    p.addResources(resources.ResourceType.SCRAP_MATERIAL, 2);
                    return true; // Turn ends
                }
        ));
        deck.add(new EventCard("You secure a water purifier. Gain 2 WATER.", CardType.STANDARD,
                (p, c) -> {
                    p.addResources(resources.ResourceType.WATER, 2);
                    return true; // Turn ends
                }
        ));
    }

    // Functional interface must also be updated
    @FunctionalInterface
    private interface CardEffect {
        boolean apply(Player player, GameController controller);
    }

    /**
     * Shuffles the main deck.
     */
    public void shuffleDeck() {
        Collections.shuffle(deck, new Random());
    }

    /**
     * Draws the top card, applies its effect, and handles discarding.
     */
    public boolean drawEvent(Player player, GameController controller) {
        UIInterface ui = controller.getUi();

        if (deck.isEmpty()) {
            ui.logMessage("The event deck is empty. Reshuffling the discard pile...");
            deck.addAll(discardPile);
            discardPile.clear();
            shuffleDeck();
        }

        EventCard card = deck.remove(0);

        ui.logMessage(player.getName() + " draws an event card...");
        ui.logMessage("EVENT: " + card.description);

        // Apply the card's effect and get its turn status
        boolean turnOver = card.effect.apply(player, controller);

        if (card.type == CardType.STANDARD) {
            discardPile.add(card);
        } else {
            ui.logMessage(player.getName() + " keeps this card.");
        }

        return turnOver;
    }
}