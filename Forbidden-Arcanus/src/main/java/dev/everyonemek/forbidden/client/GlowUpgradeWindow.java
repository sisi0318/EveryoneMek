package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.Controller;
import dev.everyonemek.forbidden.MachineMenu;
import java.util.List;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiUpgradeWindow;
import mekanism.common.inventory.container.SelectedWindowData;

public final class GlowUpgradeWindow extends GuiUpgradeWindow {
    public GlowUpgradeWindow(IGuiWrapper gui, int x, int y, Controller controller, SelectedWindowData data) {
        super(gui, x, y, controller, data);
        int moduleY = relativeY + getHeight() + 4; setHeight(getHeight() + 42);
        MachineMenu menu = (MachineMenu) ((GuiMekanism<?>) gui).getMenu();
        addChild(new GuiVirtualSlot(this, SlotType.NORMAL, gui, relativeX + 6, moduleY + 6, menu.moduleSlot()));
        addChild(new GuiInnerScreen(gui, relativeX + 30, moduleY, 162, 32, () -> List.of(
              MachineScreen.text("glow_module"), MachineScreen.text("glow_rate", controller.glowModules(),
                    String.format(java.util.Locale.ROOT, "%.2f", controller.forge.glowInterval() / 20.0)))));
    }
}
