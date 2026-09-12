package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import java.util.List;
import java.util.function.Supplier;
import mekanism.api.RelativeSide;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.progress.*;
import mekanism.client.gui.element.text.GuiTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ManaMachineScreen extends GuiConfigurableTile<ManaMachine, ManaMachineMenu> {
    private int pendingRevision = -1;
    public ManaMachineScreen(ManaMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 238; imageHeight = 276; inventoryLabelX = 29; inventoryLabelY = 180; dynamicSlots = true;
    }
    static Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism.machine." + key, args); }
    private void send(int action, String value) {
        if (pendingRevision >= 0) return;
        pendingRevision = menu.state.getInt("revision"); PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, value));
    }
    private void button(int x, int y, int width, Supplier<Component> label, Runnable action) {
        addRenderableWidget(new MekanismButton(this, x, y, width, 16, label.get(), (button, mx, my) -> { action.run(); return true; }) {
            @Override public void tick() { super.tick(); setMessage(label.get()); active = pendingRevision < 0; }
        });
    }
    private void sides(int y) {
        for (int i = 0; i < RelativeSide.values().length; i++) {
            int side = i;
            button(16 + i % 3 * 62, y + i / 3 * 20, 60, () -> Component.literal(tile.poolSide() == side ? "• " : "")
                  .append(Component.translatable(RelativeSide.values()[side].getTranslationKey())), () -> send(1, Integer.toString(side)));
        }
    }
    @Override protected void addGuiElements() {
        super.addGuiElements(); addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 224, 32, 70));
        if (tile.kind().chemical) addRenderableWidget(new GuiChemicalBar(this, GuiChemicalBar.getProvider(tile.mana(), List.of(tile.mana())), 16, 106, 204, 6, true));
        if (tile.kind() == ManaMachineKind.BRIDGE || tile.kind() == ManaMachineKind.CHARGER) {
            boolean bridge = tile.kind() == ManaMachineKind.BRIDGE;
            for (int i = 0; i < 2; i++) {
                int mode = i;
                button(16 + i * 102, bridge ? 32 : 56, 98, () -> Component.literal(tile.mode() == mode ? "• " : "")
                      .append(text((bridge ? "bridge_mode." : "charge_mode.") + mode)), () -> send(0, Integer.toString(mode)));
            }
            sides(bridge ? 56 : 76);
            if (!bridge) {
                var target = addRenderableWidget(new GuiTextField(this, 16, 120, 60, 16));
                target.setMaxLength(3); target.setText(Integer.toString(tile.targetPercent()));
                button(82, 120, 64, () -> text("target_apply"), () -> send(2, target.getText()));
                addRenderableWidget(new GuiInnerScreen(this, 152, 120, 66, 16, () -> List.of(Component.literal(tile.targetPercent() + "%"))));
            } else addRenderableWidget(new GuiInnerScreen(this, 16, 120, 204, 16, () -> List.of(text("mana", tile.mana().getStored(), ManaMachine.MANA_CAPACITY))));
        } else if (tile.kind().controller()) sides(118);
        else {
            addRenderableWidget(new GuiProgress(tile::progress, ProgressType.SMALL_RIGHT, this, 113, 48));
            if (tile.kind().chemical) addRenderableWidget(new GuiInnerScreen(this, 16, 120, 204, 16, () -> List.of(text("mana", tile.mana().getStored(), ManaMachine.MANA_CAPACITY))));
            if (!tile.kind().random()) button(16, 162, 204, () -> text("choose_recipe"), () -> addWindow(new GuiManaRecipeSelector(this, menu)));
        }
        addRenderableWidget(new GuiInnerScreen(this, 16, tile.kind().controller() ? 162 : 142, 204, 16, () -> List.of(
              pendingRevision >= 0 ? text("applying") : !menu.state.getBoolean("accepted") && menu.state.contains("revision") ? text("rejected") : text("status." + tile.status()))));
    }
    @Override public void containerTick() {
        super.containerTick(); if (pendingRevision >= 0 && pendingRevision != menu.state.getInt("revision")) pendingRevision = -1;
    }
    @Override protected void drawForegroundText(GuiGraphics gui, int mx, int my) {
        super.drawForegroundText(gui, mx, my); renderTitleText(gui);
        if (tile.kind().inputs > 0) gui.drawString(font, text(tile.kind() == ManaMachineKind.ENCHANTER ? "books" : "materials"), 16, 21, titleTextColor(), false);
        if (tile.kind().outputs > 0) gui.drawString(font, text("products"), 152, 21, titleTextColor(), false);
        if (tile.kind().extras == 1) gui.drawString(font, text("extra." + tile.kind().id), 98, 74, titleTextColor(), false);
    }
}
