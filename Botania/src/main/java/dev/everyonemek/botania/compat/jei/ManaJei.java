package dev.everyonemek.botania.compat.jei;

import java.util.*;
import dev.everyonemek.botania.BotanicalMekanism;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import vazkii.botania.api.brew.BrewContainer;
import vazkii.botania.api.recipe.BotanicalBreweryRecipe;

public final class ManaJei {
    public static final Renderer RENDERER = new Renderer();
    public static void register(IModIngredientRegistration registration) {
        registration.register(ManaIngredient.TYPE, List.of(new ManaIngredient(1000)), new Helper(), RENDERER, ManaIngredient.CODEC);
        if (net.neoforged.fml.ModList.get().isLoaded("ae2jeiintegration"))
            dev.everyonemek.botania.compat.ae2.ManaIngredientConverter.register();
    }
    public static void addCost(IRecipeLayoutBuilder builder, long amount, int x, int y) {
        if (amount > 0) builder.addSlot(RecipeIngredientRole.INPUT, x, y).setSlotName("botanicalmekanism_mana")
              .addIngredient(ManaIngredient.TYPE, new ManaIngredient(amount));
    }
    public static ItemStack brewVessel(IRecipeSlotsView slots) {
        return slots.getSlotViews(RecipeIngredientRole.INPUT).stream().map(slot -> slot.getDisplayedIngredient(VanillaTypes.ITEM_STACK))
              .flatMap(Optional::stream).filter(stack -> stack.getItem() instanceof BrewContainer).findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY);
    }
    public static int brewCost(BotanicalBreweryRecipe recipe, ItemStack vessel) {
        return vessel.getItem() instanceof BrewContainer container && !recipe.getOutput(vessel.copyWithCount(1)).isEmpty()
              ? container.getManaCost(recipe.getBrew(), vessel.copyWithCount(1)) : -1;
    }
    public static void addBrewSearch(IRecipeLayoutBuilder builder, BotanicalBreweryRecipe recipe) {
        for (var vessel : vazkii.botania.common.crafting.recipe.RecipeUtils.getBrewContainerIngredient().getItems()) {
            int mana = brewCost(recipe, vessel);
            if (mana > 0) { builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredient(ManaIngredient.TYPE, new ManaIngredient(mana)); return; }
        }
    }
    public static void decorateBrew(IAdvancedRegistration registration) {
        registration.addRecipeCategoryDecorator(vazkii.botania.client.integration.jei.BreweryRecipeCategory.TYPE,
              new mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator<BotanicalBreweryRecipe>() {
                  @Override public void draw(BotanicalBreweryRecipe recipe, mezz.jei.api.recipe.category.IRecipeCategory<BotanicalBreweryRecipe> category,
                        IRecipeSlotsView slots, GuiGraphics gui, double mx, double my) {
                      int mana = brewCost(recipe, brewVessel(slots));
                      if (mana > 0) RENDERER.render(gui, new ManaIngredient(mana), 105, 35);
                  }
                  @Override public void decorateTooltips(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, BotanicalBreweryRecipe recipe,
                        mezz.jei.api.recipe.category.IRecipeCategory<BotanicalBreweryRecipe> category, IRecipeSlotsView slots, double mx, double my) {
                      int mana = brewCost(recipe, brewVessel(slots));
                      if (mana > 0 && mx >= 105 && mx < 121 && my >= 35 && my < 51) tooltip.add(amountText(mana));
                  }
              });
    }
    public static Component amountText(long amount) { return Component.translatable("jei.botanicalmekanism.mana_amount", String.format(Locale.ROOT, "%,d", amount)); }
    public static final class Helper implements IIngredientHelper<ManaIngredient> {
        @Override public IIngredientType<ManaIngredient> getIngredientType() { return ManaIngredient.TYPE; }
        @Override public String getDisplayName(ManaIngredient value) { return Component.translatable("gui.botanicalmekanism.mana_type").getString(); }
        @Override public String getUniqueId(ManaIngredient value, UidContext context) { return getResourceLocation(value).toString(); }
        @Override public ResourceLocation getResourceLocation(ManaIngredient value) { return ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "mana"); }
        @Override public ManaIngredient copyIngredient(ManaIngredient value) { return value; }
        @Override public long getAmount(ManaIngredient value) { return value.amount(); }
        @Override public ManaIngredient copyWithAmount(ManaIngredient value, long amount) { return new ManaIngredient(Math.max(1, amount)); }
        @Override public String getErrorInfo(ManaIngredient value) { return String.valueOf(value); }
        @Override public ItemStack getCheatItemStack(ManaIngredient value) { return ItemStack.EMPTY; }
    }
    public static final class Renderer implements IIngredientRenderer<ManaIngredient> {
        @Override public void render(GuiGraphics gui, ManaIngredient value) {
            dev.everyonemek.botania.client.ManaIcon.draw(gui, 0, 0);
            String amount = value.amount() >= 1_000_000 ? String.format(Locale.ROOT, "%.1fM", value.amount() / 1_000_000.0)
                  : value.amount() >= 1000 ? String.format(Locale.ROOT, "%.1fk", value.amount() / 1000.0) : Long.toString(value.amount());
            amount = amount.replace(".0", ""); var font = Minecraft.getInstance().font;
            float scale = Math.min(.75F, 17F / font.width(amount));
            gui.pose().pushPose(); gui.pose().translate(16 - font.width(amount) * scale, 16 - font.lineHeight * scale, 200); gui.pose().scale(scale, scale, 1);
            gui.drawString(font, amount, 0, 0, 0xFFFFFF); gui.pose().popPose();
        }
        @Override public List<Component> getTooltip(ManaIngredient value, TooltipFlag flag) { return List.of(amountText(value.amount())); }
    }
    private ManaJei() { }
}
