package dev.everyonemek.overloadcore.mixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ServerLevel.class)
public interface WardServerLevelAccess {
    @Accessor("entityManager") PersistentEntitySectionManager<Entity> overload$manager();
    @Accessor("entityTickList") EntityTickList overload$tickList();
}
