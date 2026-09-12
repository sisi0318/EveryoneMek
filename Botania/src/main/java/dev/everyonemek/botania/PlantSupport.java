package dev.everyonemek.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class PlantSupport {
    public static boolean canStand(LevelReader level, BlockPos position) {
        BlockPos below = position.below();
        if (!level.hasChunkAt(below)) return false;
        if (level.getBlockState(below).is(vazkii.botania.common.block.BotaniaBlocks.RED_STRINGED_SPOOFER)) return false;
        if (Block.canSupportCenter(level, below, Direction.UP)) return true;
        // Cables have a narrow physical stem and expose FE at the flower's root.
        return level instanceof Level world && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
              && world.getCapability(Capabilities.EnergyStorage.BLOCK, below, Direction.UP) != null;
    }
    public static boolean areaLoaded(Level level, BlockPos center, int radius) {
        for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; x++)
            for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; z++)
                if (!level.hasChunk(x, z)) return false;
        return true;
    }
    private PlantSupport() { }
}
