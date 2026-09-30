package dev.everyonemek.overloadcore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.everyonemek.overloadcore.client.GearRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Add only local shader accents after the native OBJ and its native hand transform were rendered. */
@Mixin(ItemRenderer.class)
public abstract class MekaToolRenderMixin {
    @Inject(method="render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void overload$toolForm(ItemStack stack,ItemDisplayContext context,boolean left,PoseStack pose,MultiBufferSource buffers,int light,int overlay,BakedModel model,CallbackInfo ci){
        GearRenderer.renderTool(stack,context,pose,buffers);
    }
}
