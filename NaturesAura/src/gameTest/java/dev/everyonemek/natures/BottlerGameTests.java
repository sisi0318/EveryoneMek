package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.items.ItemAuraBottle;
import de.ellpeck.naturesaura.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.registries.MekanismItems;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class BottlerGameTests {
    private static void check(boolean value, String message) { if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(message); }

    private static AuraMachine machine(GameTestHelper h, int x) {
        BlockPos pos = new BlockPos(x, 1, 5);
        h.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(pos, Content.MACHINES.get(MachineKind.AURA_BOTTLER).get());
        AuraMachine m = (AuraMachine) h.getBlockEntity(pos);
        m.energy().setEnergy(m.energy().getMaxEnergy());
        return m;
    }

    private static void simulate(AuraMachine m, BottlingMode mode) {
        m.simulationModuleSlot().insertItem(new ItemStack(Content.SIMULATION_MODULE.get()), Action.EXECUTE, AutomationType.MANUAL);
        m.setBottlingMode(mode);
    }

    private static int environment(AuraMachine m) {
        return IAuraChunk.getAuraInArea(m.getLevel(), m.getBlockPos(), BottlingRules.RANGE);
    }

    private static void environment(AuraMachine m, int target) {
        check(AuraTestEnvironment.set(m, BottlingRules.RANGE, target) == target, "Environment fixture did not reach " + target);
    }

    private static ItemStack output(AuraMachine m) { return m.getInventorySlots(null).get(1).getStack(); }
    private static void bottles(AuraMachine m, int count) { m.inputs.getFirst().setStack(new ItemStack(ModItems.BOTTLE_TWO_THE_REBOTTLING, count)); }
    private static void aura(AuraMachine m, long amount) { m.auraTank().setStack(new ChemicalStack(Content.AURA, amount)); }
    private static boolean typed(ItemStack stack, IAuraType type) { return stack.is(ModItems.AURA_BOTTLE) && ItemAuraBottle.getType(stack) == type; }

    @GameTest(template = "empty", batch = "bottler_boundary", timeoutTicks = 80)
    public static void nativeBoundaryCompletesOneBottleBeforeFallingBelowThreshold(GameTestHelper h) {
        AuraMachine m = machine(h, 5);
        environment(m, 100_000);
        bottles(m, 2);
        h.runAfterDelay(20, () -> {
            check(m.progress() > 0 && m.inputs.getFirst().getCount() == 2, "Bottle input consumed before completion");
            check(environment(m) == 100_000, "Aura paid before completion invalidated the native threshold");
        });
        h.runAfterDelay(60, () -> {
            check(m.inputs.getFirst().getCount() == 1 && output(m).getCount() == 1, "Boundary should complete exactly one bottle");
            check(typed(output(m), NaturesAuraAPI.TYPE_OVERWORLD), "Native Overworld bottle has the wrong Aura type");
            check(environment(m) == 80_000 && m.auraTank().isEmpty(), "Boundary bottle did not charge exactly 20000 world Aura");
            check(m.status() == 7, "Next bottle should wait for the environmental threshold");
            m.energy().setEnergy(0);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "bottler_dimensions", timeoutTicks = 20)
    public static void nativeNetherAndEndBottlesUseTheirRealDimension(GameTestHelper h) {
        var dimensions = List.of(Level.NETHER, Level.END);
        var types = List.of(NaturesAuraAPI.TYPE_NETHER, NaturesAuraAPI.TYPE_END);
        for (int i = 0; i < dimensions.size(); i++) {
            var level = h.getLevel().getServer().getLevel(dimensions.get(i));
            check(level != null, "Test dimension missing");
            BlockPos pos = new BlockPos(0, 90, 0);
            level.getChunkAt(pos);
            level.setBlockAndUpdate(pos, Content.MACHINES.get(MachineKind.AURA_BOTTLER).defaultState());
            AuraMachine m = (AuraMachine) level.getBlockEntity(pos);
            m.energy().setEnergy(m.energy().getMaxEnergy());
            environment(m, 100_000);
            bottles(m, 1);
            for (int tick = 0; tick < MachineConfig.BOTTLER_TICKS.get(); tick++) m.onUpdateServer();
            check(typed(output(m), types.get(i)), "Bottle ignores actual dimension " + dimensions.get(i));
            check(environment(m) == 80_000, "Dimension bottle charged the wrong Aura amount");
            level.removeBlock(pos, false);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "bottler_vacuum", timeoutTicks = 110)
    public static void nativeVacuumKeepsNegativeEnvironmentAndStoredAura(GameTestHelper h) {
        AuraMachine m = machine(h, 5);
        environment(m, -100_000);
        aura(m, 20_000);
        bottles(m, 2);
        h.runAfterDelay(95, () -> {
            check(output(m).is(ModItems.VACUUM_BOTTLE) && output(m).getCount() == 2, "Native negative environment did not bottle vacuum");
            check(environment(m) == -100_000 && m.auraTank().getStored() == 20_000, "Vacuum bottling consumed or produced Aura");
            check(m.environmentAura() == -100_000, "Menu hid the negative environmental value");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "bottler_simulated", timeoutTicks = 80)
    public static void simulationSelectsFourProductsAndChargesAuraAndPowerExactly(GameTestHelper h) {
        var machines = new ArrayList<AuraMachine>();
        var modes = List.of(BottlingMode.SUNLIGHT, BottlingMode.GHOST, BottlingMode.DARKNESS, BottlingMode.VACUUM);
        var types = List.of(NaturesAuraAPI.TYPE_OVERWORLD, NaturesAuraAPI.TYPE_NETHER, NaturesAuraAPI.TYPE_END);
        for (int i = 0; i < modes.size(); i++) {
            AuraMachine m = machine(h, 2 + i * 2);
            simulate(m, modes.get(i));
            aura(m, 40_000);
            bottles(m, 1);
            machines.add(m);
        }
        environment(machines.getFirst(), 0);
        h.runAfterDelay(60, () -> {
            for (int i = 0; i < machines.size(); i++) {
                AuraMachine m = machines.get(i);
                check(m.inputs.getFirst().isEmpty() && output(m).getCount() == 1, "Simulated bottle did not finish: " + modes.get(i));
                check(i == 3 ? output(m).is(ModItems.VACUUM_BOTTLE) : typed(output(m), types.get(i)), "Wrong selected output");
                check(m.auraTank().getStored() == (i == 3 ? 40_000 : 20_000), "Wrong simulated Aura charge");
                check(m.energy().getMaxEnergy() - m.energy().getEnergy() == m.energy().getEnergyPerTick() * MachineConfig.BOTTLER_TICKS.get(),
                      "Simulation did not pay exactly one full processing cycle");
                check(m.hasSimulationModule() && m.simulationModuleSlot().getCount() == 1, "Simulation consumed its module");
            }
            check(environment(machines.getFirst()) == 0, "Simulation mutated real environmental Aura despite sufficient tanks");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "bottler_blocked", timeoutTicks = 130)
    public static void blockedBottlerWaitsForEnvironmentAuraEnergyAndOutput(GameTestHelper h) {
        AuraMachine m = machine(h, 5);
        environment(m, 0);
        bottles(m, 1);
        h.runAfterDelay(10, () -> {
            check(m.status() == 7 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "Neutral native environment spent FE");
            simulate(m, BottlingMode.GHOST);
        });
        h.runAfterDelay(20, () -> {
            check(m.status() == 5 && m.progress() == 0 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "Simulation created Aura or began without enough resources");
            aura(m, 20_000);
            m.energy().setEnergy(0);
        });
        h.runAfterDelay(30, () -> {
            check(m.status() == 6 && m.auraTank().getStored() == 20_000 && m.inputs.getFirst().getCount() == 1, "Unpowered bottler consumed a bottle or Aura");
            m.energy().setEnergy(m.energy().getMaxEnergy());
            for (int i = 1; i <= 4; i++) m.getInventorySlots(null).get(i).setStack(new ItemStack(Items.COBBLESTONE, 64));
        });
        h.runAfterDelay(40, () -> {
            check(m.status() == 3 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "Full output spent FE");
            check(m.auraTank().getStored() == 20_000 && environment(m) == 0, "Full output spent Aura");
            m.getInventorySlots(null).get(1).setStack(ItemStack.EMPTY);
        });
        h.runAfterDelay(100, () -> {
            check(typed(output(m), NaturesAuraAPI.TYPE_NETHER) && m.auraTank().isEmpty(), "Bottler failed to resume with exactly the supplied Aura");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void moduleCapacityUpgradedPowerAndSavedSelectionRemainConsistent(GameTestHelper h) {
        AuraMachine m = machine(h, 5);
        var itemHandler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, m.getBlockPos(), Direction.NORTH);
        check(itemHandler != null && itemHandler.getSlots() == 1, "Automation exposed the simulation module or output slots as inputs");
        check(!itemHandler.insertItem(0, new ItemStack(Items.GLASS_BOTTLE), true).isEmpty(), "Bottler accepted a vanilla bottle instead of Bottle and Cork");
        ItemStack modules = new ItemStack(Content.SIMULATION_MODULE.get(), 2);
        check(m.simulationModuleSlot().insertItem(modules, Action.SIMULATE, AutomationType.MANUAL).getCount() == 1 && !m.hasSimulationModule(), "Simulated insertion changed the module slot");
        check(m.simulationModuleSlot().insertItem(modules, Action.EXECUTE, AutomationType.MANUAL).getCount() == 1, "More than one module installed");
        m.getComponent().addUpgrades(Upgrade.SPEED, 4);
        m.getComponent().addUpgrades(Upgrade.ENERGY, 4);
        long normal = MekanismUtils.getEnergyPerTick(m, m.energy().getBaseEnergyPerTick());
        check(m.energy().getEnergyPerTick() == normal * MachineConfig.SIMULATION_POWER_MULTIPLIER.get(), "Mek upgrades removed simulation power cost");
        m.setBottlingMode(BottlingMode.DARKNESS);
        ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
        drop = ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow();
        AuraMachine restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
        restored.setLevel(h.getLevel());
        restored.applyComponentsFromItemStack(drop);
        check(restored.hasSimulationModule() && restored.bottlingMode() == BottlingMode.DARKNESS, "Dropped bottler lost its module or selected product");
        check(restored.simulationModuleSlot().extractItem(1, Action.EXECUTE, AutomationType.MANUAL).getCount() == 1, "Module removal did not return one item");
        check(restored.energy().getEnergyPerTick() == normal, "Removing the module left the energy surcharge");
        m.energy().setEnergy(0);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "bottler_resume", timeoutTicks = 120)
    public static void removingModuleAndChangingModeDoNotProduceTheOldTarget(GameTestHelper h) {
        AuraMachine m = machine(h, 5);
        environment(m, 0);
        simulate(m, BottlingMode.GHOST);
        bottles(m, 1);
        aura(m, 20_000);
        h.runAfterDelay(15, () -> {
            check(m.progress() > 0, "Simulation had not started");
            m.simulationModuleSlot().extractItem(1, Action.EXECUTE, AutomationType.MANUAL);
        });
        h.runAfterDelay(25, () -> {
            check(m.status() == 8 && m.progress() == 0 && output(m).isEmpty(), "Removed module silently produced another kind of bottle");
            check(m.auraTank().getStored() == 20_000 && m.inputs.getFirst().getCount() == 1, "Interrupted bottle lost ingredients");
            simulate(m, BottlingMode.DARKNESS);
        });
        h.runAfterDelay(45, () -> {
            AuraMachine restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
            restored.setLevel(h.getLevel());
            restored.loadAdditional(m.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.getLevel().setBlockEntity(restored);
        });
        h.runAfterDelay(100, () -> {
            AuraMachine restored = (AuraMachine) h.getBlockEntity(new BlockPos(5, 1, 5));
            check(typed(output(restored), NaturesAuraAPI.TYPE_END) && output(restored).getCount() == 1, "Saved selection did not resume as darkness");
            check(restored.auraTank().isEmpty() && restored.inputs.getFirst().isEmpty(), "Resumed bottle charged the wrong input or Aura amount");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void simulationCraftingRequiresFourDistinctBottledMaterials(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "environment_simulation_module")).orElseThrow();
        check(holder.value() instanceof ShapedRecipe, "Simulation module recipe missing");
        ShapedRecipe recipe = (ShapedRecipe) holder.value();
        ItemStack sunlight = ItemAuraBottle.setType(new ItemStack(ModItems.AURA_BOTTLE), NaturesAuraAPI.TYPE_OVERWORLD);
        ItemStack ghosts = ItemAuraBottle.setType(new ItemStack(ModItems.AURA_BOTTLE), NaturesAuraAPI.TYPE_NETHER);
        ItemStack darkness = ItemAuraBottle.setType(new ItemStack(ModItems.AURA_BOTTLE), NaturesAuraAPI.TYPE_END);
        ItemStack alloy = new ItemStack(MekanismItems.REINFORCED_ALLOY.get());
        var grid = new ArrayList<>(List.of(sunlight, alloy, ghosts, alloy, new ItemStack(MekanismItems.ELITE_CONTROL_CIRCUIT.get()), alloy,
              darkness, alloy, new ItemStack(ModItems.VACUUM_BOTTLE)));
        check(recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()), "Correct four-component recipe does not match");
        check(recipe.getResultItem(h.getLevel().registryAccess()).is(Content.SIMULATION_MODULE), "Crafting output is not the simulation module");
        grid.set(2, sunlight.copy());
        check(!recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()), "Sunlight incorrectly substitutes for ghosts");
        grid.set(2, ghosts);
        grid.set(6, sunlight.copy());
        check(!recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()), "Sunlight incorrectly substitutes for darkness");
        grid.set(6, darkness);
        grid.set(8, sunlight.copy());
        check(!recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()), "An Aura bottle incorrectly substitutes for vacuum");
        grid.set(8, new ItemStack(ModItems.VACUUM_BOTTLE));
        grid.getFirst().set(DataComponents.CUSTOM_NAME, Component.literal("Renamed sunlight"));
        check(recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()), "Harmless extra components broke the material match");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "bottler_speed", timeoutTicks = 35)
    public static void speedUpgradesKeepAuraCostPerBottle(GameTestHelper h) {
        AuraMachine m = machine(h, 5);
        environment(m, 0);
        simulate(m, BottlingMode.SUNLIGHT);
        m.getComponent().addUpgrades(Upgrade.SPEED, 8);
        m.getComponent().addUpgrades(Upgrade.ENERGY, 8);
        m.energy().setEnergy(m.energy().getMaxEnergy());
        aura(m, 80_000);
        bottles(m, 4);
        h.runAfterDelay(20, () -> {
            check(output(m).getCount() == 4 && typed(output(m), NaturesAuraAPI.TYPE_OVERWORLD), "Upgraded bottler produced the wrong quantity");
            check(m.auraTank().isEmpty() && environment(m) == 0, "Speed upgrades changed per-bottle Aura cost");
            h.succeed();
        });
    }
}
