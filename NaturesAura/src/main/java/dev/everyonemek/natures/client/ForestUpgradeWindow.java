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

/** Extends the existing Mek speed/energy upgrade window with one removable forest-only module. */
public final class ForestUpgradeWindow extends GuiUpgradeWindow {
    public ForestUpgradeWindow(IGuiWrapper gui, int x, int y, AuraMachine machine, SelectedWindowData data) {
        super(gui, x, y, machine, data);
        int moduleY = relativeY + getHeight() + 4;
        setHeight(getHeight() + 42);
        MachineMenu menu = (MachineMenu) ((GuiMekanism<?>) gui).getMenu();
        addChild(new GuiVirtualSlot(this, SlotType.NORMAL, gui, relativeX + 6, moduleY + 6, menu.getGoldModuleSlot()));
        addChild(new GuiInnerScreen(gui, relativeX + 30, moduleY, 162, 32, () -> List.of(
              Component.translatable("gui.naturesmekanism.gold_module_count", machine.hasInfiniteGold() ? 1 : 0),
              Component.translatable("gui.naturesmekanism.gold_module_power", MachineConfig.INFINITE_GOLD_POWER_MULTIPLIER.get()))));
    }
}
