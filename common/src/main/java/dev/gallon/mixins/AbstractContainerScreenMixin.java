package dev.gallon.mixins;

import dev.gallon.HorseStatsMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    /**
     * Adds the compare buttons once the window's position is known. The screen builds its widgets again every time the
     * game window is resized, so the buttons follow.
     */
    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void horseStatsMod$addCompareButtons(CallbackInfo ci) {
        if ((Object) this instanceof HorseInventoryScreen screen) {
            HorseStatsMod.instance().ifPresent(mod -> mod.onHorseInventoryInit(screen, this::addRenderableWidget));
        }
    }
}
