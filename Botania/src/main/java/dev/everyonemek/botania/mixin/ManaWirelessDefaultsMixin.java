package dev.everyonemek.botania.mixin;

import java.util.function.Predicate;
import appeng.api.stacks.AEKeyType;
import appeng.api.util.KeyTypeSelection;
import appeng.items.tools.powered.WirelessTerminalItem;
import dev.everyonemek.botania.compat.ae2.ManaVisibilityDefaults;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.api.util.KeyTypeSelection", remap = false)
public abstract class ManaWirelessDefaultsMixin {
    @Inject(method = "forStack", at = @At("RETURN"))
    private static void botanicalmekanism$showManaInitially(ItemStack stack, Predicate<AEKeyType> allowed, CallbackInfoReturnable<KeyTypeSelection> callback) {
        if (stack.getItem() instanceof WirelessTerminalItem) ManaVisibilityDefaults.wireless(stack, callback.getReturnValue());
    }
}
