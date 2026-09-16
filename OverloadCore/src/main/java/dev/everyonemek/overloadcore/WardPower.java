package dev.everyonemek.overloadcore;

import java.math.BigInteger;
import java.util.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.content.matrix.MatrixEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.multiblock.TileEntityInductionCell;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Emergency access to owned Mek reservoirs, not a transfer through a cable network or an output face. */
public final class WardPower {
    private record Source(DeviceScope.Device device, IEnergyContainer tank, long stored, long available) { }

    public static boolean pay(ServerPlayer player) {
        long cost = EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.WARD_COST_FE.get().longValue());
        if (cost <= 0) return false;
        var sources = new ArrayList<Source>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<IEnergyContainer, Boolean>());
        int radius = CoreConfig.RANGE.get();
        var level = player.serverLevel();
        int minX = Math.floorDiv(player.blockPosition().getX() - radius, 16), maxX = Math.floorDiv(player.blockPosition().getX() + radius, 16);
        int minZ = Math.floorDiv(player.blockPosition().getZ() - radius, 16), maxZ = Math.floorDiv(player.blockPosition().getZ() + radius, 16);
        BigInteger total = BigInteger.ZERO;
        for (int cx = minX; cx <= maxX; cx++) for (int cz = minZ; cz <= maxZ; cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) continue;
            for (var tile : chunk.getBlockEntities().values()) {
                // Matrix cells are accounted for through their assembled matrix, never again as individual reservoirs.
                if (!(tile instanceof TileEntityMekanism machine) || tile instanceof TileEntityInductionCell) continue;
                var device = DeviceScope.powerDevice(tile);
                if (!DeviceScope.permitted(device, player)) continue;
                for (var tank : machine.getEnergyContainers(null)) {
                    long stored = tank.getEnergy();
                    long available = tank instanceof MatrixEnergyContainer matrix
                          ? matrix.extract(Long.MAX_VALUE, Action.SIMULATE, AutomationType.INTERNAL)
                          : tank instanceof BasicEnergyContainer ? stored : 0;
                    if (available <= 0 || !seen.add(tank)) continue;
                    sources.add(new Source(device, tank, stored, available));
                    total = total.add(BigInteger.valueOf(available));
                }
            }
        }
        if (total.compareTo(BigInteger.valueOf(cost)) < 0) return false;
        sources.sort(Comparator.<Source>comparingInt(s -> s.tank instanceof MatrixEnergyContainer ? 0 : 1)
              .thenComparingLong(s -> s.device.anchor().getBlockPos().asLong()));
        long[] shares = new long[sources.size()];
        long remaining = cost;
        long minimum = cost >= sources.size() ? 1 : 0;
        long distributable = cost - minimum * sources.size();
        BigInteger weight = total.subtract(BigInteger.valueOf(minimum * sources.size()));
        // Give every nonempty reservoir a real share before distributing the balance proportionally.
        for (int i = 0; i < sources.size(); i++) {
            shares[i] = minimum + (distributable == 0 ? 0 : BigInteger.valueOf(distributable)
                  .multiply(BigInteger.valueOf(sources.get(i).available - minimum)).divide(weight).longValueExact());
            remaining -= shares[i];
        }
        for (int i = 0; i < sources.size() && remaining > 0; i++) {
            if (shares[i] < sources.get(i).available) { shares[i]++; remaining--; }
        }
        if (remaining != 0) return false;
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get(i);
            if (!DeviceScope.permitted(source.device, player) || source.tank.getEnergy() != source.stored) return false;
            if (source.tank instanceof MatrixEnergyContainer matrix
                  && matrix.extract(shares[i], Action.SIMULATE, AutomationType.INTERNAL) != shares[i]) return false;
        }
        // These are native reservoir objects, deduplicated across multiblock ports. Direct debit avoids
        // machine I/O rules, cube transfer limits and the cursed core's work-energy multiplier.
        for (int i = 0; i < sources.size(); i++) if (shares[i] > 0) {
            var source = sources.get(i);
            if (source.tank instanceof MatrixEnergyContainer matrix) {
                // Native matrix extraction queues a debit and updates getEnergy immediately; it cannot use setEnergy.
                matrix.extract(shares[i], Action.EXECUTE, AutomationType.INTERNAL);
            } else source.tank.setEnergy(source.stored - shares[i]);
        }
        int shown = 0;
        for (int i = 0; i < sources.size() && shown < 24; i++) if (shares[i] > 0) {
            BlockPos pos = sources.get(i).device.anchor().getBlockPos();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                  pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 3, .2, .2, .2, .01);
            shown++;
        }
        return true;
    }
    private WardPower() { }
}
