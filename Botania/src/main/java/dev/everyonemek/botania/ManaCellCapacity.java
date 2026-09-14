package dev.everyonemek.botania;

import net.minecraft.world.item.ItemStack;

/** Per-item compatibility for cells that were already filled beyond the new standard. */
public final class ManaCellCapacity {
    public static final int LEGACY_DENSITY = 8000;
    public static long legacy(long standardBytes, boolean binaryTiers) {
        return (binaryTiers ? standardBytes / 1000 * 1024 : standardBytes) * LEGACY_DENSITY;
    }
    public static long effective(ItemStack stack, long standard, long legacy, long stored) {
        if (legacy > standard && (stored > standard || stack.getOrDefault(Content.LEGACY_MANA_CAPACITY.get(), 0L) == legacy)) return legacy;
        return standard;
    }
    public static void remember(ItemStack stack, long standard, long legacy, long stored) {
        if (effective(stack, standard, legacy, stored) > standard) stack.set(Content.LEGACY_MANA_CAPACITY.get(), legacy);
    }
    private ManaCellCapacity() { }
}
