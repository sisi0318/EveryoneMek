package dev.everyonemek.botania.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.everyonemek.botania.ManaMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import vazkii.botania.api.mana.PoolOverlayProvider;
import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.core.helper.RenderHelper;

/** Uses the pool's public overlay provider and render layer on the chamber's basin floor. */
public final class InfusionCatalystRenderer implements BlockEntityRenderer<ManaMachine> {
    public InfusionCatalystRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public void render(ManaMachine tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null || !(tile.catalystVisual().getBlock() instanceof PoolOverlayProvider provider)) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(provider.getIcon(tile.getLevel(), tile.getBlockPos()));
        float alpha = (Mth.sin((ClientTickHandler.getEntityTicksInGame() + partialTick) / 20F) + 1) * .3F + .2F;
        pose.pushPose();
        pose.translate(3 / 16D, 10.25 / 16D + .001, 3 / 16D); pose.scale(.625F, .625F, .625F); pose.mulPose(Axis.XP.rotationDegrees(90));
        RenderHelper.renderIconFullBright(pose, buffers.getBuffer(RenderHelper.ICON_OVERLAY), sprite, 0xFFFFFF, alpha, light);
        pose.popPose();
    }
}
