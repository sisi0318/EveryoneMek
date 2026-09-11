package dev.everyonemek.forbidden;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** A capability view of the furnace's single soul slot, deliberately excluded from controller persistence. */
public record ClibanoSoulSlot(Controller controller) implements IInventorySlot {
    @Override public ItemStack getStack() {
        var main = controller.binding.resolve(); return main == null ? ItemStack.EMPTY : main.getStack(1);
    }
    @Override public void setStack(ItemStack stack) {
        var main = controller.binding.resolve();
        if (main != null && (stack.isEmpty() || isItemValid(stack))) {
            main.setStack(1, stack.copy()); main.setChanged(); controller.markForSave();
        }
    }
    @Override public ItemStack insertItem(ItemStack stack, Action action, AutomationType automation) {
        return controller.getLevel() == null ? stack : NativeInventory.menu(controller).insertItem(1, stack, action == Action.SIMULATE);
    }
    @Override public ItemStack extractItem(int amount, Action action, AutomationType automation) {
        return automation == AutomationType.EXTERNAL || controller.getLevel() == null ? ItemStack.EMPTY
              : NativeInventory.menu(controller).extractItem(1, amount, action == Action.SIMULATE);
    }
    @Override public int getLimit(ItemStack stack) {
        // Mek queries the slot's capacity with EMPTY, whose item stack limit is only one.
        return stack.isEmpty() ? 64 : Math.min(64, stack.getMaxStackSize());
    }
    @Override public boolean isItemValid(ItemStack stack) { return NativeInventory.acceptsSupply(controller.getLevel(), MachineKind.CLIBANO, 0, stack); }
    @Override public void onContentsChanged() {
        var main = controller.binding.resolve();
        if (main != null) { main.setStack(1, main.getStack(1).copy()); main.setChanged(); }
        controller.markForSave();
    }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) { return new CompoundTag(); }
    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) { }
}
