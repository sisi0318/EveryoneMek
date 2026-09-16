package dev.everyonemek.overloadcore.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Entity.class)
public interface WardEntityAccess {
    @Accessor("removalReason") void overload$removalReason(Entity.RemovalReason reason);
    @Accessor("levelCallback") EntityInLevelCallback overload$levelCallback();
}
