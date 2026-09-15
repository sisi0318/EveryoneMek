package dev.everyonemek.botania.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import vazkii.botania.common.block.block_entity.flower.generating.FluidGeneratorBlockEntity;
@Mixin(FluidGeneratorBlockEntity.class)
public interface CultivatedFluidFlowerAccess {
    @Accessor("consumedFluid") TagKey<Fluid> botanicalmekanism$fluid();
    @Accessor("startBurnTime") int botanicalmekanism$burnTicks();
    @Accessor("manaPerTick") int botanicalmekanism$manaPerTick();
    @Accessor("cooldownTime") int botanicalmekanism$cooldown();
}
