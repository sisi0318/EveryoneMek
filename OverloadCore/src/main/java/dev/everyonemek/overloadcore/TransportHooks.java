package dev.everyonemek.overloadcore;

import java.util.*;
import dev.everyonemek.overloadcore.mixin.NetworkAccess;
import mekanism.common.lib.transmitter.DynamicNetwork;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Limits one native allocation at an eligible endpoint; does not alter network capacity. */
public final class TransportHooks {
    private record Endpoint(BlockPos pos, Direction side) { }
    private static final class Budget { long tick = Long.MIN_VALUE, remaining; int carry; }
    public static final class Context {
        final DynamicNetwork<?, ?, ?> network;
        final IdentityHashMap<Object, Endpoint> endpoints = new IdentityHashMap<>();
        final Context previous;
        Context(DynamicNetwork<?, ?, ?> network) {
            this.network = network; previous = CURRENT.get();
            var iterator = ((NetworkAccess) network).overload$acceptors().getAcceptorFastIterator();
            while (iterator.hasNext()) {
                var entry = iterator.next(); var pos = BlockPos.of(entry.getLongKey());
                for (var side : entry.getValue().entrySet()) endpoints.put(side.getValue(), new Endpoint(pos, side.getKey()));
            }
        }
    }
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    private static final Map<DynamicNetwork<?, ?, ?>, Map<Endpoint, Budget>> BUDGETS = new WeakHashMap<>();
    public static Context begin(DynamicNetwork<?, ?, ?> network) {
        if (!CoreConfig.TRANSPORT.get()) return null;
        var context = new Context(network); CURRENT.set(context); return context;
    }
    public static void end(Context context) { if (context != null) { if (context.previous == null) CURRENT.remove(); else CURRENT.set(context.previous); } }
    public static long limit(Object handler, long offered) {
        var context = CURRENT.get(); if (context == null || offered <= 0) return offered;
        var endpoint = context.endpoints.get(handler); var level = context.network.getWorld();
        if (endpoint == null || level == null || !level.hasChunkAt(endpoint.pos)) return offered;
        var tile = level.getBlockEntity(endpoint.pos);
        if (tile == null || DeviceScope.bearer(tile) == null) return offered;
        var budgets = BUDGETS.computeIfAbsent(context.network, unused -> new HashMap<>());
        var budget = budgets.computeIfAbsent(endpoint, unused -> new Budget());
        long tick = level.getGameTime();
        if (budget.tick != tick) {
            budget.tick = tick; budget.remaining = offered / 2 + (offered % 2 + budget.carry) / 2;
            budget.carry = (int) ((offered % 2 + budget.carry) % 2);
            if (budgets.size() > context.endpoints.size() + 64) budgets.entrySet().removeIf(e -> tick - e.getValue().tick > 200);
        }
        return Math.min(offered, Math.max(0, budget.remaining));
    }
    public static void accepted(Object handler, long amount) {
        var context = CURRENT.get(); if (context == null || amount <= 0) return;
        var budgets = BUDGETS.get(context.network); if (budgets == null) return;
        var budget = budgets.get(context.endpoints.get(handler));
        if (budget != null && budget.tick == context.network.getWorld().getGameTime()) budget.remaining = Math.max(0, budget.remaining - amount);
    }
    public static int itemSpeed(int speed, BlockEntity pipe, mekanism.common.content.transporter.TransporterStack stack) {
        if (!CoreConfig.TRANSPORT.get() || pipe == null) return speed;
        var bearer = DeviceScope.bearer(pipe); if (bearer == null) return speed;
        var device = DeviceScope.device(pipe);
        if (!DeviceScope.cargoOwned(pipe.getLevel(), stack.originalLocation, device.owner()) && !DeviceScope.cargoOwned(pipe.getLevel(), stack.getDest(), device.owner())) return speed;
        // Base tier speed is read afresh per segment, so a long path costs 2x time, never 2^length.
        return speed / 2 + (speed % 2 == 1 && pipe.getLevel().getGameTime() % 2 == 0 ? 1 : 0);
    }
    private TransportHooks() { }
}
