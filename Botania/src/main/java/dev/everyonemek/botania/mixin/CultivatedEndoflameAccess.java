package dev.everyonemek.botania.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.block_entity.flower.generating.EndoflameBlockEntity;
@Mixin(EndoflameBlockEntity.class)
public interface CultivatedEndoflameAccess {
    @Accessor("FUEL_CAP") static int botanicalmekanism$fuelCap() { throw new AssertionError(); }
    @Accessor("COOLDOWN_TIME") static int botanicalmekanism$cooldown() { throw new AssertionError(); }
}
