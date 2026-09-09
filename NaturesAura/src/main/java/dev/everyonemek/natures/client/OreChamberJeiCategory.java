package dev.everyonemek.natures.client;

import de.ellpeck.naturesaura.chunk.effect.OreSpawnEffect;
import de.ellpeck.naturesaura.items.ItemEffectPowder;
import de.ellpeck.naturesaura.items.ModItems;
import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.MachineConfig;
import dev.everyonemek.natures.MachineKind;
import dev.everyonemek.natures.NaturesMekanism;
import dev.everyonemek.natures.OreChamberLogic;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import net.minecraft.world.item.Items;

public final class OreChamberJeiCategory implements IRecipeCategory<OreChamberJeiCategory.Display> {
    public record Display(OreChamberLogic.Ore ore, boolean nether, long total) { }
    public static final RecipeType<Display> TYPE = RecipeType.create(NaturesMekanism.ID, "ore_condensation", Display.class);
    private final IDrawable icon, arrow;
    public OreChamberJeiCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(Content.MACHINES.get(MachineKind.ORE_CHAMBER)));
        arrow = helper.getRecipeArrow();
    }
    public static List<Display> recipes() {
        var result = new ArrayList<Display>();
        for (boolean nether : new boolean[]{false, true}) {
            var ores = OreChamberLogic.displayOres(nether);
            long total = ores.stream().mapToLong(OreChamberLogic.Ore::weight).sum();
            for (var ore : ores) result.add(new Display(ore, nether, total));
        }
        return result;
    }
    @Override public RecipeType<Display> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.naturesmekanism.ore_condensation_chamber"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 218; }
    @Override public int getHeight() { return 118; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 30, 4).setStandardSlotBackground().addItemStack(new ItemStack(recipe.nether() ? Items.NETHERRACK : Items.STONE));
        builder.addSlot(RecipeIngredientRole.CATALYST, 60, 4).setStandardSlotBackground()
              .addItemStack(ItemEffectPowder.setEffect(new ItemStack(ModItems.EFFECT_POWDER), OreSpawnEffect.NAME));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, 4).setOutputSlotBackground().addItemStack(new ItemStack(recipe.ore().item()));
    }
    @Override public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 104, 4);
        line(graphics, Component.translatable("gui.naturesmekanism.bottling_dimension." + (recipe.nether() ? "nether" : "overworld")), 29);
        line(graphics, Component.translatable("gui.naturesmekanism.chamber_threshold"), 43);
        line(graphics, Component.translatable("gui.naturesmekanism.chamber_weight", recipe.ore().weight(),
              String.format(Locale.ROOT, "%.2f", 100D * recipe.ore().weight() / recipe.total())), 57);
        line(graphics, Component.translatable("gui.naturesmekanism.chamber_cost", recipe.ore().cost()), 71);
        line(graphics, Component.translatable("gui.naturesmekanism.bottling_work", MachineConfig.ORE_FE.get(), MachineConfig.ORE_TICKS.get()), 85);
        line(graphics, Component.translatable("gui.naturesmekanism.chamber_powder_kept"), 103);
    }
    private void line(GuiGraphics graphics, Component text, int y) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, getWidth() / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose(); graphics.pose().translate(0, y, 0); graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, 0x404040, false); graphics.pose().popPose();
    }
}
