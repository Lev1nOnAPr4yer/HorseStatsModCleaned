package dev.gallon.domain;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Works out the breeding state of the horses a client has seen, on a server it can't ask.
 * <p>
 * The server never tells a client about the breeding cooldown or the love time, but both leave traces that it does
 * see:
 * <ul>
 *     <li>A burst of hearts above a horse, when it is fed (it was off cooldown, and is in love for 30 seconds) and when
 *     it breeds.</li>
 *     <li>A baby that appears where its mother stands, in the very tick of the hearts of a breeding. Both parents
 *     start a 5 minute cooldown then.</li>
 * </ul>
 * So hearts without a baby mean "fed", and hearts with a baby mean "bred". Only what the client witnessed is known, and
 * it is an estimate: the server pauses the cooldown of a horse that isn't in a loaded chunk, and a horse that was hurt
 * in the meantime isn't in love any more.
 */
public final class BreedingLog {
    public static final int COOLDOWN_TICKS = 6000;
    public static final int LOVE_TICKS = 600;

    /**
     * Ticks between the hearts and the baby of a breeding, which the server sends in the same tick
     */
    static final int MATCH_WINDOW = 6;
    /**
     * Blocks between the mother and the baby, which is created on her
     */
    static final double MATCH_DISTANCE = 1.5;
    /**
     * Horses breed when they are less than 3 blocks apart; a little more, since the client sees them move late
     */
    static final double PARTNER_DISTANCE = 3.5;
    /**
     * How long a feeding, or a cooldown that is over, is remembered
     */
    static final int FORGET_AFTER_COOLDOWN_TICKS = 12000;

    /**
     * An adult horse that could be the partner of a mother
     */
    public record Neighbour(int id, double x, double y, double z) {
    }

    @FunctionalInterface
    public interface NeighbourFinder {
        /**
         * @return the adult horses within {@code radius} blocks of the position
         */
        @NotNull List<Neighbour> adultsAround(double x, double y, double z, double radius);
    }

    /**
     * What is known of a horse, in the same terms as the server's: the ticks left on its cooldown (0 for none) and the
     * ticks left in love
     */
    public record Estimate(int cooldownTicks, int loveTicks) {
    }

    private record Hearts(int horseId, double x, double y, double z, long tick) {
    }

    private record Baby(int babyId, double x, double y, double z, long tick) {
    }

    private final Map<Integer, Long> fedAt = new HashMap<>();
    private final Map<Integer, Long> bredAt = new HashMap<>();
    private final List<Hearts> hearts = new ArrayList<>();
    private final List<Baby> babies = new ArrayList<>();

    /**
     * A burst of hearts above an adult horse
     */
    public void hearts(int horseId, double x, double y, double z, long tick) {
        hearts.add(new Hearts(horseId, x, y, z, tick));
    }

    /**
     * A baby horse that has just appeared
     */
    public void baby(int babyId, double x, double y, double z, long tick) {
        babies.add(new Baby(babyId, x, y, z, tick));
    }

    /**
     * Decides what the hearts and the babies seen so far were. Call it every tick.
     */
    public void process(long now, @NotNull NeighbourFinder neighbours) {
        for (int i = 0; i < babies.size(); i++) {
            Baby baby = babies.get(i);
            Hearts mother = findMother(baby);
            if (mother == null) {
                continue;
            }

            hearts.remove(mother);
            babies.remove(i--);
            bred(mother, neighbours);
        }

        // Hearts that no baby came with are a horse that was fed
        hearts.removeIf(h -> {
            if (now - h.tick() > MATCH_WINDOW) {
                fedAt.put(h.horseId(), h.tick());
                return true;
            }
            return false;
        });
        babies.removeIf(baby -> now - baby.tick() > MATCH_WINDOW);

        // A horse can only be fed when it isn't on cooldown, so a feeding that was seen also tells that it was off
        // cooldown, for as long as it is remembered
        fedAt.values().removeIf(tick -> now - tick > FORGET_AFTER_COOLDOWN_TICKS);
        bredAt.values().removeIf(tick -> now - tick > COOLDOWN_TICKS + FORGET_AFTER_COOLDOWN_TICKS);
    }

    private Hearts findMother(Baby baby) {
        Hearts best = null;
        double bestDistance = MATCH_DISTANCE;
        for (Hearts h : hearts) {
            double distance = Math.sqrt(sq(h.x() - baby.x()) + sq(h.y() - baby.y()) + sq(h.z() - baby.z()));
            if (h.horseId() != baby.babyId()
                    && Math.abs(h.tick() - baby.tick()) <= MATCH_WINDOW
                    && distance <= bestDistance) {
                best = h;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void bred(Hearts mother, NeighbourFinder neighbours) {
        markBred(mother.horseId(), mother.tick());

        // The partner is within reach of the mother, and was in love: it was fed. A horse on cooldown can't be.
        List<Neighbour> candidates = new ArrayList<>();
        for (Neighbour n : neighbours.adultsAround(mother.x(), mother.y(), mother.z(), PARTNER_DISTANCE)) {
            if (n.id() != mother.horseId() && !onCooldown(n.id(), mother.tick())) {
                candidates.add(n);
            }
        }
        List<Neighbour> fed = candidates.stream()
                .filter(n -> fedAt.containsKey(n.id()) && mother.tick() - fedAt.get(n.id()) <= LOVE_TICKS + MATCH_WINDOW)
                .toList();

        if (fed.size() == 1) {
            markBred(fed.get(0).id(), mother.tick());
        } else if (fed.isEmpty() && candidates.size() == 1) {
            markBred(candidates.get(0).id(), mother.tick());
        }
        // Otherwise it can't be told which horse it was: it stays unknown rather than blaming a neighbour
    }

    private void markBred(int horseId, long tick) {
        bredAt.put(horseId, tick);
        fedAt.remove(horseId);
    }

    private boolean onCooldown(int horseId, long tick) {
        Long bred = bredAt.get(horseId);
        return bred != null && tick - bred < COOLDOWN_TICKS;
    }

    /**
     * @return what is known of the horse, or nothing if the client saw neither its feeding nor its breeding
     */
    public @NotNull Optional<Estimate> estimate(int horseId, long now) {
        Long bred = bredAt.get(horseId);
        Long fed = fedAt.get(horseId);

        if (bred != null && now - bred < COOLDOWN_TICKS) {
            return Optional.of(new Estimate((int) (COOLDOWN_TICKS - (now - bred)), 0));
        }
        if (fed != null) {
            long love = LOVE_TICKS - (now - fed);
            return Optional.of(new Estimate(0, (int) Math.max(0, love)));
        }
        if (bred != null) {
            return Optional.of(new Estimate(0, 0));
        }
        return Optional.empty();
    }

    public void clear() {
        fedAt.clear();
        bredAt.clear();
        hearts.clear();
        babies.clear();
    }

    private static double sq(double value) {
        return value * value;
    }
}
