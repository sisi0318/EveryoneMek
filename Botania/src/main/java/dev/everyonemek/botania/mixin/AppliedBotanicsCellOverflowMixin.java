package dev.everyonemek.botania.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.CellState;
import dev.everyonemek.botania.ManaCellCapacity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the old capacity on identifiable existing cells without changing new Appbot items. */
@Mixin(targets = "appbot.item.cell.ManaCellInventory", remap = false)
public abstract class AppliedBotanicsCellOverflowMixin {
    @Shadow private long storedMana;
    @Shadow @Final private boolean hasVoidUpgrade;
    @Shadow @Final private ItemStack i;
    @Shadow @Final private appbot.item.cell.IManaCellItem cellType;
    @Shadow private long getMaxMana() { throw new AssertionError(); }

    @Unique private long botanicalmekanism$standard() { return cellType.getTotalBytes() * appbot.ae2.ManaKeyType.TYPE.getAmountPerByte(); }
    @Unique private long botanicalmekanism$legacy() { return ManaCellCapacity.legacy(cellType.getTotalBytes(), i.getItem() instanceof appbot.item.ManaCellItem); }
    @Unique private long botanicalmekanism$capacity() { return ManaCellCapacity.effective(i, botanicalmekanism$standard(), botanicalmekanism$legacy(), storedMana); }
    @Unique private void botanicalmekanism$remember() { ManaCellCapacity.remember(i, botanicalmekanism$standard(), botanicalmekanism$legacy(), storedMana); }

    @Inject(method = "getMaxMana", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$existingCellCapacity(CallbackInfoReturnable<Long> result) { result.setReturnValue(botanicalmekanism$capacity()); }
    @Inject(method = "getTotalBytes", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$legacyTotalBytes(CallbackInfoReturnable<Long> result) {
        if (botanicalmekanism$capacity() > botanicalmekanism$standard()) result.setReturnValue(botanicalmekanism$capacity() / ManaCellCapacity.LEGACY_DENSITY);
    }
    @Inject(method = "getUsedBytes", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$legacyUsedBytes(CallbackInfoReturnable<Long> result) {
        if (botanicalmekanism$capacity() > botanicalmekanism$standard()) result.setReturnValue(storedMana / ManaCellCapacity.LEGACY_DENSITY + (storedMana % ManaCellCapacity.LEGACY_DENSITY == 0 ? 0 : 1));
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$rejectOverflow(AEKey key, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> result) {
        if (amount <= 0) result.setReturnValue(0L);
        else if (key instanceof appbot.ae2.ManaKey) {
            if (storedMana > getMaxMana()) result.setReturnValue(hasVoidUpgrade ? amount : 0L);
            else if (mode == Actionable.MODULATE) botanicalmekanism$remember();
        }
    }
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$positiveExtraction(AEKey key, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> result) {
        if (amount <= 0) result.setReturnValue(0L);
        else if (key instanceof appbot.ae2.ManaKey && storedMana > 0 && mode == Actionable.MODULATE) botanicalmekanism$remember();
    }
    @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$overfullStatus(CallbackInfoReturnable<CellState> result) {
        if (storedMana > getMaxMana()) result.setReturnValue(CellState.FULL);
    }
}
