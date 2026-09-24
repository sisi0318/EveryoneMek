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
        if((old.getBlock() instanceof PartBlock||old.getBlock() instanceof ControllerBlock)&&old.is(next.getBlock())&&old.getValues().entrySet().stream().allMatch(e->e.getKey().getName().equals("active")||e.getKey().getName().equals("formed")||e.getKey()==PartBlock.FACING&&old.getBlock() instanceof PartBlock part&&part.kind!=PartBlock.Kind.COIL||e.getValue().equals(next.getValues().get(e.getKey()))))return;
        Structure.changed(((LevelChunk)(Object)this).getLevel(),pos);
        if(old.getBlock() instanceof dev.everyonemek.gravity.solar.SolarBlock part&&old.is(next.getBlock())&&old.getValues().entrySet().stream().allMatch(e->e.getKey()==dev.everyonemek.gravity.solar.SolarBlock.ACTIVE||e.getKey()==dev.everyonemek.gravity.solar.SolarBlock.FORMED||e.getKey()==dev.everyonemek.gravity.solar.SolarBlock.SEGMENT||e.getKey()==dev.everyonemek.gravity.solar.SolarBlock.FACING&&part.kind!=dev.everyonemek.gravity.solar.SolarBlock.Kind.FOCUS||e.getValue().equals(next.getValues().get(e.getKey()))))return;
        if(old.getBlock() instanceof dev.everyonemek.gravity.solar.SolarControllerBlock&&old.is(next.getBlock())&&old.getValues().entrySet().stream().allMatch(e->e.getKey().getName().equals("active")||e.getKey()==dev.everyonemek.gravity.solar.SolarBlock.FORMED||e.getValue().equals(next.getValues().get(e.getKey()))))return;
        dev.everyonemek.gravity.solar.SolarStructure.changed(((LevelChunk)(Object)this).getLevel(),pos);
    }
}
