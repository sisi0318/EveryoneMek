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
import net.neoforged.neoforge.network.PacketDistributor;

/** Compact controls; secondary information is available on hover. */
public final class FlowerScreen extends AbstractContainerScreen<FlowerMenu> {
    private static final int INK = 0xE4E4E4, MUTED = 0xA5A7AA, ACCENT = 0xFF7BA69A;
    private static final int CONTENT_WIDTH = 212;
    private final List<FlowerButton> buttons = new ArrayList<>();
    private EditBox value, member;
    private int builtKind = -1, lastMode = -1;
    private Component hoveredText;

    public FlowerScreen(FlowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 240; imageHeight = 148;
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
        int kind = menu.state.contains("kind") ? menu.state.getInt("kind") : -1;
        int mode = menu.state.getInt("mode");
        boolean preserveDraft = kind == builtKind && mode == lastMode;
        String draft = preserveDraft && value != null ? value.getValue() : null;
        String memberDraft = preserveDraft && member != null ? member.getValue() : "";
        builtKind = kind; lastMode = mode;
        imageHeight = builtKind <= 1 ? 148 : builtKind == 2 ? 202 : relay() ? 140 : lastMode == NetworkPlant.RECEIVE ? 218 : 194;
        super.init(); buttons.clear(); value = null; member = null;
        button(162, imageHeight - 30, 64, () -> Component.translatable("gui.done"), this::onClose);
        if (builtKind < 0) return;
        button(14, imageHeight - 30, 64, () -> text(menu.state.getBoolean("enabled") ? "pause" : "resume"), () -> send(0, ""));
        if (builtKind == 2) {
            value = field(14, 49, 158, 32, text("network_name"), 6);
            value.setValue(draft != null ? draft : menu.state.getString("name"));
            button(178, 49, 48, () -> text("apply"), () -> send(6, value.getValue()));
            member = field(14, 91, 158, 16, text("member_name"), 7);
            member.setValue(memberDraft); member.setTooltip(Tooltip.create(text("member_hint")));
            button(178, 91, 48, () -> text("member_toggle"), () -> send(7, member.getValue()));
        } else if (builtKind == 3) {
            button(14, 36, CONTENT_WIDTH, () -> text("network", menu.state.getString("name").isEmpty() ? text("unlinked") : menu.state.getString("name")),
                  this::cycleNetwork, () -> !menu.state.getList("choices", Tag.TAG_COMPOUND).isEmpty());
            button(14, 60, relay() ? CONTENT_WIDTH : 100, () -> text("mode." + menu.state.getInt("mode")), () -> send(1, ""));
            if (!relay()) {
                button(126, 60, 100, () -> text("direction." + menu.state.getInt("direction")), () -> send(2, ""));
                if (lastMode == NetworkPlant.RECEIVE)
                    button(14, 84, CONTENT_WIDTH, () -> text("priority." + menu.state.getInt("priority")), () -> send(4, ""));
                value = field(82, quantityY(), 92, 10, text(lastMode == NetworkPlant.SUPPLY ? "reserve" : "target"), 3);
                value.setFilter(input -> input.matches("[0-9]*"));
                value.setValue(draft != null ? draft : Integer.toString(menu.state.getInt("limit")));
                button(182, quantityY(), 44, () -> text("apply"), () -> send(3, value.getValue()));
            }
            button(86, imageHeight - 30, 64, () -> text("disconnect"), () -> send(8, ""), () -> menu.state.hasUUID("network"));
        }
    }
    private boolean relay() { return lastMode == NetworkPlant.RELAY; }
    private int quantityY() { return lastMode == NetworkPlant.RECEIVE ? 108 : 84; }
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
        if (menu.state.contains("kind") && (builtKind != menu.state.getInt("kind")
              || builtKind == 3 && lastMode != menu.state.getInt("mode"))) rebuildWidgets();
        buttons.forEach(FlowerButton::refresh);
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
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0212328);
        gui.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xA04B4E53);
        if (builtKind == 2) { fieldBackground(gui, 14, 49, 158, value); fieldBackground(gui, 14, 91, 158, member); }
        else if (builtKind == 3 && !relay()) fieldBackground(gui, 82, quantityY(), 92, value);
    }
    private void fieldBackground(GuiGraphics gui, int x, int y, int width, EditBox field) {
        gui.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 19, 0xB0101114);
        gui.renderOutline(leftPos + x, topPos + y, width, 19, field != null && field.isFocused() ? ACCENT : 0xFF55585D);
    }
    private void line(GuiGraphics gui, Component label, int x, int y, int width, int color, int mx, int my) {
        String full = label.getString();
        String shown = font.width(full) > width ? font.plainSubstrByWidth(full, width - font.width("…")) + "…" : full;
        gui.drawString(font, shown, x, y, color, false);
        if (!shown.equals(full) && mx >= x && mx < x + width && my >= y && my < y + 10) hoveredText = label;
    }
    private boolean over(int mx, int my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
    private void resource(GuiGraphics gui, int y, Component label, int amount, int max, int mx, int my) {
        line(gui, label, 14, y, 90, MUTED, mx, my);
        Component quantity = text("quantity", amount, max);
        int width = Math.min(116, font.width(quantity));
        line(gui, quantity, 226 - width, y, width, INK, mx, my);
        gui.fill(14, y + 14, 226, y + 17, 0xA0101114);
        int fill = max <= 0 ? 0 : (int) (212L * Math.clamp(amount, 0, max) / max);
        gui.fill(14, y + 14, 14 + fill, y + 17, ACCENT);
    }
    private void status(GuiGraphics gui, int y, int mx, int my) {
        String state = menu.state.getString("status");
        int color = switch (state) { case "working", "relay", "paused", "ready", "waiting", "full", "reserve" -> MUTED; default -> 0xDBB990; };
        line(gui, text("status." + state), 14, y, CONTENT_WIDTH, color, mx, my);
    }
    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        line(gui, title, 14, 13, CONTENT_WIDTH, INK, mx, my);
        if (builtKind < 0) { line(gui, text("loading"), 14, 38, CONTENT_WIDTH, MUTED, mx, my); return; }
        if (builtKind <= 1) {
            resource(gui, 36, text("energy_label"), menu.state.getInt("fe"), Balance.ENERGY_CAPACITY, mx, my);
            resource(gui, 65, text("mana_label"), menu.state.getInt("mana"), menu.state.getInt("maxMana"), mx, my);
            status(gui, 94, mx, my);
            if (over(mx, my, 14, 36, CONTENT_WIDTH, 18)) hoveredText = builtKind == 0
                  ? text("lotus_hint", menu.state.getInt("rate") * menu.state.getInt("fePerMana"), menu.state.getInt("rate")) : text("amaranthus_hint");
            if (over(mx, my, 14, 94, CONTENT_WIDTH, 10)) {
                if (builtKind == 0) {
                    var bound = net.minecraft.core.BlockPos.of(menu.state.getLong("binding"));
                    hoveredText = menu.state.contains("binding") ? text("bound_to", bound.getX(), bound.getY(), bound.getZ()) : text("bind_help");
                } else hoveredText = text("amaranthus_hint");
            }
        } else if (builtKind == 2) {
            line(gui, text("network_name"), 14, 36, CONTENT_WIDTH, MUTED, mx, my);
            line(gui, text("member_name"), 14, 78, CONTENT_WIDTH, MUTED, mx, my);
            line(gui, text("nodes", menu.state.getInt("nodes"), Balance.NODE_LIMIT), 14, 122, 100, MUTED, mx, my);
            line(gui, text("member_count", menu.state.getInt("memberCount")), 126, 122, 100, MUTED, mx, my);
            status(gui, 145, mx, my);
            if (over(mx, my, 14, 36, CONTENT_WIDTH, 10)) hoveredText = text("network", menu.state.getString("name"));
            if (over(mx, my, 14, 122, 100, 10)) hoveredText = text("flow_fee", menu.state.getInt("delivered"), menu.state.getInt("fee"));
            if (over(mx, my, 126, 122, 100, 10) && !menu.state.getString("members").isEmpty())
                hoveredText = text("members", menu.state.getString("members"));
        } else if (relay()) {
            status(gui, 91, mx, my);
            if (over(mx, my, 14, 91, CONTENT_WIDTH, 10)) hoveredText = text("relay_flow", menu.state.getInt("moved"));
        } else {
            int y = quantityY();
            line(gui, text(lastMode == NetworkPlant.SUPPLY ? "reserve" : "target"), 14, y + 5, 62, MUTED, mx, my);
            line(gui, text("confirmed_limit", menu.state.getInt("limit")), 14, y + 26, CONTENT_WIDTH, MUTED, mx, my);
            line(gui, menu.state.contains("maxMana") ? text("mana", menu.state.getInt("mana"), menu.state.getInt("maxMana")) : text("unmeasured"),
                  14, y + 42, CONTENT_WIDTH, INK, mx, my);
            status(gui, y + 58, mx, my);
        }
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
            int surface = !active ? 0x502F3135 : isHoveredOrFocused() ? 0xE0494D53 : 0xD034373D;
            gui.fill(getX(), getY(), getRight(), getBottom(), surface);
            gui.renderOutline(getX(), getY(), width, height, isFocused() ? ACCENT : 0xFF55585D);
            String full = getMessage().getString();
            boolean clipped = font.width(full) > width - 12;
            String shown = clipped ? font.plainSubstrByWidth(full, width - 12 - font.width("…")) + "…" : full;
            gui.drawString(font, shown, getX() + (width - font.width(shown)) / 2, getY() + 5, active ? INK : 0x74777C, false);
            setTooltip(clipped ? Tooltip.create(getMessage()) : null);
        }
    }
}
