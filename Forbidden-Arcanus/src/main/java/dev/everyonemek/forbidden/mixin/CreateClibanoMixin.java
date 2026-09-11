package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.item.mundabitur.CreateClibanoInteraction;
import dev.everyonemek.forbidden.ClibanoEmbedding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateClibanoInteraction.class)
public abstract class CreateClibanoMixin {
    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void forbiddenmekanism$keepController(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (ClibanoEmbedding.isPart(level.getBlockState(pos))) ci.cancel();
    }
}
