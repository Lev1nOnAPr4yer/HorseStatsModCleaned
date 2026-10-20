package dev.gallon.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingOddsTest {
    private static final double MID_HEALTH = (BreedingOdds.HEALTH_MIN + BreedingOdds.HEALTH_MAX) / 2;
    private static final double MID_JUMP = (BreedingOdds.JUMP_STRENGTH_MIN + BreedingOdds.JUMP_STRENGTH_MAX) / 2;
    private static final double MID_SPEED = (BreedingOdds.SPEED_MIN + BreedingOdds.SPEED_MAX) / 2;

    @Test
    void parentsInTheMiddleOfTheRangeHaveHalfAChanceOfBeingBeaten() {
        // The distribution is symmetrical around two identical parents, so the foal is above them half of the time
        assertEquals(0.5, BreedingOdds.chanceAbove(MID_HEALTH, MID_HEALTH, 15, 30), 1e-12);
        assertEquals(0.5, BreedingOdds.chanceAbove(MID_JUMP, MID_JUMP, 0.4, 1.0), 1e-12);
    }

    @Test
    void parentsAtTheMaximumCanNeverBeBeaten() {
        BreedingOdds.Odds odds = BreedingOdds.compute(
                parent(MountType.HORSE, BreedingOdds.HEALTH_MAX, BreedingOdds.JUMP_STRENGTH_MAX, BreedingOdds.SPEED_MAX),
                parent(MountType.HORSE, BreedingOdds.HEALTH_MAX, BreedingOdds.JUMP_STRENGTH_MAX, BreedingOdds.SPEED_MAX)
        );

        assertEquals(0, odds.health(), 1e-12);
        assertEquals(0, odds.jump(), 1e-12);
        assertEquals(0, odds.speed(), 1e-12);
        assertEquals(0, odds.overall(), 1e-6);
    }

    @Test
    void parentsAtTheMinimumAreAlwaysBeaten() {
        BreedingOdds.Odds odds = BreedingOdds.compute(
                parent(MountType.HORSE, BreedingOdds.HEALTH_MIN, BreedingOdds.JUMP_STRENGTH_MIN, BreedingOdds.SPEED_MIN),
                parent(MountType.DONKEY, BreedingOdds.HEALTH_MIN, BreedingOdds.JUMP_STRENGTH_MIN, BreedingOdds.SPEED_MIN)
        );

        assertEquals(1, odds.health(), 1e-12);
        assertEquals(1, odds.jump(), 1e-12);
        assertEquals(1, odds.speed(), 1e-12);
        assertEquals(1, odds.overall(), 1e-6);
    }

    @Test
    void theOrderOfTheParentsDoesNotMatter() {
        BreedingParent first = parent(MountType.HORSE, 28, 0.9, 0.3);
        BreedingParent second = parent(MountType.DONKEY, 18, 0.5, 0.175);

        BreedingOdds.Odds firstOrder = BreedingOdds.compute(first, second);
        BreedingOdds.Odds secondOrder = BreedingOdds.compute(second, first);

        assertEquals(firstOrder.health(), secondOrder.health(), 1e-12);
        assertEquals(firstOrder.jump(), secondOrder.jump(), 1e-12);
        assertEquals(firstOrder.speed(), secondOrder.speed(), 1e-12);
        assertEquals(firstOrder.overall(), secondOrder.overall(), 1e-12);
    }

    @Test
    void matchesASimulationOfTheGameFormula() {
        BreedingParent first = parent(MountType.HORSE, 26.4, 0.83, 0.29);
        BreedingParent second = parent(MountType.HORSE, 21.7, 0.95, 0.25);

        BreedingOdds.Odds exact = BreedingOdds.compute(first, second);
        BreedingOdds.Odds simulated = simulate(first, second, 1_000_000, 12345);

        assertEquals(simulated.health(), exact.health(), 0.004);
        assertEquals(simulated.jump(), exact.jump(), 0.004);
        assertEquals(simulated.speed(), exact.speed(), 0.004);
        assertEquals(simulated.overall(), exact.overall(), 0.004);
    }

    @Test
    void matchesASimulationWhenTheParentsAreFarFromEachOther() {
        BreedingParent first = parent(MountType.DONKEY, 16.2, 0.5, 0.175);
        BreedingParent second = parent(MountType.HORSE, 29.8, 0.99, 0.33);

        BreedingOdds.Odds exact = BreedingOdds.compute(first, second);
        BreedingOdds.Odds simulated = simulate(first, second, 1_000_000, 777);

        assertEquals(simulated.health(), exact.health(), 0.004);
        assertEquals(simulated.jump(), exact.jump(), 0.004);
        assertEquals(simulated.speed(), exact.speed(), 0.004);
        assertEquals(simulated.overall(), exact.overall(), 0.004);
    }

    @Test
    void matchesASimulationNearTheLimitsWhereFoalsAreReflected() {
        BreedingParent first = parent(MountType.HORSE, 29.5, 0.98, 0.33);
        BreedingParent second = parent(MountType.HORSE, 29.9, 0.995, 0.335);

        BreedingOdds.Odds exact = BreedingOdds.compute(first, second);
        BreedingOdds.Odds simulated = simulate(first, second, 1_000_000, 4242);

        assertEquals(simulated.health(), exact.health(), 0.004);
        assertEquals(simulated.jump(), exact.jump(), 0.004);
        assertEquals(simulated.speed(), exact.speed(), 0.004);
        assertEquals(simulated.overall(), exact.overall(), 0.004);
    }

    @Test
    void theOverallChanceIsBetweenZeroAndOne() {
        BreedingOdds.Odds odds = BreedingOdds.compute(
                parent(MountType.HORSE, MID_HEALTH, MID_JUMP, MID_SPEED),
                parent(MountType.HORSE, MID_HEALTH, MID_JUMP, MID_SPEED)
        );

        assertTrue(odds.overall() > 0 && odds.overall() < 1);
        // Beating the average of three stats that are each beaten half of the time is also about half of the time
        assertEquals(0.5, odds.overall(), 0.02);
    }

    @Test
    void onlyHorsesAndDonkeysCanBeCompared() {
        assertTrue(BreedingOdds.canCompare(MountType.HORSE, MountType.HORSE));
        assertTrue(BreedingOdds.canCompare(MountType.HORSE, MountType.DONKEY));
        assertTrue(BreedingOdds.canCompare(MountType.DONKEY, MountType.DONKEY));
        assertFalse(BreedingOdds.canCompare(MountType.MULE, MountType.HORSE));
        assertFalse(BreedingOdds.canCompare(MountType.LLAMA, MountType.LLAMA));
        assertFalse(BreedingOdds.canCompare(MountType.SKELETON_HORSE, MountType.HORSE));
        assertFalse(BreedingOdds.canCompare(MountType.CAMEL, MountType.CAMEL));
    }

    @Test
    void formatsChancesWithOneDecimal() {
        assertEquals("36.9%", BreedingOdds.formatChance(0.3694));
        assertEquals("0.0%", BreedingOdds.formatChance(0));
        assertEquals("100.0%", BreedingOdds.formatChance(1));
        assertEquals("0.3%", BreedingOdds.formatChance(0.0034));
    }

    private static BreedingParent parent(MountType type, double health, double jumpStrength, double speed) {
        return new BreedingParent(UUID.randomUUID(), "Parent", type, health, jumpStrength, speed);
    }

    /**
     * Rolls foals exactly like AbstractHorse#createOffspringAttribute does and counts the ones that are better than
     * both of their parents.
     */
    private static BreedingOdds.Odds simulate(BreedingParent first, BreedingParent second, int foals, long seed) {
        Random random = new Random(seed);
        double threshold = Math.max(
                BreedingOdds.overall(first.health(), first.jumpStrength(), first.speedAttribute()),
                BreedingOdds.overall(second.health(), second.jumpStrength(), second.speedAttribute())
        );

        int betterHealth = 0;
        int betterJump = 0;
        int betterSpeed = 0;
        int betterOverall = 0;
        for (int i = 0; i < foals; i++) {
            double health = createOffspringAttribute(
                    first.health(), second.health(), BreedingOdds.HEALTH_MIN, BreedingOdds.HEALTH_MAX, random
            );
            double jump = createOffspringAttribute(
                    first.jumpStrength(), second.jumpStrength(),
                    BreedingOdds.JUMP_STRENGTH_MIN, BreedingOdds.JUMP_STRENGTH_MAX, random
            );
            double speed = createOffspringAttribute(
                    first.speedAttribute(), second.speedAttribute(), BreedingOdds.SPEED_MIN, BreedingOdds.SPEED_MAX, random
            );

            betterHealth += health > Math.max(first.health(), second.health()) ? 1 : 0;
            betterJump += jump > Math.max(first.jumpStrength(), second.jumpStrength()) ? 1 : 0;
            betterSpeed += speed > Math.max(first.speedAttribute(), second.speedAttribute()) ? 1 : 0;
            betterOverall += BreedingOdds.overall(health, jump, speed) > threshold ? 1 : 0;
        }

        return new BreedingOdds.Odds(
                (double) betterHealth / foals,
                (double) betterJump / foals,
                (double) betterSpeed / foals,
                (double) betterOverall / foals
        );
    }

    /**
     * Line by line copy of AbstractHorse#createOffspringAttribute (Minecraft 26.3)
     */
    private static double createOffspringAttribute(
            double parentAValue, double parentBValue, double attributeRangeMin, double attributeRangeMax, Random random
    ) {
        parentAValue = Math.max(attributeRangeMin, Math.min(attributeRangeMax, parentAValue));
        parentBValue = Math.max(attributeRangeMin, Math.min(attributeRangeMax, parentBValue));
        double margin = 0.15 * (attributeRangeMax - attributeRangeMin);
        double range = Math.abs(parentAValue - parentBValue) + margin * 2.0;
        double average = (parentAValue + parentBValue) / 2.0;
        double babyQuality = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 - 0.5;
        double newValue = average + range * babyQuality;
        if (newValue > attributeRangeMax) {
            double difference = newValue - attributeRangeMax;
            return attributeRangeMax - difference;
        } else if (newValue < attributeRangeMin) {
            double difference = attributeRangeMin - newValue;
            return attributeRangeMin + difference;
        } else {
            return newValue;
        }
    }
}
