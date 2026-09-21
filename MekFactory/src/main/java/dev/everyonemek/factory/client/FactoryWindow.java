package dev.everyonemek.factory.client;

import dev.everyonemek.factory.*;
import java.util.List;
import java.util.function.Supplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FactoryWindow extends GuiWindow {
    private final Controller tile;
    private final FactoryMenu menu;
    public FactoryWindow(IGuiWrapper gui, Controller tile, FactoryMenu menu) {
        super(gui, 23, 18, 184, 138, WindowType.UNSPECIFIED);
        this.tile = tile; this.menu = menu;
        button(10, 46, 20, () -> Component.literal("−"), 20);
        button(154, 46, 20, () -> Component.literal("+"), 21);
        addChild(new GuiInnerScreen(gui, relativeX + 34, relativeY + 46, 116, 16, () -> List.of(Content.text("limit", tile.parallelLimit)))
              .tooltip(() -> List.of(Content.text("limit_step"), Content.text("frame_limit", menu.frameParallel), Content.text("available_parallel", menu.availableParallel))));
        addChild(new MekanismButton(gui, relativeX + 10, relativeY + 68, 164, 16, Content.text("rotary_fluid"),
              (b, x, y) -> send(22)) {
            { refreshState(); }
            private void refreshState() { visible = Profiles.rotary(tile.template.getStack()); active = visible && menu.jobCount == 0; setMessage(Content.text(tile.rotaryReverse ? "rotary_gas" : "rotary_fluid")); }
            @Override public void tick() { super.tick(); refreshState(); }
        });
        button(10, 92, 78, () -> Content.text("preview"), 2);
        button(96, 92, 78, () -> Content.text("build"), 3);
        addChild(new MekanismButton(gui, relativeX + 10, relativeY + 114, 164, 16, Content.text("legacy_stock"),
              (b, x, y) -> { gui.addWindow(new LegacyStockWindow(gui, menu)); return true; }) {
            { refreshState(); }
            private void refreshState() { visible = menu.legacyInput || menu.legacyOutput; active = visible; }
            @Override public void tick() { super.tick(); refreshState(); }
        });
    }
    private boolean send(int action) { Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, action); return true; }
    private void button(int x, int y, int width, Supplier<Component> label, int id) {
        addChild(new MekanismButton(gui(), relativeX + x, relativeY + y, width, 16, label.get(),
              (b, mx, my) -> send((id == 20 || id == 21) && Screen.hasShiftDown() ? id + 3 : id)) {
            { refreshState(); }
            private void refreshState() { if (id == 20 || id == 21) active = id == 20 ? tile.parallelLimit > 1 : tile.parallelLimit < FactoryConfig.MAX_PARALLEL; }
            @Override public void tick() { super.tick(); refreshState(); }
            @Override public void updateTooltip(int mx, int my) { if (id == 20 || id == 21) setTooltip(TooltipUtils.create(Content.text("limit_step"))); }
        });
    }
    @Override public void renderForeground(GuiGraphics g, int x, int y) {
        super.renderForeground(g, x, y); drawTitleText(g, Content.text("settings"), 5);
        drawScaledScrollingString(g, Content.text("dimensions", tile.sizeX, tile.sizeY, tile.sizeZ), relativeX + 10, relativeY + 27, relativeX + 174, relativeY + 40,
              TextAlignment.LEFT, titleTextColor(), false, 0.9F, getTimeOpened());
        if (!Profiles.rotary(tile.template.getStack()))
            drawScaledScrollingString(g, Content.text("available_parallel", menu.availableParallel), relativeX + 10, relativeY + 68, relativeX + 174, relativeY + 84,
                  TextAlignment.LEFT, titleTextColor(), false, 0.9F, getTimeOpened());
        if (!menu.legacyInput && !menu.legacyOutput)
            drawScaledScrollingString(g, Content.text("power_hint"), relativeX + 10, relativeY + 114, relativeX + 174, relativeY + 129,
                  TextAlignment.LEFT, titleTextColor(), false, 0.8F, getTimeOpened());
    }
}
