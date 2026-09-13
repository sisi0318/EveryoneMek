package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.compat.jei.ManaJei;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.recipe.BotanicalBreweryRecipe;
import vazkii.botania.client.integration.jei.BreweryRecipeCategory;

@Mixin(value = BreweryRecipeCategory.class, remap = false)
public abstract class BrewJeiMixin {
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lvazkii/botania/api/recipe/BotanicalBreweryRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("RETURN"))
    private void botanicalmekanism$mana(IRecipeLayoutBuilder builder, BotanicalBreweryRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        ManaJei.addBrewSearch(builder, recipe);
    }
}
