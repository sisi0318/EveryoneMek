package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CorporeaFlowerScreen extends AbstractContainerScreen<FlowerMenu> {
    private int pending = -1;
    private int relativeMouseX, relativeMouseY;
    private Component hovered;
    public CorporeaFlowerScreen(FlowerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 260; imageHeight = 198; }
    private static Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism.corporea." + key, args); }
    private void send(int action, String value) {
        if (pending >= 0) return; pending = menu.state.getInt("settingsRevision");
        PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, value));
    }
    @Override protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            int mode = i;
            addRenderableWidget(Button.builder(text("mode." + mode), button -> send(17, Integer.toString(mode)))
                  .bounds(leftPos + 14 + i * 79, topPos + 136, 74, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.botanicalmekanism." + (menu.state.getBoolean("enabled") ? "pause" : "resume")), button -> send(0, ""))
              .bounds(leftPos + 14, topPos + 168, 64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(leftPos + 182, topPos + 168, 64, 18).build());
    }
    @Override public void containerTick() {
        super.containerTick(); if (pending >= 0 && menu.state.getInt("settingsRevision") != pending) pending = -1;
        int i = 0;
        for (var widget : children()) if (widget instanceof Button button) {
            if (i < 3) button.active = menu.state.contains("kind") && pending < 0 && menu.state.getInt("mode") != i;
            else if (i == 3) { button.active = menu.state.contains("kind") && pending < 0; button.setMessage(Component.translatable("gui.botanicalmekanism." + (menu.state.getBoolean("enabled") ? "pause" : "resume"))); }
            i++;
        }
    }
    @Override protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0212328);
        gui.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xA04B4E53);
    }
    private void line(GuiGraphics gui, Component text, int y, int color) {
        String shown = text.getString(); if (font.width(shown) > 232) {
            shown = font.plainSubstrByWidth(shown, 220) + "…";
            if (relativeMouseX >= 14 && relativeMouseX < 246 && relativeMouseY >= y && relativeMouseY < y + 10) hovered = text;
        }
        gui.drawString(font, shown, 14, y, color, false);
    }
    @Override protected void renderLabels(GuiGraphics gui, int mx, int my) {
        relativeMouseX = mx - leftPos; relativeMouseY = my - topPos;
        line(gui, title, 13, 0xE4E4E4);
        if (!menu.state.contains("kind")) { line(gui, Component.translatable("gui.botanicalmekanism.loading"), 36, 0xB5BCC0); return; }
        line(gui, text("ae", text(menu.state.getBoolean("ae_connected") ? "online" : "offline")), 36, 0xC8D6CF);
        line(gui, text("nodes", menu.state.getInt("nodes")), 54, 0xC8D6CF);
        line(gui, text("stock", menu.state.getInt("types"), menu.state.getLong("items")), 74, 0xB5BCC0);
        line(gui, text("me_stock", menu.state.getInt("me_types"), menu.state.getLong("me_items")), 92, 0xB5BCC0);
        line(gui, pending >= 0 ? Component.translatable("gui.botanicalmekanism.applying") : text("status." + menu.state.getString("status")), 114, 0xD4BA90);
        if (hovered == null && relativeMouseX >= 14 && relativeMouseX < 246 && relativeMouseY >= 114 && relativeMouseY < 124)
            hovered = text("transfer", menu.state.getInt("moved"), menu.state.getInt("limit"));
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        hovered = null; super.render(gui, mx, my, partial); renderTooltip(gui, mx, my);
        if (hovered != null) gui.renderTooltip(font, font.split(hovered, 232), mx, my);
    }
}
