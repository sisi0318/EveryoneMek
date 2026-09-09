package dev.everyonemek.natures.client;

import dev.everyonemek.natures.AuraMachine;
import dev.everyonemek.natures.MachineConfig;
import dev.everyonemek.natures.MachineMenu;
import java.util.List;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiUpgradeWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import net.minecraft.network.chat.Component;

public final class BottlerUpgradeWindow extends GuiUpgradeWindow {
    public BottlerUpgradeWindow(IGuiWrapper gui, int x, int y, AuraMachine machine, SelectedWindowData data) {
        super(gui, x, y, machine, data);
        int moduleY = relativeY + getHeight() + 4;
        setHeight(getHeight() + 42);
        MachineMenu menu = (MachineMenu) ((GuiMekanism<?>) gui).getMenu();
        addChild(new GuiVirtualSlot(this, SlotType.NORMAL, gui, relativeX + 6, moduleY + 6, menu.getSimulationModuleSlot()));
        addChild(new GuiInnerScreen(gui, relativeX + 30, moduleY, 162, 32, () -> List.of(
              Component.translatable("gui.naturesmekanism.simulation_module_count", machine.hasSimulationModule() ? 1 : 0),
              Component.translatable("gui.naturesmekanism.gold_module_power", MachineConfig.SIMULATION_POWER_MULTIPLIER.get()))));
    }
}
