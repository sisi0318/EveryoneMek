package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.*;
import dev.everyonemek.forbidden.mixin.GuiUpgradeScrollListAccess;
import java.util.*;
import mekanism.api.Upgrade;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.DigitalButton;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.custom.GuiSupportedUpgrades;
import mekanism.client.gui.element.scroll.GuiInstallableScrollList;
import mekanism.client.gui.element.scroll.GuiUpgradeScrollList;
import mekanism.client.gui.element.window.GuiUpgradeWindow;
import mekanism.client.render.IFancyFontRenderer.WrappedTextRenderer;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Keeps Mek's window, shared slots, installation progress and native upgrade behavior. */
public final class ResourceUpgradeWindow extends GuiUpgradeWindow {
    private final Controller controller;
    private final CombinedList combined;
    private final GuiInnerScreen details;
    private final MekanismButton uninstall;
    private record Entry(Upgrade upgrade, int resource) {
        Component name() { return upgrade == null ? Content.resourceModule(resource).getDescription() : upgrade.getTranslatedName(); }
    }

    public ResourceUpgradeWindow(IGuiWrapper gui, int x, int y, Controller controller, SelectedWindowData data) {
        super(gui, x, y, controller, data);
        this.controller = controller;
        var originalList = children().stream().filter(GuiUpgradeScrollList.class::isInstance).map(GuiUpgradeScrollList.class::cast).findFirst().orElseThrow();
        var originalButton = children().stream().filter(DigitalButton.class::isInstance).map(DigitalButton.class::cast).findFirst().orElseThrow();
        details = children().stream().filter(GuiInnerScreen.class::isInstance).map(GuiInnerScreen.class::cast).findFirst().orElseThrow();
        var originalSupported = children().stream().filter(GuiSupportedUpgrades.class::isInstance).map(GuiSupportedUpgrades.class::cast).findFirst().orElseThrow();
        children().remove(originalSupported);
        var supported = addChild(new GuiSupportedResourceUpgrades(gui, originalSupported, controller.getComponent().getSupportedTypes()));
        setHeight(Math.max(getHeight(), supported.getRelativeBottom() - relativeY + 6));
        children().remove(originalList);
        children().remove(originalButton);
        combined = addChild(new CombinedList(gui, originalList, controller));
        int containerId = ((GuiMekanism<?>) gui).getMenu().containerId;
        uninstall = addChild(new DigitalButton(gui, originalButton.getRelativeX(), originalButton.getRelativeY(),
              originalButton.getWidth(), originalButton.getHeight(), MekanismLang.UPGRADE_UNINSTALL, (button, mx, my) -> {
                  Entry entry = combined.getSelection();
                  if (entry == null) return false;
                  if (entry.upgrade != null) originalButton.onClick(mx, my, 0);
                  else Minecraft.getInstance().gameMode.handleInventoryButtonClick(containerId, 32 + entry.resource + (Screen.hasShiftDown() ? 4 : 0));
                  return true;
              })).setTooltip(MekanismLang.UPGRADE_UNINSTALL_TOOLTIP);
        uninstall.active = false;
    }
    @Override public void tick() {
        super.tick();
        combined.validateSelection();
        uninstall.active = combined.hasSelection();
    }
    public boolean renderResourceDetails(GuiGraphics graphics) {
        Entry entry = combined.getSelection();
        if (entry == null || entry.upgrade != null) return false;
        int x = details.getRelativeX() + 2, y = details.getRelativeY() + 2;
        new WrappedTextRenderer(this, entry.name()).renderWithScale(graphics, x, y, TextAlignment.LEFT,
              screenTextColor(), details.getWidth() - 4, .6F);
        new WrappedTextRenderer(this, MekanismLang.UPGRADE_COUNT.translate(controller.resourceModuleCount(entry.resource), 8))
              .renderWithScale(graphics, x, y + 16, TextAlignment.LEFT, screenTextColor(), details.getWidth() - 4, .6F);
        return true;
    }
    private static final class CombinedList extends GuiInstallableScrollList<Entry> {
        private final GuiUpgradeScrollList original;
        private final Controller controller;
        CombinedList(IGuiWrapper gui, GuiUpgradeScrollList original, Controller controller) {
            super(gui, original.getRelativeX(), original.getRelativeY(), original.getHeight(), GuiElementHolder.HOLDER, 32,
                  MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "upgrade_selection.png"), 100, 36);
            this.original = original; this.controller = controller;
        }
        @Override protected List<Entry> getCurrentInstalled() {
            var entries = new ArrayList<Entry>();
            for (Upgrade upgrade : Upgrade.values()) if (controller.getComponent().getUpgrades(upgrade) > 0) entries.add(new Entry(upgrade, -1));
            for (int i = 0; i < 4; i++) if (controller.resourceModuleCount(i) > 0) entries.add(new Entry(null, i));
            return entries;
        }
        @Override protected void drawName(GuiGraphics graphics, Entry entry, int y) { drawNameText(graphics, y, entry.name(), titleTextColor(), 1); }
        @Override protected ItemStack getRenderStack(Entry entry) {
            return entry.upgrade == null ? new ItemStack(Content.resourceModule(entry.resource)) : UpgradeUtils.getStack(entry.upgrade);
        }
        @Override protected EnumColor getColor(Entry entry) { return entry.upgrade == null ? EnumColor.GRAY : entry.upgrade.getColor(); }
        @Override protected void setSelected(Entry entry) {
            selectedType = entry;
            ((GuiUpgradeScrollListAccess) original).forbiddenmekanism$select(entry == null ? null : entry.upgrade);
        }
        void validateSelection() { if (selectedType != null && !getCurrentInstalled().contains(selectedType)) clearSelection(); }
    }
}
