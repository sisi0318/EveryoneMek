package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.item.mundabitur.CreateClibanoInteraction;
import com.stal111.forbidden_arcanus.common.item.mundabitur.TransformPatternInteraction;
import dev.everyonemek.forbidden.ClibanoEmbedding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransformPatternInteraction.class)
public abstract class ClibanoPatternMixin {
    @Inject(method = "canInteract(Lcom/stal111/forbidden_arcanus/common/item/mundabitur/TransformPatternInteraction$TransformPatternContext;)Z", at = @At("RETURN"), cancellable = true)
    private void forbiddenmekanism$singleWall(TransformPatternInteraction.TransformPatternContext context, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof CreateClibanoInteraction && (cir.getReturnValueZ()
              || ClibanoEmbedding.isController(context.level().getBlockState(context.pos()))))
            cir.setReturnValue(ClibanoEmbedding.validBase(context));
    }
}
