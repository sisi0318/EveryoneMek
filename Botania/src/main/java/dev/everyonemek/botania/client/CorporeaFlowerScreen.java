package dev.everyonemek.botania.client;

import java.util.*;
import java.util.function.Supplier;
import java.util.function.IntConsumer;
import dev.everyonemek.botania.*;
import dev.everyonemek.botania.corporea.BridgeFilter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;

/** AE runtime textures, without AE classes for an existing flower's offline screen. */
public final class CorporeaFlowerScreen extends AbstractContainerScreen<FlowerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/storagebus.png");
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/states.png");
    private final boolean ae = net.neoforged.fml.ModList.get().isLoaded("ae2");
    private final BridgeFilter filter = new BridgeFilter();
    private final List<SettingButton> settings = new ArrayList<>();
    private final SettingsProgress progress = new SettingsProgress();
    private record PendingSetting(String value, int revision) { }
    private record PendingSample(ItemStack stack, int revision) { }
    private final Map<Integer, PendingSetting> requestedSettings = new HashMap<>();
    private final Map<Integer, PendingSample> requestedSamples = new HashMap<>();
    private final Set<Integer> paintedSlots = new HashSet<>();
    private CompoundTag lastState;
    private int dragButton = -1;
    public CorporeaFlowerScreen(FlowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 253; titleLabelX = 8; titleLabelY = 6; inventoryLabelY = 158;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism.corporea." + key, args); }
    private boolean ready() { return progress.ready() && menu.state.contains("kind"); }
    private int send(int action, String value) {
        int revision = progress.submit(); requestedSettings.put(action, new PendingSetting(value, revision));
        PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, value));
        return revision;
    }
    private int desired(int action, int confirmed) {
        var request = requestedSettings.get(action); return request == null ? confirmed : Integer.parseInt(request.value());
    }
    private ItemStack shownSample(int index) {
        var request = requestedSamples.get(index); return request == null ? filter.samples[index] : request.stack();
    }
    private void editSample(int index, int button) {
        if (!ready() || !paintedSlots.add(index)) return;
        var sample = button == 1 ? ItemStack.EMPTY : menu.getCarried().copyWithCount(1);
        if (ItemStack.isSameItemSameComponents(sample, shownSample(index))) return;
        int revision = send(button == 1 ? FlowerMenu.CLEAR_FILTER_SLOT : FlowerMenu.COPY_FILTER_CURSOR, Integer.toString(index));
        requestedSamples.put(index, new PendingSample(sample, revision)); refresh();
    }
    private void clearSamples() {
        int revision = send(FlowerMenu.CLEAR_FILTER, "");
        for (int i = 0; i < BridgeFilter.SIZE; i++) requestedSamples.put(i, new PendingSample(ItemStack.EMPTY, revision));
        refresh();
    }
    @Override protected void init() {
        super.init(); settings.clear(); dragButton = -1; paintedSlots.clear();
        setting(() -> text("clear"), () -> new int[]{96, 0}, step -> clearSamples(), false);
        setting(() -> text("mode." + menu.state.getInt("mode")),
              () -> new int[]{switch (menu.state.getInt("mode")) { case 1 -> 16; case 2 -> 0; default -> 32; }, 144},
              step -> send(17, Integer.toString(Math.floorMod(desired(17, menu.state.getInt("mode")) + step, 3))), true);
        setting(() -> text("filter." + filter.mode), () -> new int[]{filter.mode == 2 ? 48 : 32, filter.mode == 0 ? 16 : 128},
              step -> send(18, Integer.toString(Math.floorMod(desired(18, filter.mode) + step, 3))), true);
        setting(() -> text(filter.exact ? "exact" : "item_only"), () -> new int[]{filter.exact ? 112 : 64, filter.exact ? 48 : 96},
              step -> send(20, Integer.toString(1 - desired(20, filter.exact ? 1 : 0))), false);
        setting(() -> text(menu.state.getBoolean("autocraft") ? "craft.on" : "craft.off"),
              () -> new int[]{menu.state.getBoolean("autocraft") ? 48 : 0, 16},
              step -> send(21, Integer.toString(1 - desired(21, menu.state.getBoolean("autocraft") ? 1 : 0))), false);
        setting(() -> Component.translatable("gui.botanicalmekanism." + (menu.state.getBoolean("enabled") ? "pause" : "resume")),
              () -> new int[]{menu.state.getBoolean("enabled") ? 80 : 64, 0}, step -> send(0, ""), false);
        refresh();
    }
    private void setting(Supplier<Component> label, Supplier<int[]> icon, IntConsumer action, boolean cycle) {
        var button = new SettingButton(leftPos - 20, topPos + 5 + settings.size() * 22, label, icon, action, cycle);
        settings.add(button); addRenderableWidget(button);
    }
    private void refresh() {
        if (lastState != menu.state) {
            lastState = menu.state;
            if (menu.state.contains("kind")) progress.acknowledge(menu.state.getInt("settingsRevision"));
            filter.load(menu.state, menu.level.registryAccess());
            requestedSettings.values().removeIf(request -> progress.confirmed(request.revision()));
            requestedSamples.values().removeIf(request -> progress.confirmed(request.revision()));
        }
        for (var button : settings) { button.active = ready(); button.updateLabel(); }
        if (!settings.isEmpty()) settings.getFirst().active &= java.util.stream.IntStream.range(0, BridgeFilter.SIZE).anyMatch(i -> !shownSample(i).isEmpty());
    }
    @Override public void containerTick() {
        super.containerTick();
        refresh();
    }
    @Override protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        if (ae) gui.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        else { gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFCBCBD5); gui.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF77788D); }
        gui.fill(leftPos - 24, topPos, leftPos, topPos + 140, 0xFF77788D);
        gui.fill(leftPos - 22, topPos + 2, leftPos, topPos + 138, 0xFFCBCBD5);
        for (int i = 0; i < BridgeFilter.SIZE; i++) {
            int x = leftPos + 7 + i % 9 * 18, y = topPos + 28 + i / 9 * 18;
            slotBackground(gui, x, y); gui.renderItem(shownSample(i), x + 1, y + 1);
            if (filter.mode == 0 && !shownSample(i).isEmpty()) gui.fill(x + 1, y + 1, x + 17, y + 17, 0x607E8095);
            if (requestedSamples.containsKey(i)) gui.renderOutline(x, y, 18, 18, 0xFFB5BEDA);
            if (sampleAt(mx, my) == i) gui.fill(x + 1, y + 1, x + 17, y + 17, 0x60FFFFFF);
        }
        if (!ae) for (var slot : menu.slots) slotBackground(gui, leftPos + slot.x - 1, topPos + slot.y - 1);
    }
    private void slotBackground(GuiGraphics gui, int x, int y) {
        if (ae) gui.blit(ICONS, x, y, 192, 192, 18, 18, 256, 256);
        else { gui.fill(x, y, x + 18, y + 18, 0xFF9294AA); gui.renderOutline(x, y, 18, 18, 0xFFE5E5EC); }
    }
    private Component status() {
        if (!menu.state.contains("kind")) return Component.translatable("gui.botanicalmekanism.loading");
        if (progress.waiting()) return Component.translatable("gui.botanicalmekanism.applying");
        if (!menu.state.getString("feedback").isEmpty()) return Component.translatable("gui.botanicalmekanism." + menu.state.getString("feedback"));
        return text("status." + menu.state.getString("status"));
    }
    @Override protected void renderLabels(GuiGraphics gui, int mx, int my) {
        gui.drawString(font, font.plainSubstrByWidth(title.getString(), 160), titleLabelX, titleLabelY, 0x40404F, false);
        Component shortStatus = !progress.waiting() && menu.state.getString("status").equals("ready") && menu.state.getString("feedback").isEmpty()
              ? text("online") : status();
        gui.drawString(font, font.plainSubstrByWidth(shortStatus.getString(), 156), 10, 18, 0x535568, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x40404F, false);
        String count = Arrays.stream(filter.samples).filter(stack -> !stack.isEmpty()).count() + "/" + BridgeFilter.SIZE;
        gui.drawString(font, count, 168 - font.width(count), inventoryLabelY, filter.mode == 0 ? 0x797B8B : 0x40404F, false);
    }
    private int sampleAt(double mx, double my) {
        double x = mx - leftPos - 7, y = my - topPos - 28;
        return x >= 0 && x < 162 && y >= 0 && y < 126 ? (int) y / 18 * 9 + (int) x / 18 : -1;
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        int sample = sampleAt(mx, my);
        if (sample >= 0) {
            if (button == 0 || button == 1) { dragButton = button; paintedSlots.clear(); editSample(sample, button); }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragButton >= 0) {
            int sample = sampleAt(mx, my); if (button == dragButton && sample >= 0) editSample(sample, button);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (dragButton >= 0) { dragButton = -1; paintedSlots.clear(); return true; }
        return super.mouseReleased(mx, my, button);
    }
    @Override protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        if (mx >= leftPos - 24 && mx < leftPos && my >= topPos && my < topPos + 140) return false;
        return super.hasClickedOutside(mx, my, left, top, button);
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        super.render(gui, mx, my, partial); renderTooltip(gui, mx, my);
        int sample = sampleAt(mx, my);
        if (sample >= 0 && menu.getCarried().isEmpty()) {
            if (!shownSample(sample).isEmpty()) gui.renderTooltip(font, shownSample(sample), mx, my);
            else gui.renderTooltip(font, font.split(text("sample_help"), 210), mx, my);
        } else if (mx >= leftPos + 128 && mx < leftPos + 168 && my >= topPos + inventoryLabelY && my < topPos + inventoryLabelY + 10) {
            var hint = text("filter." + filter.mode).copy().append("\n").append(text(filter.mode == 0 ? "samples_inactive" : "sample_help"));
            gui.renderTooltip(font, font.split(hint, 210), mx, my);
        } else if (mx >= leftPos + 8 && mx < leftPos + 168 && my >= topPos + 16 && my < topPos + 27) {
            var lines = new ArrayList<Component>(); lines.add(status());
            lines.add(text("ae", text(menu.state.getBoolean("ae_connected") ? "online" : "offline")));
            lines.add(text("nodes", menu.state.getInt("nodes"))); lines.add(text("stock", menu.state.getInt("types"), menu.state.getLong("items")));
            lines.add(text("me_stock", menu.state.getInt("me_types"), menu.state.getLong("me_items")));
            lines.add(text("transfer", menu.state.getInt("moved"), menu.state.getInt("limit")));
            lines.add(text("crafting", text("craft.status." + menu.state.getString("craft_status")), menu.state.getInt("craft_jobs")));
            gui.renderComponentTooltip(font, lines, mx, my);
        }
    }
    private final class SettingButton extends Button {
        private final Supplier<Component> label;
        private final Supplier<int[]> icon;
        private final IntConsumer action;
        private final boolean cycle;
        SettingButton(int x, int y, Supplier<Component> label, Supplier<int[]> icon, IntConsumer action, boolean cycle) {
            super(x, y, 18, 20, label.get(), button -> action.accept(1), DEFAULT_NARRATION);
            this.label = label; this.icon = icon; this.action = action; this.cycle = cycle; updateLabel();
        }
        void updateLabel() {
            Component next = label.get();
            if (getTooltip() == null || !next.equals(getMessage())) {
                setMessage(next); setTooltip(Tooltip.create(cycle ? next.copy().append("\n").append(text("cycle_help")) : next));
            }
        }
        @Override public boolean mouseClicked(double x, double y, int button) {
            if (button == 1 && visible && isMouseOver(x, y)) {
                if (active && cycle) { playDownSound(minecraft.getSoundManager()); action.accept(-1); }
                return true;
            }
            return super.mouseClicked(x, y, button);
        }
        @Override protected void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
            if (!ae) { super.renderWidget(gui, mx, my, partial); return; }
            int[] sprite = icon.get(); int backgroundX = isHovered() ? 212 : isFocused() ? 194 : 176;
            gui.blit(ICONS, getX(), getY(), backgroundX, 128, 18, 20, 256, 256);
            gui.blit(ICONS, getX() + 1, getY() + 2, sprite[0], sprite[1], 16, 16, 256, 256);
            if (!active) gui.fill(getX() + 1, getY() + 2, getX() + 17, getY() + 18, 0x808C8DA1);
        }
    }
}
