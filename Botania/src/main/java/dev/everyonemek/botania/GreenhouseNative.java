package dev.everyonemek.botania;

import java.util.List;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Compatibility facade for the original greenhouse API; dispatch now uses registered rules. */
public final class GreenhouseNative {
    public record Result(int mana, int ticks, int cooldown, ItemStack flower, int preference) { }
    public static boolean accepts(String formula, ItemStack stack) {
        var rule = GreenhouseRules.get(formula); return rule != null && !stack.isEmpty() && rule.acceptsItem(stack);
    }
    public static Result resolve(String formula, ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
        var rule = GreenhouseRules.get(formula); return rule == null ? null : rule.resolve(flower, material, fluid, level);
    }
    public static List<DyeColor> colors(ItemStack flower, Level level) { return BuiltinGreenhouseRules.colors(flower, level); }
    public static ItemStack expectedWool(ItemStack flower, Level level) { return BuiltinGreenhouseRules.expectedWool(flower, level); }
    public static dev.everyonemek.botania.mixin.CultivatedFluidFlowerAccess thermalily() { return BuiltinGreenhouseRules.thermalily(); }
    private GreenhouseNative() { }
}
