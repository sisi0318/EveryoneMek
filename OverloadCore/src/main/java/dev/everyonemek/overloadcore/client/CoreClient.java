package dev.everyonemek.overloadcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.everyonemek.overloadcore.*;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OverloadCore.ID, value = Dist.CLIENT)
public final class CoreClient {
    private static final KeyMapping KEY = new KeyMapping("key.overloadcore.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.overloadcore");
    private static boolean diagnostics;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(CoreContent.CORE.get(), PendantRenderer::new);
        });
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(KEY); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); if (mc.player == null) { CorePackets.clientState = new CompoundTag(); return; }
        if (CorePackets.clientState.getBoolean("bound") && CorePackets.clientState.getBoolean("heavy")) mc.player.setSprinting(false);
        while (KEY.consumeClick()) diagnostics = !diagnostics;
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!stack.is(CoreContent.CORE) || !Screen.hasShiftDown()) return;
        var lines = event.getToolTip();
        int hint = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (tooltipKey(lines.get(i), "details_hint")) { hint = i; break; }
        }
        if (hint < 0) return;

        var details = new ArrayList<Component>();
        details.add(CoreContent.text("effects", CoreConfig.RANGE.get()).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < 8; i++) {
            var line = i == 6 ? CoreContent.text("curse." + i, CoreConfig.METAL_LIMIT.get()) : CoreContent.text("curse." + i);
            details.add(line.withStyle(ChatFormatting.RED));
        }
        for (int i = 0; i < 4; i++) {
            var line = i == 3 ? CoreContent.text("gift." + i, KEY.getTranslatedKeyMessage()) : CoreContent.text("gift." + i);
            details.add(line.withStyle(ChatFormatting.GREEN));
        }
        var identity = stack.get(CoreContent.DATA.get());
        var player = event.getEntity();
        if (player != null && identity != null && identity.hasUUID("owner")
              && identity.getUUID("owner").equals(player.getUUID()) && CorePackets.clientState.getBoolean("bound")) {
            details.add(CoreContent.text("stored", CorePackets.clientState.getLong("energy")).withStyle(ChatFormatting.AQUA));
        }
        lines.remove(hint);
        lines.addAll(hint, details);
        // The first curse already states the removal rule; keep other mods' tooltip lines intact.
        lines.removeIf(line -> tooltipKey(line, "warning"));
    }
    private static boolean tooltipKey(Component line, String key) {
        return line.getContents() instanceof TranslatableContents content && content.getKey().equals("overloadcore." + key);
    }
    public static boolean affected(double x, double y, double z) {
        if (!CorePackets.clientState.getBoolean("bound")) return false;
        for (var value : CorePackets.clientState.getList("devices", Tag.TAG_COMPOUND)) {
            var d = (CompoundTag)value;
            if (new AABB(d.getDouble("minX"),d.getDouble("minY"),d.getDouble("minZ"),d.getDouble("maxX"),d.getDouble("maxY"),d.getDouble("maxZ")).inflate(.01).contains(x,y,z)) return true;
        }
        return false;
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance(); var data = CorePackets.clientState;
        if (mc.player == null || mc.options.hideGui || !data.getBoolean("bound")) return;
        var gui = event.getGuiGraphics(); int x = 8, y = gui.guiHeight() - 56;
        gui.drawString(mc.font, CoreContent.text("hud", data.getInt("load"), data.getInt("heat")), x, y, data.getBoolean("heavy") ? 0xFFFF7777 : 0xFFDBDEE7);
        if (diagnostics) {
            int line = 0;
            for (var value : data.getList("devices", Tag.TAG_COMPOUND)) {
                if (line >= 8) break; var d = (CompoundTag)value; var p = BlockPos.of(d.getLong("pos"));
                var label = Component.translatable(d.getString("name")).append(" " + p.getX()+", "+p.getY()+", "+p.getZ()+" ").append(CoreContent.text("state."+d.getString("issue")));
                gui.drawString(mc.font, label, 8, 28 + line++ * 11, d.getDouble("heat") >= 450 ? 0xFFFF9977 : 0xFFBDD6E3);
            }
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CorePackets.clientState = new CompoundTag(); diagnostics = false; }
}
