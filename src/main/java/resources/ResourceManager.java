package resources;

import board.fields.PropertyField;
import players.Player;
import ui.UIInterface;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

public class ResourceManager {

    private Random random = new Random();
    private final List<ResourceType> randomResourcePool = Arrays.asList(
            ResourceType.WATER,
            ResourceType.POWER,
            ResourceType.FOOD,
            ResourceType.SCRAP_MATERIAL
    );

    public void calculateResourceProduction(Player player, UIInterface ui) {
        if (player.getOwnedProperties().isEmpty()) {
            return;
        }

        ui.logMessage(player.getName() + " collects passive resources...");

        int intel = player.getSpecial().getIntelligence();
        // Base chance 0% + 5% per INT point to get a bonus resource per property
        int bonusChance = intel * 5;

        for (PropertyField property : player.getOwnedProperties()) {
            int level = property.getCurrentImprovementLevel();
            int amountToGrant = 0;

            // Standard yield
            switch (level) {
                case 0: case 1: amountToGrant = 1; break;
                case 2: amountToGrant = 2; break;
                case 3: amountToGrant = 4; break;
            }

            // Apply Intelligence Bonus
            boolean bonusProc = (random.nextInt(100) < bonusChance);
            if (bonusProc) {
                amountToGrant += 1;
            }

            ResourceType randomType = randomResourcePool.get(random.nextInt(randomResourcePool.size()));
            player.addResources(randomType, amountToGrant);

            String bonusMsg = bonusProc ? " (INT Bonus +1)" : "";
            ui.logMessage("  > '" + property.getName() + "' produced " + amountToGrant + " " + randomType.name() + bonusMsg);
        }
    }
}