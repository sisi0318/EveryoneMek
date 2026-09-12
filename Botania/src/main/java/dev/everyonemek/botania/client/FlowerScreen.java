package dev.everyonemek.botania.client;

import dev.everyonemek.botania.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.text.GuiTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FlowerScreen extends GuiMekanism<FlowerMenu> {
    private GuiTextField value, member;
    private int builtKind = -1;
    private boolean initializedValue;
    private int lastMode = -1;
    public FlowerScreen(FlowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 240; imageHeight = 218;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.botanicalmekanism." + key, args); }
    private void send(int action, String value) { PacketDistributor.sendToServer(new FlowerPackets.Settings(menu.containerId, action, value)); }
    private void button(int x, int y, int width, Supplier<Component> label, Runnable action) {
        button(x, y, width, label, action, () -> true);
    }
    private void button(int x, int y, int width, Supplier<Component> label, Runnable action, BooleanSupplier enabled) {
        addRenderableWidget(new MekanismButton(this, x, y, width, 18, label.get(), (button, mx, my) -> { action.run(); return true; }) {
            @Override public void tick() { super.tick(); setMessage(label.get()); active = enabled.getAsBoolean(); }
        });
    }
    @Override protected void addGuiElements() {
        super.addGuiElements(); builtKind = menu.state.contains("kind") ? menu.state.getInt("kind") : -1; initializedValue = false;
        button(16, 188, 100, () -> text(menu.state.getBoolean("enabled") ? "pause" : "resume"), () -> send(0, ""));
        if (builtKind < 0) return;
        if (builtKind <= 1) {
            addRenderableWidget(new GuiInnerScreen(this, 16, 34, 208, 92, () -> List.of(
                  text("energy", menu.state.getInt("fe"), Balance.ENERGY_CAPACITY),
                  text("mana", menu.state.getInt("mana"), menu.state.getInt("maxMana")),
                  text("status." + menu.state.getString("status")))));
            addRenderableWidget(new GuiInnerScreen(this, 16, 136, 208, 40, () -> List.of(builtKind == 0
                  ? text("lotus_hint", menu.state.getInt("rate") * menu.state.getInt("fePerMana"), menu.state.getInt("rate")) : text("amaranthus_hint"))));
        } else if (builtKind == 2) {
            value = addRenderableWidget(new GuiTextField(this, 16, 38, 152, 18)); value.setMaxLength(32); value.setEnterHandler(() -> send(6, value.getText()));
            button(174, 38, 50, () -> text("apply"), () -> send(6, value.getText()));
            member = addRenderableWidget(new GuiTextField(this, 16, 80, 152, 18)); member.setMaxLength(16); member.setEnterHandler(() -> send(7, member.getText()));
            button(174, 80, 50, () -> text("member_toggle"), () -> send(7, member.getText()));
            addRenderableWidget(new GuiInnerScreen(this, 16, 108, 208, 68, () -> List.of(
                  text("network", menu.state.getString("name")),
                  text("nodes", menu.state.getInt("nodes"), Balance.NODE_LIMIT),
                  text("flow_fee", menu.state.getInt("delivered"), menu.state.getInt("fee")),
                  text("status." + menu.state.getString("status")), text("member_count", menu.state.getInt("memberCount"))))
                  .tooltip(() -> Arrays.stream(menu.state.getString("members").split(", ")).filter(name -> !name.isEmpty()).map(Component::literal).map(Component.class::cast).toList()));
        } else {
            button(16, 34, 208, () -> text("network", menu.state.getString("name").isEmpty() ? text("unlinked") : menu.state.getString("name")), this::cycleNetwork);
            button(16, 58, 100, () -> text("mode." + menu.state.getInt("mode")), () -> send(1, ""));
            button(124, 58, 100, () -> text("direction." + menu.state.getInt("direction")), () -> send(2, ""), () -> menu.state.getInt("mode") != NetworkPlant.RELAY);
            button(16, 82, 208, () -> text("priority." + menu.state.getInt("priority")), () -> send(4, ""), () -> menu.state.getInt("mode") == NetworkPlant.RECEIVE);
            value = addRenderableWidget(new GuiTextField(this, 90, 108, 78, 18)); value.setMaxLength(10); value.setEnterHandler(() -> send(3, value.getText()));
            button(174, 108, 50, () -> text("apply"), () -> send(3, value.getText()), () -> menu.state.getInt("mode") != NetworkPlant.RELAY);
            addRenderableWidget(new GuiInnerScreen(this, 16, 136, 208, 40, this::nodeLines).textScale(.8F));
            button(124, 188, 100, () -> text("disconnect"), () -> send(8, ""));
        }
    }
    private List<Component> nodeLines() {
        Component status = text("status." + menu.state.getString("status"));
        if (menu.state.getInt("mode") == NetworkPlant.RELAY) return List.of(status, text("relay_flow", menu.state.getInt("moved")));
        return List.of(status, text("confirmed_limit", menu.state.getInt("limit")),
              menu.state.contains("maxMana") ? text("mana", menu.state.getInt("mana"), menu.state.getInt("maxMana")) : text("unmeasured"));
    }
    private void cycleNetwork() {
        var choices = menu.state.getList("choices", Tag.TAG_COMPOUND);
        if (choices.isEmpty()) return;
        int current = -1;
        if (menu.state.hasUUID("network")) for (int i = 0; i < choices.size(); i++)
            if (choices.getCompound(i).getUUID("id").equals(menu.state.getUUID("network"))) { current = i; break; }
        send(5, choices.getCompound((current + 1) % choices.size()).getUUID("id").toString());
    }
    @Override public void containerTick() {
        super.containerTick();
        if (menu.state.contains("kind") && builtKind != menu.state.getInt("kind")) rebuildWidgets();
        if (!initializedValue && value != null && menu.state.contains("kind")) {
            value.setText(builtKind == 2 ? menu.state.getString("name") : Integer.toString(menu.state.getInt("limit"))); initializedValue = true;
        }
        if (builtKind == 3 && value != null && lastMode != menu.state.getInt("mode")) {
            lastMode = menu.state.getInt("mode"); value.setText(Integer.toString(menu.state.getInt("limit")));
            value.setEditable(lastMode != NetworkPlant.RELAY);
        }
    }
    @Override protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawForegroundText(graphics, mouseX, mouseY); renderTitleText(graphics);
        if (builtKind == 2) {
            graphics.drawString(font, text("network_name"), 16, 25, titleTextColor(), false);
            graphics.drawString(font, text("member_name"), 16, 67, titleTextColor(), false);
        } else if (builtKind == 3) graphics.drawString(font, text(menu.state.getInt("mode") == 0 ? "reserve" : "target"), 16, 112, titleTextColor(), false);
    }
}
