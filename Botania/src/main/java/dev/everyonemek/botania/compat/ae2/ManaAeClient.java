package dev.everyonemek.botania.compat.ae2;

import appeng.api.client.*;
import appeng.api.stacks.AEItemKey;
import dev.everyonemek.botania.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.mojang.blaze3d.vertex.PoseStack;

public final class ManaAeClient {
    private static final ResourceLocation CELL_MODEL = ResourceLocation.fromNamespaceAndPath("botanicalmekanism", "block/drive/mana_storage_cell");

    public static void models(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(CELL_MODEL));
    }

    public static void register() {
        AEKeyRendering.register(ManaKey.TYPE, ManaKey.class, new AEKeyRenderHandler<ManaKey>() {
            @Override public void drawInGui(Minecraft minecraft, GuiGraphics gui, int x, int y, ManaKey key) { dev.everyonemek.botania.client.ManaIcon.draw(gui, x, y); }
            @Override public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, ManaKey key, float scale, int light, Level level) {
                AEKeyRendering.drawOnBlockFace(pose, buffers, AEItemKey.of(Content.MANA_PACKET.get()), scale, light, level);
            }
            @Override public Component getDisplayName(ManaKey key) { return key.getDisplayName(); }
        });
        StorageCellModels.registerModel(Content.MANA_CELL.get(), CELL_MODEL);
    }
    private ManaAeClient() { }
}
