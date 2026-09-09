package dev.everyonemek.natures.client;

import de.ellpeck.naturesaura.compat.jei.JEINaturesAuraPlugin;
import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.MachineKind;
import dev.everyonemek.natures.NaturesMekanism;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class JeiIntegration implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "jei"); }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.FOREST_RITUAL).asItem()), JEINaturesAuraPlugin.TREE_RITUAL);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.NATURAL_ALTAR).asItem()), JEINaturesAuraPlugin.ALTAR);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.OFFERING).asItem()), JEINaturesAuraPlugin.OFFERING);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (MachineKind kind : MachineKind.values())
            registration.addItemStackInfo(new ItemStack(Content.MACHINES.get(kind).asItem()), Component.translatable(kind.getTranslationKey()));
    }
}
