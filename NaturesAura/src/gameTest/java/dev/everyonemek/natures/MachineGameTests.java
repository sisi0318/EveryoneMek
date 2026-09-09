package dev.everyonemek.natures;

import de.ellpeck.naturesaura.blocks.ModBlocks;
import de.ellpeck.naturesaura.blocks.multi.Multiblocks;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.MekanismUtils;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.item.AttachedItems;
import mekanism.common.util.text.BooleanStateDisplay.YesNo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {
    private static AuraMachine machine(GameTestHelper h, MachineKind kind, BlockPos pos) {
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, Content.MACHINES.get(kind).get());
        AuraMachine machine = (AuraMachine) h.getBlockEntity(pos);
        machine.energy().setEnergy(machine.energy().getMaxEnergy());
        // Metadata-only controller fixtures must not keep changing neighbouring environment tests.
        if (machine.controller() != null) while (machine.controller().mode() != AuraControllerLogic.Mode.HOLD) machine.controller().cycleMode();
        return machine;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(message);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void droppedMachinesSupportInventoryTooltipAndContainerReaders(GameTestHelper h) {
        for (MachineKind kind : MachineKind.values()) {
            AuraMachine m = machine(h, kind, new BlockPos(2 + kind.ordinal() % 4 * 2, 1, 2 + kind.ordinal() / 4 * 3));
            var slots = m.getInventorySlots(null);
            for (int i = 0; i < kind.inputCount(); i++) {
                ItemStack ingredient = switch (kind) {
                    case FOREST_RITUAL -> new ItemStack(i == 8 ? Items.OAK_SAPLING : i == 9 ? ModBlocks.GOLD_POWDER : Items.STONE, i + 1);
                    case NATURAL_ALTAR -> new ItemStack(i == 0 ? Items.BONE : ModBlocks.CRUSHING_CATALYST, i + 1);
                    case OFFERING -> new ItemStack(i == 0 ? ModItems.INFUSED_IRON : ModItems.CALLING_SPIRIT, i + 1);
                    case AURA_BOTTLER -> new ItemStack(ModItems.BOTTLE_TWO_THE_REBOTTLING, 3);
                    case ANIMAL_SPAWNER -> new ItemStack(ModItems.BIRTH_SPIRIT, i + 1);
                    case INDUSTRIAL_BREEDER -> new ItemStack(Items.WHEAT, i + 1);
                    case ORE_CHAMBER -> i == 0 ? new ItemStack(Items.STONE, 3) : de.ellpeck.naturesaura.items.ItemEffectPowder.setEffect(new ItemStack(ModItems.EFFECT_POWDER), de.ellpeck.naturesaura.chunk.effect.OreSpawnEffect.NAME);
                    default -> ItemStack.EMPTY;
                };
                slots.get(i).setStack(ingredient);
            }
            int energySlot = kind.inputCount();
            if (kind.outputCount() > 0) {
                for (int i = 0; i < kind.outputCount(); i++) slots.get(kind.inputCount() + i).setStack(new ItemStack(Items.COBBLESTONE, i + 1));
                energySlot += kind.outputCount();
            }
            slots.get(energySlot).setStack(new ItemStack(MekanismItems.ENERGY_TABLET.get()));
            if (m.goldModuleSlot() != null) m.goldModuleSlot().setStack(new ItemStack(Content.INFINITE_GOLD_MODULE.get()));
            if (m.simulationModuleSlot() != null) m.simulationModuleSlot().setStack(new ItemStack(Content.SIMULATION_MODULE.get()));
            if (m.rangeModuleSlot() != null) m.rangeModuleSlot().setStack(new ItemStack(Content.RANGE_MODULE.get(), 2));
            if (m.auraTank() != null) m.auraTank().setStack(new ChemicalStack(Content.AURA, 1234));
            var expected = slots.stream().map(s -> s.getStack().copy()).toList();
            ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null,
                  new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            drop = ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow();
            // Exact reader from ItemBlockTooltip.addDetails, without starting a game client.
            check(YesNo.hasInventory(drop) == YesNo.YES_COLORED, kind + ": occupied inventory tooltip failed");
            var attachedSlots = ContainerType.ITEM.getAttachmentContainersIfPresent(drop);
            check(attachedSlots.size() == expected.size(), kind + ": attached slot count changed");
            for (int i = 0; i < expected.size(); i++)
                check(ItemStack.matches(attachedSlots.get(i).getStack(), expected.get(i)), kind + ": saved slot moved: " + i);
            check(ContainerType.ENERGY.getAttachmentContainersIfPresent(drop).getFirst().getEnergy() == m.energy().getEnergy(),
                  kind + ": saved energy unreadable");
            if (m.auraTank() != null)
                check(ContainerType.CHEMICAL.getAttachmentContainersIfPresent(drop).getFirst().getStored() == 1234,
                      kind + ": saved Aura unreadable");
            AuraMachine restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
            restored.setLevel(h.getLevel());
            restored.applyComponentsFromItemStack(drop);
            for (int i = 0; i < expected.size(); i++)
                check(ItemStack.matches(restored.getInventorySlots(null).get(i).getStack(), expected.get(i)),
                      kind + ": replaced machine lost slot " + i);
            ItemStack empty = new ItemStack(Content.MACHINES.get(kind));
            empty.set(MekanismDataComponents.ATTACHED_ITEMS, AttachedItems.create(expected.size()));
            check(YesNo.hasInventory(empty) == YesNo.NO_COLORED, kind + ": empty saved inventory tooltip failed");
            m.energy().setEnergy(0);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void legacyForestItemInventoryKeepsItsOriginalSlots(GameTestHelper h) {
        ItemStack legacy = new ItemStack(Content.MACHINES.get(MachineKind.FOREST_RITUAL));
        var oldSlots = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 8; i++) oldSlots.add(new ItemStack(Items.STONE, i + 1));
        oldSlots.add(new ItemStack(Items.OAK_SAPLING));
        oldSlots.add(new ItemStack(ModBlocks.GOLD_POWDER, 16));
        for (int i = 0; i < 4; i++) oldSlots.add(new ItemStack(Items.COBBLESTONE, i + 1));
        oldSlots.add(new ItemStack(MekanismItems.ENERGY_TABLET.get()));
        legacy.set(MekanismDataComponents.ATTACHED_ITEMS, new AttachedItems(oldSlots));
        check(YesNo.hasInventory(legacy) == YesNo.YES_COLORED, "0.1.0 forest tooltip failed");
        var reader = ContainerType.ITEM.getAttachmentContainersIfPresent(legacy);
        for (int i = 0; i < 15; i++) check(ItemStack.matches(reader.get(i).getStack(), oldSlots.get(i)), "Legacy slot changed: " + i);
        AuraMachine restored = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        restored.applyComponentsFromItemStack(legacy);
        check(restored.goldModuleSlot().isEmpty(), "Legacy inventory invented a module");
        for (int i = 0; i < 15; i++) check(ItemStack.matches(restored.getInventorySlots(null).get(i).getStack(), oldSlots.get(i)),
              "Legacy replacement lost slot " + i);
        h.succeed();
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
            generator.toggleEnvironmentOutput();
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
        m.toggleEnvironmentOutput();
        m.energy().setEnergy(0);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 35)
    public static void fastGeneratorFillsTheLastUnitAndResumesWithoutEmptying(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_GENERATOR, new BlockPos(2, 1, 2));
        m.getComponent().addUpgrades(Upgrade.SPEED, 8);
        m.getComponent().addUpgrades(Upgrade.ENERGY, 8);
        m.auraTank().setStack(new ChemicalStack(Content.AURA, AuraMachine.AURA_CAPACITY - 1));
        long before = m.energy().getEnergy();
        h.runAfterDelay(8, () -> {
            check(m.auraTank().getStored() == AuraMachine.AURA_CAPACITY, "High speed refused a partial batch despite free capacity");
            check(m.energy().getEnergy() < before, "Final unit was produced without energy");
            long fullEnergy = m.energy().getEnergy();
            h.runAfterDelay(4, () -> {
                check(m.energy().getEnergy() == fullEnergy, "Full generator consumed energy");
                m.auraTank().extract(1, Action.EXECUTE, AutomationType.INTERNAL);
            });
        });
        h.runAfterDelay(25, () -> {
            check(m.auraTank().getStored() == AuraMachine.AURA_CAPACITY, "Generator required the tank to be emptied before resuming");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void forestRoutesSaplingsAndPowderOnlyThroughExtraSlots(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        var normal = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, m.getBlockPos(), m.getDirection());
        var extra = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, m.getBlockPos(), RelativeSide.BACK.getDirection(m.getDirection()));
        check(normal != null && normal.getSlots() == 8, "Normal input still contains auxiliary or module slots");
        check(extra != null && extra.getSlots() == 2, "Extra input must expose exactly the sapling and powder slots");
        check(extra.insertItem(0, new ItemStack(Items.OAK_SAPLING), false).isEmpty(), "Extra input rejected sapling");
        check(extra.insertItem(1, new ItemStack(ModBlocks.GOLD_POWDER, 16), false).isEmpty(), "Extra input rejected gold powder");
        check(m.inputs.get(8).getCount() == 1 && m.inputs.get(9).getCount() == 16, "Auxiliary items reached the wrong saved slot indices");
        check(m.inputs.subList(0, 8).stream().allMatch(s -> s.isEmpty()), "Extra items contaminated recipe inputs");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void goldModuleIsSingleReversibleAndMultipliesUpgradedEnergy(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        long normal = m.energy().getEnergyPerTick();
        ItemStack stack = new ItemStack(Content.INFINITE_GOLD_MODULE.get(), 4);
        check(m.goldModuleSlot().insertItem(stack, Action.SIMULATE, AutomationType.MANUAL).getCount() == 3, "Module capacity is not one");
        check(!m.hasInfiniteGold() && m.energy().getEnergyPerTick() == normal, "Simulation installed the module");
        check(m.goldModuleSlot().insertItem(stack, Action.EXECUTE, AutomationType.MANUAL).getCount() == 3, "Installation consumed more than one module");
        check(m.hasInfiniteGold() && m.energy().getEnergyPerTick() == normal * MachineConfig.INFINITE_GOLD_POWER_MULTIPLIER.get(), "Module did not increase energy usage");
        check(!m.goldModuleSlot().insertItem(stack.copyWithCount(1), Action.EXECUTE, AutomationType.MANUAL).isEmpty(), "Second module installed");
        m.getComponent().addUpgrades(Upgrade.SPEED, 4);
        m.getComponent().addUpgrades(Upgrade.ENERGY, 4);
        long upgraded = MekanismUtils.getEnergyPerTick(m, m.energy().getBaseEnergyPerTick());
        check(m.energy().getEnergyPerTick() == upgraded * MachineConfig.INFINITE_GOLD_POWER_MULTIPLIER.get(), "Mek upgrades removed the module surcharge");
        check(m.goldModuleSlot().extractItem(1, Action.EXECUTE, AutomationType.MANUAL).getCount() == 1, "Uninstall did not return the module");
        check(!m.hasInfiniteGold() && m.energy().getEnergyPerTick() == upgraded, "Uninstall did not restore normal usage");
        h.succeed();
    }

    private static void prepareForestWithoutPowder(AuraMachine m) {
        m.inputs.get(0).setStack(new ItemStack(Items.STONE, 3));
        m.inputs.get(1).setStack(new ItemStack(ModItems.GOLD_LEAF));
        m.inputs.get(2).setStack(new ItemStack(Items.GOLD_INGOT));
        m.inputs.get(3).setStack(new ItemStack(ModItems.TOKEN_JOY));
        m.inputs.get(8).setStack(new ItemStack(Items.OAK_SAPLING));
    }

    @GameTest(template = "empty", timeoutTicks = 560)
    public static void moduleReplacesPowderButStillConsumesSaplingAndRecipeGoldLeaf(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        prepareForestWithoutPowder(m);
        m.goldModuleSlot().insertItem(new ItemStack(Content.INFINITE_GOLD_MODULE.get()), Action.EXECUTE, AutomationType.MANUAL);
        long before = m.energy().getEnergy();
        long costPerTick = m.energy().getEnergyPerTick();
        h.runAfterDelay(530, () -> {
            check(m.inputs.stream().allMatch(s -> s.isEmpty()), "Module bypassed sapling or recipe ingredients");
            check(m.hasInfiniteGold() && m.goldModuleSlot().getCount() == 1, "Module was consumed");
            check(before - m.energy().getEnergy() == costPerTick * 500, "Recipe did not pay the increased energy cost");
            check(m.getInventorySlots(null).stream().anyMatch(s -> s.getStack().is(ModBlocks.NATURE_ALTAR.asItem())), "Recipe without powder did not finish");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void removingModuleDuringWorkRestoresPowderRequirement(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        prepareForestWithoutPowder(m);
        m.goldModuleSlot().insertItem(new ItemStack(Content.INFINITE_GOLD_MODULE.get()), Action.EXECUTE, AutomationType.MANUAL);
        h.runAfterDelay(35, () -> {
            check(m.progress() > 0, "Fixture did not begin processing");
            m.goldModuleSlot().extractItem(1, Action.EXECUTE, AutomationType.MANUAL);
        });
        h.runAfterDelay(70, () -> {
            check(m.status() == 4 && m.progress() == 0, "Old powder-free plan survived uninstall");
            check(m.inputs.get(8).getCount() == 1 && m.inputs.get(1).getCount() == 1, "Incomplete work consumed ingredients");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void moduleAndLegacySlotsSurviveSaveAndMachineDrops(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.FOREST_RITUAL, new BlockPos(2, 1, 2));
        m.inputs.get(8).setStack(new ItemStack(Items.OAK_SAPLING, 13));
        m.inputs.get(9).setStack(new ItemStack(ModBlocks.GOLD_POWDER, 35));
        var legacy = m.saveWithoutMetadata(h.getLevel().registryAccess());
        AuraMachine oldSave = new AuraMachine(m.getBlockPos(), m.getBlockState());
        oldSave.setLevel(h.getLevel());
        oldSave.loadAdditional(legacy, h.getLevel().registryAccess());
        check(oldSave.inputs.get(8).getCount() == 13 && oldSave.inputs.get(9).getCount() == 35 && !oldSave.hasInfiniteGold(), "Pre-module slot indices changed");
        m.goldModuleSlot().insertItem(new ItemStack(Content.INFINITE_GOLD_MODULE.get()), Action.EXECUTE, AutomationType.MANUAL);
        AuraMachine restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
        restored.setLevel(h.getLevel());
        restored.loadAdditional(m.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        check(restored.hasInfiniteGold() && restored.energy().getEnergyPerTick() == m.energy().getEnergyPerTick(), "Saved module state or surcharge was lost");
        ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
        check(drop.get(MekanismDataComponents.ATTACHED_ITEMS).containers().stream().anyMatch(s -> s.is(Content.INFINITE_GOLD_MODULE) && s.getCount() == 1), "Machine drop lost or duplicated the installed module");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void everyActualContainerTitleHasEnglishAndChineseTranslations(GameTestHelper h) throws Exception {
        for (MachineKind kind : MachineKind.values()) {
            AuraMachine m = machine(h, kind, new BlockPos(1 + kind.ordinal(), 1, 2));
            String key = ((TranslatableContents) m.getDisplayName().getContents()).getKey();
            for (String language : new String[]{"en_us", "zh_cn"}) {
                try (var in = MachineGameTests.class.getResourceAsStream("/assets/naturesmekanism/lang/" + language + ".json")) {
                    check(in != null, "Language resource missing");
                    var translations = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    check(translations.has(key) && !translations.get(key).getAsString().equals(key), "Untranslated menu title: " + language + ":" + key);
                }
            }
            m.energy().setEnergy(0);
        }
        h.succeed();
    }

    private static int setEnvironmentAura(GameTestHelper h, AuraMachine machine, int target) {
        return AuraTestEnvironment.set(machine, MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get(), target);
    }

    @GameTest(template = "empty", batch = "environment_only", timeoutTicks = 130)
    public static void altarProcessesUsingEnvironmentWithAnEmptyChemicalTank(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.NATURAL_ALTAR, new BlockPos(5, 1, 8));
        int before = setEnvironmentAura(h, m, IAuraChunk.DEFAULT_AURA);
        m.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT));
        h.runAfterDelay(100, () -> {
            check(m.inputs.getFirst().isEmpty(), "Altar did not recognize environmental Aura");
            check(m.auraTank().isEmpty(), "Direct environment consumption unexpectedly duplicated Aura into the tank");
            check(m.getInventorySlots(null).stream().anyMatch(s -> s.getStack().is(ModItems.INFUSED_IRON)), "Environment-powered altar produced no output");
            int after = IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get());
            check(before - after == 15_000, "Environment-powered recipe did not consume exactly its Aura cost");
            check(m.environmentAura() > 0, "Environmental Aura is not exposed for the menu");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "environment_mixed", timeoutTicks = 130)
    public static void altarUsesTankFirstAndEnvironmentOnlyForTheRemainder(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.NATURAL_ALTAR, new BlockPos(5, 1, 8));
        int before = setEnvironmentAura(h, m, IAuraChunk.DEFAULT_AURA);
        m.auraTank().insert(new ChemicalStack(Content.AURA, 5_000), Action.EXECUTE, AutomationType.INTERNAL);
        m.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT));
        h.runAfterDelay(10, () -> {
            check(m.auraTank().getStored() < 5_000 && m.auraTank().getStored() > 0, "Tank was not used first");
            int current = IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get());
            check(current == before, "Environment changed while tank Aura was sufficient: " + before + " -> " + current);
        });
        h.runAfterDelay(100, () -> {
            check(m.inputs.getFirst().isEmpty() && m.auraTank().isEmpty(), "Mixed-source recipe did not finish");
            int after = IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get());
            var spots = new java.util.ArrayList<String>();
            IAuraChunk.getSpotsInArea(h.getLevel(), m.getBlockPos(), MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get(),
                  (pos, amount) -> spots.add(pos.toShortString() + "=" + amount));
            check(before - after == 10_000, "Mixed Aura cost changed: " + before + " -> " + after + "; machine=" + m.getBlockPos().toShortString() + "; spots=" + spots);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "environment_blocked", timeoutTicks = 170)
    public static void altarDoesNotSpendEitherSourceUntilEnergyOutputAndAuraAreReady(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.NATURAL_ALTAR, new BlockPos(5, 1, 8));
        int before = setEnvironmentAura(h, m, IAuraChunk.DEFAULT_AURA);
        m.energy().setEnergy(0);
        m.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT));
        h.runAfterDelay(10, () -> {
            check(m.status() == 6 && m.progress() == 0, "Unpowered altar made progress");
            check(IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get()) == before, "Unpowered altar consumed environment Aura");
            m.energy().setEnergy(m.energy().getMaxEnergy());
            for (int i = 2; i < 6; i++) m.getInventorySlots(null).get(i).setStack(new ItemStack(Items.COBBLESTONE, 64));
        });
        h.runAfterDelay(20, () -> {
            check(m.status() == 3 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "Blocked altar consumed power");
            check(IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get()) == before, "Blocked altar consumed environment Aura");
            m.getInventorySlots(null).get(2).setStack(ItemStack.EMPTY);
            setEnvironmentAura(h, m, -20_000);
            m.auraTank().insert(new ChemicalStack(Content.AURA, 100), Action.EXECUTE, AutomationType.INTERNAL);
        });
        h.runAfterDelay(30, () -> {
            check(m.status() == 5 && m.progress() == 0, "Altar overdrew an exhausted environment");
            check(m.auraTank().getStored() == 100 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "Insufficient combined Aura partially consumed resources");
            setEnvironmentAura(h, m, 50_000);
        });
        h.runAfterDelay(140, () -> {
            check(m.inputs.getFirst().isEmpty(), "Altar failed to resume after environment replenishment");
            check(m.getInventorySlots(null).get(2).getStack().is(ModItems.INFUSED_IRON), "Resumed environment recipe lost its output");
            h.succeed();
        });
    }
}
