package mechanics;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import resources.ResourceType;

public class CasinoResult implements Serializable {
    private static final long serialVersionUID = 1L;

    // Use a map to hold all resource changes, including caps
    private Map<ResourceType, Integer> deltas;
    private int gamesPlayed;

    public CasinoResult() {
        this.deltas = new EnumMap<>(ResourceType.class);
        this.gamesPlayed = 0;
    }

    public void addDelta(ResourceType type, int amount) {
        this.deltas.put(type, this.deltas.getOrDefault(type, 0) + amount);
    }

    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public Map<ResourceType, Integer> getDeltas() {
        return deltas;
    }

    public String getSummary() {
        if (deltas.isEmpty()) {
            return "left the casino no richer or poorer.";
        }
        StringBuilder sb = new StringBuilder("finished gambling. Net result: ");
        for (Map.Entry<ResourceType, Integer> entry : deltas.entrySet()) {
            sb.append(String.format("%+d %s, ", entry.getValue(), entry.getKey().name()));
        }
        return sb.substring(0, sb.length() - 2); // Remove last comma
    }
}