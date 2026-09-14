package dev.everyonemek.botania.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.CellState;
import dev.everyonemek.botania.ManaCellCapacity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Expand storage and its byte display for all Appbot cells, without changing ME transfer units. */
@Mixin(targets = "appbot.item.cell.ManaCellInventory", remap = false)
public abstract class AppliedBotanicsCellOverflowMixin {
    @Shadow private long storedMana;
    @Shadow @Final private boolean hasVoidUpgrade;
    @Shadow @Final private appbot.item.cell.IManaCellItem cellType;
    @Shadow private long getMaxMana() { throw new AssertionError(); }

    @Inject(method = "getMaxMana", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$expandedCapacity(CallbackInfoReturnable<Long> result) { result.setReturnValue(ManaCellCapacity.maximum(cellType.getTotalBytes())); }
    @Inject(method = "getTotalBytes", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$expandedTotalBytes(CallbackInfoReturnable<Long> result) { result.setReturnValue(ManaCellCapacity.bytes(cellType.getTotalBytes())); }
    @Inject(method = "getUsedBytes", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$expandedUsedBytes(CallbackInfoReturnable<Long> result) { result.setReturnValue(ManaCellCapacity.usedBytes(storedMana)); }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$rejectOverflow(AEKey key, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> result) {
        if (amount <= 0) result.setReturnValue(0L);
        else if (key instanceof appbot.ae2.ManaKey && storedMana > getMaxMana()) result.setReturnValue(hasVoidUpgrade ? amount : 0L);
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
