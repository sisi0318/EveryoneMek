package dev.everyonemek.natures;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.api.security.IBlockSecurityUtils;
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

    public VirtualInventoryContainerSlot getGoldModuleSlot() {
        return slots.stream().filter(slot -> slot instanceof VirtualInventoryContainerSlot virtual
                    && virtual.getInventorySlot() == tile.goldModuleSlot())
              .map(slot -> (VirtualInventoryContainerSlot) slot).findFirst().orElseThrow();
    }

    public VirtualInventoryContainerSlot getSimulationModuleSlot() {
        return slots.stream().filter(slot -> slot instanceof VirtualInventoryContainerSlot virtual
                    && virtual.getInventorySlot() == tile.simulationModuleSlot())
              .map(slot -> (VirtualInventoryContainerSlot) slot).findFirst().orElseThrow();
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && player.distanceToSqr(tile.getBlockPos().getX() + 0.5,
              tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player) || !IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), tile.getBlockPos(), tile)) return false;
        if (id == 0 && tile.kind() == MachineKind.AURA_GENERATOR) {
            if (!player.level().isClientSide) tile.toggleEnvironmentOutput();
            return true;
        }
        if (id == 1 && tile.kind() == MachineKind.AURA_BOTTLER) {
            if (!player.level().isClientSide) tile.cycleBottlingMode();
            return true;
        }
        return false;
    }
}
