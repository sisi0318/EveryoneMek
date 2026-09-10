package dev.everyonemek.forbidden;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MachineConfig {
    public static final ModConfigSpec.IntValue OPERATION_FE;
    public static final ModConfigSpec SPEC;
    static {
        var builder = new ModConfigSpec.Builder();
        OPERATION_FE = builder.comment("FE per controller logistics operation. Native processing costs and duration are unchanged.")
              .defineInRange("operationFE", 200, 1, 1_000_000);
        SPEC = builder.build();
    }
    private MachineConfig() { }
}
