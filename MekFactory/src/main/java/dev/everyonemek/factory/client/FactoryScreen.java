package dev.everyonemek.factory.client;

import dev.everyonemek.factory.*;
import java.util.List;
import java.util.Locale;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FactoryScreen extends GuiMekanismTile<Controller, FactoryMenu> {
    private PortConfigurationTab portTab;
    public FactoryScreen(FactoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 230; imageHeight = 260;
        inventoryLabelX = 34; inventoryLabelY = 166; dynamicSlots = true;
    }
    private static String exactFE(long joules) { return TextUtils.format((long) EnergyUnit.FORGE_ENERGY.convertTo(joules)); }
    private static String compactFE(long joules) {
        double value = EnergyUnit.FORGE_ENERGY.convertTo(joules);
        String[] prefixes = {"", "k", "M", "G", "T", "P", "E"};
        int unit = 0;
        while (value >= 1_000 && unit < prefixes.length - 1) { value /= 1_000; unit++; }
        return (unit == 0 ? TextUtils.format((long) value) : String.format(Locale.ROOT, "%.2f", value)) + " " + prefixes[unit] + "FE/t";
    }
    private String percent() { return String.format(Locale.ROOT, "%.1f%%", menu.progressRatio() * 100); }
    private Component product() {
        if (!menu.product.isEmpty()) return menu.product.getHoverName();
        if (!menu.productFluid.isEmpty()) return Component.translatable(menu.productFluid.getDescriptionId());
        if (!menu.productChemical.isEmpty()) return menu.productChemical.getTextComponent();
        return Content.text("no_recipe");
    }
    private List<Component> parallelDetails() {
        return List.of(Content.text("machine_lanes", tile.template.getCount(), Profiles.processingLines(tile.template.getStack()), Profiles.machineParallel(tile.template.getStack())),
              Content.text("frame_limit", menu.frameParallel), Content.text("limit", tile.parallelLimit), Content.text("available_parallel", menu.availableParallel));
    }
    @Override protected void addGuiElements() {
        super.addGuiElements();
        portTab = addRenderableWidget(new PortConfigurationTab(this, tile, menu.containerId, () -> portTab));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 214, 54, 90));
        addRenderableWidget(new GuiInnerScreen(this, 18, 54, 194, 17, () -> List.of(Content.text("current_recipe", product())))
              .tooltip(() -> List.of(product(), Content.text("batch", menu.jobUnits, menu.jobCount == 0 ? 0 : menu.jobIndex + 1, menu.jobCount))));
        addRenderableWidget(new GuiProgress(new IProgressInfoHandler() {
            @Override public double getProgress() { return menu.progressRatio(); }
            @Override public boolean isActive() { return true; }
        }, ProgressType.LARGE_RIGHT, this, 18, 80) {
            @Override public void updateTooltip(int x, int y) { setTooltip(TooltipUtils.create(Content.text("recipe_progress", percent()))); }
        });
        for (int i = 0; i < 2; i++) {
            final int action = i == 0 ? 70 : 71;
            addRenderableWidget(new MekanismButton(this, i == 0 ? 174 : 194, 76, 18, 16, Component.literal(i == 0 ? "<" : ">"),
                  (b, x, y) -> { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); return true; }) {
                @Override public void tick() { super.tick(); visible = menu.jobCount > 1; active = action == 70 ? menu.jobIndex > 0 : menu.jobIndex + 1 < menu.jobCount; }
                @Override public void updateTooltip(int x, int y) { setTooltip(TooltipUtils.create(Content.text("job_page", menu.jobIndex + 1, menu.jobCount))); }
            });
        }
        addRenderableWidget(new GuiInnerScreen(this, 18, 98, 94, 28,
              () -> List.of(Content.text("parallel_label"), Component.literal(tile.running + " / " + menu.availableParallel)))
              .spacing(0).clearScale().tooltip(this::parallelDetails));
        addRenderableWidget(new GuiInnerScreen(this, 118, 98, 94, 28,
              () -> List.of(Content.text("power_label"), Component.literal(compactFE(tile.powerUsed))))
              .spacing(0).clearScale().tooltip(() -> List.of(Content.text("power", exactFE(tile.powerUsed)), Content.text("power_limit", exactFE(tile.structure.transfer)), Content.text("power_hint"))));
        addRenderableWidget(new GuiInnerScreen(this, 18, 130, 194, 14, () -> List.of(Content.text(tile.status)))
              .tooltip(() -> List.of(Content.text(menu.formed ? "ready" : "structure"), Content.text(tile.status))));
        addRenderableWidget(new MekanismButton(this, 18, 148, 94, 14, Content.text("settings"),
              (b, x, y) -> { addWindow(new FactoryWindow(this, tile, menu)); return true; }));
        addRenderableWidget(new MekanismButton(this, 118, 148, 94, 14, Content.text(tile.enabled ? "pause" : "resume"),
              (b, x, y) -> { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1); return true; }) {
            @Override public void tick() { super.tick(); setMessage(Content.text(tile.enabled ? "pause" : "resume")); }
        });
    }
    @Override protected void drawForegroundText(GuiGraphics g, int x, int y) {
        super.drawForegroundText(g, x, y); renderTitleText(g); renderInventoryText(g);
        g.drawString(font, Content.text("template"), 18, 18, titleTextColor(), false);
        var stack = tile.template.getStack();
        var name = stack.isEmpty() ? Content.text("machine") : stack.getHoverName();
        drawScaledScrollingString(g, name, 42, 29, 212, 39, TextAlignment.LEFT, titleTextColor(), false, 1, getTimeOpened());
        drawScaledScrollingString(g, Content.text("machine_lanes", stack.getCount(), Profiles.processingLines(stack), Profiles.machineParallel(stack)),
              42, 41, 212, 50, TextAlignment.LEFT, titleTextColor(), false, 0.8F, getTimeOpened());
        g.drawString(font, Component.literal(percent()), 73, 80, titleTextColor(), false);
        drawScaledScrollingString(g, Content.text("batch_size", menu.jobUnits), 116, 78, menu.jobCount > 1 ? 170 : 212, 90,
              TextAlignment.LEFT, titleTextColor(), false, 0.8F, getTimeOpened());
    }
}
