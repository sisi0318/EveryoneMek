package dev.everyonemek.overloadcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** A small pendant follows the wearer's torso, including armour and sneaking poses. */
public final class PendantRenderer implements ICurioRenderer {
    @Override public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext context, PoseStack pose,
          RenderLayerParent<T,M> parent, MultiBufferSource buffers, int light, float swing, float swingAmount, float partial,
          float age, float yaw, float pitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> model)) return;
        pose.pushPose(); model.body.translateAndRotate(pose); pose.translate(0, .22, -.26); pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.scale(.25F,.25F,.25F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
              pose,buffers,context.entity().level(),context.entity().getId()); pose.popPose();
    }
}
