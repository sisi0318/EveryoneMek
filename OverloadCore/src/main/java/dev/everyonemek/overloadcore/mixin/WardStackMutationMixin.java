package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.WardCustody;
import net.minecraft.core.component.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemStack.class)
public abstract class WardStackMutationMixin {
    @Inject(method = "setCount", at = @At("HEAD"), cancellable = true)
    private void overload$guardCount(int count, CallbackInfo ci) {
        if (WardCustody.locked((ItemStack)(Object)this)) ci.cancel();
    }
    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private <T> void overload$guardComponent(DataComponentType<? super T> type, T value, CallbackInfoReturnable<T> ci) {
        if (WardCustody.locked((ItemStack)(Object)this)) ci.setReturnValue((T)((ItemStack)(Object)this).get(type));
    }
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private <T> void overload$guardRemove(DataComponentType<? extends T> type, CallbackInfoReturnable<T> ci) {
        if (WardCustody.locked((ItemStack)(Object)this)) ci.setReturnValue(((ItemStack)(Object)this).get(type));
    }
    @Inject(method = {"applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V", "applyComponentsAndValidate"},
          at = @At("HEAD"), cancellable = true)
    private void overload$guardPatch(DataComponentPatch patch, CallbackInfo ci) {
        if (WardCustody.locked((ItemStack)(Object)this)) ci.cancel();
    }
    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V", at = @At("HEAD"), cancellable = true)
    private void overload$guardMap(DataComponentMap components, CallbackInfo ci) {
        if (WardCustody.locked((ItemStack)(Object)this)) ci.cancel();
    }
}
