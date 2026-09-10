package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.slot.BasicInventorySlot;

public final class HammerSlot extends BasicInventorySlot {
    public HammerSlot(IContentsListener listener) {
        super(1, (stack, automation) -> automation != AutomationType.EXTERNAL, (stack, automation) -> true,
              stack -> stack.has(ModDataComponents.RITUAL_STARTER), listener, 182, 84);
    }
}
