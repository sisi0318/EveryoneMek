package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CorporeaFlowerScreen extends AbstractContainerScreen<FlowerMenu> {
    private int pending = -1, sampleSource;
    private Button filterButton, exactButton, craftButton;
    private final dev.everyonemek.botania.corporea.BridgeFilter filter = new dev.everyonemek.botania.corporea.BridgeFilter();
    private int relativeMouseX, relativeMouseY;
    private Component hovered;
    public CorporeaFlowerScreen(FlowerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 260; imageHeight = 294; sampleSource = inventory.selected; }
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
              .bounds(leftPos + 14, topPos + 264, 64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(leftPos + 182, topPos + 264, 64, 18).build());
        filterButton = addRenderableWidget(Button.builder(text("filter.0"), b -> send(18, Integer.toString((filter.mode + 1) % 3)))
              .bounds(leftPos + 14, topPos + 163, 110, 18).build());
        exactButton = addRenderableWidget(Button.builder(text("exact"), b -> send(20, filter.exact ? "0" : "1"))
              .bounds(leftPos + 132, topPos + 163, 114, 18).build());
        craftButton = addRenderableWidget(Button.builder(text("craft.off"), b -> send(21, menu.state.getBoolean("autocraft") ? "0" : "1"))
              .bounds(leftPos + 88, topPos + 264, 84, 18).build());
    }
    @Override public void containerTick() {
        super.containerTick(); if (pending >= 0 && menu.state.getInt("settingsRevision") != pending) pending = -1;
        filter.load(menu.state, menu.level.registryAccess());
        filterButton.setMessage(text("filter." + filter.mode)); exactButton.setMessage(text(filter.exact ? "exact" : "item_only"));
        craftButton.setMessage(text(menu.state.getBoolean("autocraft") ? "craft.on" : "craft.off"));
        filterButton.active = exactButton.active = craftButton.active = pending < 0 && menu.state.contains("kind");
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
        for (int i = 0; i < 9; i++) {
            int x = leftPos + 68 + i * 18;
            gui.fill(x - 1, topPos + 186, x + 17, topPos + 204, 0xFF131619);
            gui.renderItem(filter.samples[i], x, topPos + 187);
            gui.fill(x - 1, topPos + 218, x + 17, topPos + 236, 0xFF131619);
            if (sampleSource == i) gui.renderOutline(x - 1, topPos + 218, 18, 18, 0xFF9FAFA8);
            gui.renderItem(minecraft.player.getInventory().getItem(i), x, topPos + 219);
        }
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
        line(gui, text("samples"), 190, 0xB5BCC0);
        line(gui, text("hotbar"), 222, 0xB5BCC0);
        line(gui, text("crafting", text("craft.status." + menu.state.getString("craft_status")), menu.state.getInt("craft_jobs")), 245, 0xB5BCC0);
        if (relativeMouseY >= 184 && relativeMouseY < 237) hovered = text("sample_help");
        if (relativeMouseY >= 244 && relativeMouseY < 263) hovered = text("craft_help");
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
    @Override public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx - leftPos - 67, y = (int) my - topPos;
        if (x >= 0 && x < 162 && pending < 0) {
            if (y >= 218 && y < 236 && button == 0) { sampleSource = x / 18; return true; }
            if (y >= 186 && y < 204 && (button == 0 || button == 1)) {
                send(19, (x / 18) + "," + (button == 1 ? -1 : sampleSource)); return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        hovered = null; super.render(gui, mx, my, partial); renderTooltip(gui, mx, my);
        if (hovered != null) gui.renderTooltip(font, font.split(hovered, 232), mx, my);
    }
}
