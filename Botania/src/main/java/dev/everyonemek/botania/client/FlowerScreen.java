package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;
import vazkii.botania.common.block.BotaniaBlocks;

/** A compact mana HUD with native flower icons and server-confirmed controls. */
public final class FlowerScreen extends AbstractContainerScreen<FlowerMenu> {
    private static final int INK = 0xE0EAE7, MUTED = 0x9FB3AD, GREEN = 0x8FD4AD;
    private static final int CONTENT_WIDTH = 248;
    private final List<FlowerButton> buttons = new ArrayList<>();
    private EditBox value, member;
    private int builtKind = -1, lastMode = -1;
    private Component hoveredText;

    public FlowerScreen(FlowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 280; imageHeight = 224;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism." + key, args); }
    private void send(int action, String value) { PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, value)); }
    private void button(int x, int y, int width, Supplier<Component> label, Runnable action) {
        button(x, y, width, label, action, () -> true);
    }
    private void button(int x, int y, int width, Supplier<Component> label, Runnable action, BooleanSupplier enabled) {
        var button = addRenderableWidget(new FlowerButton(leftPos + x, topPos + y, width, label, action, enabled));
        buttons.add(button);
    }
    private EditBox field(int x, int y, int width, int maxLength, Component label, int action) {
        var field = new EditBox(font, leftPos + x + 5, topPos + y + 5, width - 10, 10, label) {
            @Override public boolean keyPressed(int key, int scan, int modifiers) {
                if (isFocused() && (key == 257 || key == 335)) { send(action, getValue()); return true; }
                return super.keyPressed(key, scan, modifiers);
            }
        };
        field.setBordered(false); field.setTextColor(INK); field.setTextColorUneditable(MUTED); field.setMaxLength(maxLength);
        return addRenderableWidget(field);
    }
    @Override protected void init() {
        super.init(); buttons.clear(); value = null; member = null; lastMode = -1;
        builtKind = menu.state.contains("kind") ? menu.state.getInt("kind") : -1;
        if (builtKind < 0) return;
        button(16, 198, 76, () -> text(menu.state.getBoolean("enabled") ? "pause" : "resume"), () -> send(0, ""));
        if (builtKind == 2) {
            value = field(16, 54, 184, 32, text("network_name"), 6);
            value.setValue(menu.state.getString("name"));
            button(208, 54, 56, () -> text("apply"), () -> send(6, value.getValue()));
            member = field(16, 90, 184, 16, text("member_name"), 7);
            member.setTooltip(Tooltip.create(text("member_hint")));
            button(208, 90, 56, () -> text("member_toggle"), () -> send(7, member.getValue()));
        } else if (builtKind == 3) {
            button(16, 46, CONTENT_WIDTH, () -> text("network", menu.state.getString("name").isEmpty() ? text("unlinked") : menu.state.getString("name")),
                  this::cycleNetwork, () -> !menu.state.getList("choices", Tag.TAG_COMPOUND).isEmpty());
            button(16, 74, 120, () -> text("mode." + menu.state.getInt("mode")), () -> send(1, ""));
            button(144, 74, 120, () -> text("direction." + menu.state.getInt("direction")), () -> send(2, ""), () -> !relay());
            button(16, 100, CONTENT_WIDTH, () -> text("priority." + menu.state.getInt("priority")), () -> send(4, ""), () -> menu.state.getInt("mode") == NetworkPlant.RECEIVE);
            value = field(100, 124, 100, 10, text("target"), 3);
            value.setFilter(input -> input.matches("[0-9]*"));
            button(208, 124, 56, () -> text("apply"), () -> send(3, value.getValue()), () -> !relay());
            button(188, 198, 76, () -> text("disconnect"), () -> send(8, ""), () -> menu.state.hasUUID("network"));
            updateMode();
        }
    }
    private boolean relay() { return menu.state.getInt("mode") == NetworkPlant.RELAY; }
    private void updateMode() {
        if (builtKind == 3 && value != null && lastMode != menu.state.getInt("mode")) {
            lastMode = menu.state.getInt("mode"); value.setValue(Integer.toString(menu.state.getInt("limit")));
            value.setEditable(!relay()); value.setMessage(text(lastMode == NetworkPlant.SUPPLY ? "reserve" : "target"));
        }
    }
    private void cycleNetwork() {
        var choices = menu.state.getList("choices", Tag.TAG_COMPOUND);
        if (choices.isEmpty()) return;
        int current = -1;
        if (menu.state.hasUUID("network")) for (int i = 0; i < choices.size(); i++)
            if (choices.getCompound(i).getUUID("id").equals(menu.state.getUUID("network"))) { current = i; break; }
        send(5, choices.getCompound((current + 1) % choices.size()).getUUID("id").toString());
    }
    @Override public void containerTick() {
        super.containerTick();
        if (menu.state.contains("kind") && builtKind != menu.state.getInt("kind")) rebuildWidgets();
        updateMode(); buttons.forEach(FlowerButton::refresh);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        // Text such as "Greenhouse" must not close the menu on the inventory key (E).
        if (key == 256) { onClose(); return true; }
        if (key != 258 && getFocused() instanceof EditBox field && field.canConsumeInput()) {
            field.keyPressed(key, scan, modifiers); return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0x30000000);
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xDC101C1B);
        gui.fill(leftPos + 16, topPos + 35, leftPos + 264, topPos + 36, 0x504E766B);
        if (builtKind == 2) { fieldBackground(gui, 16, 54, 184, value); fieldBackground(gui, 16, 90, 184, member); }
        else if (builtKind == 3) fieldBackground(gui, 100, 124, 100, value);
    }
    private void fieldBackground(GuiGraphics gui, int x, int y, int width, EditBox field) {
        gui.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 19, 0xA00A1312);
        gui.fill(leftPos + x, topPos + y + 18, leftPos + x + width, topPos + y + 19,
              field != null && field.isFocused() ? accent() : 0x805C756B);
    }
    private ItemLike flower() {
        return switch (builtKind) { case 1 -> Content.AMARANTHUS.get(); case 2 -> Content.CORE.get(); case 3 -> Content.NODE.get(); default -> Content.LOTUS.get(); };
    }
    private void item(GuiGraphics gui, ItemLike item, int x, int y) { gui.renderItem(new ItemStack(item), x, y); }
    private void line(GuiGraphics gui, Component label, int x, int y, int width, int color, int mx, int my) {
        String full = label.getString();
        String shown = font.width(full) > width ? font.plainSubstrByWidth(full, width - font.width("…")) + "…" : full;
        gui.drawString(font, shown, x, y, color, false);
        if (!shown.equals(full) && mx >= x && mx < x + width && my >= y && my < y + 10) hoveredText = label;
    }
    private int accent() { return builtKind == 2 ? 0xFFBB9AE4 : builtKind == 1 ? 0xFF9CCB83 : 0xFF6DCDD0; }
    private void resource(GuiGraphics gui, int x, Component label, int amount, int max, int color, int mx, int my) {
        line(gui, label, x, 48, 116, MUTED, mx, my);
        line(gui, text("quantity", amount, max), x, 66, 116, INK, mx, my);
        gui.fill(x, 84, x + 116, 88, 0xA006100F);
        int fill = max <= 0 ? 0 : (int) (116L * Math.clamp(amount, 0, max) / max);
        gui.fill(x, 84, x + fill, 88, color);
    }
    private void status(GuiGraphics gui, int y, int mx, int my) {
        String state = menu.state.getString("status");
        int color = switch (state) { case "working", "relay" -> GREEN; case "paused", "ready", "waiting", "full", "reserve" -> MUTED; default -> 0xE6B17C; };
        gui.fill(17, y + 2, 20, y + 5, 0xFF000000 | color);
        line(gui, text("status." + state), 26, y, 238, color, mx, my);
    }
    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        item(gui, flower(), 16, 12);
        line(gui, title, 40, 17, 224, INK, mx, my);
        if (builtKind < 0) { line(gui, text("loading"), 16, 48, CONTENT_WIDTH, MUTED, mx, my); return; }
        if (builtKind <= 1) {
            resource(gui, 16, text("energy_label"), menu.state.getInt("fe"), Balance.ENERGY_CAPACITY, 0xFFE2BD68, mx, my);
            resource(gui, 148, text("mana_label"), menu.state.getInt("mana"), menu.state.getInt("maxMana"), accent(), mx, my);
            gui.fill(16, 103, 264, 104, 0x304E766B);
            if (builtKind == 0) {
                item(gui, flower(), 43, 118); arrow(gui, 77, 125);
                item(gui, BotaniaBlocks.MANA_SPREADER, 131, 118); arrow(gui, 165, 125);
                item(gui, BotaniaBlocks.MANA_POOL, 219, 118);
                if (mx >= 128 && mx < 150 && my >= 115 && my < 139) {
                    var bound = net.minecraft.core.BlockPos.of(menu.state.getLong("binding"));
                    hoveredText = menu.state.contains("binding") ? text("bound_to", bound.getX(), bound.getY(), bound.getZ()) : text("bind_help");
                }
            } else {
                item(gui, flower(), 87, 118); arrow(gui, 121, 125);
                item(gui, BotaniaBlocks.WHITE_MYSTICAL_FLOWER, 175, 118);
            }
            status(gui, 155, mx, my);
            Component hint = builtKind == 0 ? text("lotus_hint", menu.state.getInt("rate") * menu.state.getInt("fePerMana"), menu.state.getInt("rate")) : text("amaranthus_hint");
            line(gui, hint, 16, 174, CONTENT_WIDTH, MUTED, mx, my);
            var lines = font.split(text(builtKind == 0 ? "bind_hint" : "amaranthus_ground"), 160);
            for (int i = 0; i < Math.min(2, lines.size()); i++) gui.drawString(font, lines.get(i), 104, 197 + i * 9, MUTED, false);
            if (mx >= 104 && mx < 264 && my >= 196 && my < 216) hoveredText = text(builtKind == 0 ? "bind_help" : "amaranthus_ground");
        } else if (builtKind == 2) {
            line(gui, text("network_name"), 16, 42, CONTENT_WIDTH, MUTED, mx, my);
            line(gui, text("member_name"), 16, 78, CONTENT_WIDTH, MUTED, mx, my);
            gui.fill(16, 120, 264, 121, 0x304E766B);
            line(gui, text("network", menu.state.getString("name")), 16, 128, CONTENT_WIDTH, INK, mx, my);
            line(gui, text("nodes", menu.state.getInt("nodes"), Balance.NODE_LIMIT), 16, 145, 116, MUTED, mx, my);
            line(gui, text("member_count", menu.state.getInt("memberCount")), 148, 145, 116, accent() & 0xFFFFFF, mx, my);
            line(gui, text("flow_fee", menu.state.getInt("delivered"), menu.state.getInt("fee")), 16, 161, CONTENT_WIDTH, MUTED, mx, my);
            if (mx >= 148 && mx < 264 && my >= 143 && my < 157 && !menu.state.getString("members").isEmpty())
                hoveredText = text("members", menu.state.getString("members"));
            status(gui, 180, mx, my);
        } else {
            line(gui, text(menu.state.getInt("mode") == NetworkPlant.SUPPLY ? "reserve" : "target"), 16, 129, 78, relay() ? MUTED : INK, mx, my);
            line(gui, relay() ? text("relay_flow", menu.state.getInt("moved")) : text("confirmed_limit", menu.state.getInt("limit")),
                  16, 151, CONTENT_WIDTH, MUTED, mx, my);
            status(gui, 166, mx, my);
            Component pool = relay() ? text("relay_hint") : menu.state.contains("maxMana")
                  ? text("mana", menu.state.getInt("mana"), menu.state.getInt("maxMana")) : text("unmeasured");
            line(gui, pool, 16, 181, CONTENT_WIDTH, MUTED, mx, my);
        }
    }
    private void arrow(GuiGraphics gui, int x, int y) {
        int color = builtKind == 0 && !menu.state.contains("binding") ? 0xFF597166 : accent();
        gui.fill(x, y, x + 28, y + 1, color);
        gui.fill(x + 25, y - 2, x + 26, y + 3, color);
        gui.fill(x + 26, y - 1, x + 27, y + 2, color);
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partialTick) {
        hoveredText = null; super.render(gui, mx, my, partialTick); renderTooltip(gui, mx, my);
        if (hoveredText != null) gui.renderTooltip(font, font.split(hoveredText, CONTENT_WIDTH), mx, my);
    }
    private final class FlowerButton extends Button {
        private final Supplier<Component> label;
        private final BooleanSupplier enabled;
        FlowerButton(int x, int y, int width, Supplier<Component> label, Runnable action, BooleanSupplier enabled) {
            super(x, y, width, 18, label.get(), b -> action.run(), DEFAULT_NARRATION);
            this.label = label; this.enabled = enabled; refresh();
        }
        void refresh() { setMessage(label.get()); active = enabled.getAsBoolean(); }
        @Override protected void renderWidget(GuiGraphics gui, int mx, int my, float partialTick) {
            int surface = !active ? 0x501E302B : isHoveredOrFocused() ? 0xD0406257 : 0xC02A433B;
            gui.fill(getX(), getY(), getRight(), getBottom(), surface);
            gui.fill(getX(), getBottom() - 1, getRight(), getBottom(), active && isHoveredOrFocused() ? accent() : 0x80617E71);
            if (isFocused()) gui.renderOutline(getX(), getY(), width, height, accent());
            String full = getMessage().getString();
            boolean clipped = font.width(full) > width - 12;
            String shown = clipped ? font.plainSubstrByWidth(full, width - 12 - font.width("…")) + "…" : full;
            gui.drawString(font, shown, getX() + (width - font.width(shown)) / 2, getY() + 5, active ? INK : 0x73867D, false);
            setTooltip(clipped ? Tooltip.create(getMessage()) : null);
        }
    }
}
