package dev.everyonemek.natures;

import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;

/** One removable module in the upgrade window, never part of the machine's automation inputs. */
public final class GoldModuleSlot extends BasicInventorySlot {
    public GoldModuleSlot(IContentsListener listener) {
        super(1, (stack, automation) -> automation != AutomationType.EXTERNAL,
              (stack, automation) -> automation != AutomationType.EXTERNAL,
              stack -> stack.is(Content.INFINITE_GOLD_MODULE), listener, 0, 0);
        setSlotOverlay(SlotOverlay.UPGRADE);
    }

    @Override
    public VirtualInventoryContainerSlot createContainerSlot() {
        return new VirtualInventoryContainerSlot(this, new SelectedWindowData(WindowType.UPGRADE), getSlotOverlay(), this::setStackUnchecked);
    }
}
