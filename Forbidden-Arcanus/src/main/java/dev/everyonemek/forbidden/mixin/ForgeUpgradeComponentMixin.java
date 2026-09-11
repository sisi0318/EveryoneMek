package dev.everyonemek.forbidden.mixin;

import dev.everyonemek.forbidden.Content;
import dev.everyonemek.forbidden.Controller;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.inventory.slot.UpgradeInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends only this forge's existing Mek installation transaction and progress bar. */
@Mixin(value = TileComponentUpgrade.class, remap = false)
public abstract class ForgeUpgradeComponentMixin {
    @Shadow @Final private TileEntityMekanism tile;
    @Shadow @Final private UpgradeInventorySlot upgradeSlot;
    @Shadow private int upgradeTicks;
    @Unique private ItemStack forbiddenmekanism$previousInput = ItemStack.EMPTY;

    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void forbiddenmekanism$installResource(CallbackInfo ci) {
        if (!(tile instanceof Controller machine) || !machine.kind().forge()) return;
        ItemStack input = upgradeSlot.getStack();
        int resource = Content.resourceModuleIndex(input);
        if ((resource >= 0 || Content.resourceModuleIndex(forbiddenmekanism$previousInput) >= 0)
              && !ItemStack.isSameItemSameComponents(input, forbiddenmekanism$previousInput)) upgradeTicks = 0;
        forbiddenmekanism$previousInput = input.copyWithCount(1);
        if (resource < 0) return;
        ci.cancel();
        var destination = machine.resourceModules.get(resource);
        int accepted = input.getCount() - destination.insertItem(input, Action.SIMULATE, AutomationType.INTERNAL).getCount();
        if (accepted == 0) { upgradeTicks = 0; return; }
        if (upgradeTicks < 20) { upgradeTicks++; return; }
        destination.insertItem(input.copyWithCount(accepted), Action.EXECUTE, AutomationType.INTERNAL);
        upgradeSlot.shrinkStack(accepted, Action.EXECUTE);
        upgradeTicks = 0;
        machine.markForSave();
    }
}
