package dev.gallon.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingLogTest {
    private static final BreedingLog.NeighbourFinder NOBODY = (x, y, z, radius) -> List.of();

    private static BreedingLog.NeighbourFinder around(BreedingLog.Neighbour... neighbours) {
        return (x, y, z, radius) -> List.of(neighbours);
    }

    private static BreedingLog.Neighbour neighbour(int id) {
        return new BreedingLog.Neighbour(id, 11, 64, 10);
    }

    @Test
    void knowsNothingAboutAHorseItNeverSawFedOrBred() {
        assertTrue(new BreedingLog().estimate(1, 100).isEmpty());
    }

    @Test
    void heartsWithoutABabyMeanTheHorseWasFedAndIsInLoveFor30Seconds() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 0, 64, 0, 100);

        log.process(110, NOBODY);

        assertEquals(Optional.of(new BreedingLog.Estimate(0, 590)), log.estimate(1, 110));
        assertEquals(Optional.of(new BreedingLog.Estimate(0, 100)), log.estimate(1, 600));
    }

    @Test
    void aFedHorseIsOffCooldownWhenItsLoveIsOver() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 0, 64, 0, 100);
        log.process(110, NOBODY);

        // It could only be fed because it had no cooldown, and nothing was seen since
        assertEquals(Optional.of(new BreedingLog.Estimate(0, 0)), log.estimate(1, 2000));
    }

    @Test
    void heartsAndABabyOnTheSameSpotMeanTheMotherBredAndHerCooldownStarts() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 10, 64, 10, 500);
        log.baby(99, 10.2, 64, 10, 500);

        log.process(501, NOBODY);

        assertEquals(Optional.of(new BreedingLog.Estimate(6000, 0)), log.estimate(1, 500));
        assertEquals(Optional.of(new BreedingLog.Estimate(5000, 0)), log.estimate(1, 1500));
        assertEquals(Optional.of(new BreedingLog.Estimate(0, 0)), log.estimate(1, 6500));
    }

    @Test
    void theBabyMayBeSeenBeforeTheHearts() {
        BreedingLog log = new BreedingLog();
        log.baby(99, 10, 64, 10, 500);
        log.hearts(1, 10, 64, 10, 502);

        log.process(503, NOBODY);

        assertEquals(5999, log.estimate(1, 503).orElseThrow().cooldownTicks());
    }

    @Test
    void aBabyThatAppearsFarFromTheHeartsIsNotTheirFoal() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 10, 64, 10, 500);
        log.baby(99, 30, 64, 10, 500);

        log.process(520, NOBODY);

        // Just a horse that was fed, while a baby came into view elsewhere
        assertEquals(Optional.of(new BreedingLog.Estimate(0, 580)), log.estimate(1, 520));
    }

    @Test
    void aBabyThatAppearsMuchLaterIsNotTheirFoal() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 10, 64, 10, 500);
        log.process(520, NOBODY);
        log.baby(99, 10, 64, 10, 540);

        log.process(541, NOBODY);

        assertEquals(Optional.of(new BreedingLog.Estimate(0, 560)), log.estimate(1, 540));
    }

    @Test
    void theBabyIsNeverItsOwnMother() {
        BreedingLog log = new BreedingLog();
        log.hearts(99, 10, 64, 10, 500);
        log.baby(99, 10, 64, 10, 500);

        log.process(520, NOBODY);

        // The hearts are a feeding, not a breeding
        assertEquals(Optional.of(new BreedingLog.Estimate(0, 580)), log.estimate(99, 520));
    }

    @Test
    void thePartnerStartsItsCooldownToo() {
        BreedingLog log = new BreedingLog();
        log.hearts(2, 0, 64, 0, 100);
        log.process(110, NOBODY);

        log.hearts(1, 10, 64, 10, 300);
        log.baby(99, 10, 64, 10, 300);
        log.process(301, around(neighbour(2)));

        assertEquals(Optional.of(new BreedingLog.Estimate(5900, 0)), log.estimate(1, 400));
        assertEquals(Optional.of(new BreedingLog.Estimate(5900, 0)), log.estimate(2, 400));
    }

    @Test
    void anAdultNextToTheMotherIsThePartnerEvenIfItsFeedingWasNotSeen() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 10, 64, 10, 300);
        log.baby(99, 10, 64, 10, 300);

        log.process(301, around(neighbour(2)));

        assertEquals(Optional.of(new BreedingLog.Estimate(5900, 0)), log.estimate(2, 400));
    }

    @Test
    void theFedNeighbourIsThePartnerWhenSeveralHorsesStandAround() {
        BreedingLog log = new BreedingLog();
        log.hearts(3, 0, 64, 0, 100);
        log.process(110, NOBODY);

        log.hearts(1, 10, 64, 10, 300);
        log.baby(99, 10, 64, 10, 300);
        log.process(301, around(neighbour(2), neighbour(3), neighbour(4)));

        assertEquals(Optional.of(new BreedingLog.Estimate(5900, 0)), log.estimate(3, 400));
        assertTrue(log.estimate(2, 400).isEmpty());
        assertTrue(log.estimate(4, 400).isEmpty());
    }

    @Test
    void whenThePartnerCantBeToldItStaysUnknownInsteadOfBlamingAnotherHorse() {
        BreedingLog log = new BreedingLog();
        log.hearts(2, 0, 64, 0, 100);
        log.hearts(3, 0, 64, 0, 100);
        log.process(110, NOBODY);

        log.hearts(1, 10, 64, 10, 300);
        log.baby(99, 10, 64, 10, 300);
        log.process(301, around(neighbour(2), neighbour(3)));

        // The mother is known, and the two horses that were fed stay as they were: in love, not on cooldown
        assertEquals(Optional.of(new BreedingLog.Estimate(5900, 0)), log.estimate(1, 400));
        assertEquals(0, log.estimate(2, 400).orElseThrow().cooldownTicks());
        assertEquals(0, log.estimate(3, 400).orElseThrow().cooldownTicks());
    }

    @Test
    void aNeighbourOnItsCooldownCantBeThePartner() {
        BreedingLog log = new BreedingLog();
        log.hearts(2, 0, 64, 0, 100);
        log.baby(98, 0, 64, 0, 100);
        log.process(101, NOBODY);

        log.hearts(1, 10, 64, 10, 300);
        log.baby(99, 10, 64, 10, 300);
        log.process(301, around(neighbour(2)));

        // Horse 2 bred at tick 100 and still has 5800 ticks of cooldown: it isn't horse 1's partner
        assertEquals(Optional.of(new BreedingLog.Estimate(5800, 0)), log.estimate(2, 300));
    }

    @Test
    void forgetsWhatItSawLongAgo() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 10, 64, 10, 500);
        log.baby(99, 10, 64, 10, 500);
        log.process(501, NOBODY);

        log.process(500 + BreedingLog.COOLDOWN_TICKS + BreedingLog.FORGET_AFTER_COOLDOWN_TICKS + 10, NOBODY);

        assertTrue(log.estimate(1, 500 + BreedingLog.COOLDOWN_TICKS + BreedingLog.FORGET_AFTER_COOLDOWN_TICKS + 10).isEmpty());
    }

    @Test
    void clearForgetsEverything() {
        BreedingLog log = new BreedingLog();
        log.hearts(1, 0, 64, 0, 100);
        log.process(110, NOBODY);

        log.clear();

        assertTrue(log.estimate(1, 120).isEmpty());
    }
}
