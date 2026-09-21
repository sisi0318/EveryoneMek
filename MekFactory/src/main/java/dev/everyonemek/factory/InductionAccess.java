package dev.everyonemek.factory;

import dev.everyonemek.factory.compat.Compat;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.tile.multiblock.TileEntityInductionCell;
import mekanism.common.tile.multiblock.TileEntityInductionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** References the actual native or Extras cell; no copied energy and no persistent aggregate battery. */
public final class InductionAccess {
    public record Cell(BlockEntity tile, IEnergyContainer getEnergyContainer) {
        public BlockPos getBlockPos() { return tile.getBlockPos(); }
    }
    public static IEnergyContainer cell(BlockEntity tile) {
        return tile instanceof TileEntityInductionCell cell ? cell.getEnergyContainer() : Compat.cell(tile);
    }
    public static long providerOutput(BlockEntity tile) {
        return tile instanceof TileEntityInductionProvider provider ? provider.tier.getOutput() : Compat.providerOutput(tile);
    }
    public static boolean part(BlockEntity tile) { return cell(tile) != null || providerOutput(tile) >= 0; }
    private InductionAccess() { }
}
