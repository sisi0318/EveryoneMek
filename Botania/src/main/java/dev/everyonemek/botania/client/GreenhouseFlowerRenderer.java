package dev.everyonemek.botania.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.everyonemek.botania.ManaMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;

/** The installed flower uses its own block model, scaled inside the chamber. */
public final class GreenhouseFlowerRenderer implements BlockEntityRenderer<ManaMachine> {
    public GreenhouseFlowerRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public void render(ManaMachine tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var flower = tile.greenhouseVisual(); if (flower.isAir()) return;
        pose.pushPose(); pose.translate(.25, .375, .25); pose.scale(.5F, .5F, .5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(flower, pose, buffers, light, overlay);
        pose.popPose();
    }
}
