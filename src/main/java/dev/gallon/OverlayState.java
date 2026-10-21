package dev.gallon;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

final class OverlayState {
    static final int DISPLAY_TICKS = 60;

    private @NotNull Optional<Supplier<Component>> message = Optional.empty();
    private int remainingTicks;

    void show(@NotNull Component message) {
        show(() -> message);
    }

    /**
     * Shows a message that is worked out again every time it is displayed, so that it can follow what changes while it
     * is on screen, like a countdown.
     */
    void show(@NotNull Supplier<Component> message) {
        this.message = Optional.of(message);
        this.remainingTicks = DISPLAY_TICKS;
    }

    void tick() {
        if (remainingTicks <= 0) {
            return;
        }

        remainingTicks--;
        if (remainingTicks == 0) {
            message = Optional.empty();
        }
    }

    void clear() {
        message = Optional.empty();
        remainingTicks = 0;
    }

    @NotNull Optional<Component> message() {
        return message.map(Supplier::get);
    }

    int remainingTicks() {
        return remainingTicks;
    }
}
