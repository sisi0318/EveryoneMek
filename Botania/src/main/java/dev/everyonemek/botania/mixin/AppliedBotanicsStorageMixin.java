package dev.everyonemek.botania.mixin;

import appeng.api.storage.MEStorage;
import dev.everyonemek.botania.compat.ae2.AppliedBotanicsCompat;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.mana.ManaReceiver;

/** One view per real inventory; a network-backed pool must not mount its own network. */
@Mixin(targets = "appbot.ae2.ManaExternalStorageStrategy", remap = false)
public abstract class AppliedBotanicsStorageMixin {
    @Shadow @Final private BlockCapabilityCache<ManaReceiver, Direction> apiCache;
    @Inject(method = "createWrapper", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$singleView(boolean extractable, Runnable changed, CallbackInfoReturnable<MEStorage> result) {
        var receiver = apiCache.getCapability();
        if (receiver instanceof AppliedBotanicsCompat.MachinePort || receiver instanceof appbot.block.FluixPoolBlockEntity) result.setReturnValue(null);
    }
}
