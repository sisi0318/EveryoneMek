package dev.everyonemek.overloadcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import dev.everyonemek.overloadcore.*;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OverloadCore.ID, value = Dist.CLIENT)
public final class CoreClient {
    private static final KeyMapping KEY = new KeyMapping("key.overloadcore.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.overloadcore");
    private static final KeyMapping EXTREME = new KeyMapping("key.overloadcore.extreme", net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.overloadcore");
    private static boolean diagnostics;
    private static final TooltipReveal REVEAL = new TooltipReveal();
    private record HoverTarget(Slot slot, Item item, DataComponentPatch components) { }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(CoreContent.CORE.get(), PendantRenderer::new);
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(CoreContent.WARD.get(), WardRenderer::new);
        });
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(KEY); event.register(EXTREME); }
    @SubscribeEvent public static void tooltipFactory(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ProgressiveCoreTooltip.class, tooltip -> tooltip);
    }
    @SubscribeEvent public static void beforeFrame(RenderFrameEvent.Pre event) {
        REVEAL.beginFrame(Screen.hasShiftDown() && Minecraft.getInstance().isWindowActive());
    }
    @SubscribeEvent public static void afterFrame(RenderFrameEvent.Post event) { REVEAL.endFrame(); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); if (mc.player == null) { CorePackets.clientState = new CompoundTag(); CorePackets.clientWardState = new CompoundTag(); return; }
        if (CorePackets.clientState.getBoolean("bound") && CorePackets.clientState.getBoolean("heavy")) mc.player.setSprinting(false);
        while (KEY.consumeClick()) diagnostics = !diagnostics;
        while (EXTREME.consumeClick()) if (mc.screen == null && CorePackets.clientWardState.getBoolean("equipped"))
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new CorePackets.WardExtreme(!CorePackets.clientWardState.getBoolean("extreme")));
        var wardState=CorePackets.clientWardState;
        if (wardState.getInt("ticks") > 0) wardState.putInt("ticks",wardState.getInt("ticks")-1);
    }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.GatherComponents event) {
        var stack = event.getItemStack();
        var mc = Minecraft.getInstance();
        boolean ward = stack.is(CoreContent.WARD);
        if ((!stack.is(CoreContent.CORE) && !ward) || !mc.isWindowActive()) return;
        String prefix = ward ? "ward." : "";
        var lines = event.getTooltipElements();
        long now = Util.getMillis();
        int maxWidth = Math.min(360, Math.max(12, event.getScreenWidth() - 24));
        if (event.getMaxWidth() > 0) maxWidth = Math.min(maxWidth, event.getMaxWidth());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).left().filter(line -> tooltipKey(line, prefix + "lore")).isPresent()) {
                var lore = ((Component) lines.get(i).left().orElseThrow()).copy().withStyle(ChatFormatting.WHITE);
                lines.set(i, Either.right(new ProgressiveCoreTooltip(java.util.List.of(lore), TooltipReveal.WRITE_MS, maxWidth, now)));
                break;
            }
        }
        if (!Screen.hasShiftDown()) return;
        int hint = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).left().filter(line -> tooltipKey(line, prefix + "details_hint")).isPresent()) { hint = i; break; }
        }
        if (hint < 0) return;

        var details = new ArrayList<Component>();
        if (ward) {
            details.add(CoreContent.text("ward.scope", CoreConfig.RANGE.get()).withStyle(ChatFormatting.GRAY));
            details.add(CoreContent.text("ward.price", CoreConfig.WARD_COST_FE.get()).withStyle(ChatFormatting.AQUA));
            details.add(CoreContent.text("ward.rescue").withStyle(ChatFormatting.GREEN));
            details.add(CoreContent.text("ward.unfunded").withStyle(ChatFormatting.RED));
            details.add(CoreContent.text("ward.removable").withStyle(ChatFormatting.GRAY));
            details.add(CoreContent.text("ward.custody").withStyle(ChatFormatting.GRAY));
            details.add(CoreContent.text("ward.extreme_hint", EXTREME.getTranslatedKeyMessage(), CoreConfig.WARD_RESERVE_PERCENT.get()).withStyle(ChatFormatting.AQUA));
            details.add(CoreContent.text("ward.shield_hint", CoreConfig.WARD_SHIELD_HITS.get(), CoreConfig.WARD_SHIELD_TICKS.get()/20.0).withStyle(ChatFormatting.GREEN));
            var seal=stack.get(CoreContent.WARD_SEAL);
            if (seal != null && (!CorePackets.clientWardState.hasUUID("seal") || !seal.equals(CorePackets.clientWardState.getUUID("seal"))))
                details.add(CoreContent.text("ward.pending_item").withStyle(ChatFormatting.YELLOW));
        } else {
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
            var player = mc.player;
            if (player != null && identity != null && identity.hasUUID("owner")
                  && identity.getUUID("owner").equals(player.getUUID()) && CorePackets.clientState.getBoolean("bound")) {
                details.add(CoreContent.text("stored", CorePackets.clientState.getLong("energy")).withStyle(ChatFormatting.AQUA));
            }
        }
        Slot slot = mc.screen instanceof AbstractContainerScreen<?> container ? container.getSlotUnderMouse() : null;
        if (slot != null && !ItemStack.isSameItemSameComponents(slot.getItem(), stack)) slot = null;
        long elapsed = REVEAL.sample(mc.screen, new HoverTarget(slot, stack.getItem(), stack.getComponentsPatch()), now);
        lines.set(hint, Either.right(new ProgressiveCoreTooltip(details, elapsed, maxWidth, now)));
        // The first curse already states the removal rule; keep other mods' tooltip lines intact.
        lines.removeIf(line -> line.left().filter(text -> tooltipKey(text, "warning")).isPresent());
    }
    private static boolean tooltipKey(FormattedText line, String key) {
        return line instanceof Component component && component.getContents() instanceof TranslatableContents content
              && content.getKey().equals("overloadcore." + key);
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
        if (mc.player == null || mc.screen != null || mc.options.hideGui || !mc.player.isAlive()
              || mc.player.isSpectator()) return;
        var gui = event.getGuiGraphics();
        if (data.getBoolean("bound")) CoreHud.render(gui, mc.font, data);
        var ward=CorePackets.clientWardState;
        if (ward.getBoolean("equipped") || ward.getBoolean("pending")) {
            int y=Math.max(8,gui.guiHeight()-(data.getBoolean("bound")?132:86));
            var mode=CoreContent.text(ward.getBoolean("pending")?"ward.state.pending":ward.getBoolean("extreme")?"ward.mode.extreme":"ward.mode.normal");
            gui.fill(8,y,20+mc.font.width(mode),y+12,0xA312101B);
            gui.drawString(mc.font,mode,12,y+2,ward.getBoolean("extreme")?0xFFFFAE74:0xFFADE5EA,false);
            if(ward.getInt("hits")>0 && ward.getInt("ticks")>0) {
                var shield=CoreContent.text("ward.shield_hud",ward.getInt("hits"),(ward.getInt("ticks")+19)/20);
                gui.drawString(mc.font,shield,12,y+15,0xFFA9E7FF,false);
            }
        }
        if (diagnostics && data.getBoolean("bound")) {
            int line = 0;
            for (var value : data.getList("devices", Tag.TAG_COMPOUND)) {
                if (line >= 8) break; var d = (CompoundTag)value; var p = BlockPos.of(d.getLong("pos"));
                var label = Component.translatable(d.getString("name")).append(" " + p.getX()+", "+p.getY()+", "+p.getZ()+" ").append(CoreContent.text("state."+d.getString("issue")));
                gui.drawString(mc.font, label, 8, 28 + line++ * 11, d.getDouble("heat") >= 450 ? 0xFFFF9977 : 0xFFBDD6E3);
            }
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        CorePackets.clientState = new CompoundTag(); CorePackets.clientWardState = new CompoundTag(); diagnostics = false; REVEAL.reset();
    }
}
