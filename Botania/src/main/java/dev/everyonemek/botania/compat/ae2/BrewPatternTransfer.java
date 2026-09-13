package dev.everyonemek.botania.compat.ae2;

import java.util.*;
import appeng.api.stacks.*;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.everyonemek.botania.compat.jei.ManaJei;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import vazkii.botania.api.brew.BrewContainer;
import vazkii.botania.api.recipe.BotanicalBreweryRecipe;
import tamaized.ae2jeiintegration.integration.modules.jei.GenericEntryStackHelper;

/** Keep vessel, mana cost and brewed output together when choosing among the displayed alternatives. */
public final class BrewPatternTransfer implements IRecipeTransferHandler<PatternEncodingTermMenu, BotanicalBreweryRecipe> {
    public static void register(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        var type = vazkii.botania.client.integration.jei.BreweryRecipeCategory.TYPE;
        registration.addRecipeTransferHandler(new BrewPatternTransfer(registration.getTransferHelper()), type);
    }
    private final IRecipeTransferHandlerHelper helper;
    public BrewPatternTransfer(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<PatternEncodingTermMenu> getContainerClass() { return PatternEncodingTermMenu.class; }
    @Override public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() { return Optional.of(PatternEncodingTermMenu.TYPE); }
    @Override public RecipeType<BotanicalBreweryRecipe> getRecipeType() { return vazkii.botania.client.integration.jei.BreweryRecipeCategory.TYPE; }
    @Override public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, BotanicalBreweryRecipe recipe,
          IRecipeSlotsView slots, Player player, boolean max, boolean execute) {
        var vessel = ManaJei.brewVessel(slots); int mana = ManaJei.brewCost(recipe, vessel);
        if (mana < 0) return helper.createUserErrorWithTooltip(Component.translatable("jei.botanicalmekanism.choose_vessel"));
        var inputs = new ArrayList<>(GenericEntryStackHelper.ofInputs(slots));
        for (int i = 0; i < inputs.size(); i++) if (inputs.get(i).stream().anyMatch(s -> s.what() instanceof AEItemKey key && key.getItem() instanceof BrewContainer)) {
            inputs.set(i, List.of(GenericStack.fromItemStack(vessel.copyWithCount(1)))); break;
        }
        if (mana > 0) inputs.add(List.of(new GenericStack(ManaKey.INSTANCE, mana)));
        if (inputs.size() > menu.getProcessingInputSlots().length) return helper.createUserErrorWithTooltip(Component.translatable("jei.botanicalmekanism.pattern_full"));
        if (execute) EncodingHelper.encodeProcessingRecipe(menu, inputs, List.of(GenericStack.fromItemStack(recipe.getOutput(vessel.copyWithCount(1)))));
        return null;
    }
}
