package dev.everyonemek.factory.compat;

import dev.everyonemek.factory.Controller;
import java.util.List;
import mekanism.api.Upgrade;
import mekanism.api.energy.IEnergyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** Keeps Extras classes behind the optional dependency boundary. */
public final class Compat {
    public static final boolean EXTRAS = ModList.get().isLoaded("mekanism_extras");
    public static void registerFactories() { if (EXTRAS) MekanismExtrasCompat.registerFactories(); }
    public static int factoryLines(ItemStack stack) { return EXTRAS ? MekanismExtrasCompat.factoryLines(stack) : 0; }
    public static int stackOperations(Controller c) { return EXTRAS ? MekanismExtrasCompat.stackOperations(c) : 1; }
    public static boolean creative(Controller c) { return EXTRAS && MekanismExtrasCompat.creative(c); }
    public static Upgrade[] upgrades() { return EXTRAS ? MekanismExtrasCompat.upgrades() : new Upgrade[]{Upgrade.SPEED, Upgrade.ENERGY, Upgrade.CHEMICAL}; }
    public static IEnergyContainer cell(BlockEntity tile) { return EXTRAS ? MekanismExtrasCompat.cell(tile) : null; }
    public static long providerOutput(BlockEntity tile) { return EXTRAS ? MekanismExtrasCompat.providerOutput(tile) : -1; }
    public static List<Block> inductionBlocks(boolean cell) { return EXTRAS ? MekanismExtrasCompat.inductionBlocks(cell) : List.of(); }
    private Compat() { }
}
