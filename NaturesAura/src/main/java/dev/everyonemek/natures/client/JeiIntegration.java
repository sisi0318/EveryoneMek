package dev.everyonemek.natures.client;

import de.ellpeck.naturesaura.compat.jei.JEINaturesAuraPlugin;
import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.MachineKind;
import dev.everyonemek.natures.NaturesMekanism;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class JeiIntegration implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "jei"); }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BottlingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.FOREST_RITUAL).asItem()), JEINaturesAuraPlugin.TREE_RITUAL);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.NATURAL_ALTAR).asItem()), JEINaturesAuraPlugin.ALTAR);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.OFFERING).asItem()), JEINaturesAuraPlugin.OFFERING);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.AURA_BOTTLER)), BottlingJeiCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.ANIMAL_SPAWNER)), JEINaturesAuraPlugin.SPAWNER);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(BottlingJeiCategory.TYPE, BottlingJeiCategory.recipes());
        for (MachineKind kind : MachineKind.values())
            registration.addItemStackInfo(new ItemStack(Content.MACHINES.get(kind).asItem()), Component.translatable(kind.getTranslationKey()));
        registration.addItemStackInfo(new ItemStack(Content.SIMULATION_MODULE.get()),
              Component.translatable("tooltip.naturesmekanism.simulation_module"),
              Component.translatable("tooltip.naturesmekanism.simulation_module.install"));
    }
}
