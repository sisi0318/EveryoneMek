package dev.everyonemek.botania.compat.ae2;

import appeng.api.client.*;
import appeng.api.stacks.AEItemKey;
import dev.everyonemek.botania.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;

public final class ManaAeClient {
    public static void register() {
        AEKeyRendering.register(ManaKey.TYPE, ManaKey.class, new AEKeyRenderHandler<ManaKey>() {
            @Override public void drawInGui(Minecraft minecraft, GuiGraphics gui, int x, int y, ManaKey key) { dev.everyonemek.botania.client.ManaIcon.draw(gui, x, y); }
            @Override public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, ManaKey key, float scale, int light, Level level) {
                AEKeyRendering.drawOnBlockFace(pose, buffers, AEItemKey.of(Content.MANA_PACKET.get()), scale, light, level);
            }
            @Override public Component getDisplayName(ManaKey key) { return key.getDisplayName(); }
        });
        StorageCellModels.registerModel(Content.MANA_CELL.get(), StorageCellModels.getDefaultModel());
    }
    private ManaAeClient() { }
}
