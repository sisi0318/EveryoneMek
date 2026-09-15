package dev.everyonemek.botania.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import vazkii.botania.api.block_entity.SpecialFlowerBlockEntity;
@Mixin(SpecialFlowerBlockEntity.class)
public interface CultivatedFlowerInterval {
    @Invoker("getUpdateInterval") int botanicalmekanism$interval();
}
