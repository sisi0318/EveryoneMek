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
    public static void migrateLegacy(Controller controller, ValhelsiaContainerBlockEntity<?> target) {
        boolean changed = false;
        if (target != null) {
            ItemStack fuel = target.getStack(2);
            if (!fuel.isEmpty() && controller.storeOutput(fuel)) { fuel.shrink(fuel.getCount()); changed = true; }
            collectOutputs(controller, target);
        }
        for (var source : controller.supplies) {
            if (source.isEmpty()) continue;
            ItemStack remaining = source.getStack();
            if (target != null && acceptsSupply(controller.getLevel(), controller.kind(), 0, remaining)) {
                remaining = target.getItemStackHandler().insertItem(1, remaining, false);
                if (remaining.getCount() != source.getCount()) { source.setStackUnchecked(remaining); changed = true; }
            }
            if (!source.isEmpty() && controller.storeOutput(source.getStack())) { source.setEmpty(); changed = true; }
        }
        if (changed) { controller.markForSave(); if (target != null) target.setChanged(); }
    }
    public static void collectOutputs(Controller controller, ValhelsiaContainerBlockEntity<?> main) {
        boolean changed = false;
        for (int index = 5; index <= 6; index++) {
            ItemStack original = main.getStack(index);
            if (original.isEmpty()) continue;
            ItemStack remaining = original.copy();
            for (int pass = 0; pass < 2; pass++) for (var slot : controller.outputs) {
                if (remaining.isEmpty()) break;
                if (pass == 0 ? slot.isEmpty() : !slot.isEmpty()) continue;
                remaining = slot.insertItem(remaining, Action.EXECUTE, mekanism.api.AutomationType.INTERNAL);
            }
            if (remaining.getCount() != original.getCount()) { main.setStack(index, remaining); changed = true; }
        }
        if (changed) { main.setChanged(); controller.markForSave(); }
    }
    public static boolean canReceive(Controller controller, ClibanoMainBlockEntity main, ItemStack result) {
        return controller.canStoreAll(main.getStack(5), main.getStack(6), result);
    }
    public static final String SOUL_DURATION = "forbiddenmekanism_soul_duration";
    public static int soulDuration(ClibanoMainBlockEntity main) {
        var tag = main.getPersistentData();
        int remaining = ((dev.everyonemek.forbidden.mixin.ClibanoAccess) main).forbiddenmekanism$data().get(0);
        int duration = tag.getInt(SOUL_DURATION);
        if (duration <= 0) {
            duration = ClibanoMainBlockEntity.SOUL_DURATION;
            var enhancer = EnhancerHelper.getEnhancer(main.getLevel().registryAccess(), main.getStack(0)).orElse(null);
            if (enhancer != null) for (var effect : enhancer.getEffects(com.stal111.forbidden_arcanus.common.item.enhancer.EnhancerTarget.CLIBANO).toList())
                if (effect instanceof com.stal111.forbidden_arcanus.common.item.enhancer.effect.MultiplySoulDurationEffect multiplier)
                    duration = multiplier.getModifiedValue(duration);
            duration = Math.max(1, Math.max(duration, remaining));
            if (remaining > 0) { tag.putInt(SOUL_DURATION, duration); main.setChanged(); }
        }
        return Math.max(1, Math.max(duration, remaining));
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
