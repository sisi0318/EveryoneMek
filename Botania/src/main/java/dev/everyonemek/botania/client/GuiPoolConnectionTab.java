package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import java.util.List;
import java.util.function.Supplier;
import mekanism.api.RelativeSide;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** The standard Mek collapsible window tab for a controller's adjacent target. */
public final class GuiPoolConnectionTab extends GuiWindowCreatorTab<ManaMachineMenu, GuiPoolConnectionTab> {
    public GuiPoolConnectionTab(IGuiWrapper gui, ManaMachineMenu menu, Supplier<GuiPoolConnectionTab> supplier) {
        super(null, gui, menu, -26, 64, 26, 18, true, supplier);
        setTooltip(Tooltip.create(ManaMachineScreen.text("connection_settings")));
    }
    @Override protected void colorTab(GuiGraphics gui) { MekanismRenderer.color(gui, SpecialColors.TAB_CONFIGURATION); }
    @Override protected void drawBackgroundOverlay(GuiGraphics gui) { ManaIcon.draw(gui, getButtonX() + 1, getButtonY() + 1); }
    @Override protected SelectedWindowData getNextWindowData() { return new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED); }
    @Override protected GuiWindow createWindow(SelectedWindowData data) { return new ConnectionWindow(gui(), dataSource, data); }

    private static final class ConnectionWindow extends GuiWindow {
        private final ManaMachineMenu menu;
        private int pending = -1;
        private ConnectionWindow(IGuiWrapper gui, ManaMachineMenu menu, SelectedWindowData data) {
            super(gui, 25, 28, 188, 110, data); this.menu = menu;
            var sides = RelativeSide.values();
            for (int i = 0; i < sides.length; i++) {
                final int side = i;
                addChild(new MekanismButton(gui, relativeX + 8 + i % 3 * 58, relativeY + 26 + i / 3 * 23, 56, 20,
                      label(side), (button, mx, my) -> { select(side); return true; }) {
                    @Override public void tick() { super.tick(); setMessage(label(side)); active = pending < 0; }
                });
            }
            addChild(new GuiInnerScreen(gui, relativeX + 8, relativeY + 79, 172, 18,
                  () -> List.of(ManaMachineScreen.text(pending >= 0 ? "applying" : !menu.state.getBoolean("accepted") && menu.state.contains("revision")
                        ? "rejected" : "connected_side", sideName(selected())))));
        }
        private int selected() { return Math.clamp(menu.state.getInt("side"), 0, 5); }
        private Component sideName(int side) { return Component.translatable(RelativeSide.values()[side].getTranslationKey()); }
        private Component label(int side) { return Component.literal(selected() == side ? "• " : "").append(sideName(side)); }
        private void select(int side) {
            if (pending >= 0 || selected() == side) return;
            pending = menu.state.getInt("revision");
            PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, 1, Integer.toString(side)));
        }
        @Override public void tick() { super.tick(); if (pending >= 0 && menu.state.getInt("revision") != pending) pending = -1; }
        @Override public void renderForeground(GuiGraphics gui, int mouseX, int mouseY) {
            super.renderForeground(gui, mouseX, mouseY); drawTitleText(gui, ManaMachineScreen.text("connection_settings"), 5);
        }
    }
}
