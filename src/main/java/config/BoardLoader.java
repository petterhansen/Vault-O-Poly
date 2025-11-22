package config;

import board.fields.*;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import resources.ResourceType;
import util.FieldType;
import util.PropertyType;
import util.SpecialEffect;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class BoardLoader {

    public static List<BoardField> loadBoard(String filename) {
        Gson gson = new Gson();
        // Load from resources folder
        InputStream in = BoardLoader.class.getClassLoader().getResourceAsStream(filename);
        if (in == null) {
            throw new IllegalArgumentException("File not found in resources: " + filename);
        }
        Reader reader = new InputStreamReader(in);
        BoardLayout layout = gson.fromJson(reader, BoardLayout.class);

        List<BoardField> fields = new ArrayList<>();
        for (FieldData data : layout.fields) {
            switch (data.type) {
                case START:
                    fields.add(new StartField(data.name, data.description, data.position, data.passReward));
                    break;
                case PROPERTY:
                    // FIX: Pass data.imageUrl as the 9th argument
                    fields.add(new PropertyField(data.name, data.description, data.position, data.purchaseCost, data.rentCosts, data.improvementCost, data.propertyType, data.groupId, data.imageUrl));
                    break;
                case RESOURCE:
                    fields.add(new ResourceField(data.name, data.description, data.position, data.resourceType, data.productionRate));
                    break;
                case SPECIAL:
                    fields.add(new SpecialField(data.name, data.description, data.position, data.effect, data.effectValue));
                    break;
            }
        }

        fields.sort((f1, f2) -> Integer.compare(f1.getPosition(), f2.getPosition()));
        return fields;
    }

    // --- DTO Classes for Gson ---
    private static class BoardLayout {
        String boardName;
        List<FieldData> fields;
    }

    private static class FieldData {
        int position;
        FieldType type;
        String name;
        String description;

        // START
        int passReward;

        // PROPERTY
        int purchaseCost;
        PropertyType propertyType;
        int[] rentCosts;
        int improvementCost;
        String groupId;
        String imageUrl; // <-- ADDED: Read from JSON

        // RESOURCE
        ResourceType resourceType;
        int productionRate;

        // SPECIAL
        SpecialEffect effect;
        @SerializedName("effectValue") // Use this if JSON name doesn't match
        int effectValue;
    }
}