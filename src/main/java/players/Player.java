package players;

import board.fields.PropertyField;
import config.GameConstants;
import resources.ResourceType;
import util.PlayerToken;
import game.network.ClientHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L; // Good practice

    private String name;
    private int bottleCaps;
    private int position;

    // --- FIX: REMOVED 'transient' ---
    // This list WILL be serialized.
    private List<PropertyField> ownedProperties;

    // --- FIX: REMOVED 'transient' (if you had it) ---
    // This map WILL be serialized.
    private Map<ResourceType, Integer> resources;

    private PlayerToken token;
    private boolean isInGame;
    private boolean hasGetOutOfJailCard;
    private boolean inJail;
    private int jailTurns;
    private SpecialStats specialStats;

    // --- FIX: This MUST be transient ---
    // ClientHandler is part of the server and cannot be sent to the client.
    private transient ClientHandler networkHandler;

    public Player(String name, PlayerToken token) {
        this.name = name;
        this.token = token;
        this.bottleCaps = GameConstants.STARTING_CAPS;
        this.position = 0;
        this.isInGame = true;

        // --- FIX: Initialize all fields here ---
        // This ensures they are never null, even if serialization fails
        this.ownedProperties = new ArrayList<>();
        this.resources = new EnumMap<>(ResourceType.class);
        this.hasGetOutOfJailCard = false;
        this.inJail = false;
        this.jailTurns = 0;
        this.specialStats = SpecialStats.generateRandom();

        // Initialize resources
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.BOTTLECAPS) {
                this.resources.put(type, 0);
            }
        }
    }

    public SpecialStats getSpecial() {
        if (specialStats == null) {
            specialStats = SpecialStats.generateRandom();
        }
        return specialStats;
    }

    public void setSpecial(SpecialStats stats) {
        this.specialStats = stats;
    }

    // --- Getters ---
    public String getName() {
        return name;
    }

    public int getBottleCaps() {
        return bottleCaps;
    }

    public int getPosition() {
        return position;
    }

    public List<PropertyField> getOwnedProperties() {
        // FIX: Ensure the list is never null
        if (ownedProperties == null) {
            ownedProperties = new ArrayList<>();
        }
        return ownedProperties;
    }

    public Map<ResourceType, Integer> getResources() {
        // FIX: Ensure the map is never null
        if (resources == null) {
            resources = new EnumMap<>(ResourceType.class);
        }
        return resources;
    }

    public PlayerToken getToken() {
        return token;
    }

    public boolean isInGame() {
        return isInGame;
    }

    public boolean hasGetOutOfJailCard() {
        return hasGetOutOfJailCard;
    }

    public boolean isInJail() {
        return inJail;
    }

    // --- Setters ---
    public void setPosition(int position) {
        this.position = position;
    }

    public void setInGame(boolean inGame) {
        isInGame = inGame;
    }

    /**
     * Sets the player's jail status.
     * When jailing a player, this also resets their turn counter.
     */
    public void setInJail(boolean inJail) {
        this.inJail = inJail;
        if (inJail) {
            this.jailTurns = 0;
        }
        if (inJail) {
            setPosition(10); // Direkt auf Position 10 setzen (Gefängnis)
        }
    }

    public int getJailTurns() {
        return jailTurns;
    }

    public void incrementJailTurns() {
        this.jailTurns++;
    }

    public void setHasGetOutOfJailCard(boolean hasCard) {
        this.hasGetOutOfJailCard = hasCard;
    }

    // --- Network Handler ---
    public void setNetworkHandler(ClientHandler handler) {
        this.networkHandler = handler;
    }

    public ClientHandler getNetworkHandler() {
        return networkHandler;
    }

    public void setName(String name) {
        this.name = name;
    }

    // --- Resource Management ---
    public boolean canAfford(int cost) {
        return this.bottleCaps >= cost;
    }

    public void addCaps(int amount) {
        this.bottleCaps += amount;
    }

    public void payCaps(int amount) {
        this.bottleCaps -= amount;
        // Bankruptcy is checked by the GameController
    }

    public void addResources(ResourceType type, int amount) {
        if (type == ResourceType.BOTTLECAPS) {
            addCaps(amount);
        } else {
            // Use the getter to ensure the map is not null
            getResources().put(type, getResources().getOrDefault(type, 0) + amount);
        }
    }

    public boolean consumeResources(ResourceType type, int amount) {
        if (type == ResourceType.BOTTLECAPS) {
            if (canAfford(amount)) {
                payCaps(amount);
                return true;
            }
            return false;
        } else {
            // Use the getter to ensure the map is not null
            int currentAmount = getResources().getOrDefault(type, 0);
            if (currentAmount >= amount) {
                getResources().put(type, currentAmount - amount);
                return true;
            }
            return false;
        }
    }

    // --- Property Management ---
    public void addProperty(PropertyField property) {
        // Use the getter to ensure the list is not null
        getOwnedProperties().add(property);
    }

    public void removeProperty(PropertyField property) {
        // Use the getter to ensure the list is not null
        getOwnedProperties().remove(property);
    }

    // Override equals and hashCode for proper network comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return name.equals(player.name) && token == player.token;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, token);
    }
}