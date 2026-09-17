package dev.everyonemek.factory.client;

import dev.everyonemek.factory.Content;
import dev.everyonemek.factory.Controller;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import java.util.function.Supplier;

public final class PortConfigurationTab extends GuiWindowCreatorTab<Controller, PortConfigurationTab> {
    private final int menuId;

    public PortConfigurationTab(IGuiWrapper gui, Controller tile, int menuId, Supplier<PortConfigurationTab> self) {
        super(MekanismUtils.getResource(ResourceType.GUI, "configuration.png"), gui, tile, -26, 6, 26, 18, true, self);
        this.menuId = menuId;
        setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(Content.text("port_config")));
    }
    @Override protected void colorTab(net.minecraft.client.gui.GuiGraphics graphics) {
        mekanism.client.render.MekanismRenderer.color(graphics, mekanism.client.SpecialColors.TAB_CONFIGURATION);
    }

    @Override protected GuiWindow createWindow(SelectedWindowData data) {
        return new PortConfigurationWindow(gui(), dataSource, menuId);
    }

    @Override protected SelectedWindowData getNextWindowData() {
        return new SelectedWindowData(WindowType.UNSPECIFIED);
    }
}
