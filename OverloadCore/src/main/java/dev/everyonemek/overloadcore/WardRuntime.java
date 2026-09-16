package dev.everyonemek.overloadcore;

import com.google.common.collect.MapMaker;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Transient afterguard and feedback. Extraction mode is durable in WardLedger, never client-authoritative. */
public final class WardRuntime {
    private static final class State {
        int hits, sources;
        long shieldUntil, lastFx = Long.MIN_VALUE / 2, lastRequest = Long.MIN_VALUE / 2, lastNotice = Long.MIN_VALUE / 2;
        long lastCost;
        String result = "idle";
        boolean pulse;
        CompoundTag lastSent;
    }
    private static final Map<ServerPlayer, State> STATES = new MapMaker().weakKeys().makeMap();
    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p, unused -> new State()); }
    public static int hits(ServerPlayer p) {
        var s = state(p);
        if (p.level().getGameTime() >= s.shieldUntil) s.hits = 0;
        return s.hits;
    }
    public static void clearShield(ServerPlayer p) { var s = state(p); s.hits = 0; s.shieldUntil = 0; }
    public static void afterRescue(ServerPlayer p) {
        var s = state(p);
        s.hits = CoreConfig.WARD_SHIELD_HITS.get();
        s.shieldUntil = p.level().getGameTime() + CoreConfig.WARD_SHIELD_TICKS.get();
        sync(p);
    }
    public static void block(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getNewDamage() > 0) || hits(p) == 0) return;
        if (!ThunderWard.equipped(p)) { clearShield(p); return; }
        state(p).hits--;
        event.setNewDamage(0);
        sync(p);
    }
    public static boolean setExtreme(ServerPlayer p, boolean value) {
        if (!ThunderWard.tracked(p) || p.isSpectator() || !ThunderWard.equipped(p)) return false;
        var s = state(p); long now = p.level().getGameTime();
        if (now - s.lastRequest < 5) { sync(p); return false; }
        s.lastRequest = now;
        WardLedger.get(p).extreme(p, value);
        p.displayClientMessage(CoreContent.text(value ? "ward.extreme_on" : "ward.extreme_off"), true);
        sync(p);
        return true;
    }
    public static void paid(ServerPlayer p, long cost, int sources) {
        var s = state(p); s.lastCost = cost; s.sources = sources; s.result = "paid";
        s.pulse = p.level().getGameTime() - s.lastFx >= 10;
        if (s.pulse) s.lastFx = p.level().getGameTime();
    }
    public static boolean pulse(ServerPlayer p) { return state(p).pulse; }
    public static void failed(ServerPlayer p, String reason) {
        var s = state(p); s.result = reason;
        long now = p.level().getGameTime();
        if (now - s.lastNotice >= 20) {
            s.lastNotice = now;
            p.displayClientMessage(CoreContent.text("ward.failure." + reason), true);
        }
    }
    public static void quarantined(ServerPlayer p) { failed(p, "pending"); }
    public static CompoundTag status(ServerPlayer p) {
        boolean equipped = ThunderWard.equipped(p);
        var s = state(p); if (!equipped) clearShield(p);
        var tag = new CompoundTag();
        tag.putBoolean("equipped", equipped); tag.putBoolean("pending", WardCustody.pending(p));
        tag.putBoolean("extreme", WardLedger.get(p).extreme(p));
        tag.putInt("hits", hits(p)); tag.putInt("ticks", s.hits == 0 ? 0 : (int)Math.clamp(s.shieldUntil - p.level().getGameTime(), 0, 1200));
        tag.putLong("cost", s.lastCost); tag.putInt("sources", s.sources); tag.putString("result", s.result);
        var entry = WardLedger.get(p).worn.get(p.getUUID()); if (entry != null) tag.putUUID("seal", entry.token);
        return tag;
    }
    public static void sync(ServerPlayer p) {
        var tag = status(p); var s = state(p);
        if (!tag.equals(s.lastSent)) { s.lastSent = tag.copy(); CorePackets.sendWardStatus(p, tag); }
    }
    public static void report(ServerPlayer p, net.minecraft.commands.CommandSourceStack to) {
        var tag = status(p);
        to.sendSuccess(() -> CoreContent.text("ward.report", p.getDisplayName(), CoreContent.text("ward.state." +
              (tag.getBoolean("pending") ? "pending" : tag.getBoolean("equipped") ? "equipped" : "absent")),
              CoreContent.text(tag.getBoolean("extreme") ? "ward.mode.extreme" : "ward.mode.normal"),
              tag.getInt("hits"), CoreContent.text("ward.failure." + tag.getString("result")), tag.getLong("cost"), tag.getInt("sources")), false);
    }
    public static void forget(ServerPlayer p) { STATES.remove(p); WardPower.forget(p); }
    private WardRuntime() { }
}
