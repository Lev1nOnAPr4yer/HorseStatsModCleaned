package dev.gallon.mixins;

import dev.gallon.HorseStatsMod;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin {
    /**
     * The server sends this event when an animal is fed and when it has a foal, and nothing else about breeding.
     */
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), remap = false)
    private void horseStatsMod$onHearts(byte id, CallbackInfo ci) {
        if (id == 18 && (Object) this instanceof AbstractHorse horse && horse.level().isClientSide()) {
            HorseStatsMod.instance().ifPresent(mod -> mod.onHeartsEvent(horse));
        }
    }
}
