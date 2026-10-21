package dev.gallon.mixins;

import dev.gallon.HorseStatsMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMountInventoryScreen.class)
public abstract class AbstractMountInventoryScreenMixin {
    /**
     * The panels are drawn with the background, so that the buttons and the tooltips are displayed above them.
     */
    @Inject(method = "extractBackground", at = @At("TAIL"), remap = false)
    private void horseStatsMod$extractPanels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof HorseInventoryScreen screen) {
            HorseStatsMod.instance().ifPresent(mod -> mod.onRenderHorseInventoryPanels(screen, graphics));
        }
    }
}
