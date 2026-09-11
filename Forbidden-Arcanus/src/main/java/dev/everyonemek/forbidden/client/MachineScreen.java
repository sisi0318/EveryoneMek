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
        imageWidth = 258; imageHeight = 324; inventoryLabelX = 48; inventoryLabelY = 228; dynamicSlots = true;
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
        }) { @Override public void tick() { super.tick(); setMessage(label.get()); active = action != 0 || !tile.binding.embedded; } });
    }
    @Override protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 242, 24, 94));
        addRenderableWidget(new GuiEnergyTab(this, tile.energy(), tile::getActive));
        for (int i = 0; i < (tile.kind().forge() ? 0 : 7); i++) {
            if (i == 2) continue;
            int[] xy = MachineMenu.nativeCoordinates(tile.kind(), i);
            addRenderableWidget(new GuiSlot(SlotType.NORMAL, this, xy[0] - 1, xy[1] - 1));
        }
        ProgressType arrow = ProgressType.SMALL_RIGHT;
        addRenderableWidget(new GuiProgress(() -> Math.min(1, tile.progress / (double) tile.duration), arrow, this,
              tile.kind().forge() ? (71 + 199 - arrow.getWidth()) / 2 : (89 + 143 - arrow.getWidth()) / 2,
              (tile.kind().forge() ? 88 : 110) - arrow.getHeight() / 2));
        if (tile.kind().forge()) for (int i = 0; i < 4; i++) addRenderableWidget(new GuiResourceBar(this, tile, i, 18 + i * 55, 98));
        addRenderableWidget(new GuiInnerScreen(this, 18, tile.kind().forge() ? 154 : 126, 220, 18,
              () -> List.of(text(tile.kind().forge() && tile.status == Controller.STRUCTURE ? "platform_missing" : "status." + tile.status))));
        addRenderableWidget(new GuiInnerScreen(this, 18, tile.kind().forge() ? 176 : 148, 220, tile.kind().forge() ? 26 : 54, this::nativeDetails));
        if (!tile.kind().forge()) button(18, 207, 60, () -> text(tile.binding.embedded ? "auto_connected" : "bind"), 0);
        button(tile.kind().forge() ? 18 : 82, 207, tile.kind().forge() ? 106 : 50, () -> text(tile.enabled ? "pause" : "resume"), 1);
        addRenderableWidget(new MekanismButton(this, tile.kind().forge() ? 132 : 136, 207, tile.kind().forge() ? 106 : 58, 16, text("recipes"),
              (button, mx, my) -> { addWindow(new GuiRecipeSelector(this, tile, menu.containerId)); return true; }));
        if (!tile.kind().forge()) button(198, 207, 40, () -> text("xp"), 3);
    }
    private List<Component> nativeDetails() {
        var lines = new ArrayList<Component>();
        if (!tile.kind().forge()) {
            lines.add(text("target", tile.binding.label().isEmpty() ? text("unbound") : tile.binding.label()));
            if (tile.observed[0] < 0) { lines.add(text("unmeasured")); return lines; }
        }
        if (tile.kind().forge()) {
            lines.add(text("tier_progress", tile.nativeTier, tile.progress, tile.duration));
        } else {
            lines.add(text("fire_power", text("fire." + Math.max(0, tile.observed[7])), text(tile.observed[1] > 0 ? "heating" : "heat_idle"), Math.max(0, tile.observed[0]) / 20));
            lines.add(text("clibano_progress", Math.max(0, tile.observed[3]), Math.max(0, tile.observed[5]), Math.max(0, tile.observed[4]), Math.max(0, tile.observed[6])));
            lines.add(text("residues", value(8), 64));
        }
        if (choices == null) choices = Recipes.choices(tile.getLevel(), tile.kind());
        String selected = tile.recipeLock.isEmpty() ? tile.selectedRecipe : tile.recipeLock;
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
        if (!tile.kind().forge()) graphics.drawString(font, text("resource.1"), 18, 90, titleTextColor(), false);
        graphics.drawString(font, text("enhancers"), 112, 19, titleTextColor(), false);
        graphics.drawString(font, text(tile.kind().forge() ? "resources" : "resource.1"), 112, 54, titleTextColor(), false);
        if (!tile.kind().forge()) graphics.drawString(font, text("products"), 130, 90, titleTextColor(), false);
    }
}
