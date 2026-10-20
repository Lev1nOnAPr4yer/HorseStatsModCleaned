package dev.gallon.domain;

import dev.gallon.services.HorseStatsService;
import dev.gallon.services.JumpHeightConverter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * What a mount passes down to its foals: the base values of its attributes, without any potion effect.
 *
 * @param jumpStrength   the 'jump_strength' attribute (not the height in blocks)
 * @param speedAttribute the 'movement_speed' attribute (not the speed in blocks per second)
 */
public record BreedingParent(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull MountType mountType,
        double health,
        double jumpStrength,
        double speedAttribute
) {
    /**
     * @return the stats the mod displays for this mount, computed from its base values
     */
    public @NotNull HorseStats toStats() {
        return new HorseStats(
                name,
                health,
                JumpHeightConverter.getJumpHeight(jumpStrength),
                HorseStatsService.convertSpeedToBlocksPerSeconds(speedAttribute),
                Optional.empty(),
                Optional.empty(),
                mountType
        );
    }
}
