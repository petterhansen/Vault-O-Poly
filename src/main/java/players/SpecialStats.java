package players;

import java.io.Serializable;
import java.util.Random;

public class SpecialStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int strength;
    private int perception;
    private int endurance;
    private int charisma;
    private int intelligence;
    private int agility;
    private int luck;

    public SpecialStats(int s, int p, int e, int c, int i, int a, int l) {
        this.strength = s;
        this.perception = p;
        this.endurance = e;
        this.charisma = c;
        this.intelligence = i;
        this.agility = a;
        this.luck = l;
    }

    public static SpecialStats generateRandom() {
        Random r = new Random();
        // Generate stats between 1 and 10, biased towards 5
        return new SpecialStats(
                r.nextInt(10) + 1,
                r.nextInt(10) + 1,
                r.nextInt(10) + 1,
                r.nextInt(10) + 1,
                r.nextInt(10) + 1,
                r.nextInt(10) + 1,
                r.nextInt(10) + 1
        );
    }

    // Getters
    public int getStrength() { return strength; }
    public int getPerception() { return perception; }
    public int getEndurance() { return endurance; }
    public int getCharisma() { return charisma; }
    public int getIntelligence() { return intelligence; }
    public int getAgility() { return agility; }
    public int getLuck() { return luck; }

    @Override
    public String toString() {
        return String.format("S:%d P:%d E:%d C:%d I:%d A:%d L:%d",
                strength, perception, endurance, charisma, intelligence, agility, luck);
    }
}