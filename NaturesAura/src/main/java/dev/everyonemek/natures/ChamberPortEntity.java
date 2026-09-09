package dev.everyonemek.natures;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.integration.energy.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** A forwarding port with no independent inventory. Every access revalidates its controller and shell. */
public final class ChamberPortEntity extends BlockEntity {
    private BlockPos controllerPos;
    private long nextSearch = Long.MIN_VALUE;
    private boolean wasFormed;
    public ChamberPortEntity(BlockPos pos, BlockState state) { super(Content.CHAMBER_PORT_TILE.get(), pos, state); }
    public DataType itemMode() { return ChamberPortBlock.itemMode(getBlockState()); }
    public boolean output() { return itemMode().canOutput(); }
    private boolean acceptsSupplies() { return itemMode() == DataType.INPUT || itemMode() == DataType.INPUT_OUTPUT || itemMode() == DataType.ENERGY; }
    public AuraMachine controller() {
        if (level == null || isRemoved() || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return null;
        if (controllerPos != null && level.hasChunkAt(controllerPos) && level.getBlockEntity(controllerPos) instanceof AuraMachine m
              && m.chamber() != null && m.chamber().containsShell(worldPosition)) return m.chamber().formed() ? m : null;
        controllerPos = null;
        if (level.getGameTime() < nextSearch) return null;
        nextSearch = level.getGameTime() + 10;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2))) {
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof AuraMachine m && m.chamber() != null
                  && m.chamber().containsShell(worldPosition) && m.chamber().formed()) {
                controllerPos = pos.immutable(); return m;
            }
        }
        return null;
    }
    private IInventorySlot slot(int index) {
        AuraMachine m = controller();
        int offset = itemMode() == DataType.OUTPUT ? 2 : itemMode() == DataType.ENERGY ? 6 : 0;
        return m == null || index < 0 || index >= items.getSlots() ? null : m.getInventorySlots(null).get(index + offset);
    }
    public final IItemHandler items = new IItemHandler() {
        @Override public int getSlots() { return switch (itemMode()) { case INPUT -> 2; case OUTPUT -> 4; case INPUT_OUTPUT -> 6; case ENERGY -> 1; default -> 0; }; }
        @Override public ItemStack getStackInSlot(int index) { var slot = slot(index); return slot == null ? ItemStack.EMPTY : slot.getStack(); }
        @Override public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            var slot = slot(index);
            return !acceptsSupplies() || slot == null ? stack : slot.insertItem(stack, simulate ? Action.SIMULATE : Action.EXECUTE, AutomationType.EXTERNAL);
        }
        @Override public ItemStack extractItem(int index, int amount, boolean simulate) {
            var slot = slot(index);
            return !output() && itemMode() != DataType.ENERGY || slot == null ? ItemStack.EMPTY : slot.extractItem(amount, simulate ? Action.SIMULATE : Action.EXECUTE, AutomationType.EXTERNAL);
        }
        @Override public int getSlotLimit(int index) { var slot = slot(index); return slot == null ? 0 : slot.getLimit(ItemStack.EMPTY); }
        @Override public boolean isItemValid(int index, ItemStack stack) { var slot = slot(index); return acceptsSupplies() && slot != null && slot.isItemValid(stack); }
    };
    private AuraMachine energyController(int container) { return container == 0 && acceptsSupplies() ? controller() : null; }
    public final IStrictEnergyHandler strictEnergy = new IStrictEnergyHandler() {
        @Override public int getEnergyContainerCount() { return acceptsSupplies() ? 1 : 0; }
        @Override public long getEnergy(int container) { var m = energyController(container); return m == null ? 0 : m.energy().getEnergy(); }
        @Override public long getMaxEnergy(int container) { var m = energyController(container); return m == null ? 0 : m.energy().getMaxEnergy(); }
        @Override public long getNeededEnergy(int container) { var m = energyController(container); return m == null ? 0 : m.energy().getNeeded(); }
        @Override public void setEnergy(int container, long amount) { throw new UnsupportedOperationException("Chamber port is an insertion interface"); }
        @Override public long insertEnergy(int container, long amount, Action action) {
            var m = energyController(container); return m == null ? amount : m.energy().insert(amount, action, AutomationType.EXTERNAL);
        }
        @Override public long extractEnergy(int container, long amount, Action action) { return 0; }
    };
    // Use Mek's conversion/rounding rules; its null-sided block capability is intentionally read-only.
    public final IEnergyStorage energy = new ForgeEnergyIntegration(strictEnergy) {
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return energyController(0) != null; }
    };
    public final IChemicalHandler chemicals = new IChemicalHandler() {
        @Override public int getChemicalTanks() { return acceptsSupplies() ? 1 : 0; }
        @Override public ChemicalStack getChemicalInTank(int tank) {
            AuraMachine m = controller(); return tank != 0 || !acceptsSupplies() || m == null ? ChemicalStack.EMPTY : m.auraTank().getStack();
        }
        @Override public void setChemicalInTank(int tank, ChemicalStack stack) { throw new UnsupportedOperationException("Chamber port is an insertion interface"); }
        @Override public long getChemicalTankCapacity(int tank) { return tank == 0 && acceptsSupplies() ? AuraMachine.AURA_CAPACITY : 0; }
        @Override public boolean isValid(int tank, ChemicalStack stack) { return tank == 0 && acceptsSupplies() && stack.is(Content.AURA); }
        @Override public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
            AuraMachine m = controller(); return m == null || !isValid(tank, stack) ? stack : m.auraTank().insert(stack, action, AutomationType.EXTERNAL);
        }
        @Override public ChemicalStack extractChemical(int tank, long amount, Action action) { return ChemicalStack.EMPTY; }
    };
    public static void tick(Level level, BlockPos pos, BlockState state, ChamberPortEntity port) {
        if (level.getGameTime() % 10 != 0) return;
        AuraMachine m = port.controller();
        if (port.wasFormed != (m != null)) {
            port.wasFormed = m != null;
            level.invalidateCapabilities(pos);
            level.updateNeighborsAt(pos, state.getBlock());
        }
        if (m == null || !port.output() || !m.getConfig().getConfig(TransmissionType.ITEM).isEjecting()) return;
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (!level.hasChunkAt(next) || m.chamber().containsShell(next) || next.equals(m.chamber().center())) continue;
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, next, side.getOpposite());
            if (target == null) continue;
            for (int slot = 0; slot < port.items.getSlots(); slot++) {
                var stack = port.items.extractItem(slot, 64, true);
                if (stack.isEmpty()) continue;
                var remaining = ItemHandlerHelper.insertItemStacked(target, stack, false);
                port.items.extractItem(slot, stack.getCount() - remaining.getCount(), false);
            }
        }
    }
}
