package dev.everyonemek.forbidden.client;

import com.stal111.forbidden_arcanus.common.integration.ForbiddenArcanusJEIPlugin;
import dev.everyonemek.forbidden.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class JeiIntegration implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(ForbiddenMekanism.ID, "jei"); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.FORGE)),
              ForbiddenArcanusJEIPlugin.HEPHAESTUS_SMITHING);
        registration.addRecipeCatalyst(new ItemStack(Content.MACHINES.get(MachineKind.CLIBANO)), ForbiddenArcanusJEIPlugin.CLIBANO_COMBUSTION);
    }
}
