package dev.everyonemek.overloadcore;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CoreConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RANGE, METAL_LIMIT, BUFFER_FE, CHARGE_FE;
    public static final ModConfigSpec.DoubleValue SOUND_GAIN;
    public static final ModConfigSpec.BooleanValue WORK, GENERATION, TRANSPORT, HAZARDS;
    static {
        var b = new ModConfigSpec.Builder();
        RANGE = b.comment("Radius in blocks; only owned or explicitly shared devices.").defineInRange("range", 32, 1, 64);
        METAL_LIMIT = b.comment("Metal ingot equivalents that prevent sprinting.").defineInRange("metalLimit", 384, 32, 4096);
        BUFFER_FE = b.defineInRange("bufferFE", 100000, 0, 10000000);
        CHARGE_FE = b.defineInRange("chargeFEPerTick", 1000, 0, 100000);
        SOUND_GAIN = b.defineInRange("soundGain", 1.8, 1, 3);
        WORK = b.define("workCurse", true); GENERATION = b.define("generationCurse", true);
        TRANSPORT = b.define("transportCurse", true); HAZARDS = b.define("workplaceHazards", true);
        SPEC = b.build();
    }
    private CoreConfig() { }
}
