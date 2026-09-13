package dev.everyonemek.botania.corporea;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import vazkii.botania.api.corporea.CorporeaRequest;

/** Optional integration boundary: common blocks never expose AE2 classes in their signatures. */
public interface BridgeBackend {
    default void loaded() { }
    default void tick() { }
    default void destroy() { }
    default void removed() { }
    default void load(CompoundTag tag) { }
    default CompoundTag save() { return new CompoundTag(); }
    default boolean connected() { return false; }
    default void settingsChanged() { }
    default List<ItemStack> request(CorporeaRequest request, boolean execute) { return List.of(); }
    default void requestCraft(vazkii.botania.api.corporea.CorporeaRequestMatcher matcher, int missing) { }
    default void describe(CompoundTag state) { state.putString("status", "missing_ae2"); }
}
