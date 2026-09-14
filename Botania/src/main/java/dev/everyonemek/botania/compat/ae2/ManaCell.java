package dev.everyonemek.botania.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.cells.*;
import dev.everyonemek.botania.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record ManaCell(ItemStack stack, ISaveProvider host) implements StorageCell {
    public static final ICellHandler HANDLER = new ICellHandler() {
        @Override public boolean isCell(ItemStack stack) { return ManaStorageItem.isCell(stack) && stack.getCount() == 1; }
        @Override public StorageCell getCellInventory(ItemStack stack, ISaveProvider host) { return isCell(stack) ? new ManaCell(stack, host) : null; }
    };
    @Override public Component getDescription() { return stack.getHoverName(); }
    @Override public long insert(AEKey key, long amount, Actionable action, IActionSource source) {
        if (key != ManaKeys.current() || amount <= 0 || !HANDLER.isCell(stack)) return 0;
        long moved = Math.min(amount, Math.max(0, ManaStorageItem.capacity(stack) - ManaStorageItem.stored(stack)));
        if (action == Actionable.MODULATE && moved > 0) { ManaStorageItem.store(stack, ManaStorageItem.stored(stack) + moved); changed(); }
        return moved;
    }
    @Override public long extract(AEKey key, long amount, Actionable action, IActionSource source) {
        if (key != ManaKeys.current() || amount <= 0 || !HANDLER.isCell(stack)) return 0;
        long moved = Math.min(amount, ManaStorageItem.stored(stack));
        if (action == Actionable.MODULATE && moved > 0) { ManaStorageItem.store(stack, ManaStorageItem.stored(stack) - moved); changed(); }
        return moved;
    }
    @Override public void getAvailableStacks(KeyCounter out) { if (HANDLER.isCell(stack) && ManaStorageItem.stored(stack) > 0) out.add(ManaKeys.current(), ManaStorageItem.stored(stack)); }
    @Override public CellState getStatus() {
        long amount = ManaStorageItem.stored(stack), capacity = ManaStorageItem.capacity(stack);
        return amount == 0 ? CellState.EMPTY : amount >= capacity ? CellState.FULL : amount > capacity / 2 ? CellState.TYPES_FULL : CellState.NOT_EMPTY;
    }
    @Override public double getIdleDrain() { return ManaStorageItem.idleDrain(stack); }
    @Override public boolean canFitInsideCell() { return ManaStorageItem.stored(stack) == 0; }
    private void changed() { if (host != null) host.saveChanges(); }
    @Override public void persist() { /* Mutations write the stack component immediately. */ }
}
