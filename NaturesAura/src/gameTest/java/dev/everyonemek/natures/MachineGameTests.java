package dev.everyonemek.natures;

import de.ellpeck.naturesaura.blocks.ModBlocks;
import de.ellpeck.naturesaura.blocks.multi.Multiblocks;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {
    private static AuraMachine machine(GameTestHelper h, MachineKind kind, BlockPos pos) {
        h.setBlock(pos, Content.MACHINES.get(kind).get());
        AuraMachine machine = (AuraMachine) h.getBlockEntity(pos);
        machine.energy().setEnergy(machine.energy().getMaxEnergy());
        return machine;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void generatorCreatesChemicalAndExposesCapabilities(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_GENERATOR, new BlockPos(2, 1, 2));
        var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, m.getBlockPos(), Direction.NORTH);
        check(fe != null && fe.canReceive(), "Generator must accept FE");
        var chem = h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), m.getBlockPos(), Direction.NORTH);
        check(chem != null, "Generator must expose Mek chemicals");
        long before = m.energy().getEnergy();
        h.runAfterDelay(25, () -> {
            check(m.auraTank().getStored() >= MachineConfig.AURA_PER_CYCLE.get(), "No Aura generated");
            check(m.energy().getEnergy() < before, "Generation did not consume energy");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 130)
    public static void altarUsesExactAuraCostAndProducesInfusedIron(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.NATURAL_ALTAR, new BlockPos(2, 1, 2));
        m.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT));
        m.auraTank().insert(new ChemicalStack(Content.AURA, 15_000), Action.EXECUTE, AutomationType.INTERNAL);
        h.runAfterDelay(100, () -> {
            check(m.inputs.getFirst().isEmpty(), "Input not consumed");
            check(m.auraTank().isEmpty(), "Recipe must consume exactly 15000 Aura");
            check(m.getInventorySlots(null).stream().anyMatch(s -> s.getStack().is(ModItems.INFUSED_IRON)), "Infused iron missing");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 580)
    public static void forestAcceptsRepeatedMaterialsFromOneStack(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        m.inputs.get(0).setStack(new ItemStack(Items.STONE, 3));
        m.inputs.get(1).setStack(new ItemStack(ModItems.GOLD_LEAF));
        m.inputs.get(2).setStack(new ItemStack(Items.GOLD_INGOT));
        m.inputs.get(3).setStack(new ItemStack(ModItems.TOKEN_JOY));
        m.inputs.get(8).setStack(new ItemStack(Items.OAK_SAPLING));
        m.inputs.get(9).setStack(new ItemStack(ModBlocks.GOLD_POWDER, MachineConfig.FOREST_GOLD.get()));
        h.runAfterDelay(530, () -> {
            check(m.inputs.stream().allMatch(s -> s.isEmpty()), "Forest ingredients not consumed correctly");
            check(m.getInventorySlots(null).stream().anyMatch(s -> s.getStack().is(ModBlocks.NATURE_ALTAR.asItem())), "Forest output missing");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 190)
    public static void offeringRequiresFlowersAndResumesWithOneSpiritPerBatch(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.OFFERING, new BlockPos(5, 1, 5));
        m.inputs.get(0).setStack(new ItemStack(ModItems.INFUSED_IRON, 8));
        m.inputs.get(1).setStack(new ItemStack(ModItems.CALLING_SPIRIT));
        long before = m.energy().getEnergy();
        h.runAfterDelay(15, () -> {
            check(m.status() == 2 && m.energy().getEnergy() == before, "Missing flowers must block processing without spending FE");
            flowers(h, m);
        });
        h.runAfterDelay(45, () -> {
            check(m.progress() > 0, "Complete flower ring did not start offering");
            h.getLevel().setBlockAndUpdate(m.getBlockPos().offset(0, 0, -4), Blocks.AIR.defaultBlockState());
        });
        h.runAfterDelay(60, () -> check(m.status() == 2 && m.progress() > 0, "Removing a flower must pause, preserving progress"));
        h.runAfterDelay(65, () -> flowers(h, m));
        h.runAfterDelay(160, () -> {
            check(m.inputs.get(0).isEmpty() && m.inputs.get(1).isEmpty(), "Offering batch inputs incorrect");
            int result = m.getInventorySlots(null).stream().filter(s -> s.getStack().is(ModItems.SKY_INGOT)).mapToInt(s -> s.getCount()).sum();
            check(result == 8, "Expected 8 sky ingots from one calling spirit");
            h.succeed();
        });
    }

    private static void flowers(GameTestHelper h, AuraMachine m) {
        Multiblocks.OFFERING_TABLE.forEach(m.getBlockPos(), 'R', (pos, matcher) -> {
            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, Blocks.POPPY.defaultBlockState());
            return true;
        });
        check(OfferingFlowers.complete(h.getLevel(), m.getBlockPos()), "Flower predicate rejected original R positions");
    }

    @GameTest(template = "empty", timeoutTicks = 130)
    public static void pressurizedTubeSuppliesTheAltar(GameTestHelper h) {
        AuraMachine generator = machine(h, MachineKind.AURA_GENERATOR, new BlockPos(2, 1, 2));
        h.setBlock(new BlockPos(3, 1, 2), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        AuraMachine altar = machine(h, MachineKind.NATURAL_ALTAR, new BlockPos(4, 1, 2));
        h.runAfterDelay(100, () -> {
            check(altar.auraTank().getStored() > 0, "Real Mek pressurized tube did not supply the altar");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void environmentalReleaseAddsRealWorldAuraAndDrainsTank(GameTestHelper h) {
        AuraMachine generator = machine(h, MachineKind.AURA_GENERATOR, new BlockPos(5, 1, 5));
        generator.energy().setEnergy(0);
        generator.auraTank().insert(new ChemicalStack(Content.AURA, 10_000), Action.EXECUTE, AutomationType.INTERNAL);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            h.getLevel().getChunkAt(generator.getBlockPos().offset(x * 16, 0, z * 16));
        int before = IAuraChunk.getAuraInArea(h.getLevel(), generator.getBlockPos(), 16);
        generator.toggleEnvironmentOutput();
        h.runAfterDelay(30, () -> {
            int after = IAuraChunk.getAuraInArea(h.getLevel(), generator.getBlockPos(), 16);
            check(after > before, "Environment release did not modify NaturesAura's actual chunk storage");
            check(generator.auraTank().getStored() < 10_000, "Environment release duplicated the tank's Aura");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 110)
    public static void fullOutputDoesNotSpendResourcesAndCatalystIsRetained(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.NATURAL_ALTAR, new BlockPos(2, 1, 2));
        m.inputs.get(0).setStack(new ItemStack(Items.BONE));
        m.inputs.get(1).setStack(new ItemStack(ModBlocks.CRUSHING_CATALYST));
        m.auraTank().insert(new ChemicalStack(Content.AURA, 3_000), Action.EXECUTE, AutomationType.INTERNAL);
        for (int i = 2; i < 6; i++) m.getInventorySlots(null).get(i).setStack(new ItemStack(Items.COBBLESTONE, 64));
        long energy = m.energy().getEnergy();
        h.runAfterDelay(15, () -> {
            check(m.status() == 3, "Full output should pause");
            check(m.energy().getEnergy() == energy && m.auraTank().getStored() == 3_000, "Full output wasted resources");
            m.getInventorySlots(null).get(2).setStack(ItemStack.EMPTY);
        });
        h.runAfterDelay(80, () -> {
            check(m.inputs.get(0).isEmpty(), "Input not consumed after output was freed");
            check(m.inputs.get(1).getStack().is(ModBlocks.CRUSHING_CATALYST.asItem()), "Catalyst was consumed");
            check(m.getInventorySlots(null).get(2).getStack().is(Items.BONE_MEAL), "Crushing output missing");
            check(m.getInventorySlots(null).get(2).getCount() == 6, "Crushing output amount incorrect");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void processingResumesFromSavedBlockEntity(GameTestHelper h) {
        BlockPos relative = new BlockPos(2, 1, 2);
        AuraMachine m = machine(h, MachineKind.NATURAL_ALTAR, relative);
        m.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT));
        m.auraTank().insert(new ChemicalStack(Content.AURA, 15_000), Action.EXECUTE, AutomationType.INTERNAL);
        h.runAfterDelay(35, () -> {
            check(m.progress() > 0, "Save fixture had not started processing");
            var saved = m.saveWithoutMetadata(h.getLevel().registryAccess());
            AuraMachine restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
            restored.setLevel(h.getLevel());
            restored.loadAdditional(saved, h.getLevel().registryAccess());
            h.getLevel().setBlockEntity(restored);
        });
        h.runAfterDelay(110, () -> {
            AuraMachine restored = (AuraMachine) h.getBlockEntity(relative);
            check(restored != m, "Test did not replace the entity");
            check(restored.inputs.getFirst().isEmpty() && restored.auraTank().isEmpty(), "Restored work did not consume exact remaining resources");
            check(restored.getInventorySlots(null).stream().anyMatch(s -> s.getStack().is(ModItems.INFUSED_IRON)), "Restored recipe output missing");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void machineLootPreservesAuraEnergyAndOutputMode(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_GENERATOR, new BlockPos(2, 1, 2));
        m.auraTank().insert(new ChemicalStack(Content.AURA, 1234), Action.EXECUTE, AutomationType.INTERNAL);
        m.toggleEnvironmentOutput();
        var drops = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE));
        check(drops.size() == 1, "Expected one machine drop");
        ItemStack drop = drops.getFirst();
        check(drop.has(MekanismDataComponents.ATTACHED_CHEMICALS), "Drop lost chemical storage");
        check(drop.has(MekanismDataComponents.ATTACHED_ENERGY), "Drop lost stored energy");
        check(Boolean.TRUE.equals(drop.get(Content.ENVIRONMENT_OUTPUT)), "Drop lost environment output mode");
        h.succeed();
    }
}
