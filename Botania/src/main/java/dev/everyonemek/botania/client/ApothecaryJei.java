package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import vazkii.botania.client.integration.jei.PetalApothecaryRecipeCategory;

@JeiPlugin
public final class ApothecaryJei implements IModPlugin {
    private static final RecipeType<MechanicalFlowerRecipe> TYPE = RecipeType.create(BotanicalMekanism.ID, "mechanical_apothecary", MechanicalFlowerRecipe.class);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "apothecary_jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) { registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper())); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ApothecaryContent.BLOCK), PetalApothecaryRecipeCategory.TYPE, TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.INFUSER)), vazkii.botania.client.integration.jei.ManaPoolRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.RUNIC)), vazkii.botania.client.integration.jei.RunicAltarRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.PURE)), vazkii.botania.client.integration.jei.PureDaisyRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.TERRA)), vazkii.botania.client.integration.jei.TerrestrialAgglomerationRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.BREWERY)), vazkii.botania.client.integration.jei.BreweryRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.ORE)), vazkii.botania.client.integration.jei.orechid.OrechidRecipeCategory.TYPE,
              vazkii.botania.client.integration.jei.orechid.OrechidIgnemRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.METAMORPHIC)), vazkii.botania.client.integration.jei.orechid.MarimorphosisRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.ELVEN)), vazkii.botania.client.integration.jei.ElvenTradeRecipeCategory.TYPE);
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(TYPE, level.getRecipeManager().getAllRecipesFor(ApothecaryContent.RECIPE_TYPE.get()).stream().map(holder -> holder.value()).toList());
    }
    private static final class Category implements IRecipeCategory<MechanicalFlowerRecipe> {
        private final IDrawable icon, slot;
        Category(IGuiHelper helper) { icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ApothecaryContent.BLOCK)); slot = helper.getSlotDrawable(); }
        @Override public RecipeType<MechanicalFlowerRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("gui.botanicalmekanism.apothecary.jei"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 154; }
        @Override public int getHeight() { return 116; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, MechanicalFlowerRecipe recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.materials().size(); i++) builder.addSlot(RecipeIngredientRole.INPUT, 1 + i % 4 * 18, 1 + i / 4 * 18)
                  .setBackground(slot, -1, -1).addIngredients(recipe.materials().get(i));
            builder.addSlot(RecipeIngredientRole.INPUT, 92, 50).setBackground(slot, -1, -1).addIngredients(recipe.reagent())
                  .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("gui.botanicalmekanism.apothecary.reagent")));
            builder.addSlot(RecipeIngredientRole.INPUT, 92, 74).addFluidStack(Fluids.WATER, MechanicalApothecary.WATER_PER_CRAFT);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 24).setBackground(slot, -1, -1).addItemStack(recipe.output());
        }
        @Override public void draw(MechanicalFlowerRecipe recipe, IRecipeSlotsView slots, GuiGraphics gui, double mx, double my) {
            var font = Minecraft.getInstance().font;
            gui.drawString(font, "→", 92, 27, 0x666666, false);
            gui.drawString(font, Component.translatable("gui.botanicalmekanism.apothecary.energy_cost", (long) recipe.ticks() * recipe.fePerTick()), 0, 95, 0x444444, false);
            gui.drawString(font, Component.translatable("gui.botanicalmekanism.apothecary.time_water", recipe.ticks()), 0, 106, 0x444444, false);
        }
    }
}
