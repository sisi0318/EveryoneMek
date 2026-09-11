package dev.everyonemek.forbidden.mixin;

import mekanism.api.Upgrade;
import mekanism.client.gui.element.scroll.GuiUpgradeScrollList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiUpgradeScrollList.class, remap = false)
public interface GuiUpgradeScrollListAccess {
    @Invoker("setSelected") void forbiddenmekanism$select(Upgrade upgrade);
}
