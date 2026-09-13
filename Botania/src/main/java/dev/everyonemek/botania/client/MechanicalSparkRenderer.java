package dev.everyonemek.botania.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.everyonemek.botania.MechanicalSparkEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.render.entity.ManaSparkRenderer;
import vazkii.botania.common.entity.ManaSparkEntity;

/** The original spark and its orbiting dye/augment icons remain rendered by Botania. */
public final class MechanicalSparkRenderer extends ManaSparkRenderer {
    private static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath("mekanism", "block/block_steel");
    public MechanicalSparkRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(ManaSparkEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partial, pose, buffers, light);
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(STEEL);
        float alpha = entity.isInvisible() ? .1F : .85F;
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation()); pose.translate(-.3, -.075, -.008);
        float[][] bars = {{0,0,.6F,.035F},{0,.565F,.6F,.035F},{0,.035F,.035F,.53F},{.565F,.035F,.035F,.53F}};
        for (var bar : bars) {
            pose.pushPose(); pose.translate(bar[0], bar[1], 0); pose.scale(bar[2], bar[3], 1);
            RenderHelper.renderIconFullBright(pose, buffers.getBuffer(RenderHelper.ICON_OVERLAY), sprite, 0xFFFFFF, alpha); pose.popPose();
        }
        if (entity instanceof MechanicalSparkEntity spark && spark.isMaster()) {
            pose.translate(.25, .6, .002); pose.scale(.1F, .055F, 1);
            RenderHelper.renderIconFullBright(pose, buffers.getBuffer(RenderHelper.ICON_OVERLAY), sprite, 0x66E1CF, alpha);
        }
        pose.popPose();
    }
}
