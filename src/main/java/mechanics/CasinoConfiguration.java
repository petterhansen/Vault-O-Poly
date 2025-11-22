package mechanics;

import resources.ResourceType;
import java.io.Serializable;
import java.util.Random;

public class CasinoConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    public final ResourceType coinflipCurrency;
    public final ResourceType blackjackCurrency;
    public final ResourceType baccaratCurrency;
    public final ResourceType diceCurrency;

    public CasinoConfiguration() {
        Random random = new Random();
        this.coinflipCurrency = getRandomResource(random);
        this.blackjackCurrency = getRandomResource(random);
        this.baccaratCurrency = getRandomResource(random);
        this.diceCurrency = getRandomResource(random);
    }

    private ResourceType getRandomResource(Random random) {
        ResourceType[] values = ResourceType.values();
        return values[random.nextInt(values.length)];
    }
}