package dev.gallon.services;

import dev.gallon.domain.BreedingStatus;
import dev.gallon.domain.I18nKeys;
import dev.gallon.domain.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The words and colors of a {@link BreedingStatus}.
 */
public final class BreedingText {
    private static final int GREEN = 0xFF77BF04;
    private static final int YELLOW = 0xFFFAE314;
    private static final int ORANGE = 0xFFD18800;
    private static final int MUTED = 0xFFA0A0A0;

    private BreedingText() {
    }

    /**
     * @return "Ready", "On cooldown", "Foal"...
     */
    public static @NotNull String label(@NotNull BreedingStatus status) {
        return I18n.get("horsestatsmod.breeding.state." + status.state().name().toLowerCase());
    }

    /**
     * @return what to know besides the label: the time left, what is in the way, or what to do
     */
    public static @NotNull Optional<String> detail(@NotNull BreedingStatus status) {
        String time = BreedingStatus.formatTime(status.ticks());
        return switch (status.state()) {
            case FOAL -> status.ticks() >= 0 ? Optional.of(I18n.get("horsestatsmod.breeding.detail.foal", time)) : Optional.empty();
            case COOLDOWN -> Optional.of(I18n.get("horsestatsmod.breeding.detail.cooldown", time));
            case READY -> Optional.of(I18n.get("horsestatsmod.breeding.detail.ready", time));
            case IN_LOVE, CAN_BE_FED -> status.blocker()
                    .map(blocker -> I18n.get("horsestatsmod.breeding.detail." + blocker.name().toLowerCase()))
                    .or(() -> status.state() == BreedingStatus.State.CAN_BE_FED
                            ? Optional.of(I18n.get("horsestatsmod.breeding.detail.can_be_fed"))
                            : Optional.empty());
            case UNKNOWN -> Optional.of(I18n.get("horsestatsmod.breeding.detail.unknown"));
            case NOT_TAMED, CHECKING -> Optional.empty();
        };
    }

    public static int color(@NotNull BreedingStatus status) {
        return switch (status.state()) {
            case READY -> GREEN;
            case CAN_BE_FED -> status.blocker().isPresent() ? YELLOW : GREEN;
            case IN_LOVE -> YELLOW;
            case COOLDOWN -> ORANGE;
            case FOAL, NOT_TAMED, UNKNOWN, CHECKING -> MUTED;
        };
    }

    private static ChatFormatting chatColor(BreedingStatus status) {
        return switch (color(status)) {
            case GREEN -> ChatFormatting.GREEN;
            case YELLOW -> ChatFormatting.YELLOW;
            case ORANGE -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
    }

    /**
     * @return the line displayed above a horse's head, like "Breeding: On cooldown 4:32"
     */
    public static @NotNull Component aboveHeadLine(@NotNull ModConfig config, @NotNull BreedingStatus status) {
        String time = status.ticks() >= 0 ? " " + BreedingStatus.formatTime(status.ticks()) : "";
        String color = config.getColoredStats() ? chatColor(status).toString() : "";
        String reset = config.getColoredStats() ? ChatFormatting.RESET.toString() : "";
        return Component.literal(I18n.get(I18nKeys.BREEDING) + ": " + color + label(status) + time + reset);
    }
}
