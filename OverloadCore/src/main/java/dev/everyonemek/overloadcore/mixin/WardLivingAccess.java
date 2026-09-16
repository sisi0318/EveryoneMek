package dev.everyonemek.overloadcore.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LivingEntity.class)
public interface WardLivingAccess {
    @Accessor("dead") void overload$dead(boolean dead);
    @Accessor("DATA_HEALTH_ID") static EntityDataAccessor<Float> overload$healthId() { throw new AssertionError(); }
}
