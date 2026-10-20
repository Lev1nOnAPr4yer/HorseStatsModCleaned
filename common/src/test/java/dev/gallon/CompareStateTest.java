package dev.gallon;

import dev.gallon.domain.BreedingParent;
import dev.gallon.domain.MountType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompareStateTest {
    @Test
    void needsTwoMountsToCompare() {
        CompareState state = new CompareState();
        state.set(CompareState.FIRST, parent(MountType.HORSE));

        state.run();

        assertEquals(CompareState.Problem.MISSING, state.outcome().orElseThrow().problem());
    }

    @Test
    void refusesToCompareAMountWithItself() {
        CompareState state = new CompareState();
        BreedingParent horse = parent(MountType.HORSE);
        state.set(CompareState.FIRST, horse);
        state.set(CompareState.SECOND, horse);

        state.run();

        assertEquals(CompareState.Problem.SAME_MOUNT, state.outcome().orElseThrow().problem());
    }

    @Test
    void mulesCantBreed() {
        CompareState state = new CompareState();
        state.set(CompareState.FIRST, parent(MountType.MULE));
        state.set(CompareState.SECOND, parent(MountType.HORSE));

        state.run();

        assertEquals(CompareState.Problem.MULE, state.outcome().orElseThrow().problem());
    }

    @Test
    void onlyHorsesAndDonkeysAreSupported() {
        CompareState state = new CompareState();
        state.set(CompareState.FIRST, parent(MountType.LLAMA));
        state.set(CompareState.SECOND, parent(MountType.LLAMA));

        state.run();

        assertEquals(CompareState.Problem.UNSUPPORTED, state.outcome().orElseThrow().problem());
    }

    @Test
    void comparesTwoDifferentHorsesAndForgetsTheResultWhenAMountChanges() {
        CompareState state = new CompareState();
        state.set(CompareState.FIRST, parent(MountType.HORSE));
        state.set(CompareState.SECOND, parent(MountType.DONKEY));

        state.run();

        CompareState.Outcome outcome = state.outcome().orElseThrow();
        assertNull(outcome.problem());
        assertNotNull(outcome.odds());

        state.set(CompareState.SECOND, parent(MountType.HORSE));

        assertTrue(state.outcome().isEmpty());
    }

    @Test
    void clearForgetsEverything() {
        CompareState state = new CompareState();
        state.set(CompareState.FIRST, parent(MountType.HORSE));
        state.set(CompareState.SECOND, parent(MountType.HORSE));
        state.run();

        state.clear();

        assertTrue(state.get(CompareState.FIRST).isEmpty());
        assertTrue(state.get(CompareState.SECOND).isEmpty());
        assertTrue(state.outcome().isEmpty());
    }

    private static BreedingParent parent(MountType type) {
        return new BreedingParent(UUID.randomUUID(), "Parent", type, 22, 0.7, 0.225);
    }
}
