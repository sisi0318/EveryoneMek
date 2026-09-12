package dev.everyonemek.botania;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Balance {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue FE_PER_MANA = BUILDER.comment("FE per mana. Must remain above the native 10 FE/mana return rate.")
          .defineInRange("fePerMana", 50, 11, 100_000);
    public static final ModConfigSpec.IntValue LOTUS_RATE = BUILDER.comment("Mana produced by one lotus per world tick.")
          .defineInRange("lotusManaPerTick", 4, 1, 128);
    public static final ModConfigSpec SPEC = BUILDER.build();
    public static final int ENERGY_CAPACITY = 20_000, LOTUS_CAPACITY = 800;
    public static final int RANGE = 32, NODE_LIMIT = 16, BATCH_TICKS = 5;
    public static final int NETWORK_BUDGET = 640, ENDPOINT_BUDGET = 320, RELAY_BUDGET = 640;
    private Balance() { }
}
