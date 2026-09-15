package dev.everyonemek.overloadcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.everyonemek.overloadcore.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OverloadCore.ID, value = Dist.CLIENT)
public final class CoreClient {
    private static final KeyMapping KEY = new KeyMapping("key.overloadcore.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.overloadcore");
    private static boolean diagnostics;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CorePackets.onClient = data -> { if (data.getBoolean("open")) Minecraft.getInstance().setScreen(new CoreScreen()); };
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(CoreContent.CORE.get(), PendantRenderer::new);
        });
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(KEY); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); if (mc.player == null) { CorePackets.clientState = new CompoundTag(); return; }
        if (CorePackets.clientState.getBoolean("bound") && CorePackets.clientState.getBoolean("heavy")) mc.player.setSprinting(false);
        while (KEY.consumeClick()) {
            if (Screen.hasShiftDown()) diagnostics = !diagnostics;
            else PacketDistributor.sendToServer(new CorePackets.Request(0));
        }
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
    private static final class CoreScreen extends Screen {
        private int page;
        CoreScreen() { super(Component.translatable("item.overloadcore.overloaded_short_circuit_core")); }
        @Override protected void init() {
            int x=(width-300)/2, y=(height-228)/2;
            addRenderableWidget(Button.builder(CoreContent.text("page"), b -> { page=(page+1)%3; b.setMessage(CoreContent.text("page").append(" "+(page+1)+"/3")); }).bounds(x+16,y+202,128,20).build());
            addRenderableWidget(Button.builder(CoreContent.text("close"), b -> onClose()).bounds(x+156,y+202,128,20).build());
        }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
            int x=(width-300)/2, y=(height-228)/2; var data=CorePackets.clientState;
            gui.fill(x-1,y-1,x+301,y+229,0xFF647180); gui.fill(x,y,x+300,y+228,0xEF171E28);
            gui.drawCenteredString(font,title,width/2,y+12,0xFFE6E9EF);
            gui.drawString(font,CoreContent.text(data.getBoolean("bound")?"bound":"warning"),x+16,y+32,0xFFFF99AD);
            gui.drawString(font,CoreContent.text("stored",data.getLong("energy")),x+16,y+47,0xFF88D6DC);
            gui.drawString(font,CoreContent.text("hud",data.getInt("load"),data.getInt("heat")),x+16,y+62,0xFFD9DFE7);
            if (page<2) for(int i=0;i<4;i++) gui.drawWordWrap(font,CoreContent.text("curse."+(page*4+i)),x+16,y+83+i*24,268,0xFFCFD4DE);
            else {
                for(int i=0;i<4;i++) gui.drawWordWrap(font,CoreContent.text("gift."+i),x+16,y+83+i*24,268,0xFFACDCC7);
                gui.drawWordWrap(font,CoreContent.text("sharing_hint"),x+16,y+178,268,0xFFBAC3D3);
            }
            super.render(gui,mx,my,partial);
        }
    }
}
