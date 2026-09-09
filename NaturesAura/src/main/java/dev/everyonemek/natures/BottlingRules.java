package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.items.ItemAuraBottle;
import de.ellpeck.naturesaura.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Matches Nature's Aura 41.10 ItemAuraBottle.create, with an explicit optional simulation upgrade. */
public final class BottlingRules {
    public static final int RANGE = 30;
    public static final int MIN_AURA = 100_000;
    public static final int MAX_VACUUM_AURA = -100_000;
    public static final int AURA_PER_BOTTLE = 20_000;

    public static ItemStack output(Level level, int environment, BottlingMode mode, boolean simulated) {
        if (mode.requiresSimulation() && !simulated) return ItemStack.EMPTY;
        if (mode == BottlingMode.VACUUM || mode == BottlingMode.AUTO && !simulated && environment <= MAX_VACUUM_AURA) {
            return simulated || environment <= MAX_VACUUM_AURA ? new ItemStack(ModItems.VACUUM_BOTTLE) : ItemStack.EMPTY;
        }
        if (!simulated && environment < MIN_AURA) return ItemStack.EMPTY;
        IAuraType type = switch (mode) {
            case SUNLIGHT -> NaturesAuraAPI.TYPE_OVERWORLD;
            case GHOST -> NaturesAuraAPI.TYPE_NETHER;
            case DARKNESS -> NaturesAuraAPI.TYPE_END;
            default -> IAuraType.forLevel(level);
        };
        return ItemAuraBottle.setType(new ItemStack(ModItems.AURA_BOTTLE), type);
    }

    private BottlingRules() { }
}
