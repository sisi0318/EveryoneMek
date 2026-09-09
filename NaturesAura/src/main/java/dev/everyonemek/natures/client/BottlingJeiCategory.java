package dev.everyonemek.natures.client;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.items.ItemAuraBottle;
import de.ellpeck.naturesaura.items.ModItems;
import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.MachineConfig;
import dev.everyonemek.natures.MachineKind;
import dev.everyonemek.natures.NaturesMekanism;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class BottlingJeiCategory implements IRecipeCategory<BottlingJeiCategory.Display> {
    public record Display(ItemStack output, String dimension, boolean vacuum) { }
    public static final RecipeType<Display> TYPE = RecipeType.create(NaturesMekanism.ID, "bottling", Display.class);
    private final IDrawable icon;
    private final IDrawable arrow;

    public BottlingJeiCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(Content.MACHINES.get(MachineKind.AURA_BOTTLER)));
        arrow = helper.getRecipeArrow();
    }

    public static List<Display> recipes() {
        var result = new ArrayList<Display>();
        for (var type : List.of(NaturesAuraAPI.TYPE_OVERWORLD, NaturesAuraAPI.TYPE_NETHER, NaturesAuraAPI.TYPE_END, NaturesAuraAPI.TYPE_OTHER))
            result.add(new Display(ItemAuraBottle.setType(new ItemStack(ModItems.AURA_BOTTLE), type), type.getName().getPath(), false));
        result.add(new Display(new ItemStack(ModItems.VACUUM_BOTTLE), "any", true));
        return result;
    }

    @Override public RecipeType<Display> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.naturesmekanism.aura_bottler"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 218; }
    @Override public int getHeight() { return 109; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 5).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.BOTTLE_TWO_THE_REBOTTLING));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 122, 5).setOutputSlotBackground().addItemStack(recipe.output());
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 193, 5).addItemStack(new ItemStack(Content.SIMULATION_MODULE.get()));
    }

    @Override
    public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 78, 5);
        line(graphics, Component.translatable("gui.naturesmekanism.bottling_dimension." + recipe.dimension()), 32, 0x404040);
        line(graphics, Component.translatable(recipe.vacuum() ? "gui.naturesmekanism.bottling_vacuum_threshold"
              : "gui.naturesmekanism.bottling_threshold"), 46, 0x404040);
        line(graphics, Component.translatable("gui.naturesmekanism.bottling_aura_cost", recipe.vacuum() ? "0" : "20,000"), 60, 0x404040);
        line(graphics, Component.translatable("gui.naturesmekanism.bottling_work", MachineConfig.BOTTLER_FE.get(), MachineConfig.BOTTLER_TICKS.get()), 74, 0x404040);
        line(graphics, Component.translatable("gui.naturesmekanism.bottling_simulation_hint"), 94, 0x397638);
    }

    private void line(GuiGraphics graphics, Component text, int y, int color) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, getWidth() / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(0, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }
}
