package dev.everyonemek.overloadcore;

import java.util.Map;
import com.google.common.collect.MapMaker;
import dev.everyonemek.overloadcore.mixin.WardLivingAccess;
import dev.everyonemek.overloadcore.mixin.WardEntityAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import top.theillusivec4.curios.api.CuriosApi;

/** One debit per fatal call chain; a fresh hurt call always opens a fresh incident, even in the same tick. */
public final class ThunderWard {
    public static final String FINALIZED_KEY = "overloadcore_ward_death_finalized";
    private static final class State {
        long protectedTick = Long.MIN_VALUE;
        long failedProbeTick = Long.MIN_VALUE;
        int damageDepth, lifecycleDepth;
        boolean paying, finalized, cleanup, restoring, discardAfterLifecycle;
        AttributeInstance healthyMax;
        float lastMax = 20;
    }
    // Entity.equals uses the entity ID, which vanilla reuses for a replacement player on respawn.
    // Weak identity keys keep the old life's finalized flag away from the new player object.
    private static final Map<ServerPlayer, State> STATES = new MapMaker().weakKeys().makeMap();
    private static State state(ServerPlayer player) { return STATES.computeIfAbsent(player, p -> new State()); }

    public static boolean tracked(ServerPlayer player) {
        return player.serverLevel().getServer().isSameThread() && player.getUUID() != null && player.serverLevel().getEntity(player.getId()) == player
              && player.serverLevel().getEntity(player.getUUID()) == player;
    }
    private static boolean destructive(Entity.RemovalReason reason) {
        return reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED;
    }
    private static boolean eligible(ServerPlayer player, State state) {
        var reason = player.getRemovalReason();
        return !state.finalized && !player.getPersistentData().getBoolean(FINALIZED_KEY)
              && !state.paying && !state.restoring && state.lifecycleDepth == 0
              && !player.isSpectator() && (reason == null || destructive(reason)) && tracked(player);
    }
    private static float rawHealth(ServerPlayer player) {
        return player.getEntityData().get(WardLivingAccess.overload$healthId());
    }
    private static boolean valid(float value) { return Float.isFinite(value) && value > 0; }
    public static void beginLifecycle(ServerPlayer player) { state(player).lifecycleDepth++; }
    public static void endLifecycle(ServerPlayer player) {
        var state = state(player);
        if (--state.lifecycleDepth == 0 && state.discardAfterLifecycle) STATES.remove(player);
    }

    public static boolean equipped(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT)).map(h ->
              h.getStacks().getSlots() > 0 && !h.getActiveStates().isEmpty() && h.getActiveStates().get(0)
                    && h.getStacks().getStackInSlot(0).is(CoreContent.WARD)).orElse(false);
    }
    public static void beginDamage(ServerPlayer player) {
        var state = state(player);
        if (state.damageDepth++ == 0) {
            if (state.cleanup && state.protectedTick == player.level().getGameTime()) {
                player.deathTime = 0;
                ((WardLivingAccess)player).overload$dead(false);
            }
            state.cleanup = false;
            state.protectedTick = Long.MIN_VALUE;
            state.failedProbeTick = Long.MIN_VALUE;
        }
    }
    public static void endDamage(ServerPlayer player) { state(player).damageDepth--; }
    public static boolean protectedNow(ServerPlayer player) {
        var state = STATES.get(player);
        var reason = player.getRemovalReason();
        return state != null && !state.finalized && !player.getPersistentData().getBoolean(FINALIZED_KEY) && state.lifecycleDepth == 0
              && (reason == null || destructive(reason)) && state.protectedTick == player.level().getGameTime() && tracked(player);
    }
    public static float healthInput(ServerPlayer player, float value) {
        // NaN must not bypass the native <= 0 check or the normal totem path.
        return Float.isNaN(value) && tracked(player) && equipped(player) ? 0 : value;
    }
    public static boolean setter(ServerPlayer player, float health) {
        if (health > 0) return false;
        var state = STATES.get(player);
        // Ordinary damage keeps vanilla totem priority. Direct clear-health calls need their own interception.
        return (state == null || state.damageDepth == 0) && rescue(player);
    }
    public static boolean rescue(ServerPlayer player) {
        var state = state(player);
        if (!eligible(player, state)) return false;
        if (protectedNow(player)) { restore(player); return true; }
        if (!equipped(player)) return false;
        state.paying = true;
        try {
            if (!WardPower.pay(player)) return false;
            state.protectedTick = player.level().getGameTime();
            state.failedProbeTick = Long.MIN_VALUE;
            state.cleanup = true;
            restore(player);
            player.displayClientMessage(CoreContent.text("ward.saved"), true);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                  player.getX(), player.getY() + 1, player.getZ(), 32, .4, .7, .4, .03);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.TOTEM_USE,
                  net.minecraft.sounds.SoundSource.PLAYERS, .7F, .7F);
            CorePackets.wardPulse(player);
            return true;
        } finally { state.paying = false; }
    }
    private static void restore(ServerPlayer player) {
        var state = state(player);
        if (state.restoring) return;
        state.restoring = true;
        try {
        var access = (WardLivingAccess) player;
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null && !valid((float)attribute.getValue())) {
            if (state.healthyMax != null) attribute.replaceFrom(state.healthyMax);
            else {
                // A freshly equipped ward may have no snapshot yet. Recompute in a detached instance first;
                // setting an unchanged base value would leave a forged cachedValue/dirty pair untouched.
                var repaired = new AttributeInstance(Attributes.MAX_HEALTH, ignored -> { });
                repaired.replaceFrom(attribute);
                if (!Double.isFinite(repaired.getBaseValue())) repaired.setBaseValue(state.lastMax);
                for (var modifier : repaired.getModifiers()) if (!Double.isFinite(modifier.amount())) repaired.removeModifier(modifier);
                if (!valid((float)repaired.getValue())) { repaired.removeModifiers(); repaired.setBaseValue(state.lastMax); }
                attribute.replaceFrom(repaired);
            }
        }
        float cap = attribute == null ? state.lastMax : (float)attribute.getValue();
        if (!valid(cap)) cap = state.lastMax;
        // Write the real synchronized value instead of relying on another mod's setHealth override.
        player.getEntityData().set(WardLivingAccess.overload$healthId(), Math.min(1F, cap), true);
        if (destructive(player.getRemovalReason())) ((WardEntityAccess)player).overload$removalReason(null);
        if (!Float.isFinite(player.getAbsorptionAmount()) || player.getAbsorptionAmount() < 0) player.setAbsorptionAmount(0);
        access.overload$dead(false);
        player.deathTime = 0;
        if (player.getPose() == Pose.DYING) player.setPose(Pose.STANDING);
        } finally { state.restoring = false; }
    }
    public static boolean removal(ServerPlayer player, Entity.RemovalReason reason) {
        return destructive(reason) && rescue(player);
    }
    /** For world-manager calls without a RemovalReason argument. Normal lifecycle contexts are whitelisted. */
    public static boolean untracking(ServerPlayer player) {
        var reason = player.getRemovalReason();
        return (reason == null || destructive(reason)) && rescue(player);
    }
    private static boolean observedFailure(ServerPlayer player) {
        return observedFailure(player, false);
    }
    private static boolean observedFailure(ServerPlayer player, boolean inconsistentRead) {
        var state = state(player);
        if ((state.damageDepth > 0 && !inconsistentRead) || !eligible(player, state)) return false;
        if (protectedNow(player)) { restore(player); return true; }
        if (state.failedProbeTick == player.level().getGameTime()) return false;
        boolean rescued = rescue(player);
        if (!rescued) state.failedProbeTick = player.level().getGameTime();
        return rescued;
    }
    public static float visibleHealth(ServerPlayer player, float original) {
        if (!(original > 0) || Float.isNaN(original)) {
            if (observedFailure(player, valid(rawHealth(player))) || protectedNow(player)) {
                if (!state(player).restoring && !valid(rawHealth(player))) restore(player);
                float stored = rawHealth(player);
                return valid(stored) ? stored : 1F;
            }
            if (Float.isNaN(original) && tracked(player) && equipped(player)) return 0;
        }
        return original;
    }
    public static float maximumHealth(ServerPlayer player, float original) {
        if (!valid(original) && (observedFailure(player, true) || protectedNow(player))) {
            var attribute = player.getAttribute(Attributes.MAX_HEALTH);
            float value = attribute == null ? Float.NaN : (float)attribute.getValue();
            return valid(value) ? value : state(player).lastMax;
        }
        return original;
    }
    public static boolean aliveView(ServerPlayer player, boolean original) {
        return original || observedFailure(player, valid(rawHealth(player))) || protectedNow(player);
    }
    public static boolean dyingView(ServerPlayer player, boolean original) {
        return original && !(observedFailure(player, valid(rawHealth(player))) || protectedNow(player));
    }
    public static boolean removedView(ServerPlayer player, boolean original) {
        return original && !(destructive(player.getRemovalReason()) && (observedFailure(player, true) || protectedNow(player)));
    }
    public static void inspect(ServerPlayer player) {
        var state = state(player);
        if (state.damageDepth > 0 || !eligible(player, state) || !equipped(player)) return;
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        float maximum = attribute == null ? Float.NaN : (float)attribute.getValue();
        if (!valid(rawHealth(player)) || !valid(maximum) || ((WardLivingAccess)player).overload$dead()
              || player.deathTime > 0 || destructive(player.getRemovalReason())) {
            observedFailure(player);
        } else {
            state.lastMax = maximum;
            if (state.healthyMax == null) state.healthyMax = new AttributeInstance(Attributes.MAX_HEALTH, ignored -> { });
            state.healthyMax.replaceFrom(attribute);
        }
    }
    public static boolean deathTick(ServerPlayer player) { return rescue(player); }
    public static void beforeTick(ServerPlayer player) {
        var state = STATES.get(player);
        if (state != null && state.cleanup) {
            state.cleanup = false;
            if (!state.finalized && !player.isRemoved() && state.protectedTick != Long.MIN_VALUE
                  && player.level().getGameTime() - state.protectedTick <= 1) {
                // Some forced-kill chains assign deathTime after our canceled die call.
                player.deathTime = 0;
                ((WardLivingAccess)player).overload$dead(false);
            }
        }
        inspect(player);
    }
    public static void finalized(ServerPlayer player) {
        state(player).finalized = true;
        player.getPersistentData().putBoolean(FINALIZED_KEY, true);
    }
    public static void newLife(ServerPlayer player) {
        player.getPersistentData().remove(FINALIZED_KEY);
        STATES.remove(player);
    }
    public static void forget(ServerPlayer player) {
        var state = STATES.get(player);
        if (state != null && state.lifecycleDepth > 0) state.discardAfterLifecycle = true;
        else STATES.remove(player);
    }
    private ThunderWard() { }
}
