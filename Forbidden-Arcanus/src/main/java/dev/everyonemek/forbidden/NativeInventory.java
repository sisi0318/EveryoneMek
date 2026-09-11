package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoFireType;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.item.enhancer.EnhancerHelper;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import mekanism.api.Action;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.valhelsia.valhelsia_core.api.common.block.entity.neoforge.ValhelsiaContainerBlockEntity;

public final class NativeInventory {
    public static boolean acceptsSupply(Level level, MachineKind kind, int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!kind.forge()) return ClibanoFireType.fromItem(stack) != ClibanoFireType.FIRE;
        if (level == null) return true;
        return level.registryAccess().registryOrThrow(FARegistries.FORGE_INPUT).stream()
              .anyMatch(input -> input.canInput(EssenceType.values()[slot], stack));
    }
    public static boolean supply(Controller controller, ValhelsiaContainerBlockEntity<?> target) {
        boolean changed = false;
        for (int i = 0; i < controller.supplies.size(); i++) {
            int slot = 1;
            ItemStack present = target.getStack(slot);
            if (!present.isEmpty() && !acceptsSupply(controller.getLevel(), controller.kind(), i, present)) {
                if (!controller.storeOutput(present)) continue;
                target.setStack(slot, ItemStack.EMPTY); changed = true;
            }
            var source = controller.supplies.get(i);
            if (source.isEmpty() || !acceptsSupply(controller.getLevel(), controller.kind(), i, source.getStack())) continue;
            ItemStack offered = source.getStack().copyWithCount(Math.min(16, source.getCount()));
            ItemStack remainder = target.getItemStackHandler().insertItem(slot, offered, false);
            int moved = offered.getCount() - remainder.getCount();
            if (moved > 0) { source.shrinkStack(moved, Action.EXECUTE); changed = true; }
        }
        return changed;
    }
    public static void recoverFuel(Controller controller, ValhelsiaContainerBlockEntity<?> target) {
        boolean changed = false;
        ItemStack fuel = target.getStack(2);
        if (!fuel.isEmpty() && controller.storeOutput(fuel)) {
            // Like native fuel consumption, changing the count preserves its existing burn-duration metadata.
            fuel.shrink(fuel.getCount()); changed = true;
        }
        for (var source : controller.supplies) {
            if (!source.isEmpty() && !acceptsSupply(controller.getLevel(), controller.kind(), 0, source.getStack()) && controller.storeOutput(source.getStack())) {
                source.setStack(ItemStack.EMPTY); changed = true;
            }
        }
        if (changed) { controller.markForSave(); target.setChanged(); }
    }
    public static boolean acceptsNative(Controller controller, int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0) return EnhancerHelper.getEnhancer(controller.getLevel().registryAccess(), stack).isPresent();
        if (slot == 1) return acceptsSupply(controller.getLevel(), controller.kind(), 1, stack);
        return slot == 3 || slot == 4;
    }
    public static boolean editable(Controller controller, ValhelsiaContainerBlockEntity<?> block, int slot) {
        return block != null && !controller.kind().forge();
    }
    /** Menu-only facade. The controller's persistent inventory never contains these native items. */
    public static ItemStackHandler menu(Controller controller) {
        int size = 7;
        if (controller.getLevel().isClientSide) return new ItemStackHandler(size);
        return new ItemStackHandler(size) {
            @Override public ItemStack getStackInSlot(int slot) {
                var block = controller.binding.resolve(); return block == null ? ItemStack.EMPTY : block.getStack(slot);
            }
            @Override public void setStackInSlot(int slot, ItemStack stack) {
                var block = controller.binding.resolve();
                if (editable(controller, block, slot)) { block.setStack(slot, stack); block.setChanged(); }
            }
            @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return editable(controller, controller.binding.resolve(), slot) && acceptsNative(controller, slot, stack);
            }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) return stack;
                var block = controller.binding.resolve(); ItemStack old = block.getStack(slot);
                if (!old.isEmpty() && !ItemStack.isSameItemSameComponents(old, stack)) return stack;
                int moved = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()) - old.getCount());
                if (moved <= 0) return stack;
                if (!simulate) { block.setStack(slot, stack.copyWithCount(old.getCount() + moved)); block.setChanged(); }
                return stack.copyWithCount(stack.getCount() - moved);
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                var block = controller.binding.resolve();
                if (!editable(controller, block, slot) || amount <= 0) return ItemStack.EMPTY;
                ItemStack old = block.getStack(slot); int moved = Math.min(old.getCount(), amount);
                ItemStack result = old.copyWithCount(moved);
                if (!simulate && moved > 0) { block.setStack(slot, old.copyWithCount(old.getCount() - moved)); block.setChanged(); }
                return result;
            }
        };
    }
    private NativeInventory() { }
}
