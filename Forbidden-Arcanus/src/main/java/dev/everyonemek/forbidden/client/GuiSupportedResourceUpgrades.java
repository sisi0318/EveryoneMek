package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.Content;
import java.util.Set;
import mekanism.api.Upgrade;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.custom.GuiSupportedUpgrades;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.MekanismLang;
import mekanism.common.util.EnumUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Appends resource icons inside Mek's existing supported-upgrade frame. */
public final class GuiSupportedResourceUpgrades extends GuiSupportedUpgrades {
    public GuiSupportedResourceUpgrades(IGuiWrapper gui, GuiSupportedUpgrades original, Set<Upgrade> supported) {
        super(gui, original.getRelativeX(), original.getRelativeY(), supported);
        // Match the native 12-pixel icons, localized heading and wrapped rows.
        int rowWidth = getWidth() - 2;
        int firstRowStart = Math.min(font().width(MekanismLang.UPGRADES_SUPPORTED.translate()) + 1, rowWidth);
        int firstRowRoom = (rowWidth - firstRowStart) / 12;
        int rowRoom = rowWidth / 12;
        for (int resource = 0; resource < 4; resource++) {
            int index = EnumUtils.UPGRADES.length + resource;
            int row = index < firstRowRoom ? 0 : 1 + (index - firstRowRoom) / rowRoom;
            int x = row == 0 ? firstRowStart + index * 12 : (index - firstRowRoom) % rowRoom * 12;
            addChild(new ResourceIcon(gui, relativeX + 1 + x, relativeY + 1 + row * 12, new ItemStack(Content.resourceModule(resource))));
            setHeight(Math.max(getHeight(), (row + 1) * 12 + 2));
        }
    }
    private static final class ResourceIcon extends GuiElement {
        private final ItemStack stack;
        ResourceIcon(IGuiWrapper gui, int x, int y, ItemStack stack) {
            super(gui, x, y, 12, 12);
            this.stack = stack;
            setTooltip(TooltipUtils.create(stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL)));
        }
        @Override public void drawBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            gui().renderItem(graphics, stack, relativeX, relativeY, .75F);
        }
        @Override public boolean isValidClickButton(int button) { return false; }
    }
}
