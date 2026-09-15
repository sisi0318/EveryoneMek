package dev.everyonemek.botania;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * A registered flower rule. Evaluation must be deterministic and must not change the world or
 * caller-owned stacks. Return a copied flower containing the state to apply after a successful cycle.
 * An item rule consumes one item; a fluid rule declares its per-cycle amount. Fixed recipes handle
 * multiple ingredients and mixed item/fluid inputs without a custom rule.
 */
public interface GreenhouseFlowerRule {
    default boolean acceptsItem(ItemStack stack) { return false; }
    default boolean acceptsFluid(FluidStack stack) { return false; }
    default int fluidAmount() { return 0; }
    @Nullable GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level);

    /** Hooks receive detached copies. They must retain the flower item and its count. */
    default ItemStack prepare(ItemStack flower, Level level) { return flower; }
    default ItemStack onBlocked(ItemStack flower, Level level) { return flower; }
    default ItemStack previewFlower(ItemStack flower, ItemStack material, Level level) { return flower; }
    default Component statusInfo(ItemStack flower, Level level) { return Component.empty(); }
    default Component recipeNote() { return Component.empty(); }
    /** Variable yields must not be offered as fixed AE pattern outputs. */
    default boolean variableOutput() { return false; }
}
