package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.compat.jei.ManaJei;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.client.integration.jei.TerrestrialAgglomerationRecipeCategory;

@Mixin(value = TerrestrialAgglomerationRecipeCategory.class, remap = false)
public abstract class TerraJeiMixin {
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lvazkii/botania/api/recipe/TerrestrialAgglomerationRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("RETURN"))
    private void botanicalmekanism$mana(IRecipeLayoutBuilder builder, TerrestrialAgglomerationRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        ManaJei.addCost(builder, recipe.getMana(), 96, 107);
    }
}
