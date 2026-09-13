package dev.everyonemek.botania.mixin;

import java.util.List;
import dev.everyonemek.botania.ManaStorageItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A unified key must retain the addon's recoverable interface contents. */
@Mixin(targets = "appbot.ae2.ManaKey", remap = false)
public abstract class AppliedBotanicsManaKeyMixin {
    @Inject(method = "addDrops", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$recover(long amount, List<ItemStack> drops, Level level, BlockPos pos, CallbackInfo callback) {
        if (amount > 0) drops.add(ManaStorageItem.recovery(amount)); callback.cancel();
    }
}
