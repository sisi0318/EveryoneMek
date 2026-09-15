package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import dev.everyonemek.botania.compat.jei.ManaIngredient;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

@JeiPlugin
public final class GreenhouseJei implements IModPlugin {
    public record Display(GreenhouseRecipe recipe, List<Ingredient> inputs, FluidStack fluid, int mana, int ticks, int cooldown) { }
    private static final RecipeType<Display> TYPE = RecipeType.create(BotanicalMekanism.ID, "mana_greenhouse", Display.class);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "greenhouse_jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) { registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper())); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) { registration.addRecipeCatalyst(new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.GREENHOUSE)), TYPE); }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level; if (level == null) return;
        var displays = new ArrayList<Display>();
        for (var holder : GreenhouseWork.recipes(level)) {
            var r = holder.value();
            if (r.fixed()) { displays.add(new Display(r, r.materials(), r.fluid(), r.mana(), r.ticks(), r.cooldown())); continue; }
            var flowers = r.flower().getItems(); if (flowers.length == 0) continue;
            var flower = flowers[0].copyWithCount(1);
            var rule = r.rule();
            if (rule.fluidAmount() > 0) {
                for (var fluid : BuiltInRegistries.FLUID) {
                    if (!fluid.isSource(fluid.defaultFluidState())) continue;
                    var stack = new FluidStack(fluid, rule.fluidAmount());
                    if (!rule.acceptsFluid(stack)) continue;
                    var result = rule.resolve(flower.copy(), ItemStack.EMPTY, stack, level);
                    if (GreenhouseWork.validResult(result, flower)) displays.add(new Display(r, List.of(), stack, result.mana(), result.ticks(), result.cooldown()));
                }
                continue;
            }
            for (var item : BuiltInRegistries.ITEM) {
                var input = new ItemStack(item); if (!rule.acceptsItem(input)) continue;
                var preview = rule.previewFlower(flower.copy(), input.copy(), level);
                if (preview == null || preview.isEmpty()) continue;
                var result = rule.resolve(preview, input, FluidStack.EMPTY, level);
                if (GreenhouseWork.validResult(result, flower)) displays.add(new Display(r, List.of(Ingredient.of(input)), FluidStack.EMPTY, result.mana(), result.ticks(), result.cooldown()));
            }
        }
        registration.addRecipes(TYPE, displays);
    }
    private static Component text(String key, Object... args) { return Component.translatable("jei.botanicalmekanism.greenhouse." + key, args); }
    private static final class Category implements IRecipeCategory<Display> {
        private final IDrawable slot, icon;
        Category(IGuiHelper helper) { slot = helper.getSlotDrawable(); icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ManaContent.MACHINES.get(ManaMachineKind.GREENHOUSE))); }
        @Override public RecipeType<Display> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("block.botanicalmekanism.mana_greenhouse"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 118; }
        @Override public void setRecipe(IRecipeLayoutBuilder b, Display d, IFocusGroup focuses) {
            b.addSlot(RecipeIngredientRole.CATALYST, 1, 1).setBackground(slot, -1, -1).addIngredients(d.recipe.flower());
            for (int i = 0; i < d.inputs.size(); i++) b.addSlot(RecipeIngredientRole.INPUT, 1 + i % 4 * 18, 25 + i / 4 * 18).setBackground(slot, -1, -1).addIngredients(d.inputs.get(i));
            if (!d.fluid.isEmpty()) b.addSlot(RecipeIngredientRole.INPUT, 80, 25).setBackground(slot, -1, -1).addFluidStack(d.fluid.getFluid(), d.fluid.getAmount());
            // History-dependent yields are visible but must not become fixed AE pattern outputs.
            var role = !d.recipe.fixed() && d.recipe.rule().variableOutput()
                  ? RecipeIngredientRole.RENDER_ONLY : RecipeIngredientRole.OUTPUT;
            b.addSlot(role, 134, 25).setBackground(slot, -1, -1).addIngredient(ManaIngredient.TYPE, new ManaIngredient(d.mana));
        }
        @Override public void draw(Display d, IRecipeSlotsView slots, GuiGraphics gui, double mx, double my) {
            var font = Minecraft.getInstance().font;
            gui.drawString(font, text("flower"), 23, 5, 0x444444, false);
            gui.drawString(font, "→", 110, 28, 0x666666, false);
            gui.drawString(font, text("ticks", d.ticks), 0, 98, 0x444444, false);
            var note = d.recipe.fixed() ? Component.empty() : d.recipe.rule().recipeNote();
            if (!note.getString().isEmpty()) gui.drawString(font, note, 0, 109, 0x444444, false);
            else if (d.cooldown > 0) gui.drawString(font, text("cooldown", d.cooldown), 0, 109, 0x444444, false);
        }
    }
}
