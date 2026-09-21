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
              .defineInRange("processingCyclesPerTick", 4, 1, 8);
        PERFORMANCE_REVISION = builder.comment("One-time migration marker for pre-alpha.10 default throughput settings.").defineInRange("performanceRevision", 0, 0, 1);
        SPEC = builder.build();
    }
    public static void loaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC || PERFORMANCE_REVISION.get() >= 1) return;
        for (int i = 0; i < 4; i++) if (PARALLEL[i].get() == OLD_PARALLEL[i]) PARALLEL[i].set(DEFAULT_PARALLEL[i]);
        if (PROCESSING_CYCLES.get() == 2) PROCESSING_CYCLES.set(4);
        PERFORMANCE_REVISION.set(1);
        event.getConfig().getLoadedConfig().save();
    }
    private FactoryConfig() { }
}
