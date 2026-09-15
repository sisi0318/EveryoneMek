package dev.everyonemek.botania.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.block_entity.flower.generating.MunchdewBlockEntity;
@Mixin(MunchdewBlockEntity.class)
public interface CultivatedMunchdewAccess {
    @Accessor("MANA_PER_LEAF") static int botanicalmekanism$mana() { throw new AssertionError(); }
    @Accessor("COOLDOWN_TICKS") static int botanicalmekanism$cooldown() { throw new AssertionError(); }
}
