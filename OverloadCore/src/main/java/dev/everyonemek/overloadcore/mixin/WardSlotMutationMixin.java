package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.WardCustody;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemStackHandler.class)
public abstract class WardSlotMutationMixin {
    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void overload$guardSet(int slot, ItemStack stack, CallbackInfo ci) {
        if ((Object)this instanceof WardSlotAccess access && WardCustody.locked(access.overload$context().apply(slot))) ci.cancel();
    }
    @Inject(method = "setStackInSlot", at = @At("TAIL"))
    private void overload$recordSet(int slot, ItemStack stack, CallbackInfo ci) {
        if ((Object)this instanceof WardSlotAccess access) WardCustody.changed(access.overload$context().apply(slot));
    }
    @Inject(method = "insertItem", at = @At("RETURN"))
    private void overload$recordInsert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> ci) {
        if (!simulate && (Object)this instanceof WardSlotAccess access) WardCustody.changed(access.overload$context().apply(slot));
    }
    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void overload$guardExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> ci) {
        if ((Object)this instanceof WardSlotAccess access && WardCustody.locked(access.overload$context().apply(slot))) ci.setReturnValue(ItemStack.EMPTY);
    }
}
