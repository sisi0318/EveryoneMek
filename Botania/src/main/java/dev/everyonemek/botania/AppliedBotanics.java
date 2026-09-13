package dev.everyonemek.botania;

import net.minecraft.world.level.block.entity.BlockEntity;

/** Optional boundary: common machines never load Appbot or AE types while they are absent. */
public final class AppliedBotanics {
    public static boolean loaded() { return net.neoforged.fml.ModList.get().isLoaded("appbot"); }
    public static boolean isPool(BlockEntity tile) { return loaded() && tile != null && tile.getClass().getName().equals("appbot.block.FluixPoolBlockEntity"); }
    private AppliedBotanics() { }
}
