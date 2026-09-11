package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import mekanism.common.lib.inventory.TransitRequest;

public final class ClibanoPorts {
    public static void register(RegisterCapabilitiesEvent event) {
        // Return guards even before assembly: a previously cached port becomes usable on repair.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Content.CLIBANO_PORT_TILE.get(), Items::new);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Content.CLIBANO_PORT_TILE.get(), Energy::new);
    }
    public static Controller controller(ClibanoPort port, Direction outward) {
        var level = port.getLevel();
        if (outward == null || level == null || level.isClientSide || port.isRemoved() || !level.hasChunkAt(port.getBlockPos())
              || level.getBlockEntity(port.getBlockPos()) != port) return null;
        BlockPos center = port.getBlockPos().relative(outward.getOpposite());
        if (!level.hasChunkAt(center) || !(level.getBlockEntity(center) instanceof ClibanoMainBlockEntity main)) return null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var pos = center.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Controller controller
                  && controller.binding.embedded && controller.binding.resolve() == main) return controller;
        }
        return null;
    }
    private record Items(ClibanoPort port, Direction side) implements IItemHandler {
        private IItemHandler target() {
            var controller = controller(port, side);
            return controller == null ? null : port.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, controller.getBlockPos(), side);
        }
        @Override public int getSlots() { var target = target(); return target == null ? 0 : target.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) {
            var target = target(); return target == null || slot < 0 || slot >= target.getSlots() ? ItemStack.EMPTY : target.getStackInSlot(slot).copy();
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var target = target(); return target == null || slot < 0 || slot >= target.getSlots() ? stack : target.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            var target = target(); return target == null || slot < 0 || slot >= target.getSlots() ? ItemStack.EMPTY : target.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { var target = target(); return target == null || slot < 0 || slot >= target.getSlots() ? 0 : target.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            var target = target(); return target != null && slot >= 0 && slot < target.getSlots() && target.isItemValid(slot, stack);
        }
    }
    private record Energy(ClibanoPort port, Direction side) implements IEnergyStorage {
        private IEnergyStorage target() {
            var controller = controller(port, side);
            // Use Mek's registered FE integration, including its configured conversion and rounding.
            return controller == null ? null : port.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, controller.getBlockPos(), side);
        }
        @Override public int receiveEnergy(int amount, boolean simulate) { var target = target(); return target == null ? 0 : target.receiveEnergy(amount, simulate); }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { var target = target(); return target == null ? 0 : target.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { var target = target(); return target == null ? 0 : target.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { var target = target(); return target != null && target.canReceive(); }
    }
    public static void eject(Controller controller) {
        if (!controller.binding.embedded || controller.getLevel().getGameTime() % 10 != 0
              || !controller.getConfig().getConfig(TransmissionType.ITEM).isEjecting()
              || !(controller.binding.resolve() instanceof ClibanoMainBlockEntity main)) return;
        var level = controller.getLevel();
        for (Direction side : Direction.values()) {
            BlockPos wall = main.getBlockPos().relative(side), destination = wall.relative(side);
            if (!level.hasChunkAt(wall) || !level.hasChunkAt(destination) || !(level.getBlockEntity(wall) instanceof ClibanoPort port)) continue;
            var source = new Items(port, side);
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, destination, side.getOpposite());
            if (target == null) continue;
            // Mek's transfer request supports both ordinary handlers and transporter routing/color.
            var request = TransitRequest.anyItem(source, 64);
            if (!request.isEmpty()) {
                var response = request.eject(port, target, 0, transporter -> controller.getEjector().getOutputColor());
                if (!response.isEmpty()) response.useAll();
            }
        }
    }
    private ClibanoPorts() { }
}
