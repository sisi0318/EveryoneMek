package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.clibano.AbstractClibanoFrameBlock;
import dev.everyonemek.forbidden.ClibanoEmbedding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClibanoFrameBlock.class)
public abstract class ClibanoFrameMixin {
    @Redirect(method = "onRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean forbiddenmekanism$installWall(Level level, BlockPos main, boolean moving,
          BlockState oldState, Level originalLevel, BlockPos wall, BlockState replacement, boolean isMoving) {
        return ClibanoEmbedding.preserveMain(level, wall, main, replacement) || level.removeBlock(main, moving);
    }
}
