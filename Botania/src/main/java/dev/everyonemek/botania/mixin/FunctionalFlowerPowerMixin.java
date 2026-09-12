package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.Flowers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;

@Mixin(value = FunctionalFlowerBlockEntity.class, remap = false)
public abstract class FunctionalFlowerPowerMixin {
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void saveBionicData(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo callback) {
        var flower = (FunctionalFlowerBlockEntity) (Object) this;
        if (Flowers.isAmaranthus(flower)) Flowers.saveData(flower, tag);
    }
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void loadBionicData(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo callback) {
        var flower = (FunctionalFlowerBlockEntity) (Object) this;
        if (Flowers.isAmaranthus(flower)) Flowers.loadData(flower, tag);
    }
    @Inject(method = "drawManaFromPool", at = @At("HEAD"), cancellable = true)
    private void botanicalPower(CallbackInfo callback) {
        var flower = (FunctionalFlowerBlockEntity) (Object) this;
        if (Flowers.isAmaranthus(flower)) { Flowers.supplyAmaranthus(flower); callback.cancel(); }
    }
    @Inject(method = "getBindingRadius", at = @At("HEAD"), cancellable = true)
    private void noPoolBinding(CallbackInfoReturnable<Integer> callback) {
        if (Flowers.isAmaranthus((FunctionalFlowerBlockEntity) (Object) this)) callback.setReturnValue(0);
    }
}
