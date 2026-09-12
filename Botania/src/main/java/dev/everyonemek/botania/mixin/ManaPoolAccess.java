package dev.everyonemek.botania.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

@Mixin(value = ManaPoolBlockEntity.class, remap = false)
public interface ManaPoolAccess {
    @Accessor("canAccept") boolean botanicalmekanism$canAccept();
    @Accessor("canSpare") boolean botanicalmekanism$canSpare();
}
