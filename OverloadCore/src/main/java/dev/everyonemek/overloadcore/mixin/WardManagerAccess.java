package dev.everyonemek.overloadcore.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PersistentEntitySectionManager.class)
public interface WardManagerAccess {
    @Accessor("visibleEntityStorage") EntityLookup<Entity> overload$lookup();
}
