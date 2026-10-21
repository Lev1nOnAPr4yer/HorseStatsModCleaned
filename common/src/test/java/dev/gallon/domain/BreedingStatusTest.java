package dev.gallon.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreedingStatusTest {
    private static BreedingStatus.Inputs adult(Integer age, Integer inLove) {
        return new BreedingStatus.Inputs(false, true, false, false, age, inLove);
    }

    @Test
    void aFoalCantBreedAndKnowsWhenItGrowsUp() {
        BreedingStatus status = BreedingStatus.of(new BreedingStatus.Inputs(true, true, false, false, -2400, 0));

        assertEquals(BreedingStatus.State.FOAL, status.state());
        assertEquals(2400, status.ticks());
    }

    @Test
    void aFoalIsRecognizedWithoutTheServer() {
        BreedingStatus status = BreedingStatus.of(new BreedingStatus.Inputs(true, true, false, false, null, null));

        assertEquals(BreedingStatus.State.FOAL, status.state());
        assertEquals(-1, status.ticks());
    }

    @Test
    void anUntamedHorseCantBreed() {
        assertEquals(
                BreedingStatus.State.NOT_TAMED,
                BreedingStatus.of(new BreedingStatus.Inputs(false, false, false, false, 0, 0)).state()
        );
    }

    @Test
    void theCooldownIsTheAgeOfAnAdult() {
        BreedingStatus status = BreedingStatus.of(adult(6000, 0));

        assertEquals(BreedingStatus.State.COOLDOWN, status.state());
        assertEquals(6000, status.ticks());
    }

    @Test
    void aHorseInLoveWithoutAnythingInTheWayIsReady() {
        BreedingStatus status = BreedingStatus.of(adult(0, 380));

        assertEquals(BreedingStatus.State.READY, status.state());
        assertEquals(380, status.ticks());
        assertEquals(Optional.empty(), status.blocker());
    }

    @Test
    void aHorseThatIsRiddenOrHurtIsInLoveButNotReady() {
        BreedingStatus ridden = BreedingStatus.of(new BreedingStatus.Inputs(false, true, true, true, 0, 100));
        BreedingStatus hurt = BreedingStatus.of(new BreedingStatus.Inputs(false, true, true, false, 0, 100));

        assertEquals(BreedingStatus.State.IN_LOVE, ridden.state());
        assertEquals(Optional.of(BreedingStatus.Blocker.RIDDEN), ridden.blocker());
        assertEquals(BreedingStatus.State.IN_LOVE, hurt.state());
        assertEquals(Optional.of(BreedingStatus.Blocker.HURT), hurt.blocker());
    }

    @Test
    void anAdultWithoutCooldownBreedsWhenItIsFed() {
        BreedingStatus status = BreedingStatus.of(new BreedingStatus.Inputs(false, true, false, true, 0, 0));

        assertEquals(BreedingStatus.State.CAN_BE_FED, status.state());
        assertEquals(Optional.of(BreedingStatus.Blocker.RIDDEN), status.blocker());
    }

    @Test
    void theCooldownIsUnknownWhenTheServerCantBeRead() {
        assertEquals(BreedingStatus.State.UNKNOWN, BreedingStatus.of(adult(null, null)).state());
    }

    @Test
    void theTimeCountsDownAndStopsAtZero() {
        BreedingStatus status = BreedingStatus.of(adult(6000, 0));

        assertEquals(5900, status.elapsed(100).ticks());
        assertEquals(0, status.elapsed(10_000).ticks());
        assertEquals(-1, BreedingStatus.checking().elapsed(100).ticks());
    }

    @Test
    void formatsTimesAsMinutesAndSecondsRoundedUp() {
        assertEquals("5:00", BreedingStatus.formatTime(6000));
        assertEquals("4:59", BreedingStatus.formatTime(5980));
        assertEquals("0:01", BreedingStatus.formatTime(1));
        assertEquals("0:00", BreedingStatus.formatTime(0));
        assertEquals("20:00", BreedingStatus.formatTime(24000));
    }
}
