package dev.everyonemek.botania.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.CellState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve previously expanded Appbot cells when restoring upstream capacity. */
@Mixin(targets = "appbot.item.cell.ManaCellInventory", remap = false)
public abstract class AppliedBotanicsCellOverflowMixin {
    @Shadow private long storedMana;
    @Shadow @Final private boolean hasVoidUpgrade;
    @Shadow private long getMaxMana() { throw new AssertionError(); }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$rejectOverflow(AEKey key, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> result) {
        if (amount <= 0) result.setReturnValue(0L);
        else if (key instanceof appbot.ae2.ManaKey && storedMana >= getMaxMana()) result.setReturnValue(hasVoidUpgrade ? amount : 0L);
    }
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$positiveExtraction(AEKey key, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> result) {
        if (amount <= 0) result.setReturnValue(0L);
    }
    @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$overfullStatus(CallbackInfoReturnable<CellState> result) {
        if (storedMana > getMaxMana()) result.setReturnValue(CellState.FULL);
    }
}
