package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.Controller;
import dev.everyonemek.forbidden.Content;
import dev.everyonemek.forbidden.MachineMenu;
import java.util.List;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiUpgradeWindow;
import mekanism.common.inventory.container.SelectedWindowData;

public final class ResourceUpgradeWindow extends GuiUpgradeWindow {
    public ResourceUpgradeWindow(IGuiWrapper gui, int x, int y, Controller controller, SelectedWindowData data) {
        super(gui, x, y, controller, data);
        int moduleY = relativeY + getHeight() + 4; setHeight(getHeight() + 150);
        MachineMenu menu = (MachineMenu) ((GuiMekanism<?>) gui).getMenu();
        for (int resource = 0; resource < 4; resource++) {
            final int index = resource;
            int rowY = moduleY + resource * 36;
            addChild(new GuiVirtualSlot(this, SlotType.NORMAL, gui, relativeX + 6, rowY + 6, menu.moduleSlot(resource)));
            addChild(new GuiInnerScreen(gui, relativeX + 30, rowY, 162, 32, () -> List.of(
                  Content.resourceModule(index).getDescription(), MachineScreen.text("module_rate", controller.resourceModuleCount(index),
                        String.format(java.util.Locale.ROOT, "%.2f", controller.forge.moduleInterval() / 20.0)))));
        }
    }
}
