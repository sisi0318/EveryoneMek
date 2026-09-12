package dev.everyonemek.botania.mixin;

import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.block_entity.ManaEnchanterBlockEntity;
import vazkii.patchouli.api.IMultiblock;

@Mixin(value = ManaEnchanterBlockEntity.class, remap = false)
public interface EnchanterAccess {
    @Accessor("FORMED_MULTIBLOCK") static Supplier<IMultiblock> botanicalmekanism$formed() { throw new AssertionError(); }
}
