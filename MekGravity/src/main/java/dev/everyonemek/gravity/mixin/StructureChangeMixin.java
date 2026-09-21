package dev.everyonemek.gravity.mixin;
import dev.everyonemek.gravity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class StructureChangeMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void gravity$changed(BlockPos pos,BlockState next,boolean moving,CallbackInfoReturnable<BlockState> ci){var old=ci.getReturnValue();if(old==null)return;
        if((old.getBlock() instanceof PartBlock||old.getBlock() instanceof ControllerBlock)&&old.is(next.getBlock())&&old.getValues().entrySet().stream().allMatch(e->e.getKey().getName().equals("active")||e.getValue().equals(next.getValues().get(e.getKey()))))return;
        Structure.changed(((LevelChunk)(Object)this).getLevel(),pos);
    }
}
