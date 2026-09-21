package dev.everyonemek.factory;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class FactoryConfig {
    public static final int MAX_PARALLEL = 2048;
    private static final int[] OLD_PARALLEL = {8, 32, 128, 512}, DEFAULT_PARALLEL = {32, 128, 512, 2048};
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue[] PARALLEL = new ModConfigSpec.IntValue[4], SIZE = new ModConfigSpec.IntValue[4];
    public static final ModConfigSpec.IntValue PROCESSING_CYCLES, PERFORMANCE_REVISION;
    static {
        var builder = new ModConfigSpec.Builder();
        for (int i = 0; i < 4; i++) {
            PARALLEL[i] = builder.defineInRange("parallelTier" + i, DEFAULT_PARALLEL[i], 1, MAX_PARALLEL);
            SIZE[i] = builder.defineInRange("sizeTier" + i, 5 + 2 * i, 4, 11);
        }
        PROCESSING_CYCLES = builder.comment("Native work steps per server tick. Normal energy cost per step; shared provider throughput.")
              .defineInRange("processingCyclesPerTick", 8, 1, 8);
        PERFORMANCE_REVISION = builder.comment("One-time migration marker for factory throughput defaults.").defineInRange("performanceRevision", 0, 0, 2);
        SPEC = builder.build();
    }
    public static void loaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC || PERFORMANCE_REVISION.get() >= 2) return;
        int revision = PERFORMANCE_REVISION.get();
        if (revision < 1) for (int i = 0; i < 4; i++) if (PARALLEL[i].get() == OLD_PARALLEL[i]) PARALLEL[i].set(DEFAULT_PARALLEL[i]);
        PROCESSING_CYCLES.set(migratedCycles(revision, PROCESSING_CYCLES.get()));
        PERFORMANCE_REVISION.set(2);
        event.getConfig().getLoadedConfig().save();
    }
    static int migratedCycles(int revision, int cycles) {
        if (revision < 1 && cycles == 2) cycles = 4;
        return revision < 2 && cycles == 4 ? 8 : cycles;
    }
    private FactoryConfig() { }
}
