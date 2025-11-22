package resources;

import java.io.Serializable;

public enum ResourceType implements Serializable {
    BOTTLECAPS("Currency"),
    WATER("Essential for survival"),
    POWER("Energy for buildings"),
    FOOD("Sustenance"),
    SCRAP_MATERIAL("Construction and upgrades");

    private static final long serialVersionUID = 1L;

    private final String description;

    ResourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}