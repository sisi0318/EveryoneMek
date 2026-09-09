package dev.everyonemek.natures;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MachineConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue GENERATOR_FE, AURA_PER_CYCLE, FOREST_FE, ALTAR_FE, OFFERING_FE;
    public static final ModConfigSpec.IntValue OFFERING_TICKS, FOREST_GOLD, EMIT_RATE, EMIT_LIMIT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        GENERATOR_FE = b.comment("Base FE per tick; a generation cycle takes 20 ticks before speed upgrades.")
              .defineInRange("generatorFEPerTick", 400, 1, 1_000_000);
        AURA_PER_CYCLE = b.defineInRange("auraPerCycle", 2000, 1, 100_000);
        FOREST_FE = b.defineInRange("forestFEPerTick", 100, 1, 1_000_000);
        ALTAR_FE = b.defineInRange("altarFEPerTick", 100, 1, 1_000_000);
        OFFERING_FE = b.defineInRange("offeringFEPerTick", 200, 1, 1_000_000);
        OFFERING_TICKS = b.comment("OfferingRecipe has no time field; this is the machine's base batch duration.")
              .defineInRange("offeringTicks", 100, 1, 12000);
        FOREST_GOLD = b.comment("Gold powder consumed per forest ritual, replacing the original 16 powder positions.")
              .defineInRange("forestGoldPowder", 16, 0, 64);
        EMIT_RATE = b.comment("Aura per tick released when the generator's environment output is enabled.")
              .defineInRange("environmentAuraPerTick", 100, 1, 100_000);
        EMIT_LIMIT = b.comment("Stop releasing when the 16-block area's aura reaches this amount. Baseline is 1000000.")
              .defineInRange("environmentAuraLimit", 2_000_000, 1_000_000, 100_000_000);
        SPEC = b.build();
    }

    public static int baseFE(MachineKind kind) {
        return switch (kind) {
            case AURA_GENERATOR -> GENERATOR_FE.get();
            case FOREST_RITUAL -> FOREST_FE.get();
            case NATURAL_ALTAR -> ALTAR_FE.get();
            case OFFERING -> OFFERING_FE.get();
        };
    }

    private MachineConfig() { }
}
