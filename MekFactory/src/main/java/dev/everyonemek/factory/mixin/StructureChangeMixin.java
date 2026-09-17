package dev.everyonemek.factory.mixin;
import dev.everyonemek.factory.FactoryStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class StructureChangeMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void factory$changed(BlockPos pos,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> ci){
        var before=ci.getReturnValue();
        // The controller's active lamp does not change the structure or its assembled appearance.
        if(before!=null&&before.getBlock() instanceof dev.everyonemek.factory.ControllerBlock&&before.is(state.getBlock())
              &&before.getValues().entrySet().stream().allMatch(e->e.getKey().getName().equals("active")||e.getValue().equals(state.getValues().get(e.getKey()))))return;
        if(before!=null)FactoryStructure.changed(((LevelChunk)(Object)this).getLevel(),pos);
    }
}
