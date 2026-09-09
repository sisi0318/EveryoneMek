package dev.everyonemek.natures;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class MachineMenu extends MekanismTileContainer<AuraMachine> {
    public MachineMenu(int id, Inventory inventory, AuraMachine tile) {
        super(Content.MENU, id, inventory, tile);
    }

    @Override
    protected int getInventoryXOffset() { return 38; }

    @Override
    protected int getInventoryYOffset() { return 136; }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && player.distanceToSqr(tile.getBlockPos().getX() + 0.5,
              tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !stillValid(player) || tile.kind() != MachineKind.AURA_GENERATOR) return false;
        if (!player.level().isClientSide) tile.toggleEnvironmentOutput();
        return true;
    }
}
