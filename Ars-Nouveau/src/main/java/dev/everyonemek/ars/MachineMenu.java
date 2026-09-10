package dev.everyonemek.ars;

import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class MachineMenu extends MekanismTileContainer<SourceMachine> {
    public MachineMenu(int id, Inventory inventory, SourceMachine tile) { super(Content.MENU, id, inventory, tile); }
    @Override protected int getInventoryXOffset() { return 38; }
    @Override protected int getInventoryYOffset() { return 180; }

    private boolean canConfigure(Player player) {
        return !player.level().isClientSide && stillValid(player)
              && IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), tile.getBlockPos(), tile);
    }

    public boolean setRecipeLock(Player player, String value) {
        if (!canConfigure(player)) return false;
        boolean applied = tile.setRecipeLock(value);
        if (!applied) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.arsmekanism.invalid_recipe"), true);
        return applied;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && player.distanceToSqr(tile.getBlockPos().getX() + 0.5,
              tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!canConfigure(player)) return false;
        if (id == 0) { tile.cycleMode(); return true; }
        if (id == 1) { tile.cycleArsSide(); return true; }
        if (id == 2) return tile.setRecipeLock(tile.selectedRecipe());
        if (id == 3) return tile.setRecipeLock("");
        if (id == 4) return tile.depositExperience(player);
        if (id == 7) return tile.requestRitualStart(player);
        return false;
    }
}
