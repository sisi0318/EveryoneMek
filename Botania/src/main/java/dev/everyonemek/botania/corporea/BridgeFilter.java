package dev.everyonemek.botania.corporea;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Samples are descriptions, never an inventory or a source of drops. */
public final class BridgeFilter {
    public static final int SIZE = 63;
    public final ItemStack[] samples = new ItemStack[SIZE];
    public int mode; // all, allow samples, deny samples
    public boolean exact = true;
    public BridgeFilter() { java.util.Arrays.fill(samples, ItemStack.EMPTY); }
    public boolean allows(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (mode == 0) return true;
        boolean matches = java.util.Arrays.stream(samples).anyMatch(sample -> !sample.isEmpty()
              && (exact ? ItemStack.isSameItemSameComponents(sample, stack) : ItemStack.isSameItem(sample, stack)));
        return mode == 1 ? matches : !matches;
    }
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("filter_mode", mode); tag.putBoolean("filter_exact", exact);
        var list = new ListTag();
        for (var sample : samples) list.add(sample.saveOptional(registries));
        tag.put("filter_samples", list);
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mode = Math.clamp(tag.getInt("filter_mode"), 0, 2);
        exact = !tag.contains("filter_exact") || tag.getBoolean("filter_exact");
        var list = tag.getList("filter_samples", Tag.TAG_COMPOUND);
        for (int i = 0; i < SIZE; i++) samples[i] = i < list.size()
              ? ItemStack.parseOptional(registries, list.getCompound(i)).copyWithCount(1) : ItemStack.EMPTY;
    }
}
