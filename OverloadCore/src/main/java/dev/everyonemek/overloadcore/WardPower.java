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
import com.google.common.collect.MapMaker;

/** Emergency access to owned Mek reservoirs, not a transfer through a cable network or an output face. */
public final class WardPower {
    private record Source(DeviceScope.Device device, IEnergyContainer tank, long stored, long available) { }
    private record Candidates(net.minecraft.server.level.ServerLevel level, long tick, BlockPos origin, int radius, List<BlockPos> positions) { }
    private static final Map<ServerPlayer, Candidates> CANDIDATES = new MapMaker().weakKeys().makeMap();
    public static void forget(ServerPlayer player) { CANDIDATES.remove(player); }
    private static List<BlockPos> candidates(ServerPlayer p, boolean fresh) {
        var cached = CANDIDATES.get(p); var level = p.serverLevel(); int radius = CoreConfig.RANGE.get();
        if (!fresh && cached != null && cached.level == level && cached.tick == level.getGameTime()
              && cached.origin.equals(p.blockPosition()) && cached.radius == radius) return cached.positions;
        var result = new ArrayList<BlockPos>();
        int minX = Math.floorDiv(p.blockPosition().getX()-radius,16), maxX = Math.floorDiv(p.blockPosition().getX()+radius,16);
        int minZ = Math.floorDiv(p.blockPosition().getZ()-radius,16), maxZ = Math.floorDiv(p.blockPosition().getZ()+radius,16);
        for (int cx=minX;cx<=maxX;cx++) for(int cz=minZ;cz<=maxZ;cz++) {
            var chunk=level.getChunkSource().getChunkNow(cx,cz); if(chunk==null)continue;
            for(var tile:chunk.getBlockEntities().values()) if(tile instanceof TileEntityMekanism && !(tile instanceof TileEntityInductionCell)) result.add(tile.getBlockPos());
        }
        CANDIDATES.put(p,new Candidates(level,level.getGameTime(),p.blockPosition(),radius,result));
        return result;
    }

    public static boolean pay(ServerPlayer player) {
        return pay(player, false);
    }
    private static boolean pay(ServerPlayer player, boolean fresh) {
        long cost = EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.WARD_COST_FE.get().longValue());
        if (cost <= 0) return false;
        var sources = new ArrayList<Source>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<IEnergyContainer, Boolean>());
        var level = player.serverLevel();
        BigInteger total = BigInteger.ZERO, unreserved = BigInteger.ZERO;
        boolean extreme = WardLedger.get(player).extreme(player), found = false;
        var previous = CANDIDATES.get(player);
        var positions = candidates(player, fresh);
        boolean reused = !fresh && previous != null && previous.positions == positions;
        for (var pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            var tile = level.getBlockEntity(pos);
                // Matrix cells are accounted for through their assembled matrix, never again as individual reservoirs.
                if (!(tile instanceof TileEntityMekanism machine) || tile instanceof TileEntityInductionCell) continue;
                var device = DeviceScope.powerDevice(tile);
                if (!DeviceScope.powerPermitted(device, player)) continue;
                for (var tank : machine.getEnergyContainers(null)) {
                    if (!(tank instanceof BasicEnergyContainer) && !(tank instanceof MatrixEnergyContainer)) continue;
                    if (!seen.add(tank)) continue;
                    found = true;
                    long stored = tank.getEnergy();
                    long available = tank instanceof MatrixEnergyContainer matrix
                          ? matrix.extract(Long.MAX_VALUE, Action.SIMULATE, AutomationType.INTERNAL)
                          : tank instanceof BasicEnergyContainer ? stored : 0;
                    unreserved = unreserved.add(BigInteger.valueOf(available));
                    if (!extreme) available = Math.min(available, Math.max(0,stored-WardSources.reserve(device,tank.getMaxEnergy())));
                    if (available <= 0) continue;
                    sources.add(new Source(device, tank, stored, available));
                    total = total.add(BigInteger.valueOf(available));
                }
        }
        if (total.compareTo(BigInteger.valueOf(cost)) < 0) {
            // A same-tick placement/refill may add a previously absent source. Cache identities, never money or failures.
            if (reused) return pay(player, true);
            WardRuntime.failed(player, !found ? "no_source" : !extreme && unreserved.compareTo(BigInteger.valueOf(cost)) >= 0 ? "reserved" : "energy");
            return false;
        }
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
            if (!DeviceScope.powerPermitted(source.device, player) || source.tank.getEnergy() != source.stored) return false;
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
        int count = 0; for (long share : shares) if (share > 0) count++;
        WardRuntime.paid(player, CoreConfig.WARD_COST_FE.get(), count);
        int shown = 0;
        for (int i = 0; WardRuntime.pulse(player) && i < sources.size() && shown < 24; i++) if (shares[i] > 0) {
            BlockPos pos = sources.get(i).device.anchor().getBlockPos();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                  pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 3, .2, .2, .2, .01);
            shown++;
        }
        return true;
    }
    private WardPower() { }
}
