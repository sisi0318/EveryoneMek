package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.clibano.AbstractClibanoFrameBlock;
import dev.everyonemek.forbidden.ClibanoEmbedding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClibanoFrameBlock.class)
public abstract class ClibanoFrameMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void forbiddenmekanism$openController(BlockState state, Level level, BlockPos pos,
          net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit,
          CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        var result = ClibanoEmbedding.openFromPart(level, pos, player);
        if (result != null) cir.setReturnValue(result);
    }
    @Redirect(method = "onRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean forbiddenmekanism$installWall(Level level, BlockPos main, boolean moving,
          BlockState oldState, Level originalLevel, BlockPos wall, BlockState replacement, boolean isMoving) {
        return ClibanoEmbedding.preserveMain(level, wall, main, replacement) || level.removeBlock(main, moving);
    }
}
