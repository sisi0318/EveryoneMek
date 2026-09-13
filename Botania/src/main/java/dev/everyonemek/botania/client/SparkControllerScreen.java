package dev.everyonemek.botania.client;

import dev.everyonemek.botania.SparkControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SparkControllerScreen extends AbstractContainerScreen<SparkControllerMenu> {
    public SparkControllerScreen(SparkControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 184; inventoryLabelY = 90;
    }
    private Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism.spark." + key, args); }
    @Override protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        gui.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF777D7D);
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEB242A2C);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            gui.fill(x - 1, y - 1, x + 17, y + 17, 0xFF687171); gui.fill(x, y, x + 16, y + 16, 0xFF171C1D);
        }
    }
    @Override protected void renderLabels(GuiGraphics gui, int x, int y) {
        gui.drawCenteredString(font, title, imageWidth / 2, 7, 0xE3E9E7);
        gui.drawCenteredString(font, text("range_label"), 61, 21, 0xADBAB8); gui.drawCenteredString(font, text("efficiency_label"), 115, 21, 0xADBAB8);
        gui.drawString(font, text("range", menu.stat(0)), 8, 57, 0xCDE6E0, false);
        gui.drawString(font, text("rate", menu.stat(1)), 98, 57, 0xCDE6E0, false);
        gui.drawCenteredString(font, menu.stat(3) == 0 ? text("members", menu.stat(2)) : text(menu.stat(3) == 2 ? "conflict" : "no_master"), imageWidth / 2, 74, 0xADBAB8);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xADBAB8, false);
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) { super.render(gui, mx, my, partial); renderTooltip(gui, mx, my); }
}
