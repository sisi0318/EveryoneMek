package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ApothecaryScreen extends GuiConfigurableTile<MechanicalApothecary, ApothecaryMenu> {
    public ApothecaryScreen(ApothecaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 238; imageHeight = 240;
        inventoryLabelX = 29; inventoryLabelY = 144; dynamicSlots = true;
    }
    private Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism.apothecary." + key, args); }
    @Override protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 224, 32, 70));
        addRenderableWidget(new GuiProgress(tile::progress, ProgressType.SMALL_RIGHT, this, 111, 47));
    }
    @Override protected void drawForegroundText(GuiGraphics gui, int mx, int my) {
        super.drawForegroundText(gui, mx, my); renderTitleText(gui);
        gui.drawString(font, text("materials"), 16, 21, titleTextColor(), false);
        gui.drawString(font, text("products"), 152, 23, titleTextColor(), false);
        gui.drawString(font, text("reagent"), 99, 74, titleTextColor(), false);
        gui.drawString(font, text("buckets"), 154, 74, titleTextColor(), false);
        gui.fill(16, 106, 220, 110, 0xFF444B50);
        gui.fill(16, 106, 16 + (int) (204L * tile.water().getFluidAmount() / MechanicalApothecary.WATER_CAPACITY), 110, 0xFF5CAAD1);
        gui.drawString(font, text("water", tile.water().getFluidAmount(), MechanicalApothecary.WATER_CAPACITY), 16, 115, titleTextColor(), false);
        gui.drawString(font, text("status." + tile.status()), 16, 130, titleTextColor(), false);
    }
}
