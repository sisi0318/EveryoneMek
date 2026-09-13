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
    private Component recipeLabel() {
        String id = menu.state.getString("recipe");
        if (id.isEmpty()) return text("choose_recipe");
        for (var value : menu.state.getList("choices", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var entry = (net.minecraft.nbt.CompoundTag) value;
            if (entry.getString("id").equals(id)) {
                var icon = net.minecraft.world.item.ItemStack.parseOptional(tile.getLevel().registryAccess(), entry.getCompound("icon"));
                return text("recipe_selected", icon.getHoverName());
            }
        }
        return text("recipe_missing");
    }
    private void send(int action, String value) {
        if (pendingRevision >= 0) return;
        pendingRevision = menu.state.getInt("revision"); PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, value));
    }
    private void button(int x, int y, int width, Supplier<Component> label, Runnable action) {
        addRenderableWidget(new MekanismButton(this, x, y, width, 16, label.get(), (button, mx, my) -> { action.run(); return true; }) {
            @Override public void tick() { super.tick(); setMessage(label.get()); active = pendingRevision < 0; }
        });
    }
    private void targetSide(int y) {
        button(16, y, 204, () -> text("connected_side", Component.translatable(RelativeSide.values()[tile.poolSide()].getTranslationKey())),
              () -> send(1, Integer.toString((tile.poolSide() + 1) % 6)));
    }
    @Override protected void addGuiElements() {
        super.addGuiElements(); addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 224, 32, 70));
        if (tile.kind().chemical) addRenderableWidget(new GuiChemicalBar(this, GuiChemicalBar.getProvider(tile.mana(), List.of(tile.mana())), 16, 106, 204, 6, true) {
            @Override protected List<Component> getTooltip(mekanism.api.chemical.ChemicalStack stack) {
                return List.of(text("mana", tile.mana().getStored(), ManaMachine.MANA_CAPACITY));
            }
        });
        if (tile.kind() == ManaMachineKind.BRIDGE || tile.kind() == ManaMachineKind.CHARGER) {
            boolean bridge = tile.kind() == ManaMachineKind.BRIDGE;
            for (int i = 0; i < 2; i++) {
                int mode = i;
                button(16 + i * 102, bridge ? 32 : 56, 98, () -> Component.literal(tile.mode() == mode ? "• " : "")
                      .append(text((bridge ? "bridge_mode." : "charge_mode.") + mode)), () -> send(0, Integer.toString(mode)));
            }
            if (bridge) targetSide(58);
            if (!bridge) {
                var target = addRenderableWidget(new GuiTextField(this, 16, 82, 60, 16));
                target.setMaxLength(3); target.setText(Integer.toString(tile.targetPercent()));
                button(82, 82, 64, () -> text("target_apply"), () -> send(2, target.getText()));
                addRenderableWidget(new GuiInnerScreen(this, 152, 82, 44, 16, () -> List.of(Component.literal(tile.targetPercent() + "%"))));
            }
            addRenderableWidget(new GuiInnerScreen(this, 16, 120, 204, 16, () -> List.of(text("mana", tile.mana().getStored(), ManaMachine.MANA_CAPACITY))));
        } else if (tile.kind().controller()) targetSide(120);
        else {
            addRenderableWidget(new GuiProgress(tile::progress, ProgressType.SMALL_RIGHT, this, 113, 48));
            if (tile.kind().chemical) addRenderableWidget(new GuiInnerScreen(this, 16, 120, 204, 16, () -> List.of(text("mana", tile.mana().getStored(), ManaMachine.MANA_CAPACITY))));
            if (!tile.kind().random()) button(16, 162, 204, this::recipeLabel, () -> addWindow(new GuiManaRecipeSelector(this, menu)));
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
