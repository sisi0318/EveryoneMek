package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.Flowers;
import dev.everyonemek.botania.PlantSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.block_entity.flower.functional.*;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;

@Mixin(value = {JadedAmaranthusBlockEntity.class, ClayconiaBlockEntity.class, AgricarnationBlockEntity.class,
      HopperhockBlockEntity.class, RannuncarpusBlockEntity.class, ExoflameBlockEntity.class}, remap = false)
public abstract class AmaranthusWorkMixin {
    @Inject(method = "tickFlower", at = @At("HEAD"), cancellable = true)
    private void checkWorkArea(CallbackInfo callback) {
        var flower = (FunctionalFlowerBlockEntity) (Object) this;
        if (!Flowers.isBionic(flower) || flower.getLevel() == null || flower.getLevel().isClientSide) return;
        if (!Flowers.workAllowed(flower)) { callback.cancel(); return; }
        Flowers.supplyBionic(flower);
        if (flower.getMana() < Flowers.workReserve(flower)) callback.cancel();
    }
}
