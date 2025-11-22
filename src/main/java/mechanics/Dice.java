package mechanics;

import java.util.Random;

public class Dice {
    private Random random;
    private int numberOfDice;
    private int sides;

    private int lastRoll1;
    private int lastRoll2;

    public Dice(int numberOfDice, int sides) {
        this.numberOfDice = numberOfDice;
        this.sides = sides;
        this.random = new Random();
        this.lastRoll1 = 0;
        this.lastRoll2 = 0;
    }

    /**
     * Rolls 2 dice and returns the total.
     * Stores the individual rolls.
     */
    public int roll() {
        // This class is set for N dice, but jail logic requires 2.
        lastRoll1 = random.nextInt(sides) + 1;
        lastRoll2 = random.nextInt(sides) + 1;

        // If you wanted to support more dice, you'd need a loop.
        // For this game, 2 dice is standard.
        return lastRoll1 + lastRoll2;
    }

    /**
     * Checks if the last roll was doubles.
     */
    public boolean didRollDoubles() {
        return lastRoll1 == lastRoll2;
    }

    /**
     * Gets the values of the last roll (e.g., "3 and 5").
     */
    public String getLastRollDescription() {
        return lastRoll1 + " and " + lastRoll2;
    }
}