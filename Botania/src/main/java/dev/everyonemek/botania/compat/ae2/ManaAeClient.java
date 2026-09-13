package dev.everyonemek.botania.compat.ae2;

import appeng.api.client.*;
import appeng.api.stacks.AEItemKey;
import dev.everyonemek.botania.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;
import vazkii.botania.common.item.BotaniaItems;

public final class ManaAeClient {
    public static void register() {
        AEKeyRendering.register(ManaKey.TYPE, ManaKey.class, new AEKeyRenderHandler<ManaKey>() {
            @Override public void drawInGui(Minecraft minecraft, GuiGraphics gui, int x, int y, ManaKey key) { gui.renderItem(new ItemStack(BotaniaItems.MANA_SPARK), x, y); }
            @Override public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, ManaKey key, float scale, int light, Level level) {
                AEKeyRendering.drawOnBlockFace(pose, buffers, AEItemKey.of(BotaniaItems.MANA_SPARK), scale, light, level);
            }
            @Override public Component getDisplayName(ManaKey key) { return key.getDisplayName(); }
        });
        StorageCellModels.registerModel(Content.MANA_CELL.get(), StorageCellModels.getDefaultModel());
    }
    private ManaAeClient() { }
}
