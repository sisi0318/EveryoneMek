package dev.everyonemek.ars;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.EnumMap;
import java.util.Map;

public final class MachineConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue GENERATOR_FE, GENERATOR_RATE, CONVERTER_FE, CONVERTER_RATE;
    public static final ModConfigSpec.IntValue IMBUEMENT_FE, IMBUEMENT_TICKS, APPARATUS_FE, APPARATUS_TICKS;
    public static final ModConfigSpec.IntValue DRYGMY_HARVEST_TICKS;
    public static final Map<MachineKind, ModConfigSpec.IntValue> PROCESS_FE = new EnumMap<>(MachineKind.class);
    public static final Map<MachineKind, ModConfigSpec.IntValue> PROCESS_TICKS = new EnumMap<>(MachineKind.class);

    static {
        var builder = new ModConfigSpec.Builder();
        GENERATOR_FE = builder.comment("Base FE per tick at the full Source generation rate.")
              .defineInRange("generatorFE", 2_000, 1, 1_000_000);
        GENERATOR_RATE = builder.comment("Base Source generated per tick; 1 Source equals 1 Chemical unit.")
              .defineInRange("generatorSourcePerTick", 20, 1, 10_000);
        CONVERTER_FE = builder.comment("Base FE per tick at the full Source transfer rate.")
              .defineInRange("converterFE", 100, 1, 1_000_000);
        CONVERTER_RATE = builder.comment("Base Source transferred per tick to or from the selected adjacent Ars container.")
              .defineInRange("converterSourcePerTick", 200, 1, 100_000);
        IMBUEMENT_FE = builder.defineInRange("imbuementFE", 100, 1, 1_000_000);
        IMBUEMENT_TICKS = builder.defineInRange("imbuementTicks", 100, 1, 100_000);
        APPARATUS_FE = builder.defineInRange("apparatusFE", 200, 1, 1_000_000);
        APPARATUS_TICKS = builder.defineInRange("apparatusTicks", 100, 1, 100_000);
        for (MachineKind kind : MachineKind.values()) {
            if (!kind.advanced()) continue;
            builder.push(kind.id);
            PROCESS_FE.put(kind, builder.defineInRange("energyPerTick", kind.worldController() ? 20 : 100, 1, 1_000_000));
            PROCESS_TICKS.put(kind, builder.defineInRange("operationTicks", kind.worldController() ? 5
                  : kind == MachineKind.POTION_BOTTLER ? 20 : kind == MachineKind.POTION_MIXER ? 160 : 100, 1, 100_000));
            builder.pop();
        }
        builder.push("drygmy_station");
        DRYGMY_HARVEST_TICKS = builder.comment("Base ticks per harvest from the station's captive jars; affected by speed upgrades.")
              .defineInRange("harvestTicks", 200, 1, 100_000);
        builder.pop();
        SPEC = builder.build();
    }

    public static long baseFE(MachineKind kind) {
        return switch (kind) {
            case SOURCE_GENERATOR -> GENERATOR_FE.get();
            case SOURCE_CONVERTER -> CONVERTER_FE.get();
            case IMBUEMENT_CHAMBER -> IMBUEMENT_FE.get();
            case ENCHANTING_APPARATUS -> APPARATUS_FE.get();
            default -> PROCESS_FE.get(kind).get();
        };
    }

    private MachineConfig() { }
}
