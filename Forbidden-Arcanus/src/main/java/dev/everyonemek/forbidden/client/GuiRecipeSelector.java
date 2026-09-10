package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.*;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import java.util.*;
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
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuiRecipeSelector extends GuiWindow {
    private final Controller machine;
    private final int containerId;
    private final List<Recipes.Choice> all;
    private List<Recipes.Choice> filtered;
    private final GuiTextField search;
    private final ChoiceList list;
    private boolean applicable;
    private static Component text(String key, Object... args) { return MachineScreen.text(key, args); }
    public GuiRecipeSelector(IGuiWrapper gui, Controller machine, int containerId) {
        super(gui, 21, 24, 216, 230, WindowType.UNSPECIFIED);
        this.machine = machine; this.containerId = containerId;
        all = Recipes.choices(machine.getLevel(), machine.kind()).stream()
              .sorted(Comparator.comparing(c -> c.name().getString(), String.CASE_INSENSITIVE_ORDER)).toList();
        filtered = all;
        search = addChild(new GuiTextField(gui, this, relativeX + 6, relativeY + 23, 204, 16));
        search.setMaxLength(80);
        list = addChild(new ChoiceList(gui, relativeX + 6, relativeY + 65));
        search.setResponder(value -> refresh());
        addChild(new MekanismButton(gui, relativeX + 6, relativeY + 44, 204, 16, text("all_recipes"), (button, mx, my) -> {
            applicable = !applicable; refresh(); return true;
        }) { @Override public void tick() { super.tick(); setMessage(text(applicable ? "applicable" : "all_recipes")); } });
        addChild(new MekanismButton(gui, relativeX + 6, relativeY + 211, 99, 16, text("automatic"), (button, mx, my) -> { select(""); return true; }));
        addChild(new MekanismButton(gui, relativeX + 111, relativeY + 211, 99, 16, text("close"), this::close));
        setFocused(search); search.setFocused(true);
    }
    private void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        filtered = all.stream().filter(c -> c.name().getString().toLowerCase(Locale.ROOT).contains(query))
              .filter(c -> !applicable || usable(c)).toList();
        list.resetScroll();
    }
    private boolean usable(Recipes.Choice choice) {
        if (machine.kind().forge()) {
            var ritual = machine.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL).get(choice.id());
            return ritual != null && ritual.requirements().tier().test(machine.nativeTier);
        }
        return Recipes.allocate(choice.materials(), machine.stock.stream().map(s -> s.getStack()).toList()) != null;
    }
    private void select(String id) { PacketDistributor.sendToServer(new SetRecipePayload(containerId, id)); close(); }
    @Override protected boolean isFocusOverlay() { return true; }
    @Override public boolean isMouseOver(double x, double y) { return true; }
    @Override public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, text("recipes"), 5);
        if (search.getText().isEmpty()) graphics.drawString(font(), text("search"), search.getRelativeX() + 4, search.getRelativeY() + 4, 0x888888, false);
        if (filtered.isEmpty()) graphics.drawString(font(), text("no_results"), list.getRelativeX() + 8, list.getRelativeY() + 10, 0xDDDDDD, false);
    }
    private static List<Component> details(Recipes.Choice choice) {
        var counts = new LinkedHashMap<String, Integer>();
        for (var ingredient : choice.materials()) {
            var items = ingredient.getItems();
            String name = items.length == 0 ? text("missing_ingredient").getString() : items[0].getHoverName().getString();
            counts.merge(name, 1, Integer::sum);
        }
        var result = new ArrayList<Component>();
        if (choice.upgrade()) result.add(text("upgrade_once"));
        counts.forEach((name, count) -> result.add(text("material", name, count)));
        return result;
    }
    private final class ChoiceList extends GuiScrollList {
        ChoiceList(IGuiWrapper gui, int x, int y) { super(gui, x, y, 204, 140, 22, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE); }
        void resetScroll() { scroll = 0; }
        @Override protected int getMaxElements() { return filtered.size(); }
        @Override public boolean hasSelection() { return false; }
        @Override public void clearSelection() { }
        @Override protected void setSelected(int index) { if (index >= 0 && index < filtered.size()) select(filtered.get(index).id().toString()); }
        private int hovered(int x, int y) {
            if (x < getX() + 1 || x >= getX() + barXShift || y < getY() + 1 || y >= getY() + 1 + getFocusedElements() * elementHeight) return -1;
            int index = getCurrentSelection() + (y - getY() - 1) / elementHeight;
            return index < filtered.size() ? index : -1;
        }
        @Override protected void renderElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int hovered = hovered(mouseX, mouseY);
            for (int row = 0; row < getFocusedElements() && getCurrentSelection() + row < filtered.size(); row++) {
                var choice = filtered.get(getCurrentSelection() + row);
                if (getCurrentSelection() + row == hovered || choice.id().toString().equals(machine.recipeLock))
                    graphics.fill(relativeX + 1, relativeY + 1 + row * elementHeight, relativeX + barXShift,
                          relativeY + 1 + (row + 1) * elementHeight, 0xFF424252);
            }
        }
        @Override public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderForeground(graphics, mouseX, mouseY);
            for (int row = 0; row < getFocusedElements() && getCurrentSelection() + row < filtered.size(); row++) {
                var choice = filtered.get(getCurrentSelection() + row); int y = 3 + row * elementHeight;
                gui().renderItem(graphics, choice.icon(), relativeX + 3, relativeY + y);
                drawScaledScrollingString(graphics, choice.name(), 24, y, TextAlignment.LEFT, 0xEEEEEE, barXShift - 24, 9, 2, false, .85F, getTimeOpened());
                drawScaledScrollingString(graphics, text(choice.upgrade() ? "upgrade_once" : "batch_production"), 24, y + 10,
                      TextAlignment.LEFT, 0xB5B5C8, barXShift - 24, 9, 2, false, .75F, getTimeOpened());
            }
        }
        @Override public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            int index = hovered(mouseX, mouseY);
            if (index >= 0) { var choice = filtered.get(index); gui().renderItemTooltipWithExtra(graphics, choice.icon(), mouseX, mouseY, details(choice)); }
            else super.renderToolTip(graphics, mouseX, mouseY);
        }
    }
}
