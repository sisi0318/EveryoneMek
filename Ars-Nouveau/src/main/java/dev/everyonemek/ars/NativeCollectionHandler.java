package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.block.tile.DrygmyTile;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.hollingsworth.arsnouveau.common.block.tile.WhirlisprigTile;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Ars deposits through a null side; limit that entry point to the configured front collection slots. */
final class NativeCollectionHandler implements IItemHandler {
    private final SourceMachine machine;
    NativeCollectionHandler(SourceMachine machine) { this.machine = machine; }
    private int first() { return machine.kind() == MachineKind.RITUAL_CONTROLLER ? 2 : 1; }
    @Override public int getSlots() { return machine.inputs.size() - first(); }

    private IInventorySlot slot(int index) {
        var level = machine.getLevel();
        if (index < 0 || index >= getSlots() || level == null || machine.internalDrygmy() || machine.isRemoved() || !level.hasChunkAt(machine.getBlockPos())
              || level.getBlockEntity(machine.getBlockPos()) != machine) return null;
        var direction = machine.getDirection();
        var targetPos = machine.getBlockPos().relative(direction);
        if (!level.hasChunkAt(targetPos)) return null;
        var target = level.getBlockEntity(targetPos);
        boolean valid = switch (machine.kind()) {
            case DRYGMY_STATION -> target instanceof DrygmyTile;
            case WHIRLISPRIG_STATION -> target instanceof WhirlisprigTile;
            case RITUAL_CONTROLLER -> target instanceof RitualBrazierTile;
            default -> false;
        };
        if (!valid) return null;
        IInventorySlot slot = machine.inputs.get(first() + index);
        return machine.getInventorySlots(direction).contains(slot) ? slot : null;
    }

    @Override public ItemStack getStackInSlot(int index) {
        var slot = slot(index); return slot == null ? ItemStack.EMPTY : slot.getStack().copy();
    }
    @Override public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        var slot = slot(index);
        return slot == null ? stack : slot.insertItem(stack, simulate ? Action.SIMULATE : Action.EXECUTE, AutomationType.EXTERNAL);
    }
    @Override public ItemStack extractItem(int index, int amount, boolean simulate) { return ItemStack.EMPTY; }
    @Override public int getSlotLimit(int index) { return 64; }
    @Override public boolean isItemValid(int index, ItemStack stack) {
        var slot = slot(index); return slot != null && slot.isItemValid(stack) && WorldControllers.acceptsCollected(machine, stack);
    }
}
