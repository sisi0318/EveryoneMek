package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockEntity.class)
public abstract class MachineDataMixin {
    @Inject(method = "applyComponentsFromItemStack", at = @At("TAIL"))
    private void overload$restore(ItemStack stack, CallbackInfo ci) {
        var tile=(BlockEntity)(Object)this;
        var data=stack.get(CoreContent.MACHINE.get());
        if (data != null && (DeviceScope.supported(tile) || tile instanceof mekanism.common.tile.base.TileEntityMekanism))
            tile.getPersistentData().put(DeviceScope.KEY,data.copy());
    }
}
