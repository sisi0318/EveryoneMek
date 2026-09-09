package dev.everyonemek.natures.client;

import dev.everyonemek.natures.AuraMachine;
import dev.everyonemek.natures.MachineKind;
import dev.everyonemek.natures.MachineMenu;
import dev.everyonemek.natures.SetMachineSettingPayload;
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
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MachineScreen extends GuiConfigurableTile<AuraMachine, MachineMenu> {
    private GuiUpgradeWindowTab moduleUpgradeTab;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 238;
        imageHeight = 220 + tile.kind().guiExtraHeight();
        inventoryLabelX = 38;
        inventoryLabelY = 124 + tile.kind().guiExtraHeight();
        dynamicSlots = true;
    }

    @Override
    protected void addGenericTabs() {
        super.addGenericTabs();
        if (tile.kind() == MachineKind.FOREST_RITUAL || tile.kind() == MachineKind.AURA_BOTTLER || tile.kind().supportsRange()) {
            // Keep the native security/redstone tabs and swap only the upgrade-window factory.
            var original = children().stream().filter(GuiUpgradeWindowTab.class::isInstance).findFirst().orElseThrow();
            removeWidget(original);
            moduleUpgradeTab = addRenderableWidget(new GuiUpgradeWindowTab(this, tile, () -> moduleUpgradeTab) {
                @Override
                protected GuiWindow createWindow(SelectedWindowData data) {
                    return tile.kind() == MachineKind.FOREST_RITUAL
                          ? new ForestUpgradeWindow(MachineScreen.this, (getGuiWidth() - 198) / 2, 15, tile, data)
                          : tile.kind() == MachineKind.AURA_BOTTLER
                                ? new BottlerUpgradeWindow(MachineScreen.this, (getGuiWidth() - 198) / 2, 15, tile, data)
                                : new RangeUpgradeWindow(MachineScreen.this, (getGuiWidth() - 198) / 2, 15, tile, data);
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
        } else if (tile.controller() != null) {
            addRenderableWidget(new MekanismButton(this, 18, 22, 180, 16,
                  Component.translatable(tile.controller().mode().translationKey()), (button, x, y) -> {
                      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                      return true;
                  }) {
                @Override public void tick() {
                    super.tick();
                    setMessage(Component.translatable(tile.controller().mode().translationKey()));
                }
            });
            addRenderableWidget(new GuiInnerScreen(this, 18, 40, 86, 15, () -> List.of(
                  Component.translatable("gui.naturesmekanism.control_lower", TextUtils.format(tile.controller().lower())))));
            addRenderableWidget(new GuiInnerScreen(this, 112, 40, 86, 15, () -> List.of(
                  Component.translatable("gui.naturesmekanism.control_upper", TextUtils.format(tile.controller().upper())))));
            addNumericSetting(18, 57, 86, SetMachineSettingPayload.CONTROL_LOWER);
            addNumericSetting(112, 57, 86, SetMachineSettingPayload.CONTROL_UPPER);
        } else if (tile.spawner() != null) {
            addRenderableWidget(new GuiProgress(tile::progress, ProgressType.SMALL_RIGHT, this, 78, 45));
            addRenderableWidget(new GuiInnerScreen(this, 106, 24, 92, 48, () -> List.of(
                  tile.spawner().target() == null ? Component.translatable("gui.naturesmekanism.no_target") : tile.spawner().target().getDescription(),
                  Component.translatable("gui.naturesmekanism.area_count", tile.spawner().nearby(), tile.area().cap()))));
        } else if (tile.breeder() != null) {
            addRenderableWidget(new MekanismButton(this, 78, 22, 74, 16,
                  Component.translatable(tile.breeder().modeKey()), (button, x, y) -> {
                      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 3);
                      return true;
                  }) {
                @Override public void tick() {
                    super.tick(); setMessage(Component.translatable(tile.breeder().modeKey()));
                }
            });
            addRenderableWidget(new GuiProgress(tile::progress, ProgressType.SMALL_RIGHT, this, 107, 49));
            addRenderableWidget(new GuiInnerScreen(this, 78, 74, 120, 15, () -> List.of(
                  Component.translatable("gui.naturesmekanism.area_count", tile.breeder().nearby(), tile.area().cap()))));
        } else {
            ProgressType progressType = ProgressType.SMALL_RIGHT;
            // Center within the gap between the input frame and the 2x2 output frame.
            int inputFrameRight = tile.kind() == MachineKind.FOREST_RITUAL ? 121 : 99;
            int outputFrameLeft = 159;
            int progressX = (inputFrameRight + outputFrameLeft - progressType.getWidth()) / 2;
            int progressY = 47 - progressType.getHeight() / 2;
            addRenderableWidget(new GuiProgress(tile::progress, progressType, this, progressX, progressY));
        }
        if (tile.kind() == MachineKind.AURA_BOTTLER) {
            addRenderableWidget(new MekanismButton(this, 18, 39, 58, 16,
                  Component.translatable(tile.bottlingMode().translationKey()), (button, x, y) -> {
                      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                      return true;
                  }) {
                @Override
                public void tick() {
                    super.tick();
                    setMessage(Component.translatable(tile.bottlingMode().translationKey()));
                }
            });
        }
        boolean environment = tile.kind().usesEnvironmentAura();
        addRenderableWidget(new GuiInnerScreen(this, 18, 94, tile.kind() == MachineKind.AURA_GENERATOR ? 108 : 180, environment ? 28 : 18,
              () -> environment ? List.of(Component.translatable("gui.naturesmekanism.status." + tile.status()),
                    Component.translatable("gui.naturesmekanism.environment_aura", tile.environmentRadius(), TextUtils.format(tile.environmentAura())))
                    : List.of(Component.translatable("gui.naturesmekanism.status." + tile.status()))));
        if (tile.area() != null) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 126, 180, 15, () -> List.of(
                  Component.translatable("gui.naturesmekanism.area_offset", tile.area().x(), tile.area().y(), tile.area().z()))));
            addNumericSetting(18, 143, 56, SetMachineSettingPayload.AREA_X);
            addNumericSetting(80, 143, 56, SetMachineSettingPayload.AREA_Y);
            addNumericSetting(142, 143, 56, SetMachineSettingPayload.AREA_Z);
            addRenderableWidget(new GuiInnerScreen(this, 18, 160, 86, 15, () -> List.of(
                  Component.translatable("gui.naturesmekanism.area_radius", tile.area().radius(), tile.area().maxRadius()))));
            addRenderableWidget(new GuiInnerScreen(this, 112, 160, 86, 15, () -> List.of(
                  Component.translatable("gui.naturesmekanism.area_cap", tile.area().cap()))));
            addNumericSetting(18, 177, 86, SetMachineSettingPayload.AREA_RADIUS);
            addNumericSetting(112, 177, 86, SetMachineSettingPayload.AREA_CAP);
        }
        if (tile.chamber() != null) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 126, 180, 28, () -> List.of(
                  tile.chamber().target().isEmpty() ? Component.translatable("gui.naturesmekanism.chamber_structure")
                        : tile.chamber().target().getHoverName(),
                  Component.translatable("gui.naturesmekanism.chamber_cost", TextUtils.format(tile.chamber().auraCost())))));
        }
    }

    private GuiTextField addNumericSetting(int x, int y, int width, int setting) {
        GuiTextField field = addRenderableWidget(new GuiTextField(this, x, y, width, 15));
        field.setMaxLength(11);
        field.setInputValidator(c -> c == '-' || c >= '0' && c <= '9');
        field.configureDigitalBorderInput(() -> {
            try {
                int value = Math.clamp(Long.parseLong(field.getText()), Integer.MIN_VALUE, Integer.MAX_VALUE);
                PacketDistributor.sendToServer(new SetMachineSettingPayload(menu.containerId, setting, value));
                field.setText("");
            } catch (NumberFormatException ignored) { }
        });
        return field;
    }

    @Override
    protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        renderInventoryText(graphics);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }
}
