package dev.everyonemek.natures;

import de.ellpeck.naturesaura.blocks.multi.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class OfferingFlowers {
    private OfferingFlowers() { }

    public static boolean complete(Level level, BlockPos center) {
        // Check the original R positions only: our machine deliberately replaces its 0 position.
        return Multiblocks.OFFERING_TABLE.forEach(center, 'R', (pos, matcher) ->
              level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.SMALL_FLOWERS));
    }

    public static boolean allWitherRoses(Level level, BlockPos center) {
        return Multiblocks.OFFERING_TABLE.forEach(center, 'R', (pos, matcher) ->
              level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.WITHER_ROSE));
    }
}
