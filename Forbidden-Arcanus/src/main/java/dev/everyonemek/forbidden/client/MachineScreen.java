package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.*;
import java.util.*;
import java.util.function.Supplier;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.window.GuiUpgradeWindowTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MachineScreen extends GuiConfigurableTile<Controller, MachineMenu> {
    private GuiUpgradeWindowTab upgradeTab;
    private List<Recipes.Choice> choices;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 258; imageHeight = tile.kind().forge() ? 324 : 300;
        inventoryLabelX = 48; inventoryLabelY = tile.kind().forge() ? 228 : 204; dynamicSlots = true;
    }
    public static Component text(String key, Object... args) { return Component.translatable("gui.forbiddenmekanism." + key, args); }
    @Override protected void addGenericTabs() {
        super.addGenericTabs();
        if (tile.kind().forge()) {
            var original = children().stream().filter(GuiUpgradeWindowTab.class::isInstance).findFirst().orElseThrow();
            removeWidget(original);
            upgradeTab = addRenderableWidget(new GuiUpgradeWindowTab(this, tile, () -> upgradeTab) {
                @Override protected GuiWindow createWindow(SelectedWindowData data) {
                    return new ResourceUpgradeWindow(MachineScreen.this, (getGuiWidth() - 198) / 2, 15, tile, data);
                }
            });
        }
    }
    private void button(int x, int y, int width, Supplier<Component> label, int action) {
        addRenderableWidget(new MekanismButton(this, x, y, width, 16, label.get(), (button, mx, my) -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); return true;
        }) { @Override public void tick() { super.tick(); setMessage(label.get()); } });
    }
    @Override protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 242, 24, 94));
        addRenderableWidget(new GuiEnergyTab(this, tile.energy(), tile::getActive));
        for (int i = 0; i < (tile.kind().forge() ? 0 : 2); i++) {
            int[] xy = MachineMenu.nativeCoordinates(tile.kind(), i);
            addRenderableWidget(new GuiSlot(SlotType.NORMAL, this, xy[0] - 1, xy[1] - 1));
        }
        if (tile.kind().forge()) {
            ProgressType arrow = ProgressType.SMALL_RIGHT;
            addRenderableWidget(new GuiProgress(() -> Math.min(1, tile.progress / (double) tile.duration), arrow, this,
                  (71 + 199 - arrow.getWidth()) / 2, 88 - arrow.getHeight() / 2));
            for (int i = 0; i < 4; i++) addRenderableWidget(new GuiResourceBar(this, tile, i, 18 + i * 55, 98));
        } else {
            addRenderableWidget(new GuiClibanoBar(this, tile, -1, 100, 65, 86));
            addRenderableWidget(new GuiClibanoBar(this, tile, 0, 94, 92, 92));
            addRenderableWidget(new GuiClibanoBar(this, tile, 1, 94, 106, 92));
        }
        addRenderableWidget(new GuiInnerScreen(this, 18, tile.kind().forge() ? 154 : 120, 220, 18,
              () -> List.of(text(tile.kind().forge() && tile.status == Controller.STRUCTURE ? "platform_missing" : "status." + tile.status))));
        addRenderableWidget(new GuiInnerScreen(this, 18, tile.kind().forge() ? 176 : 142, 220, tile.kind().forge() ? 26 : 28, this::nativeDetails));
        int buttonY = tile.kind().forge() ? 207 : 178;
        button(18, buttonY, tile.kind().forge() ? 106 : 62, () -> text(tile.enabled ? "pause" : "resume"), 1);
        addRenderableWidget(new MekanismButton(this, tile.kind().forge() ? 132 : 86, buttonY, tile.kind().forge() ? 106 : 100, 16, text("recipes"),
              (button, mx, my) -> { addWindow(new GuiRecipeSelector(this, tile, menu.containerId)); return true; }));
        if (!tile.kind().forge()) button(192, buttonY, 46, () -> text("xp"), 3);
    }
    private List<Component> nativeDetails() {
        var lines = new ArrayList<Component>();
        if (!tile.kind().forge() && tile.observed[0] < 0) return List.of(text("unmeasured"));
        if (tile.kind().forge()) {
            lines.add(text("tier_progress", tile.nativeTier, tile.progress, tile.duration));
        } else {
            lines.add(text("clibano_summary", text("fire." + Math.max(0, tile.observed[7])), text(tile.observed[1] > 0 ? "heating" : "heat_idle"), value(8)));
        }
        if (choices == null) choices = Recipes.choices(tile.getLevel(), tile.kind());
        String selected = tile.recipeLock.isEmpty() && tile.kind().forge() ? tile.selectedRecipe : tile.recipeLock;
        var choice = choices.stream().filter(c -> c.id().toString().equals(selected)).findFirst().orElse(null);
        lines.add(choice == null ? text("automatic") : choice.name());
        return lines;
    }
    private Object value(int index) { return tile.observed[index] < 0 ? "—" : tile.observed[index]; }
    @Override protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawForegroundText(graphics, mouseX, mouseY);
        renderTitleText(graphics);
        renderInventoryText(graphics);
        graphics.drawString(font, text("stock"), 18, 19, titleTextColor(), false);
        graphics.drawString(font, text("output"), 200, 19, titleTextColor(), false);
        if (tile.kind().forge()) {
            graphics.drawString(font, text("enhancers"), 112, 19, titleTextColor(), false);
            graphics.drawString(font, text("resources"), 112, 54, titleTextColor(), false);
        } else {
            graphics.drawString(font, text("enhancer"), 100, 19, titleTextColor(), false);
            graphics.drawString(font, text("resource.1"), 156, 19, titleTextColor(), false);
            graphics.drawString(font, text("soul_burning"), 100, 54, titleTextColor(), false);
            graphics.drawString(font, text("processing"), 100, 81, titleTextColor(), false);
            graphics.drawString(font, "1", 82, 92, titleTextColor(), false);
            if (tile.observed[9] != 1) graphics.drawString(font, "2", 82, 106, titleTextColor(), false);
        }
    }
}
