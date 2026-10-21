package dev.gallon.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Whether a horse can breed right now, or how long it has to wait.
 * <p>
 * A horse can have a foal when both parents are tamed adults at full health that nobody rides, that are "in love" (they
 * were fed a golden apple or a golden carrot less than 30 seconds ago), and that are not on their breeding cooldown (5
 * minutes after a foal is born). The game keeps the cooldown and the love time on the server only, so they are unknown
 * (null) when the client can't read them.
 *
 * @param ticks the time left for {@link State#COOLDOWN}, {@link State#READY}, {@link State#IN_LOVE} and
 *              {@link State#FOAL} (20 ticks per second), or -1 when it isn't known
 */
public record BreedingStatus(@NotNull State state, int ticks, @NotNull Optional<Blocker> blocker) {
    public enum State {
        /**
         * A baby horse: it can't breed until it has grown up
         */
        FOAL,
        NOT_TAMED,
        /**
         * An adult that bred recently
         */
        COOLDOWN,
        /**
         * In love, and nothing prevents it from breeding
         */
        READY,
        /**
         * In love, but something prevents it from breeding, see {@link Blocker}
         */
        IN_LOVE,
        /**
         * An adult without cooldown that isn't in love: it breeds when it is fed
         */
        CAN_BE_FED,
        /**
         * The client can't know, because the game only tells the server about breeding
         */
        UNKNOWN,
        /**
         * The answer of the server hasn't arrived yet
         */
        CHECKING
    }

    public enum Blocker {
        RIDDEN, HURT
    }

    /**
     * What the client knows about a horse. {@code age} and {@code inLove} are null unless they were read from the
     * server.
     *
     * @param age    the game's age: negative for a foal, positive for the ticks left on the breeding cooldown
     * @param inLove the ticks left in love
     */
    public record Inputs(
            boolean baby,
            boolean tamed,
            boolean hurt,
            boolean ridden,
            @Nullable Integer age,
            @Nullable Integer inLove
    ) {
    }

    public static @NotNull BreedingStatus checking() {
        return new BreedingStatus(State.CHECKING, -1, Optional.empty());
    }

    public static @NotNull BreedingStatus of(@NotNull Inputs inputs) {
        if (inputs.baby() || (inputs.age() != null && inputs.age() < 0)) {
            return new BreedingStatus(
                    State.FOAL,
                    inputs.age() != null && inputs.age() < 0 ? -inputs.age() : -1,
                    Optional.empty()
            );
        }
        if (!inputs.tamed()) {
            return new BreedingStatus(State.NOT_TAMED, -1, Optional.empty());
        }
        if (inputs.age() == null || inputs.inLove() == null) {
            return new BreedingStatus(State.UNKNOWN, -1, Optional.empty());
        }
        if (inputs.age() > 0) {
            return new BreedingStatus(State.COOLDOWN, inputs.age(), Optional.empty());
        }

        Optional<Blocker> blocker = inputs.ridden()
                ? Optional.of(Blocker.RIDDEN)
                : inputs.hurt() ? Optional.of(Blocker.HURT) : Optional.empty();
        if (inputs.inLove() > 0) {
            return new BreedingStatus(blocker.isEmpty() ? State.READY : State.IN_LOVE, inputs.inLove(), blocker);
        }
        return new BreedingStatus(State.CAN_BE_FED, -1, blocker);
    }

    /**
     * @return the same status with less time left, used to count down between two answers of the server
     */
    public @NotNull BreedingStatus elapsed(int elapsedTicks) {
        if (ticks < 0 || elapsedTicks <= 0) {
            return this;
        }
        return new BreedingStatus(state, Math.max(0, ticks - elapsedTicks), blocker);
    }

    /**
     * @return a time in ticks as minutes and seconds, like "4:32". A started second counts as a whole one.
     */
    public static @NotNull String formatTime(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
