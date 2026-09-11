package dev.everyonemek.forbidden;

import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;

public final class ResourceModuleSlot extends BasicInventorySlot {
    public ResourceModuleSlot(IContentsListener listener, int resource) {
        super(8, (stack, automation) -> automation != AutomationType.EXTERNAL,
              (stack, automation) -> automation != AutomationType.EXTERNAL,
              stack -> stack.is(Content.resourceModule(resource)), listener, 0, 0);
        setSlotOverlay(SlotOverlay.UPGRADE);
    }
    @Override public VirtualInventoryContainerSlot createContainerSlot() {
        // Keep the released persistent inventory indices; installation uses Mek's shared upgrade input.
        return null;
    }
}
