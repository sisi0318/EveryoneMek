package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Independent, compact implementation of list selection and separate management pages. */
public final class ResonanceScreen extends AbstractContainerScreen<FlowerMenu> {
    private static final int SETTINGS = 0, NETWORKS = 1, MEMBERS = 2, ONLINE = 3, CONNECTIONS = 4;
    private static final int INK = 0xE4E4E4, MUTED = 0xA5A7AA, ACCENT = 0xFF7BA69A;
    private final List<ChoiceButton> buttons = new ArrayList<>();
    private EditBox value, search;
    private int pageType, page, builtKind = -1, builtMode = -1;
    private String filter = "";
    private boolean core;
    private Component hover;
    private boolean initialPageChosen;
    private UUID selectedNetwork;
    private Pending pending;
    private int pendingMode;
    private String confirmedValue = "";
    private record Pending(int action, String value, int revision, String draft, int mode) {
        boolean acknowledged(CompoundTag state) {
            return state.getInt("settingsRevision") != revision && state.getInt("settingsAction") == action
                  && state.getString("settingsValue").equals(value);
        }
    }

    public ResonanceScreen(FlowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 280; imageHeight = 234;
        core = menu.level.getBlockState(menu.position).is(Content.CORE.get());
    }
    private Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism." + key, args); }
    private void send(int action, String payload) {
        if (pending != null) return;
        pending = new Pending(action, payload, menu.state.getInt("settingsRevision"), value == null ? "" : value.getValue(), builtMode);
        PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, payload));
    }
    private void page(int next) {
        initialPageChosen = true; pageType = next; page = 0; filter = ""; selectedNetwork = null;
        if (next == NETWORKS) selectDefaultNetwork();
        rebuildWidgets();
    }
    private ChoiceButton button(int x, int y, int width, Supplier<Component> label, Runnable action, BooleanSupplier enabled, BooleanSupplier selected) {
        return button(x, y, width, 18, label, action, enabled, selected);
    }
    private ChoiceButton button(int x, int y, int width, int height, Supplier<Component> label, Runnable action, BooleanSupplier enabled, BooleanSupplier selected) {
        var button = addRenderableWidget(new ChoiceButton(leftPos + x, topPos + y, width, height, label, action, enabled, selected));
        buttons.add(button); return button;
    }
    private ChoiceButton button(int x, int y, int width, Supplier<Component> label, Runnable action) {
        return button(x, y, width, label, action, () -> true, () -> false);
    }
    private EditBox field(int x, int y, int width, int length, Component label, int action) {
        var field = new EditBox(font, leftPos + x + 5, topPos + y + 5, width - 10, 10, label) {
            @Override public boolean keyPressed(int key, int scan, int modifiers) {
                if (isFocused() && (key == 257 || key == 335) && action >= 0) { send(action, getValue()); return true; }
                return super.keyPressed(key, scan, modifiers);
            }
        };
        field.setBordered(false); field.setTextColor(INK); field.setMaxLength(length);
        return addRenderableWidget(field);
    }
    @Override protected void init() {
        String draft = pageType == SETTINGS && value != null && !value.getValue().equals(confirmedValue) && builtKind == menu.state.getInt("kind") && builtMode == menu.state.getInt("mode") ? value.getValue() : null;
        super.init(); buttons.clear(); value = null; search = null;
        builtKind = menu.state.contains("kind") ? menu.state.getInt("kind") : -1;
        builtMode = menu.state.getInt("mode");
        if (builtKind >= 0) core = builtKind == 2;
        if (!initialPageChosen && builtKind >= 0) {
            initialPageChosen = true;
            if (!core && !menu.state.hasUUID("network")) { pageType = NETWORKS; selectDefaultNetwork(); }
        }
        confirmedValue = fieldValue();
        button(210, 208, 54, () -> Component.translatable("gui.done"), this::onClose).allowWhilePending = true;
        button(14, 30, 78, () -> text("tab.settings"), () -> page(SETTINGS), () -> true, () -> pageType == SETTINGS);
        if (core) {
            button(100, 30, 78, () -> text("tab.members"), () -> page(MEMBERS), () -> true, () -> pageType == MEMBERS || pageType == ONLINE);
            button(186, 30, 78, () -> text("tab.connections"), () -> page(CONNECTIONS), () -> true, () -> pageType == CONNECTIONS);
        } else button(100, 30, 78, () -> text("tab.networks"), () -> page(NETWORKS), () -> true, () -> pageType == NETWORKS);
        if (builtKind < 0) return;
        if (pageType != SETTINGS) { addList(); return; }
        button(14, 208, 62, () -> text(menu.state.getBoolean("enabled") ? "pause" : "resume"), () -> send(0, ""));
        if (core) {
            value = field(14, 76, 192, 32, text("network_name"), 6); value.setValue(draft != null ? draft : menu.state.getString("name"));
            button(214, 76, 50, () -> text("apply"), () -> send(6, value.getValue()));
        } else {
            for (int i = 0; i < 3; i++) {
                int mode = i;
                button(14 + i * 86, 55, 78, () -> text("mode." + mode), () -> send(FlowerMenu.SET_MODE, Integer.toString(mode)),
                      () -> menu.state.getInt("mode") != mode, () -> menu.state.getInt("mode") == mode);
            }
            if (builtMode != NetworkPlant.RELAY) {
                button(192, 77, 72, () -> text("detect_pool"), () -> send(FlowerMenu.DETECT_POOL, ""));
                for (int i = 0; i < 6; i++) {
                    int side = i;
                    button(14 + i * 42, 98, 38, () -> text("side." + side), () -> send(FlowerMenu.SET_DIRECTION, Integer.toString(side)),
                          () -> menu.state.getInt("direction") != side, () -> menu.state.getInt("direction") == side);
                }
                if (builtMode == NetworkPlant.RECEIVE) for (int i = 0; i < 3; i++) {
                    int priority = i;
                    button(98 + i * 56, 121, 54, () -> text("priority_short." + priority), () -> send(FlowerMenu.SET_PRIORITY, Integer.toString(priority)),
                          () -> menu.state.getInt("priority") != priority, () -> menu.state.getInt("priority") == priority);
                }
                int y = builtMode == NetworkPlant.RECEIVE ? 145 : 121;
                value = field(82, y, 88, 10, text(builtMode == NetworkPlant.SUPPLY ? "reserve" : "target"), 3);
                value.setFilter(s -> s.matches("[0-9]*")); value.setValue(draft != null ? draft : Integer.toString(menu.state.getInt("limit")));
                button(176, y, 44, () -> text("apply"), () -> send(3, value.getValue()), () -> pending == null && !value.getValue().equals(fieldValue()), () -> false);
                if (builtMode == NetworkPlant.RECEIVE) button(226, y, 38, () -> text("fill_target"), () -> send(FlowerMenu.FILL_TARGET, ""), () -> pending == null && menu.state.contains("maxMana"), () -> false);
            }
            button(84, 208, 62, () -> text("disconnect"), () -> send(8, ""), () -> menu.state.hasUUID("network"), () -> false);
        }
    }
    private String listKey() { return switch (pageType) { case NETWORKS -> "choices"; case MEMBERS -> "memberEntries"; case ONLINE -> "onlinePlayers"; default -> "connections"; }; }
    private List<CompoundTag> entries() {
        return menu.state.getList(listKey(), Tag.TAG_COMPOUND).stream().map(CompoundTag.class::cast)
              .filter(row -> row.getString("name").toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))).toList();
    }
    private CompoundTag row(int slot) {
        var rows = entries(); int index = page * rowsPerPage() + slot;
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }
    private boolean connected(CompoundTag row) { return row != null && row.hasUUID("id") && menu.state.hasUUID("network") && row.getUUID("id").equals(menu.state.getUUID("network")); }
    private int rowsPerPage() { return pageType == NETWORKS ? 5 : 6; }
    private int rowHeight() { return pageType == NETWORKS ? 16 : 18; }
    private int pagerY() { return pageType == NETWORKS ? 160 : 188; }
    private CompoundTag selectedNetwork() {
        for (int i = 0; i < rowsPerPage(); i++) {
            CompoundTag row = row(i);
            if (row != null && row.hasUUID("id") && row.getUUID("id").equals(selectedNetwork)) return row;
        }
        return null;
    }
    private void selectDefaultNetwork() {
        var rows = entries();
        if (menu.state.hasUUID("network")) for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getUUID("id").equals(menu.state.getUUID("network"))) {
                selectedNetwork = rows.get(i).getUUID("id"); page = i / rowsPerPage(); return;
            }
        }
        if (rows.size() == 1) selectedNetwork = rows.getFirst().getUUID("id");
    }
    private void turnPage(int delta) {
        page = Math.clamp(page + delta, 0, Math.max(0, (entries().size() - 1) / rowsPerPage()));
        if (pageType == NETWORKS) selectedNetwork = null;
    }
    private boolean canConnect() {
        CompoundTag row = selectedNetwork();
        return pending == null && row != null && (connected(row) || row.getInt("nodes") < Balance.NODE_LIMIT);
    }
    private void connectAs(int mode) {
        if (!canConnect()) return;
        String payload = selectedNetwork + "," + mode;
        pendingMode = mode;
        send(FlowerMenu.CONNECT_AS, payload);
    }
    private void addList() {
        search = field(14, 52, 250, 64, text("search"), -1); search.setValue(filter);
        search.setHint(text("search")); search.setResponder(s -> {
            filter = s; page = 0;
            if (pageType == NETWORKS) {
                selectedNetwork = null; var rows = entries();
                if (rows.size() == 1) selectedNetwork = rows.getFirst().getUUID("id");
            }
        });
        for (int i = 0; i < rowsPerPage(); i++) {
            int slot = i;
            button(14, 76 + i * rowHeight(), 250, rowHeight(), () -> listLabel(row(slot)), () -> clickRow(row(slot)),
                  () -> row(slot) != null && pageType != CONNECTIONS && pending == null,
                  () -> pageType == NETWORKS && row(slot) != null && row(slot).getUUID("id").equals(selectedNetwork));
        }
        button(14, pagerY(), 28, () -> Component.literal("<"), () -> turnPage(-1), () -> page > 0 && pending == null, () -> false);
        button(236, pagerY(), 28, () -> Component.literal(">"), () -> turnPage(1), () -> (page + 1) * rowsPerPage() < entries().size() && pending == null, () -> false);
        if (pageType == MEMBERS) button(14, 208, 96, () -> text("add_member"), () -> page(ONLINE));
        else if (pageType == ONLINE) button(14, 208, 96, () -> text("back"), () -> page(MEMBERS));
        else if (pageType == NETWORKS) {
            for (int i = 0; i < 3; i++) {
                int mode = i;
                button(14 + i * 86, 182, 78, () -> text("join_as." + mode), () -> connectAs(mode), this::canConnect, () -> false);
            }
            button(14, 208, 62, () -> text("disconnect"), () -> send(8, ""), () -> menu.state.hasUUID("network") && pending == null, () -> false);
        }
    }
    private Component listLabel(CompoundTag row) {
        if (row == null) return Component.empty();
        return switch (pageType) {
            case NETWORKS -> connected(row) ? text("connected_name", row.getString("name")) : Component.literal(row.getString("name"));
            case MEMBERS -> text("remove_name", row.getString("name"));
            case ONLINE -> text("add_name", row.getString("name"));
            default -> text("connection_row", row.contains("mode") ? text("mode." + row.getInt("mode")) : text("status.unloaded"), row.getString("name"));
        };
    }
    private void clickRow(CompoundTag row) {
        if (row == null || !row.hasUUID("id")) return;
        if (pageType == NETWORKS) selectedNetwork = row.getUUID("id");
        else if (pageType == MEMBERS) send(FlowerMenu.REMOVE_MEMBER, row.getUUID("id").toString());
        else if (pageType == ONLINE) send(FlowerMenu.ADD_MEMBER, row.getUUID("id").toString());
    }
    private String fieldValue() { return core ? menu.state.getString("name") : Integer.toString(menu.state.getInt("limit")); }
    private void updateConfirmedValue(Pending acknowledged) {
        String next = fieldValue();
        if (value != null) {
            boolean submittedUnchanged = acknowledged != null && (acknowledged.action() == 3 || acknowledged.action() == 6 || acknowledged.action() == FlowerMenu.FILL_TARGET)
                  && acknowledged.mode() == builtMode && value.getValue().equals(acknowledged.draft()) && menu.state.getString("feedback").isEmpty();
            if ((value.getValue().equals(confirmedValue) || submittedUnchanged) && !value.getValue().equals(next)) value.setValue(next);
        }
        confirmedValue = next;
    }
    @Override public void containerTick() {
        super.containerTick();
        Pending acknowledged = pending != null && pending.acknowledged(menu.state) ? pending : null;
        if (acknowledged != null) {
            pending = null;
            if (acknowledged.action() == FlowerMenu.CONNECT_AS) {
                String requestedId = acknowledged.value().split(",")[0];
                if (menu.state.getString("feedback").isEmpty() && menu.state.hasUUID("network")
                      && menu.state.getUUID("network").toString().equals(requestedId) && menu.state.getInt("mode") == pendingMode) page(SETTINGS);
            }
        }
        if (menu.state.contains("kind") && (builtKind != menu.state.getInt("kind") || builtMode != menu.state.getInt("mode"))) rebuildWidgets();
        updateConfirmedValue(acknowledged);
        if (value != null) value.setEditable(pending == null);
        if (search != null) search.setEditable(pending == null);
        page = Math.clamp(page, 0, Math.max(0, (entries().size() - 1) / rowsPerPage()));
        if (pageType == NETWORKS && selectedNetwork == null && entries().size() == 1) selectedNetwork = entries().getFirst().getUUID("id");
        buttons.forEach(ChoiceButton::refresh);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) { onClose(); return true; }
        if (key != 258 && getFocused() instanceof EditBox field) { field.keyPressed(key, scan, modifiers); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (pageType != SETTINGS && mx >= leftPos + 14 && mx < leftPos + 264 && my >= topPos + 76 && my < topPos + 76 + rowsPerPage() * rowHeight() && dy != 0 && pending == null) {
            turnPage(dy < 0 ? 1 : -1); return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
    @Override protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0212328);
        gui.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xA04B4E53);
        for (EditBox field : new EditBox[]{value, search}) if (field != null) {
            gui.fill(field.getX() - 5, field.getY() - 5, field.getRight() + 5, field.getBottom() + 4, 0xB0101114);
            gui.renderOutline(field.getX() - 5, field.getY() - 5, field.getWidth() + 10, 19, field.isFocused() ? ACCENT : 0xFF55585D);
        }
    }
    private void line(GuiGraphics gui, Component label, int x, int y, int width, int color, int mx, int my) {
        String full = label.getString(); boolean clip = font.width(full) > width;
        String shown = clip ? font.plainSubstrByWidth(full, width - font.width("…")) + "…" : full;
        gui.drawString(font, shown, x, y, color, false);
        if (clip && mx >= x && mx < x + width && my >= y && my < y + 10) hover = label;
    }
    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        line(gui, title, 14, 12, 116, INK, mx, my);
        line(gui, menu.state.getString("name").isEmpty() ? text("unlinked") : Component.literal(menu.state.getString("name")), 142, 12, 122, MUTED, mx, my);
        if (builtKind < 0) return;
        String feedback = menu.state.getString("feedback");
        Component status = text(pending != null ? "applying" : feedback.isEmpty() ? "status." + menu.state.getString("status") : feedback);
        if (pageType != SETTINGS) {
            if (entries().isEmpty()) line(gui, text(pageType == NETWORKS && filter.isEmpty() ? "no_networks" : "empty_list"), 20, 81, 236, MUTED, mx, my);
            gui.drawCenteredString(font, (page + 1) + " / " + Math.max(1, (entries().size() + rowsPerPage() - 1) / rowsPerPage()), 140, pagerY() + 4, MUTED);
            for (int i = 0; i < rowsPerPage(); i++) {
                CompoundTag row = row(i);
                if (row != null && mx >= 14 && mx < 264 && my >= 76 + i * rowHeight() && my < 76 + (i + 1) * rowHeight()) {
                    if (pageType == NETWORKS) hover = text("network_details", row.getString("name"), row.getInt("nodes"), Balance.NODE_LIMIT,
                          text(row.getBoolean("online") ? "online" : "offline"), row.getString("location"));
                    else if (pageType == CONNECTIONS) hover = text("connection_details", row.getString("name"), text("status." + row.getString("status")), row.getInt("moved"));
                    else hover = listLabel(row);
                }
            }
            if (pageType == NETWORKS && my >= 182 && my < 200) {
                int mode = (mx - 14) / 86;
                if (mx >= 14 && mode >= 0 && mode < 3 && mx < 14 + mode * 86 + 78) {
                    CompoundTag row = selectedNetwork();
                    hover = row == null ? text("select_network_first") : row.getInt("nodes") >= Balance.NODE_LIMIT && !connected(row)
                          ? text("network_full") : text("join_help", row.getString("name"), text("mode_help." + mode));
                }
            }
        } else if (core) {
            line(gui, text("network_name"), 14, 60, 250, MUTED, mx, my);
            line(gui, text("nodes", menu.state.getInt("nodes"), Balance.NODE_LIMIT), 14, 128, 120, MUTED, mx, my);
            line(gui, text("member_count", menu.state.getInt("memberCount")), 144, 128, 120, MUTED, mx, my);
            line(gui, text("flow_fee", menu.state.getInt("delivered"), menu.state.getInt("fee")), 14, 148, 250, MUTED, mx, my);
            line(gui, status, 14, 180, 250, MUTED, mx, my);
        } else {
            if (builtMode != NetworkPlant.RELAY) {
                line(gui, text("target_side"), 14, 81, 168, MUTED, mx, my);
                if (builtMode == NetworkPlant.RECEIVE) line(gui, text("priority_label"), 14, 126, 78, MUTED, mx, my);
                int y = builtMode == NetworkPlant.RECEIVE ? 145 : 121;
                line(gui, text(builtMode == NetworkPlant.SUPPLY ? "reserve" : "target"), 14, y + 5, 62, MUTED, mx, my);
                int summary = builtMode == NetworkPlant.RECEIVE ? 166 : 146;
                if (value != null && !value.getValue().equals(fieldValue()))
                    line(gui, text("confirmed_limit", menu.state.getInt("limit")), 14, summary, 250, MUTED, mx, my);
                line(gui, menu.state.contains("maxMana") ? text("mana", menu.state.getInt("mana"), menu.state.getInt("maxMana")) : text("unmeasured"), 14, summary + 12, 250, MUTED, mx, my);
            } else line(gui, text("relay_hint"), 14, 94, 250, MUTED, mx, my);
            line(gui, status, 14, 188, 250, MUTED, mx, my);
            if (my >= 55 && my < 73) {
                int mode = (mx - 14) / 86;
                if (mx >= 14 && mode >= 0 && mode < 3 && mx < 14 + mode * 86 + 78) hover = text("mode_help." + mode);
            }
        }
        if (pageType != SETTINGS && pending != null) line(gui, text(pending.action() == FlowerMenu.CONNECT_AS ? "connecting" : "applying"), 112, 212, 92, MUTED, mx, my);
        else if (pageType != SETTINGS && !feedback.isEmpty()) line(gui, text(feedback), 112, 212, 92, 0xE6B17C, mx, my);
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        hover = null; super.render(gui, mx, my, partial); renderTooltip(gui, mx, my);
        if (hover != null) gui.renderTooltip(font, font.split(hover, 250), mx, my);
    }
    private final class ChoiceButton extends Button {
        private final Supplier<Component> label;
        private final BooleanSupplier enabled, selected;
        private boolean allowWhilePending;
        ChoiceButton(int x, int y, int width, int height, Supplier<Component> label, Runnable action, BooleanSupplier enabled, BooleanSupplier selected) {
            super(x, y, width, height, label.get(), b -> action.run(), DEFAULT_NARRATION);
            this.label = label; this.enabled = enabled; this.selected = selected; refresh();
        }
        void refresh() { setMessage(label.get()); active = enabled.getAsBoolean() && (pending == null || allowWhilePending); }
        @Override protected void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
            if (getMessage().getString().isEmpty()) return;
            gui.fill(getX(), getY(), getRight(), getBottom(), isHoveredOrFocused() && active ? 0xE0494D53 : 0xD034373D);
            gui.renderOutline(getX(), getY(), width, height, selected.getAsBoolean() || isFocused() ? ACCENT : 0xFF55585D);
            String full = getMessage().getString(); boolean clip = font.width(full) > width - 10;
            String shown = clip ? font.plainSubstrByWidth(full, width - 10 - font.width("…")) + "…" : full;
            gui.drawString(font, shown, getX() + (width - font.width(shown)) / 2, getY() + (height - font.lineHeight) / 2 + 1, active || selected.getAsBoolean() ? INK : MUTED, false);
            // List rows have richer tooltips (location, state and flow) drawn by the screen.
            boolean listRow = pageType != SETTINGS && width == 250 && getY() >= topPos + 76 && getY() < topPos + 184;
            setTooltip(clip && !listRow ? Tooltip.create(getMessage()) : null);
        }
    }
}
