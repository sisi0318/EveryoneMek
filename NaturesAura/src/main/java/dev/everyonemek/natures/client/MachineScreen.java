package dev.everyonemek.natures.client;

import dev.everyonemek.natures.AuraMachine;
import dev.everyonemek.natures.MachineKind;
import dev.everyonemek.natures.MachineMenu;
import dev.everyonemek.natures.MachineConfig;
import java.util.List;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiChemicalBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.window.GuiUpgradeWindowTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MachineScreen extends GuiConfigurableTile<AuraMachine, MachineMenu> {
    private GuiUpgradeWindowTab forestUpgradeTab;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 238;
        imageHeight = 220;
        inventoryLabelX = 38;
        inventoryLabelY = 124;
        dynamicSlots = true;
    }

    @Override
    protected void addGenericTabs() {
        super.addGenericTabs();
        if (tile.kind() == MachineKind.FOREST_RITUAL) {
            // Keep the native security/redstone tabs and swap only the upgrade-window factory.
            var original = children().stream().filter(GuiUpgradeWindowTab.class::isInstance).findFirst().orElseThrow();
            removeWidget(original);
            forestUpgradeTab = addRenderableWidget(new GuiUpgradeWindowTab(this, tile, () -> forestUpgradeTab) {
                @Override
                protected GuiWindow createWindow(SelectedWindowData data) {
                    return new ForestUpgradeWindow(MachineScreen.this, (getGuiWidth() - 198) / 2, 15, tile, data);
                }
            });
        }
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 224, 22, 72));
        addRenderableWidget(new GuiEnergyTab(this, tile.energy(), tile::getActive));
        if (tile.auraTank() != null)
            addRenderableWidget(new GuiChemicalBar(this, GuiChemicalBar.getProvider(tile.auraTank(), List.of(tile.auraTank())), 18, 78, 180, 8, true));
        if (tile.kind() == MachineKind.AURA_GENERATOR) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 26, 180, 38, () -> List.of(
                  Component.translatable("gui.naturesmekanism.electric_aura"),
                  Component.translatable(tile.environmentOutput() ? "gui.naturesmekanism.environment_on" : "gui.naturesmekanism.environment_off"))));
            addRenderableWidget(new MekanismButton(this, 130, 94, 68, 16,
                  Component.translatable("gui.naturesmekanism.toggle_environment"), (button, x, y) -> {
                      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                      return true;
                  }));
        } else {
            addRenderableWidget(new GuiProgress(tile::progress, ProgressType.SMALL_RIGHT, this, 132, 40));
        }
        boolean altar = tile.kind() == MachineKind.NATURAL_ALTAR;
        addRenderableWidget(new GuiInnerScreen(this, 18, 94, tile.kind() == MachineKind.AURA_GENERATOR ? 108 : 180, altar ? 28 : 18,
              () -> altar ? List.of(Component.translatable("gui.naturesmekanism.status." + tile.status()),
                    Component.translatable("gui.naturesmekanism.environment_aura", MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get(), TextUtils.format(tile.environmentAura())))
                    : List.of(Component.translatable("gui.naturesmekanism.status." + tile.status()))));
    }

    @Override
    protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        renderInventoryText(graphics);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }
}
