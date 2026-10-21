package dev.gallon.domain;

import dev.gallon.services.HorseStatsService;
import dev.gallon.services.JumpHeightConverter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Probabilities that a foal is better than both of its parents.
 * <p>
 * The foal's health, jump strength and speed are rolled independently by {@code AbstractHorse#createOffspringAttribute}:
 * <pre>
 * margin  = 0.15 * (max - min)
 * range   = |parentA - parentB| + 2 * margin
 * average = (parentA + parentB) / 2
 * foal    = average + range * ((U1 + U2 + U3) / 3 - 0.5)    // U1, U2, U3 are uniform in [0, 1)
 * </pre>
 * and a foal that lands outside of [min, max] is reflected back inside. This class works out the exact distribution
 * of that formula instead of simulating it, so the probabilities don't move from one run to the next.
 */
public final class BreedingOdds {
    // The ranges used by the game for horses, donkeys and mules, computed the same way (AbstractHorse#generateXxx)
    static final double HEALTH_MIN = 15.0F;
    static final double HEALTH_MAX = 15.0F + 7 + 8;
    static final double JUMP_STRENGTH_MIN = (float) (0.4F + 0.0 * 0.2 + 0.0 * 0.2 + 0.0 * 0.2);
    static final double JUMP_STRENGTH_MAX = (float) (0.4F + 1.0 * 0.2 + 1.0 * 0.2 + 1.0 * 0.2);
    static final double SPEED_MIN = (float) ((0.45F + 0.0 * 0.3 + 0.0 * 0.3 + 0.0 * 0.3) * 0.25);
    static final double SPEED_MAX = (float) ((0.45F + 1.0 * 0.3 + 1.0 * 0.3 + 1.0 * 0.3) * 0.25);

    /**
     * Number of slices used to integrate over two of the three stats. 1000 slices put the error far below 0.01%.
     */
    private static final int SLICES = 1000;

    private static final HorseStats REFERENCE = new HorseStats(
            "", 0.0, 0.0, 0.0, Optional.empty(), Optional.empty(), MountType.HORSE
    );

    /**
     * @param health  chance that the foal's health is higher than the health of both parents (0 - 1)
     * @param jump    chance that the foal's jump height is higher than the jump height of both parents (0 - 1)
     * @param speed   chance that the foal's speed is higher than the speed of both parents (0 - 1)
     * @param overall chance that the foal's overall percentage is higher than the overall percentage of both
     *                parents (0 - 1)
     */
    public record Odds(double health, double jump, double speed, double overall) {
    }

    private BreedingOdds() {
    }

    /**
     * @return whether the two mounts can have a foal together, and the foal's health, jump strength and speed follow
     * the formula described above: horses and donkeys (a horse and a donkey have a mule)
     */
    public static boolean canCompare(@NotNull MountType first, @NotNull MountType second) {
        return canBeParent(first) && canBeParent(second);
    }

    /**
     * @return whether the mount can be a parent that this class knows the foals of: horses and donkeys. Mules can't
     * have foals, and llamas, camels and the undead horses don't have foals that follow this formula.
     */
    public static boolean canBeParent(@NotNull MountType type) {
        return type == MountType.HORSE || type == MountType.DONKEY;
    }

    public static @NotNull Odds compute(@NotNull BreedingParent first, @NotNull BreedingParent second) {
        double healthA = first.health();
        double healthB = second.health();
        double jumpA = first.jumpStrength();
        double jumpB = second.jumpStrength();
        double speedA = first.speedAttribute();
        double speedB = second.speedAttribute();

        double health = chanceAbove(healthA, healthB, HEALTH_MIN, HEALTH_MAX);
        double jump = chanceAbove(jumpA, jumpB, JUMP_STRENGTH_MIN, JUMP_STRENGTH_MAX);
        double speed = chanceAbove(speedA, speedB, SPEED_MIN, SPEED_MAX);

        double threshold = Math.max(
                overall(healthA, jumpA, speedA),
                overall(healthB, jumpB, speedB)
        );

        return new Odds(health, jump, speed, chanceOverallAbove(healthA, healthB, jumpA, jumpB, speedA, speedB, threshold));
    }

    /**
     * @return the average of the health, jump height and speed percentages (0 - 1), like
     * {@link HorseStats#getGroupedStats()}
     */
    static double overall(double health, double jumpStrength, double speedAttribute) {
        return (normalizedHealth(health) + normalizedJump(jumpStrength) + normalizedSpeed(speedAttribute)) / 3;
    }

    private static double normalizedHealth(double health) {
        return (health - REFERENCE.minHealth()) / (REFERENCE.maxHealth() - REFERENCE.minHealth());
    }

    private static double normalizedJump(double jumpStrength) {
        double min = REFERENCE.minJumpHeight();
        return (JumpHeightConverter.getJumpHeight(jumpStrength) - min) / (REFERENCE.maxJumpHeight() - min);
    }

    private static double normalizedSpeed(double speedAttribute) {
        double min = REFERENCE.minSpeed();
        return (HorseStatsService.convertSpeedToBlocksPerSeconds(speedAttribute) - min) / (REFERENCE.maxSpeed() - min);
    }

    /**
     * @return the speed attribute that has the given normalized speed
     */
    private static double speedAttributeOf(double normalizedSpeed) {
        double min = REFERENCE.minSpeed();
        double speed = min + normalizedSpeed * (REFERENCE.maxSpeed() - min);
        return speed / HorseStatsService.convertSpeedToBlocksPerSeconds(1.0);
    }

    /**
     * @return the chance that a foal of the two parents ends up above both of them, for one attribute
     */
    static double chanceAbove(double parentA, double parentB, double min, double max) {
        double best = Math.max(clamp(parentA, min, max), clamp(parentB, min, max));
        return 1 - foalCdf(parentA, parentB, min, max, best);
    }

    private static double chanceOverallAbove(
            double healthA, double healthB,
            double jumpA, double jumpB,
            double speedA, double speedB,
            double threshold
    ) {
        // overall = (health + jump + speed) / 3 where the three stats are independent. Two of them are cut in slices,
        // the third one is handled exactly: P(health + jump + speed > 3 * threshold).
        double[] healthMass = new double[SLICES];
        double[] healthValue = new double[SLICES];
        double[] jumpMass = new double[SLICES];
        double[] jumpValue = new double[SLICES];
        double previousHealth = 0;
        double previousJump = 0;
        for (int i = 0; i < SLICES; i++) {
            double healthEnd = HEALTH_MIN + (HEALTH_MAX - HEALTH_MIN) * (i + 1) / SLICES;
            double healthCdf = foalCdf(healthA, healthB, HEALTH_MIN, HEALTH_MAX, healthEnd);
            healthMass[i] = healthCdf - previousHealth;
            previousHealth = healthCdf;
            healthValue[i] = normalizedHealth(HEALTH_MIN + (HEALTH_MAX - HEALTH_MIN) * (i + 0.5) / SLICES);

            double jumpEnd = JUMP_STRENGTH_MIN + (JUMP_STRENGTH_MAX - JUMP_STRENGTH_MIN) * (i + 1) / SLICES;
            double jumpCdf = foalCdf(jumpA, jumpB, JUMP_STRENGTH_MIN, JUMP_STRENGTH_MAX, jumpEnd);
            jumpMass[i] = jumpCdf - previousJump;
            previousJump = jumpCdf;
            jumpValue[i] = normalizedJump(JUMP_STRENGTH_MIN + (JUMP_STRENGTH_MAX - JUMP_STRENGTH_MIN) * (i + 0.5) / SLICES);
        }

        double total = 0;
        for (int h = 0; h < SLICES; h++) {
            if (healthMass[h] == 0) {
                continue;
            }
            for (int j = 0; j < SLICES; j++) {
                if (jumpMass[j] == 0) {
                    continue;
                }
                double neededSpeed = 3 * threshold - healthValue[h] - jumpValue[j];
                double chanceSpeedAbove = 1 - foalCdf(speedA, speedB, SPEED_MIN, SPEED_MAX, speedAttributeOf(neededSpeed));
                total += healthMass[h] * jumpMass[j] * chanceSpeedAbove;
            }
        }

        return clamp(total, 0, 1);
    }

    /**
     * @return the chance that the foal's attribute is lower than {@code value} (0 - 1)
     */
    static double foalCdf(double parentA, double parentB, double min, double max, double value) {
        if (value <= min) {
            return 0;
        }
        if (value >= max) {
            return 1;
        }

        double a = clamp(parentA, min, max);
        double b = clamp(parentB, min, max);
        double margin = 0.15 * (max - min);
        double range = Math.abs(a - b) + margin * 2.0;
        double average = (a + b) / 2.0;

        // Before the reflection the foal is average + range * q, where q is the sum of three uniform numbers over 3,
        // minus a half. A foal above max is reflected to 2 * max - foal, a foal under min to 2 * min - foal, and the
        // reflected foals never go out of the range on the other side because range / 2 < max - min.
        double cdf = unreflectedCdf(average, range, value)
                + 1 - unreflectedCdf(average, range, 2 * max - value)
                - unreflectedCdf(average, range, 2 * min - value);
        return clamp(cdf, 0, 1);
    }

    private static double unreflectedCdf(double average, double range, double value) {
        return sumOfThreeUniformsCdf(3 * ((value - average) / range + 0.5));
    }

    /**
     * @return the chance that the sum of three uniform numbers in [0, 1) is lower than {@code sum} (Irwin-Hall)
     */
    private static double sumOfThreeUniformsCdf(double sum) {
        if (sum <= 0) {
            return 0;
        }
        if (sum <= 1) {
            return sum * sum * sum / 6;
        }
        if (sum <= 2) {
            return (-2 * sum * sum * sum + 9 * sum * sum - 9 * sum + 3) / 6;
        }
        if (sum < 3) {
            double rest = 3 - sum;
            return 1 - rest * rest * rest / 6;
        }
        return 1;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @return a probability (0 - 1) as a percentage with one decimal, like "36.9%"
     */
    public static @NotNull String formatChance(double probability) {
        return HorseStats.formatDecimals(probability * 100, 1) + "%";
    }
}
