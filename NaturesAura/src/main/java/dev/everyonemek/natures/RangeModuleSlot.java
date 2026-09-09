package dev.everyonemek.natures;

import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;

public final class RangeModuleSlot extends BasicInventorySlot {
    public static final int LIMIT = 4;
    public RangeModuleSlot(IContentsListener listener) {
        super(LIMIT, (stack, automation) -> automation != AutomationType.EXTERNAL,
              (stack, automation) -> automation != AutomationType.EXTERNAL,
              stack -> stack.is(Content.RANGE_MODULE), listener, 0, 0);
        setSlotOverlay(SlotOverlay.UPGRADE);
    }

    @Override
    public VirtualInventoryContainerSlot createContainerSlot() {
        return new VirtualInventoryContainerSlot(this, new SelectedWindowData(WindowType.UPGRADE), getSlotOverlay(), this::setStackUnchecked);
    }
}
