package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.scroll.GuiScrollList;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuiManaRecipeSelector extends GuiWindow {
    private final List<ManaWork.Choice> all;
    private List<ManaWork.Choice> filtered;
    private final ManaMachine machine;
    private final int containerId;
    private final GuiTextField search;
    private final ChoiceList list;
    private final ManaMachineMenu menu;
    private int pending = -1;
    private String requested = "";

    static Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism.machine." + key, args); }

    public GuiManaRecipeSelector(IGuiWrapper gui, ManaMachineMenu menu) {
        super(gui, 11, 22, 216, 220, WindowType.UNSPECIFIED);
        this.menu = menu; this.machine = menu.getTileEntity(); this.containerId = menu.containerId;
        var entries = new ArrayList<ManaWork.Choice>();
        for (var raw : menu.state.getList("choices", Tag.TAG_COMPOUND)) {
            var entry = (net.minecraft.nbt.CompoundTag) raw;
            var id = net.minecraft.resources.ResourceLocation.tryParse(entry.getString("id"));
            var icon = ItemStack.parseOptional(machine.getLevel().registryAccess(), entry.getCompound("icon"));
            if (id != null && !icon.isEmpty()) entries.add(new ManaWork.Choice(id, icon));
        }
        all = entries.stream().sorted(Comparator.comparing(c -> c.icon().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)).toList();
        filtered = all;
        search = addChild(new GuiTextField(gui, this, relativeX + 6, relativeY + 23, 204, 16));
        search.setMaxLength(80);
        list = addChild(new ChoiceList(gui, relativeX + 6, relativeY + 45, 154));
        search.setResponder(value -> refresh());
        addChild(new MekanismButton(gui, relativeX + 6, relativeY + 201, 99, 16,
              text("automatic"), (button, mx, my) -> { select(""); return true; }));
        addChild(new MekanismButton(gui, relativeX + 111, relativeY + 201, 99, 16,
              text("close"), this::close));
        setFocused(search);
        search.setFocused(true);
    }

    private void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        filtered = all.stream().filter(c -> c.icon().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        list.resetScroll();
    }

    private void select(String id) {
        if (pending >= 0) return;
        pending = menu.state.getInt("revision"); requested = id;
        PacketDistributor.sendToServer(new FlowerPackets.Settings(containerId, 3, id));
    }

    @Override protected boolean isFocusOverlay() { return true; }
    @Override public boolean isMouseOver(double x, double y) { return true; }

    @Override public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, text(pending >= 0 ? "applying" : "choose_recipe"), 5);
        if (search.getText().isEmpty())
            graphics.drawString(font(), text("search_name"), search.getRelativeX() + 4, search.getRelativeY() + 4, 0x888888, false);
        if (filtered.isEmpty()) graphics.drawString(font(), text("no_results"), list.getRelativeX() + 8, list.getRelativeY() + 10, 0xDDDDDD, false);
    }

    @Override public void tick() {
        super.tick();
        if (pending >= 0 && menu.state.getInt("revision") != pending) {
            pending = -1;
            if (menu.state.getBoolean("accepted") && menu.state.getString("recipe").equals(requested)) close();
        }
    }

    private final class ChoiceList extends GuiScrollList {
        private ChoiceList(IGuiWrapper gui, int x, int y, int height) {
            super(gui, x, y, 204, height, 22, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE);
        }
        void resetScroll() { scroll = 0; }
        @Override protected int getMaxElements() { return filtered.size(); }
        @Override public boolean hasSelection() { return false; }
        @Override public void clearSelection() { }
        @Override protected void setSelected(int index) {
            if (index >= 0 && index < filtered.size()) select(filtered.get(index).id().toString());
        }
        private int hovered(int x, int y) {
            if (x < getX() + 1 || x >= getX() + barXShift || y < getY() + 1 || y >= getY() + 1 + getFocusedElements() * elementHeight) return -1;
            int index = getCurrentSelection() + (y - getY() - 1) / elementHeight;
            return index < filtered.size() ? index : -1;
        }
        @Override protected void renderElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int hovered = hovered(mouseX, mouseY);
            for (int row = 0; row < getFocusedElements() && getCurrentSelection() + row < filtered.size(); row++) {
                var choice = filtered.get(getCurrentSelection() + row);
                if (getCurrentSelection() + row == hovered || choice.id().toString().equals(menu.state.getString("recipe")))
                    graphics.fill(relativeX + 1, relativeY + 1 + row * elementHeight, relativeX + barXShift,
                          relativeY + 1 + (row + 1) * elementHeight, 0xFF424252);
            }
        }
        @Override public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderForeground(graphics, mouseX, mouseY);
            for (int row = 0; row < getFocusedElements() && getCurrentSelection() + row < filtered.size(); row++) {
                var choice = filtered.get(getCurrentSelection() + row);
                int y = 3 + row * elementHeight;
                gui().renderItem(graphics, choice.icon(), relativeX + 3, relativeY + y);
                drawScaledScrollingString(graphics, choice.icon().getHoverName(), 24, y, TextAlignment.LEFT, 0xEEEEEE,
                      barXShift - 24, 9, 2, false, 0.85F, getTimeOpened());

            }
        }
        @Override public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            int index = hovered(mouseX, mouseY);
            if (index >= 0) {
                var choice = filtered.get(index);
                gui().renderItemTooltipWithExtra(graphics, choice.icon(), mouseX, mouseY, List.of());
            } else super.renderToolTip(graphics, mouseX, mouseY);
        }
    }
}
