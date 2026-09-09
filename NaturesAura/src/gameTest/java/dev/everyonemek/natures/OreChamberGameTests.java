package dev.everyonemek.natures;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.misc.WeightedOre;
import de.ellpeck.naturesaura.chunk.effect.OreSpawnEffect;
import de.ellpeck.naturesaura.items.ItemEffectPowder;
import de.ellpeck.naturesaura.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class OreChamberGameTests {
    private static void check(boolean value, String message) {
        if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(message);
    }
    private static WeightedOre ore(String tag, int weight) { return new WeightedOre(ResourceLocation.fromNamespaceAndPath("c", tag), weight); }
    private static ItemStack powder() { return ItemEffectPowder.setEffect(new ItemStack(ModItems.EFFECT_POWDER), OreSpawnEffect.NAME); }
    private static AuraMachine machine(ServerLevel level, BlockPos pos, Direction facing) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Content.MACHINES.get(MachineKind.ORE_CHAMBER).defaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        var m = (AuraMachine) level.getBlockEntity(pos);
        for (BlockPos part : BlockPos.betweenClosed(m.chamber().center().offset(-1, -1, -1), m.chamber().center().offset(1, 1, 1))) {
            level.getChunkAt(part);
            if (!part.equals(pos)) level.setBlockAndUpdate(part, part.equals(m.chamber().center()) ? Blocks.AIR.defaultBlockState() : Content.CHAMBER_CASING.defaultState());
        }
        m.energy().setEnergy(m.energy().getMaxEnergy());
        return m;
    }
    private static AuraMachine machine(GameTestHelper h) { return machine(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), Direction.NORTH); }
    private static void feed(AuraMachine m, int amount) {
        m.inputs.get(0).setStack(new ItemStack(OreChamberLogic.substrate(m.getLevel()), amount));
        m.inputs.get(1).setStack(powder());
    }
    private static void cycle(AuraMachine m) { for (int i = 0; i < MekanismUtils.getTicks(m, MachineConfig.ORE_TICKS.get()); i++) m.onUpdateServer(); }
    private static int count(AuraMachine m) { return m.getInventorySlots(null).subList(2, 6).stream().mapToInt(s -> s.getCount()).sum(); }
    private static void clean(AuraMachine m) {
        m.energy().setEnergy(0);
        for (BlockPos part : BlockPos.betweenClosed(m.chamber().center().offset(-1, -1, -1), m.chamber().center().offset(1, 1, 1)))
            m.getLevel().setBlockAndUpdate(part, Blocks.AIR.defaultBlockState());
    }
    private static ChamberPortEntity port(AuraMachine m, BlockPos pos, boolean output) {
        m.getLevel().setBlockAndUpdate(pos, Content.CHAMBER_PORT.defaultState().setValue(ChamberPortBlock.OUTPUT, output));
        return (ChamberPortEntity) m.getLevel().getBlockEntity(pos);
    }

    private static void clickItemSide(AuraMachine m, RelativeSide side, mekanism.common.network.MekClickType click) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft((ServerLevel) m.getLevel());
        var context = (net.neoforged.neoforge.network.handling.IPayloadContext) java.lang.reflect.Proxy.newProxyInstance(
              OreChamberGameTests.class.getClassLoader(), new Class[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},
              (proxy, method, args) -> {
                  if (method.getName().equals("player")) return player;
                  throw new UnsupportedOperationException(method.getName());
              });
        new mekanism.common.network.to_server.configuration_update.PacketSideData(m.getBlockPos(), click, side, TransmissionType.ITEM).handle(context);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sideIconsFindRemotePortsOnAllFacesAndRefreshAfterChanges(GameTestHelper h) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            AuraMachine m = machine(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), facing);
            try {
                for (RelativeSide side : RelativeSide.values()) {
                    var positions = m.chamber().facePositions(side.getDirection(facing));
                    check(positions.size() == 9, "Side preview did not inspect a full face");
                    var inputPos = positions.stream().filter(p -> !p.equals(m.getBlockPos())).findFirst().orElseThrow();
                    var outputPos = positions.getLast();
                    port(m, inputPos, false);
                    check(m.chamber().sideDisplayPosition(side).equals(inputPos), "Port icon ignored controller orientation " + facing + "/" + side);
                    port(m, outputPos, true);
                    check(m.chamber().sideDisplayPosition(side).equals(outputPos), "Output port was hidden behind another port or casing");
                    var display = OreChamberLogic.portDisplayStack(h.getLevel().getBlockState(outputPos));
                    check(display.is(Content.CHAMBER_PORT.asItem()) && "true".equals(display.get(DataComponents.BLOCK_STATE).properties().get("output")), "Output icon lost its item or lit model state");
                    h.getLevel().setBlockAndUpdate(outputPos, Content.CHAMBER_CASING.defaultState());
                    check(m.chamber().sideDisplayPosition(side).equals(inputPos), "Port removal left a stale icon");
                    h.getLevel().setBlockAndUpdate(inputPos, Content.CHAMBER_CASING.defaultState());
                    check(m.chamber().sideDisplayPosition(side).equals(m.getBlockPos().relative(side.getDirection(facing))), "No-port preview failed to restore the adjacent block");
                }
            } finally { clean(m); }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "port_gui_chest", timeoutTicks = 85)
    public static void nativeGuiOutputPacketEjectsToChestAndHandlesFullDisabledAndResume(GameTestHelper h) {
        AuraMachine m = machine(h);
        m.energy().setEnergy(0);
        var p = port(m, m.chamber().center().south(), false);
        var chestPos = p.getBlockPos().south();
        h.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(chestPos);
        m.getInventorySlots(null).get(2).setStack(new ItemStack(Items.IRON_ORE, 5));
        clickItemSide(m, RelativeSide.BACK, mekanism.common.network.MekClickType.LEFT);
        check(p.output(), "Clicking Output in Mek's item configuration did not configure the actual port");
        h.runAfterDelay(20, () -> {
            check(chest.getItem(0).is(Items.IRON_ORE) && chest.getItem(0).getCount() == 5 && count(m) == 0, "GUI-configured port failed to eject directly into a chest");
            for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.STONE, 64));
            m.getInventorySlots(null).get(2).setStack(new ItemStack(Items.COAL_ORE, 7));
        });
        h.runAfterDelay(35, () -> {
            check(count(m) == 7, "Full chest lost chamber output");
            clickItemSide(m, RelativeSide.BACK, mekanism.common.network.MekClickType.SHIFT_LEFT);
            check(p.itemMode() == DataType.NONE && p.items.getSlots() == 0, "Clearing a side left its port enabled");
            chest.setItem(0, ItemStack.EMPTY);
        });
        h.runAfterDelay(50, () -> {
            check(count(m) == 7 && chest.getItem(0).isEmpty(), "Disabled port continued ejecting");
            clickItemSide(m, RelativeSide.BACK, mekanism.common.network.MekClickType.LEFT);
            clickItemSide(m, RelativeSide.BACK, mekanism.common.network.MekClickType.LEFT);
        });
        h.runAfterDelay(70, () -> {
            try {
                check(count(m) == 0 && chest.getItem(0).is(Items.COAL_ORE) && chest.getItem(0).getCount() == 7, "Re-enabled output did not resume exactly once");
                h.succeed();
            } finally { clean(m); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void guiPortModesSupportCombinedIoEnergyDropsAndLegacySaves(GameTestHelper h) {
        AuraMachine m = machine(h);
        try {
            var p = port(m, m.chamber().center().south(), true);
            // Old worlds have only the output boolean. Loading the controller must not erase those settings.
            check(p.itemMode() == DataType.OUTPUT, "Legacy output port changed mode");
            m.loadAdditional(m.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            check(p.output(), "Loading a controller overwrote an existing port mode");
            var config = m.getConfig().getConfig(TransmissionType.ITEM);
            config.setDataType(DataType.INPUT_OUTPUT, RelativeSide.BACK);
            m.getConfig().sideChanged(TransmissionType.ITEM, RelativeSide.BACK);
            check(p.items.getSlots() == 6, "Input/output mode did not expose both inventories");
            check(p.items.insertItem(0, new ItemStack(Items.STONE, 2), false).isEmpty(), "Combined port did not accept feedstock");
            check(p.items.extractItem(0, 1, false).isEmpty(), "Combined port extracted feedstock");
            m.getInventorySlots(null).get(2).setStack(new ItemStack(Items.DIAMOND_ORE));
            check(p.items.extractItem(2, 1, false).getCount() == 1, "Combined port did not expose ore output");
            config.setDataType(DataType.ENERGY, RelativeSide.BACK);
            m.getConfig().sideChanged(TransmissionType.ITEM, RelativeSide.BACK);
            var battery = new ItemStack(mekanism.common.registries.MekanismItems.ENERGY_TABLET.get());
            battery.getCapability(Capabilities.EnergyStorage.ITEM).receiveEnergy(1000, false);
            check(p.items.getSlots() == 1 && p.items.insertItem(0, battery, false).isEmpty(), "Energy-item mode did not target the charging slot");
            var drop = Block.getDrops(p.getBlockState(), h.getLevel(), p.getBlockPos(), p, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            check("energy".equals(drop.get(DataComponents.BLOCK_STATE).properties().get("item_mode")), "Dropped port lost its GUI mode");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void allHorizontalRotationsRequireCompleteHollowShell(GameTestHelper h) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            AuraMachine m = machine(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), face);
            try {
                check(m.chamber().formed(), "Valid chamber failed facing " + face);
                var corner = m.chamber().center().offset(1, 1, 1);
                h.getLevel().removeBlock(corner, false);
                m.onUpdateServer(); check(m.status() == 20 && !m.chamber().formed(), "Missing shell was accepted");
                h.getLevel().setBlockAndUpdate(corner, Content.CHAMBER_CASING.defaultState());
                h.getLevel().setBlockAndUpdate(m.chamber().center(), Blocks.STONE.defaultBlockState());
                check(!m.chamber().formed(), "Solid center was accepted");
                h.getLevel().setBlockAndUpdate(m.chamber().center(), Blocks.AIR.defaultBlockState());
                var p = port(m, corner, false);
                check(m.chamber().formed() && p.controller() == m, "Shell port failed to find rotated controller");
                check(count(m) == 0 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "Invalid structure spent resources");
            } finally { clean(m); }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ore_saved", timeoutTicks = 20)
    public static void weightedCostMixedAuraSavedProgressAndStructuralPause(GameTestHelper h) {
        AuraMachine m = machine(h);
        var original = new ArrayList<>(NaturesAuraAPI.OVERWORLD_ORES);
        try {
            NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/coal", 5000));
            feed(m, 2); m.auraTank().setStack(new ChemicalStack(Content.AURA, 5000));
            AuraTestEnvironment.set(m, 30, 3_000_000);
            for (int i = 0; i < 60; i++) m.onUpdateServer();
            check(m.progress() == .3 && m.chamber().auraCost() == 20_000 && count(m) == 0, "Wrong weighted cost or premature output");
            var savedTarget = m.chamber().target();
            m.loadAdditional(m.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            var corner = m.chamber().center().offset(1, 1, 1);
            h.getLevel().removeBlock(corner, false); long energy = m.energy().getEnergy();
            cycle(m); check(m.energy().getEnergy() == energy && m.progress() == .3, "Broken chamber consumed progress or FE");
            h.getLevel().setBlockAndUpdate(corner, Content.CHAMBER_CASING.defaultState());
            for (int i = 0; i < 140; i++) m.onUpdateServer();
            check(count(m) == 1 && m.getInventorySlots(null).get(2).getStack().is(savedTarget.getItem()), "Save/repair changed the selected ore");
            check(m.inputs.get(0).getCount() == 1 && OreChamberLogic.isPowder(m.inputs.get(1).getStack()), "Substrate or reusable powder cost wrong");
            check(m.auraTank().isEmpty() && IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), 30) == 2_985_000, "Aura sources did not total the native 20000 cost");
            check(m.energy().getMaxEnergy() - m.energy().getEnergy() == 200 * m.energy().getEnergyPerTick(), "Pause/resume charged incorrect FE");
            h.succeed();
        } finally { NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.addAll(original); clean(m); }
    }

    @GameTest(template = "empty", batch = "ore_blocked", timeoutTicks = 20)
    public static void environmentPowderSubstrateOutputPowerAndNativeDisableAreRequired(GameTestHelper h) {
        AuraMachine m = machine(h); boolean enabled = ModConfig.instance.oreEffect.get();
        try {
            feed(m, 1); m.auraTank().setStack(new ChemicalStack(Content.AURA, 100_000));
            AuraTestEnvironment.set(m, 30, 2_000_000); long energy = m.energy().getEnergy();
            m.onUpdateServer(); check(m.status() == 23, "Exact 2M threshold was accepted");
            AuraTestEnvironment.set(m, 30, 3_000_000);
            m.inputs.get(1).setStackUnchecked(ItemEffectPowder.setEffect(new ItemStack(ModItems.EFFECT_POWDER), ResourceLocation.parse("naturesaura:animal")));
            m.onUpdateServer(); check(m.status() == 22, "Wrong powder was accepted");
            ItemStack invalid = new ItemStack(ModItems.EFFECT_POWDER); invalid.set(ItemEffectPowder.Data.TYPE, new ItemEffectPowder.Data("bad:%%%"));
            check(!OreChamberLogic.isPowder(invalid), "Malformed powder component accepted");
            m.inputs.get(1).setStack(powder()); m.inputs.get(0).setStack(new ItemStack(Items.NETHERRACK));
            m.onUpdateServer(); check(m.status() == 22, "Nether substrate accepted in Overworld");
            feed(m, 1); m.getInventorySlots(null).subList(2, 6).forEach(s -> s.setStack(new ItemStack(Items.STONE, 64)));
            m.onUpdateServer(); check(m.status() == 3, "Full outputs accepted work");
            check(m.energy().getEnergy() == energy && m.auraTank().getStored() == 100_000, "Blocked work spent resources");
            m.getInventorySlots(null).subList(2, 6).forEach(s -> s.setStack(ItemStack.EMPTY));
            m.energy().setEnergy(0); m.onUpdateServer(); check(m.status() == 6 && count(m) == 0, "Unpowered chamber worked");
            m.energy().setEnergy(energy); ModConfig.instance.oreEffect.set(false); m.onUpdateServer();
            check(m.status() == 21 && m.energy().getEnergy() == energy, "Bypassed Nature's Aura oreEffect config");
            h.succeed();
        } finally { ModConfig.instance.oreEffect.set(enabled); clean(m); }
    }

    @GameTest(template = "empty", batch = "ore_reload", timeoutTicks = 20)
    public static void emptyTagsExceptionsReloadAndHugeWeightsRemainBounded(GameTestHelper h) {
        AuraMachine m = machine(h);
        var original = new ArrayList<>(NaturesAuraAPI.OVERWORLD_ORES);
        var exceptions = new java.util.HashSet<>(OreSpawnEffect.SPAWN_EXCEPTIONS);
        try {
            feed(m, 3); AuraTestEnvironment.set(m, 30, 3_000_000);
            NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/missing_test_tag", 10));
            m.onUpdateServer(); check(m.status() == 24 && m.progress() == 0, "Empty tag did not halt safely");
            NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/coal", 5000));
            for (var o : OreChamberLogic.ores(h.getLevel(), m.chamber().center()))
                if (o.item() instanceof net.minecraft.world.item.BlockItem block) OreSpawnEffect.SPAWN_EXCEPTIONS.add(block.getBlock().defaultBlockState());
            // Exclude both standard variants; a valid fallback in the tag must also be respected.
            OreSpawnEffect.SPAWN_EXCEPTIONS.add(Blocks.COAL_ORE.defaultBlockState());
            OreSpawnEffect.SPAWN_EXCEPTIONS.add(Blocks.DEEPSLATE_COAL_ORE.defaultBlockState());
            m.onUpdateServer(); check(m.status() == 24, "Native spawn exception bypassed");
            OreSpawnEffect.SPAWN_EXCEPTIONS.clear(); OreSpawnEffect.SPAWN_EXCEPTIONS.addAll(exceptions);
            for (int i = 0; i < 30; i++) m.onUpdateServer(); check(m.progress() == .15, "Valid weighted entry did not start");
            NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/iron", 3000));
            m.onUpdateServer(); check(m.progress() == .005 && m.chamber().auraCost() == 28_000, "Changed table reused old ore progress/cost");
            NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/coal", Integer.MAX_VALUE));
            NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/iron", Integer.MAX_VALUE));
            m.getComponent().addUpgrades(Upgrade.SPEED, 8); m.getComponent().addUpgrades(Upgrade.ENERGY, 8);
            m.auraTank().setStack(new ChemicalStack(Content.AURA, 100));
            cycle(m);
            check(count(m) == 1 && m.auraTank().getStored() == 99, "Large weights overflowed or generated Aura");
            h.succeed();
        } finally {
            NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.addAll(original);
            OreSpawnEffect.SPAWN_EXCEPTIONS.clear(); OreSpawnEffect.SPAWN_EXCEPTIONS.addAll(exceptions); clean(m);
        }
    }

    @GameTest(template = "empty", batch = "ore_dimensions", timeoutTicks = 20)
    public static void netherUsesNetherrackTableAndEndCannotRun(GameTestHelper h) {
        for (var dimension : List.of(Level.NETHER, Level.END)) {
            var level = h.getLevel().getServer().getLevel(dimension);
            level.getChunkAt(new BlockPos(4, 95, 4));
            AuraMachine m = machine(level, new BlockPos(4, 95, 4), Direction.EAST);
            var original = new ArrayList<>(NaturesAuraAPI.NETHER_ORES);
            try {
                AuraTestEnvironment.set(m, 30, 3_000_000);
                m.inputs.get(0).setStack(new ItemStack(Items.NETHERRACK)); m.inputs.get(1).setStack(powder());
                NaturesAuraAPI.NETHER_ORES.clear(); NaturesAuraAPI.NETHER_ORES.add(ore("ores/quartz", 8000));
                m.auraTank().setStack(new ChemicalStack(Content.AURA, 10_000)); cycle(m);
                if (dimension == Level.NETHER) {
                    check(count(m) == 1 && m.getInventorySlots(null).get(2).getStack().is(Items.NETHER_QUARTZ_ORE), "Nether ignored native substrate/table");
                    check(m.auraTank().getStored() == 2000, "Nether quartz native weight cost is not 8000");
                } else check(count(m) == 0 && m.status() == 21 && m.energy().getEnergy() == m.energy().getMaxEnergy(), "End bypassed dimension restriction");
            } finally { NaturesAuraAPI.NETHER_ORES.clear(); NaturesAuraAPI.NETHER_ORES.addAll(original); clean(m); }
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void portCapabilitiesPreserveResourcesAndRejectStaleControllers(GameTestHelper h) {
        AuraMachine m = machine(h);
        try {
            var input = port(m, m.chamber().center().west(), false);
            var output = port(m, m.chamber().center().east(), true);
            var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, input.getBlockPos(), Direction.WEST);
            var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, input.getBlockPos(), Direction.WEST);
            var aura = h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), input.getBlockPos(), Direction.WEST);
            m.energy().setEnergy(0);
            check(items.insertItem(0, new ItemStack(Items.STONE, 4), true).isEmpty() && m.inputs.get(0).isEmpty(), "Simulated port insertion mutated inventory");
            items.insertItem(0, new ItemStack(Items.STONE, 4), false); items.insertItem(1, powder(), false);
            check(items.extractItem(0, 1, false).isEmpty(), "Input port extracted feedstock");
            check(energy.receiveEnergy(1234, true) == 1234 && m.energy().getEnergy() == 0, "Simulated FE was consumed");
            check(energy.receiveEnergy(1234, false) == 1234, "Port FE did not reach controller");
            aura.insertChemical(new ChemicalStack(Content.AURA, 4567), Action.EXECUTE);
            check(m.auraTank().getStored() == 4567 && aura.extractChemical(1, Action.EXECUTE).isEmpty(), "Input port chemical direction is wrong");
            m.getInventorySlots(null).get(2).setStack(new ItemStack(Items.DIAMOND_ORE, 3));
            check(output.items.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1, "Output port accepted items");
            check(output.items.extractItem(0, 2, false).getCount() == 2 && count(m) == 1, "Output port duplicated or lost items");
            ItemStack drop = Block.getDrops(output.getBlockState(), h.getLevel(), output.getBlockPos(), output, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            check("true".equals(drop.get(DataComponents.BLOCK_STATE).properties().get("output")), "Dropped port lost output mode");
            var corner = m.chamber().center().offset(1, 1, 1); h.getLevel().removeBlock(corner, false);
            check(energy.receiveEnergy(10, false) == 0 && !items.insertItem(0, new ItemStack(Items.STONE), false).isEmpty()
                  && output.items.extractItem(0, 1, false).isEmpty(), "Cached handlers operated with a broken shell");
            h.getLevel().setBlockAndUpdate(corner, Content.CHAMBER_CASING.defaultState());
            check(output.items.extractItem(0, 1, false).getCount() == 1, "Repaired port did not reconnect");
            h.getLevel().removeBlock(m.getBlockPos(), false);
            check(energy.receiveEnergy(10, false) == 0 && aura.insertChemical(new ChemicalStack(Content.AURA, 10), Action.EXECUTE).getAmount() == 10,
                  "Removed controller remained accessible through a cached handler");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", batch = "ore_logistics", timeoutTicks = 350)
    public static void realHopperCableTubeAndTransporterOperateThroughChamberPorts(GameTestHelper h) {
        AuraMachine m = machine(h);
        var original = new ArrayList<>(NaturesAuraAPI.OVERWORLD_ORES);
        NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.add(ore("ores/coal", 5000));
        AuraTestEnvironment.set(m, 30, 3_000_000);
        m.energy().setEnergy(0);
        port(m, m.chamber().center().west(), false);
        port(m, m.chamber().center().east(), false);
        port(m, m.chamber().center().above(), false);
        port(m, m.chamber().center().south(), true);
        h.setBlock(new BlockPos(2, 1, 6), MekanismBlocks.BASIC_ENERGY_CUBE.get());
        var cube = (TileEntityEnergyCube) h.getBlockEntity(new BlockPos(2, 1, 6));
        cube.getEnergyContainers(null).getFirst().setEnergy(2_000_000);
        cube.getConfig().getConfig(TransmissionType.ENERGY).setDataType(DataType.OUTPUT, RelativeSide.fromDirections(cube.getDirection(), Direction.EAST));
        cube.getConfig().getConfig(TransmissionType.ENERGY).setEjecting(true);
        h.setBlock(new BlockPos(3, 1, 6), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.setBlock(new BlockPos(8, 1, 6), Content.MACHINES.get(MachineKind.AURA_GENERATOR).get());
        var generator = (AuraMachine) h.getBlockEntity(new BlockPos(8, 1, 6));
        generator.auraTank().setStack(new ChemicalStack(Content.AURA, 100_000));
        h.setBlock(new BlockPos(7, 1, 6), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        h.setBlock(new BlockPos(5, 3, 6), Blocks.HOPPER);
        var hopper = (HopperBlockEntity) h.getBlockEntity(new BlockPos(5, 3, 6));
        hopper.setItem(0, new ItemStack(Items.STONE)); hopper.setItem(1, powder());
        h.setBlock(new BlockPos(5, 1, 9), Blocks.CHEST);
        h.setBlock(new BlockPos(5, 1, 8), MekanismBlocks.BASIC_LOGISTICAL_TRANSPORTER.get());
        h.runAfterDelay(300, () -> {
            try {
                var chest = (ChestBlockEntity) h.getBlockEntity(new BlockPos(5, 1, 9));
                int stored = 0; for (int i = 0; i < chest.getContainerSize(); i++) stored += chest.getItem(i).getCount();
                check(stored == 1 && count(m) == 0, "Port output did not reach the chest through a real transporter; status=" + m.status() + ", progress=" + m.progress() + ", energy=" + m.energy().getEnergy() + ", cube=" + cube.getEnergyContainers(null).getFirst().getEnergy());
                check(m.inputs.get(0).isEmpty() && OreChamberLogic.isPowder(m.inputs.get(1).getStack()), "Hopper did not supply native powder and stone");
                check(m.auraTank().getStored() > 0 && generator.auraTank().getStored() < 100_000, "Real tube failed to supply port Aura");
                check(cube.getEnergyContainers(null).getFirst().getEnergy() < 2_000_000, "Real cable did not power chamber");
                h.succeed();
            } finally { NaturesAuraAPI.OVERWORLD_ORES.clear(); NaturesAuraAPI.OVERWORLD_ORES.addAll(original); clean(m); generator.energy().setEnergy(0); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void chamberCraftingRequiresTheOrePowderComponent(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "ore_condensation_chamber")).orElseThrow();
        var recipe = (net.minecraft.world.item.crafting.ShapedRecipe) holder.value();
        var alloy = new ItemStack(mekanism.common.registries.MekanismItems.ATOMIC_ALLOY.get());
        var circuit = new ItemStack(mekanism.common.registries.MekanismItems.ELITE_CONTROL_CIRCUIT.get());
        var casing = new ItemStack(Content.CHAMBER_CASING);
        var ingredients = new ArrayList<>(List.of(alloy, circuit, alloy, casing, powder(), casing, alloy, circuit, alloy));
        check(recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, ingredients), h.getLevel()), "Valid chamber recipe failed");
        ingredients.set(4, ItemEffectPowder.setEffect(new ItemStack(ModItems.EFFECT_POWDER), ResourceLocation.parse("naturesaura:animal")));
        check(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, ingredients), h.getLevel()), "Other powder bypassed the ore component ingredient");
        h.succeed();
    }
}
