package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.Flowers;
import dev.everyonemek.botania.PlantSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.block_entity.flower.functional.JadedAmaranthusBlockEntity;

@Mixin(value = JadedAmaranthusBlockEntity.class, remap = false)
public abstract class AmaranthusWorkMixin {
    @Inject(method = "tickFlower", at = @At("HEAD"), cancellable = true)
    private void checkWorkArea(CallbackInfo callback) {
        var flower = (JadedAmaranthusBlockEntity) (Object) this;
        if (Flowers.isAmaranthus(flower) && flower.getLevel() != null && !flower.getLevel().isClientSide
              && (!Flowers.enabled(flower) || !PlantSupport.areaLoaded(flower.getLevel(), flower.getBlockPos(), 5))) callback.cancel();
    }
}
