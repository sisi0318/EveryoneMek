package dev.everyonemek.overloadcore;

import java.util.Map;
import com.google.common.collect.MapMaker;
import dev.everyonemek.overloadcore.mixin.WardLivingAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import top.theillusivec4.curios.api.CuriosApi;

/** One debit per fatal call chain; a fresh hurt call always opens a fresh incident, even in the same tick. */
public final class ThunderWard {
    private static final class State {
        long protectedTick = Long.MIN_VALUE;
        int damageDepth;
        boolean paying, finalized, cleanup;
    }
    // Entity.equals uses the entity ID, which vanilla reuses for a replacement player on respawn.
    // Weak identity keys keep the old life's finalized flag away from the new player object.
    private static final Map<ServerPlayer, State> STATES = new MapMaker().weakKeys().makeMap();
    private static State state(ServerPlayer player) { return STATES.computeIfAbsent(player, p -> new State()); }

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
        }
    }
    public static void endDamage(ServerPlayer player) { state(player).damageDepth--; }
    public static boolean protectedNow(ServerPlayer player) {
        var state = STATES.get(player);
        return state != null && !state.finalized && !player.isRemoved() && state.protectedTick == player.level().getGameTime();
    }
    public static boolean setter(ServerPlayer player, float health) {
        if (health > 0) return false;
        var state = STATES.get(player);
        // Ordinary damage keeps vanilla totem priority. Direct clear-health calls need their own interception.
        return (state == null || state.damageDepth == 0 || Float.isNaN(health)) && rescue(player);
    }
    public static boolean rescue(ServerPlayer player) {
        var state = state(player);
        if (state.finalized || state.paying || player.isRemoved() || player.isSpectator()) return false;
        if (protectedNow(player)) { restore(player); return true; }
        if (!equipped(player) || !Float.isFinite(player.getMaxHealth()) || player.getMaxHealth() <= 0) return false;
        state.paying = true;
        try {
            if (!WardPower.pay(player)) return false;
            state.protectedTick = player.level().getGameTime();
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
        var access = (WardLivingAccess) player;
        player.setHealth(Math.min(1F, player.getMaxHealth()));
        if (!Float.isFinite(player.getAbsorptionAmount()) || player.getAbsorptionAmount() < 0) player.setAbsorptionAmount(0);
        access.overload$dead(false);
        player.deathTime = 0;
        if (player.getPose() == Pose.DYING) player.setPose(Pose.STANDING);
    }
    public static boolean removal(ServerPlayer player, Entity.RemovalReason reason) {
        return (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) && rescue(player);
    }
    public static float visibleHealth(ServerPlayer player, float original) {
        if (protectedNow(player) && (!(original > 0) || !Float.isFinite(original))) {
            float stored = player.getEntityData().get(WardLivingAccess.overload$healthId());
            return Float.isFinite(stored) && stored > 0 ? stored : 1F;
        }
        return original;
    }
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
    }
    public static void finalized(ServerPlayer player) { state(player).finalized = true; }
    public static void forget(ServerPlayer player) { STATES.remove(player); }
    private ThunderWard() { }
}
