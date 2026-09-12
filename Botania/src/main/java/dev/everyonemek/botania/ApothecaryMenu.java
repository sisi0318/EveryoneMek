package dev.everyonemek.botania;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class ApothecaryMenu extends MekanismTileContainer<MechanicalApothecary> {
    public ApothecaryMenu(int id, Inventory inventory, MechanicalApothecary tile) { super(ApothecaryContent.MENU, id, inventory, tile); }
    @Override protected int getInventoryXOffset() { return 29; }
    @Override protected int getInventoryYOffset() { return 156; }
    @Override public boolean stillValid(Player player) { return super.stillValid(player) && player.distanceToSqr(tile.getBlockPos().getCenter()) <= 64; }
}
