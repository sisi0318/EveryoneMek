package dev.everyonemek.ars.client;

import com.hollingsworth.arsnouveau.client.jei.JEIArsNouveauPlugin;
import dev.everyonemek.ars.ArsMekanism;
import dev.everyonemek.ars.Content;
import dev.everyonemek.ars.MachineKind;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class JeiIntegration implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(ArsMekanism.ID, "jei"); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.IMBUEMENT_CHAMBER)),
              JEIArsNouveauPlugin.IMBUEMENT_RECIPE_TYPE.get());
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.ENCHANTING_APPARATUS)),
              JEIArsNouveauPlugin.ENCHANTING_APP_RECIPE_TYPE.get(), JEIArsNouveauPlugin.ENCHANTING_RECIPE_TYPE.get());
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.MAGIC_CRUSHER)), JEIArsNouveauPlugin.CRUSH_RECIPE_TYPE.get());
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.GLYPH_SCRIBE)), JEIArsNouveauPlugin.GLYPH_RECIPE_TYPE.get());
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        for (MachineKind kind : MachineKind.values()) registration.addItemStackInfo(new ItemStack(Content.MACHINES.get(kind)),
              Component.translatable(kind.getTranslationKey()));
        registration.addItemStackInfo(new ItemStack(Content.FE_SOURCELINK_ITEM.get()), Component.translatable("description.arsmekanism.fe_sourcelink"));
    }
}
