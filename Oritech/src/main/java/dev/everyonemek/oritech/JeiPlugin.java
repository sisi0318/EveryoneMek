package dev.everyonemek.oritech;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.constants.RecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.init.recipes.*;

@mezz.jei.api.JeiPlugin
public final class JeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid(){return Content.id("jei");}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){
        var stack=new ItemStack(Content.ITEM.get());r.addRecipeCatalyst(stack,RecipeTypes.SMELTING);
        for(var p:Profiles.values())if(p.recipes!=null){var id=p.recipes.getIdentifier();r.addRecipeCatalyst(stack,mezz.jei.api.recipe.RecipeType.create(id.getNamespace(),id.getPath(),OritechRecipe.class));}
        var id=RecipeContent.CENTRIFUGE_FLUID.getIdentifier();r.addRecipeCatalyst(stack,mezz.jei.api.recipe.RecipeType.create(id.getNamespace(),id.getPath(),OritechRecipe.class));
    }
}
