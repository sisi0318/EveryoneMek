package dev.everyonemek.botania.client;

import java.util.Optional;
import dev.everyonemek.botania.*;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.neoforged.neoforge.network.PacketDistributor;

final class MachineJeiTransfer<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
    private final Class<C> container;
    private final MenuType<C> menuType;
    private final RecipeType<R> recipeType;
    private final IRecipeTransferHandlerHelper helper;
    MachineJeiTransfer(Class<C> container, MenuType<C> menuType, RecipeType<R> recipeType, IRecipeTransferHandlerHelper helper) {
        this.container = container; this.menuType = menuType; this.recipeType = recipeType; this.helper = helper;
    }
    @Override public Class<C> getContainerClass() { return container; }
    @Override public Optional<MenuType<C>> getMenuType() { return Optional.of(menuType); }
    @Override public RecipeType<R> getRecipeType() { return recipeType; }
    @Override public IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots, Player player, boolean max, boolean execute) {
        var holder = player.level().getRecipeManager().getRecipes().stream().filter(h -> h.value() == recipe).findFirst().orElse(null);
        if (holder == null) return helper.createInternalError();
        String error = MachineRecipeTransfer.transfer(player, menu, holder.id(), max, false);
        if (!error.isEmpty()) return helper.createUserErrorWithTooltip(Component.translatable("gui.botanicalmekanism.transfer." + error));
        if (execute) PacketDistributor.sendToServer(new FlowerPackets.FillRecipe(menu.containerId, holder.id(), max));
        return null;
    }
}
