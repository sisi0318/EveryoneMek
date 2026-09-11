package dev.everyonemek.forbidden;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MachineConfig {
    public static final ModConfigSpec.IntValue OPERATION_FE;
    public static final ModConfigSpec.IntValue FORGE_FE;
    public static final ModConfigSpec.IntValue CLIBANO_HEAT_FE;
    public static final ModConfigSpec SPEC;
    static {
        var builder = new ModConfigSpec.Builder();
        OPERATION_FE = builder.comment("FE per controller logistics operation, separate from Clibano electric heating.")
              .defineInRange("operationFE", 200, 1, 1_000_000);
        CLIBANO_HEAT_FE = builder.comment("FE per productive Clibano heating tick. Energy upgrades improve efficiency; native recipe durations are retained.")
              .defineInRange("clibanoHeatFE", 50, 1, 1_000_000);
        FORGE_FE = builder.comment("Base FE per forge processing tick, or per resource point produced by an installed module.")
              .defineInRange("forgeFE", 100, 1, 1_000_000);
        SPEC = builder.build();
    }
    private MachineConfig() { }
}
