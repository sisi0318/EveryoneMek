package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.compat.jei.ManaJei;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.recipe.ManaInfusionRecipe;
import vazkii.botania.client.integration.jei.ManaPoolRecipeCategory;

@Mixin(value = ManaPoolRecipeCategory.class, remap = false)
public abstract class ManaInfusionJeiMixin {
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lvazkii/botania/api/recipe/ManaInfusionRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("RETURN"))
    private void botanicalmekanism$mana(IRecipeLayoutBuilder builder, ManaInfusionRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        ManaJei.addCost(builder, recipe.getManaToConsume(), 122, 30);
    }
}
