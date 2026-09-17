package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import mezz.jei.api.*;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
@JeiPlugin
public final class JeiIntegration implements IModPlugin {
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath(MekFactory.ID,"jei");}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){if(!MekanismJEI.shouldLoad())return;for(var block:Content.CONTROLLERS.values())r.addRecipeCatalyst(new ItemStack(block),MekanismJEI.recipeType(
          RecipeViewerRecipeType.VANILLA_SMELTING,RecipeViewerRecipeType.ENRICHING,RecipeViewerRecipeType.CRUSHING,RecipeViewerRecipeType.SMELTING,RecipeViewerRecipeType.OXIDIZING,
          RecipeViewerRecipeType.CRYSTALLIZING,RecipeViewerRecipeType.CHEMICAL_INFUSING,RecipeViewerRecipeType.WASHING,RecipeViewerRecipeType.SEPARATING,
          RecipeViewerRecipeType.REACTION,RecipeViewerRecipeType.CONDENSENTRATING,RecipeViewerRecipeType.DECONDENSENTRATING));}
}
