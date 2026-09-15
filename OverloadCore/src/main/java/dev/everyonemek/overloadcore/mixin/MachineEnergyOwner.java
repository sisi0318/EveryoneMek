package dev.everyonemek.overloadcore.mixin;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value = MachineEnergyContainer.class, remap = false)
public interface MachineEnergyOwner { @Accessor("tile") TileEntityMekanism overload$tile(); }
