package dev.gallon;

import dev.gallon.domain.BreedingOdds;
import dev.gallon.domain.BreedingParent;
import dev.gallon.domain.MountType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The two mounts selected to be compared, and the result of the last comparison. It lives as long as the game does, so
 * a mount can be added from one inventory and the other from another one.
 */
public final class CompareState {
    public static final int FIRST = 1;
    public static final int SECOND = 2;

    /**
     * Why two mounts can't be compared. The names are the end of the translation keys {@code horsestatsmod.compare.error.*}.
     */
    public enum Problem {
        MISSING, SAME_MOUNT, UNSUPPORTED, MULE;

        public @NotNull String translationKey() {
            return "horsestatsmod.compare.error." + name().toLowerCase();
        }
    }

    /**
     * The result of "Run compare": the odds, or why there are none.
     */
    public record Outcome(@Nullable BreedingOdds.Odds odds, @Nullable Problem problem) {
        public static @NotNull Outcome success(@NotNull BreedingOdds.Odds odds) {
            return new Outcome(odds, null);
        }

        public static @NotNull Outcome failure(@NotNull Problem problem) {
            return new Outcome(null, problem);
        }
    }

    private @Nullable BreedingParent first;
    private @Nullable BreedingParent second;
    private @Nullable Outcome outcome;

    public void set(int slot, @NotNull BreedingParent parent) {
        switch (slot) {
            case FIRST -> first = parent;
            case SECOND -> second = parent;
            default -> throw new IllegalArgumentException("Unknown compare slot " + slot);
        }
        // The previous result is about other mounts
        outcome = null;
    }

    public @NotNull Optional<BreedingParent> get(int slot) {
        return Optional.ofNullable(switch (slot) {
            case FIRST -> first;
            case SECOND -> second;
            default -> throw new IllegalArgumentException("Unknown compare slot " + slot);
        });
    }

    public @NotNull Optional<Outcome> outcome() {
        return Optional.ofNullable(outcome);
    }

    public void run() {
        outcome = compare(first, second);
    }

    public void clear() {
        first = null;
        second = null;
        outcome = null;
    }

    static @NotNull Outcome compare(@Nullable BreedingParent first, @Nullable BreedingParent second) {
        if (first == null || second == null) {
            return Outcome.failure(Problem.MISSING);
        }
        if (first.id().equals(second.id())) {
            return Outcome.failure(Problem.SAME_MOUNT);
        }
        if (first.mountType() == MountType.MULE || second.mountType() == MountType.MULE) {
            return Outcome.failure(Problem.MULE);
        }
        if (!BreedingOdds.canCompare(first.mountType(), second.mountType())) {
            return Outcome.failure(Problem.UNSUPPORTED);
        }

        return Outcome.success(BreedingOdds.compute(first, second));
    }
}
