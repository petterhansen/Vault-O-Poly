package mechanics;

import resources.ResourceType;
import board.fields.PropertyField; // <-- ADDED: Import PropertyField
import java.io.Serializable;
import java.util.EnumMap;
import java.util.List; // <-- ADDED: Import List
import java.util.ArrayList; // <-- ADDED: Import ArrayList
import java.util.Map;

/**
 * A serializable data-transfer-object (DTO) to hold the contents of a trade.
 */
public class TradeOffer implements Serializable {

    private static final long serialVersionUID = 1L;

    public int caps;
    public Map<ResourceType, Integer> resources;
    public List<PropertyField> properties; // <-- ADDED: List of properties being offered

    // Used by the server to track the state of the trade
    public transient boolean responseReceived = false;
    public transient boolean accepted = false;

    public TradeOffer() {
        this.caps = 0;
        this.resources = new EnumMap<>(ResourceType.class);
        this.properties = new ArrayList<>(); // <-- INITIALIZE
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("  - Caps: ").append(caps).append("\n");
        if (resources.isEmpty() || resources.values().stream().allMatch(v -> v == 0)) {
            sb.append("  - Resources: None\n");
        } else {
            for (Map.Entry<ResourceType, Integer> entry : resources.entrySet()) {
                if (entry.getValue() > 0) {
                    sb.append("  - ").append(entry.getKey().name()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }

        // --- ADDED: Property Summary ---
        if (properties.isEmpty()) {
            sb.append("  - Properties: None\n");
        } else {
            sb.append("  - Properties:\n");
            for (PropertyField property : properties) {
                sb.append("    > ").append(property.getName()).append("\n");
            }
        }

        return sb.toString();
    }
}