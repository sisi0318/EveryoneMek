package dev.everyonemek.ars.client;

import dev.everyonemek.ars.MachineKind;
import dev.everyonemek.ars.RecipeChoices;
import dev.everyonemek.ars.SetRecipeLockPayload;
import dev.everyonemek.ars.SourceMachine;
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

public final class GuiRecipeSelector extends GuiWindow {
    private final List<RecipeChoices.Choice> all;
    private List<RecipeChoices.Choice> filtered;
    private final SourceMachine machine;
    private final int containerId;
    private final GuiTextField search;
    private final ChoiceList list;
    private int tier;

    static Component text(String key, Object... args) { return Component.translatable("gui.arsmekanism." + key, args); }

    public GuiRecipeSelector(IGuiWrapper gui, SourceMachine machine, int containerId) {
        super(gui, 11, 22, 216, 220, WindowType.UNSPECIFIED);
        this.machine = machine;
        this.containerId = containerId;
        all = RecipeChoices.available(machine.getLevel(), machine.kind(), machine.mode()).stream()
              .sorted(Comparator.comparing(c -> c.name().getString(), String.CASE_INSENSITIVE_ORDER)).toList();
        filtered = all;
        boolean glyph = machine.kind() == MachineKind.GLYPH_SCRIBE;
        search = addChild(new GuiTextField(gui, this, relativeX + 6, relativeY + 23, 204, 16));
        search.setMaxLength(80);
        list = addChild(new ChoiceList(gui, relativeX + 6, relativeY + (glyph ? 65 : 45), glyph ? 134 : 154));
        search.setResponder(value -> refresh());
        if (glyph) for (int i = 0; i <= 3; i++) {
            final int value = i;
            addChild(new MekanismButton(gui, relativeX + 6 + i * 52, relativeY + 44, 48, 16,
                  text(i == 0 ? "tier_all" : "tier." + i), (button, mx, my) -> { tier = value; refresh(); return true; }) {
                @Override public void tick() { super.tick(); active = tier != value; }
            });
        }
        addChild(new MekanismButton(gui, relativeX + 6, relativeY + 201, 99, 16,
              text(glyph ? "clear_selection" : "automatic"), (button, mx, my) -> { select(""); return true; }));
        addChild(new MekanismButton(gui, relativeX + 111, relativeY + 201, 99, 16,
              text("close"), this::close));
        setFocused(search);
        search.setFocused(true);
    }

    private void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        filtered = all.stream().filter(c -> (tier == 0 || c.tier() == tier)
              && c.name().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        list.resetScroll();
    }

    private void select(String id) {
        PacketDistributor.sendToServer(new SetRecipeLockPayload(containerId, id));
        close();
    }

    @Override protected boolean isFocusOverlay() { return true; }
    @Override public boolean isMouseOver(double x, double y) { return true; }

    @Override public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, text(machine.kind() == MachineKind.GLYPH_SCRIBE ? "choose_glyph" : "choose_recipe"), 5);
        if (search.getText().isEmpty())
            graphics.drawString(font(), text("search_name"), search.getRelativeX() + 4, search.getRelativeY() + 4, 0x888888, false);
        if (filtered.isEmpty()) graphics.drawString(font(), text("no_results"), list.getRelativeX() + 8, list.getRelativeY() + 10, 0xDDDDDD, false);
    }

    static Component cost(RecipeChoices.Choice choice) {
        if (choice.tier() > 0) return text("glyph_cost", text("tier." + choice.tier()), choice.experience());
        return choice.source() > 0 ? text("source_cost", choice.source()) : text("power_only");
    }

    static List<Component> details(RecipeChoices.Choice choice) {
        var result = new ArrayList<Component>();
        result.add(cost(choice));
        if (!choice.input().isEmpty()) result.add(text("recipe_input", ingredientName(choice.input())));
        var counts = new LinkedHashMap<String, Integer>();
        for (Ingredient ingredient : choice.materials()) counts.merge(ingredientName(ingredient), 1, Integer::sum);
        if (!counts.isEmpty()) {
            result.add(text("required_materials"));
            counts.forEach((name, count) -> result.add(text("material_count", name, count)));
        }
        return result;
    }

    private static String ingredientName(Ingredient ingredient) {
        var items = ingredient.getItems();
        if (items.length == 0) return text("missing_ingredient").getString();
        String name = items[0].getHoverName().getString();
        return items.length == 1 ? name : text("ingredient_options", name, items.length).getString();
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
                if (getCurrentSelection() + row == hovered || choice.id().toString().equals(machine.recipeLock()))
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
                drawScaledScrollingString(graphics, choice.name(), 24, y, TextAlignment.LEFT, 0xEEEEEE,
                      barXShift - 24, 9, 2, false, 0.85F, getTimeOpened());
                drawScaledScrollingString(graphics, cost(choice), 24, y + 10, TextAlignment.LEFT, 0xB5B5C8,
                      barXShift - 24, 9, 2, false, 0.75F, getTimeOpened());
            }
        }
        @Override public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            int index = hovered(mouseX, mouseY);
            if (index >= 0) {
                var choice = filtered.get(index);
                gui().renderItemTooltipWithExtra(graphics, choice.icon(), mouseX, mouseY, details(choice));
            } else super.renderToolTip(graphics, mouseX, mouseY);
        }
    }
}
