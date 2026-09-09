package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import java.util.HashMap;
import net.minecraft.core.BlockPos;

/** Test-only isolation: vanilla NA spread/balance otherwise moves Aura from earlier test fixtures. */
final class AuraTestEnvironment {
    static int set(AuraMachine machine, int radius, int target) {
        var level = machine.getLevel();
        BlockPos center = machine.getBlockPos();
        var previous = new HashMap<BlockPos, Integer>();
        IAuraChunk.getSpotsInArea(level, center, 128, previous::put);
        previous.forEach((pos, amount) -> {
            var chunk = IAuraChunk.getAuraChunk(level, pos);
            if (amount > 0) chunk.drainAura(pos, amount, false, false);
            else if (amount < 0) chunk.storeAura(pos, -amount, false, false);
        });
        var chunk = IAuraChunk.getAuraChunk(level, center);
        int current = IAuraChunk.getAuraInArea(level, center, radius);
        if (current > target) chunk.drainAura(center, current - target, false, false);
        else if (current < target) chunk.storeAura(center, target - current, false, false);
        return IAuraChunk.getAuraInArea(level, center, radius);
    }

    private AuraTestEnvironment() { }
}
