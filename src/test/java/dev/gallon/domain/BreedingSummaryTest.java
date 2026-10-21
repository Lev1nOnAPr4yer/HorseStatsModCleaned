package dev.gallon.domain;

import dev.gallon.services.BreedingText;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingSummaryTest {
    private static BreedingStatus status(BreedingStatus.State state, int ticks, boolean estimated) {
        return new BreedingStatus(state, ticks, Optional.empty(), estimated);
    }

    @Test
    void theClickMessageOnlyMentionsBreedingWhenThereIsATimer() {
        ModConfig config = new ModConfig();

        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.COOLDOWN, 5980, false)).isPresent());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.READY, 400, false)).isPresent());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.IN_LOVE, 400, false)).isPresent());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.FOAL, 2400, false)).isPresent());

        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.CAN_BE_FED, -1, false)).isEmpty());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.UNKNOWN, -1, false)).isEmpty());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.NOT_TAMED, -1, false)).isEmpty());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.CHECKING, -1, false)).isEmpty());
        assertTrue(BreedingText.timerSummary(config, status(BreedingStatus.State.FOAL, -1, false)).isEmpty());
    }

    @Test
    void theTimerIsMarkedWhenItIsAnEstimate() {
        ModConfig config = new ModConfig();

        String exact = BreedingText.timerSummary(config, status(BreedingStatus.State.COOLDOWN, 5980, false)).orElseThrow();
        String estimate = BreedingText.timerSummary(config, status(BreedingStatus.State.COOLDOWN, 5980, true)).orElseThrow();

        assertTrue(exact.contains("4:59"), exact);
        assertFalse(exact.contains("~"), exact);
        assertTrue(estimate.contains("~4:59"), estimate);
    }

    @Test
    void theSummaryHasNoColorCodesWhenColorsAreOff() {
        ModConfig config = new ModConfig();
        config.setColoredStats(false);

        String summary = BreedingText.timerSummary(config, status(BreedingStatus.State.COOLDOWN, 5980, false)).orElseThrow();

        assertFalse(summary.contains("§"), summary);
    }
}
