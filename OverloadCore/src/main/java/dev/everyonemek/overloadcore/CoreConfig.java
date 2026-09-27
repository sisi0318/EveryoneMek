package dev.everyonemek.overloadcore;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CoreConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RANGE, METAL_LIMIT, BUFFER_FE, CHARGE_FE;
    public static final ModConfigSpec.DoubleValue SOUND_GAIN;
    public static final ModConfigSpec.BooleanValue WORK, GENERATION, TRANSPORT, HAZARDS;
    public static final ModConfigSpec.IntValue WARD_COST_FE;
    public static final ModConfigSpec.IntValue WARD_RESERVE_PERCENT, WARD_SHIELD_HITS, WARD_SHIELD_TICKS;
    public static final java.util.List<ModConfigSpec.IntValue> COUPLING_RATES;
    public static final ModConfigSpec.IntValue HEAT_SINK_COST_FE, HEAT_SINK_PER_LEVEL, MAGNETIC_COST_FE, MAGNETIC_PER_LEVEL;
    public static final ModConfigSpec.IntValue CAPACITOR_CHARGES, GEAR_CHARGE_RATE, AFTERGUARD_HITS, AFTERGUARD_TICKS;
    public static final ModConfigSpec.IntValue RAIL_COST, RAIL_CHARGE, RAIL_RANGE, BLADE_COST, BLADE_BURST_COST, BLADE_CHARGE, ARC_COST;
    public static final ModConfigSpec.DoubleValue RAIL_DAMAGE, BLADE_DAMAGE, ARC_DAMAGE;
    static {
        var b = new ModConfigSpec.Builder();
        RANGE = b.comment("Radius in blocks; only owned or explicitly shared devices.").defineInRange("range", 32, 1, 64);
        METAL_LIMIT = b.comment("Metal ingot equivalents that prevent sprinting.").defineInRange("metalLimit", 384, 32, 4096);
        BUFFER_FE = b.defineInRange("bufferFE", 100000, 0, 10000000);
        CHARGE_FE = b.defineInRange("chargeFEPerTick", 1000, 0, 100000);
        var rates = new java.util.ArrayList<ModConfigSpec.IntValue>();
        b.push("equipment");
        for (int level = 1; level <= 4; level++) rates.add(b.comment("Coupling transfer cap in FE/t. Limited by recovered energy and chest acceptance; not an energy source.")
              .defineInRange("couplingFEPerTickLevel" + level, 4000 << (2 * (level - 1)), 0, 10000000));
        HEAT_SINK_COST_FE = b.defineInRange("heatSinkFEPerHalfSecondPerLevel", 500, 1, 1000000);
        HEAT_SINK_PER_LEVEL = b.defineInRange("heatSinkCoolingPerLevel", 2, 1, 20);
        MAGNETIC_COST_FE = b.defineInRange("magneticFEPerHalfSecondPerLevel", 250, 1, 1000000);
        MAGNETIC_PER_LEVEL = b.defineInRange("magneticIngotCompensationPerLevel", 128, 1, 4096);
        CAPACITOR_CHARGES = b.defineInRange("wardCapacitorChargesPerLevel", 3, 1, 100);
        GEAR_CHARGE_RATE = b.defineInRange("equipmentChargeFEPerTick", 100000, 1, 10000000);
        AFTERGUARD_HITS = b.defineInRange("afterguardHitsPerLevel", 1, 1, 5);
        AFTERGUARD_TICKS = b.defineInRange("afterguardTicksPerLevel", 20, 1, 200);
        b.pop();
        COUPLING_RATES = java.util.List.copyOf(rates);
        b.push("combat");
        RAIL_COST = b.defineInRange("railShotFE", 50000, 1, 100000000);
        RAIL_CHARGE = b.defineInRange("railChargeTicks", 32, 8, 200);
        RAIL_RANGE = b.defineInRange("railRange", 48, 8, 128);
        RAIL_DAMAGE = b.defineInRange("railDamage", 32D, 1D, 10000D);
        BLADE_COST = b.defineInRange("bladeHitFE", 4000, 1, 100000000);
        BLADE_BURST_COST = b.defineInRange("bladeBurstFE", 25000, 1, 100000000);
        BLADE_CHARGE = b.defineInRange("bladeChargeTicks", 24, 8, 200);
        BLADE_DAMAGE = b.defineInRange("bladeDamage", 18D, 4D, 10000D);
        ARC_COST = b.defineInRange("resonanceTargetFE", 5000, 1, 100000000);
        ARC_DAMAGE = b.defineInRange("resonanceDamage", 6D, 1D, 10000D);
        b.pop();
        SOUND_GAIN = b.defineInRange("soundGain", 1.8, 1, 3);
        WARD_COST_FE = b.comment("Thunder Ward: total FE cost per averted fatal incident; no cooldown.")
              .defineInRange("wardCostFE", 100000, 1, 1000000000);
        WARD_RESERVE_PERCENT = b.comment("Stored capacity reserved by normal ward extraction; extreme mode bypasses this reserve.")
              .defineInRange("wardReservePercent", 10, 0, 100);
        WARD_SHIELD_HITS = b.comment("Damage hits absorbed after a paid rescue. Does not stack across rescues.")
              .defineInRange("wardShieldHits", 3, 0, 20);
        WARD_SHIELD_TICKS = b.comment("Lifetime of the post-rescue hit shield, in server ticks.")
              .defineInRange("wardShieldTicks", 60, 1, 1200);
        WORK = b.define("workCurse", true); GENERATION = b.define("generationCurse", true);
        TRANSPORT = b.define("transportCurse", true); HAZARDS = b.define("workplaceHazards", true);
        SPEC = b.build();
    }
    public static int couplingRateFE(int installed) {
        int rate = CHARGE_FE.get();
        // More installed modules cannot lower throughput, even with non-monotonic pack settings.
        for (int i = 0; i < Math.clamp(installed, 0, COUPLING_RATES.size()); i++) rate = Math.max(rate, COUPLING_RATES.get(i).get());
        return rate;
    }
    private CoreConfig() { }
}
