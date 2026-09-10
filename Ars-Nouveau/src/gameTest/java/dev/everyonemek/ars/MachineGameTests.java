package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.capabilities.Capabilities;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsMekanism.ID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {
    private static void check(boolean value, String message) {
        if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(message);
    }
    private static SourceMachine machine(GameTestHelper h, MachineKind kind, int x, int z) {
        BlockPos pos = new BlockPos(x, 1, z);
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, Content.MACHINES.get(kind).defaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var m = (SourceMachine) h.getBlockEntity(pos);
        m.energy().setEnergy(m.energy().getMaxEnergy());
        return m;
    }
    private static void source(SourceMachine m, long amount) { m.sourceTank().setStack(new ChemicalStack(Content.SOURCE, amount)); }
    private static void stop(SourceMachine m) { m.energy().setEnergy(0); }
    private static void cycle(SourceMachine m) {
        int ticks = m.kind() == MachineKind.IMBUEMENT_CHAMBER ? MachineConfig.IMBUEMENT_TICKS.get() : MachineConfig.APPARATUS_TICKS.get();
        for (int i = 0; i < MekanismUtils.getTicks(m, ticks); i++) m.onUpdateServer();
    }
    private static int output(SourceMachine m, net.minecraft.world.level.ItemLike item) {
        return m.outputs().stream().filter(s -> s.getStack().is(item.asItem())).mapToInt(s -> s.getCount()).sum();
    }
    private static void water(SourceMachine m, int count) {
        m.inputs.get(0).setStack(new ItemStack(ItemsRegistry.SOURCE_GEM, count));
        m.inputs.get(1).setStack(new ItemStack(Items.WATER_BUCKET));
        m.inputs.get(2).setStack(new ItemStack(Items.SNOW_BLOCK));
        m.inputs.get(3).setStack(new ItemStack(Items.KELP));
        check(m.setRecipeLock("ars_nouveau:imbuement_water_essence"), "Native water recipe unavailable");
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void generatorChargesExactPartialPowerAndUsesMekUpgrades(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.SOURCE_GENERATOR, 5, 5);
        try {
            m.energy().setEnergy(0); m.onUpdateServer();
            check(m.sourceTank().isEmpty() && m.status() == SourceMachine.NEED_ENERGY, "Unpowered generator created Source");
            m.energy().setEnergy(m.energy().getMaxEnergy()); source(m, SourceMachine.SOURCE_CAPACITY - 3);
            long before = m.energy().getEnergy(); m.onUpdateServer();
            long expected = (long) Math.ceil(m.energy().getEnergyPerTick() * 3D / MachineConfig.GENERATOR_RATE.get());
            check(m.sourceTank().getStored() == SourceMachine.SOURCE_CAPACITY && before - m.energy().getEnergy() == expected,
                  "Partial generation did not pay the exact proportional energy");
            before = m.energy().getEnergy(); m.onUpdateServer();
            check(m.status() == SourceMachine.OUTPUT_FULL && m.energy().getEnergy() == before, "Full generator wasted energy");
            source(m, 0); m.getComponent().addUpgrades(Upgrade.SPEED, 2); m.getComponent().addUpgrades(Upgrade.ENERGY, 2);
            before = m.energy().getEnergy(); m.onUpdateServer();
            check(m.sourceTank().getStored() == (long) MachineConfig.GENERATOR_RATE.get() * MekanismUtils.getOperationsPerTick(m, 1, 1), "Speed upgrades ignored");
            check(before - m.energy().getEnergy() == m.energy().getEnergyPerTick(), "Upgraded power accounting differs from Mek");
            h.succeed();
        } finally { stop(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void converterMenuRotatedFacesAndBothDirectionsConserveSource(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.SOURCE_CONVERTER, 5, 5);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var oldPosition = player.position(); var oldMenu = player.containerMenu;
        try {
            h.getLevel().setBlockAndUpdate(m.getBlockPos(), m.getBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
            player.setPos(m.getBlockPos().getCenter());
            var menu = new MachineMenu(73, player.getInventory(), m); player.containerMenu = menu;
            var jarPos = m.getBlockPos().east(); h.getLevel().setBlockAndUpdate(jarPos, BlockRegistry.SOURCE_JAR.defaultBlockState());
            var jar = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, jarPos, Direction.WEST);
            check(jar != null, "Real Ars jar capability missing");
            jar.setSource(9_950); source(m, 75); m.energy().setEnergy(0); m.onUpdateServer();
            check(jar.getSource() == 9_950 && m.sourceTank().getStored() == 75, "Simulation or unpowered conversion moved Source");
            m.energy().setEnergy(m.energy().getMaxEnergy()); long before = m.energy().getEnergy(); m.onUpdateServer();
            check(jar.getSource() == 10_000 && m.sourceTank().getStored() == 25, "Partial import did not conserve exactly 50 Source");
            check(before - m.energy().getEnergy() == (long) Math.ceil(m.energy().getEnergyPerTick() * 50D / MachineConfig.CONVERTER_RATE.get()), "Partial import energy incorrect");
            Direction tubeSide = Direction.WEST;
            var cached = h.getLevel().getCapability(Capabilities.CHEMICAL.block(), m.getBlockPos(), tubeSide);
            check(cached != null && cached.insertChemical(new ChemicalStack(Content.SOURCE, 10), Action.SIMULATE).isEmpty(),
                  "Import mode does not accept tube Source");
            check(m.sourceTank().getStored() == 25, "Chemical simulation mutated the buffer");
            check(menu.clickMenuButton(player, 0) && m.mode() == 1, "Menu failed to select export");
            check(cached.insertChemical(new ChemicalStack(Content.SOURCE, 10), Action.EXECUTE).getAmount() == 10, "Cached handler still accepts input after export selected");
            check(h.getLevel().getCapability(Capabilities.CHEMICAL.block(), m.getBlockPos(), Direction.EAST) == null, "Ars face also exposed a tube capability");
            m.onUpdateServer();
            check(jar.getSource() == 9_800 && m.sourceTank().getStored() == 225, "Export conversion duplicated or lost Source");
            check(menu.clickMenuButton(player, 0) && m.mode() == 2, "Menu failed to disable conversion");
            before = m.energy().getEnergy(); m.onUpdateServer();
            check(jar.getSource() == 9_800 && before == m.energy().getEnergy(), "Disabled converter transferred resources");
            check(menu.clickMenuButton(player, 1), "Ars face control failed");
            check(m.getConfig().getConfig(TransmissionType.CHEMICAL).getDataType(m.arsSide()) == DataType.NONE, "New Ars face was not reserved");
            h.succeed();
        } finally { player.containerMenu = oldMenu; player.setPos(oldPosition); stop(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void imbuementRetainsCatalystsAndWaitsForCompleteResources(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.IMBUEMENT_CHAMBER, 5, 5);
        try {
            water(m, 2); source(m, 1_999); long before = m.energy().getEnergy(); m.onUpdateServer();
            check(m.status() == SourceMachine.NEED_SOURCE && m.progress() == 0 && m.energy().getEnergy() == before, "Incomplete Source started processing");
            source(m, 4_000); m.energy().setEnergy(0); m.onUpdateServer();
            check(m.status() == SourceMachine.NEED_ENERGY && m.sourceTank().getStored() == 4_000, "Unpowered recipe spent Source");
            m.energy().setEnergy(m.energy().getMaxEnergy()); before = m.energy().getEnergy();
            for (var slot : m.outputs()) slot.setStack(new ItemStack(Items.COBBLESTONE, 64));
            m.onUpdateServer(); check(m.status() == SourceMachine.OUTPUT_FULL && m.energy().getEnergy() == before, "Full output spent energy");
            m.outputs().get(1).setStack(new ItemStack(ItemsRegistry.WATER_ESSENCE, 63));
            m.outputs().get(0).setStack(ItemStack.EMPTY); cycle(m);
            check(m.outputs().get(1).getCount() == 64 && m.outputs().get(0).isEmpty(), "Output failed to fill the compatible stack first");
            cycle(m);
            check(output(m, ItemsRegistry.WATER_ESSENCE) == 65 && m.inputs.get(0).isEmpty() && m.sourceTank().isEmpty(), "Two imbuements did not consume exactly two gems and 4000 Source");
            check(m.inputs.get(1).getStack().is(Items.WATER_BUCKET) && m.inputs.get(2).getCount() == 1 && m.inputs.get(3).getCount() == 1, "Imbuement consumed its catalysts");
            check(before - m.energy().getEnergy() == 2L * MachineConfig.IMBUEMENT_TICKS.get() * m.energy().getEnergyPerTick(), "Imbuement cycle energy incorrect");
            h.succeed();
        } finally { stop(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void apparatusCountsMaterialsPreservesComponentsAndReturnsContainers(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.ENCHANTING_APPARATUS, 5, 5);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved name")); sword.setDamageValue(42);
            m.inputs.get(0).setStack(sword); m.inputs.get(1).setStack(new ItemStack(Items.GOLD_INGOT));
            m.inputs.get(2).setStack(new ItemStack(Items.WATER_BUCKET)); m.inputs.get(3).setStack(new ItemStack(ItemsRegistry.SPELL_PARCHMENT));
            source(m, 500); check(m.setRecipeLock("arsmekanism:component_contract"), "Test recipe unavailable");
            m.onUpdateServer(); check(m.progress() == 0 && m.status() == SourceMachine.MISSING_MATERIALS, "One ingot satisfied two pedestal ingredients");
            m.inputs.get(1).setStack(new ItemStack(Items.GOLD_INGOT, 2));
            var plan = RecipeAdapter.find(m);
            check(plan != null && m.inputs.get(0).getStack().getDamageValue() == 42, "Recipe preview mutated the target");
            cycle(m);
            check(m.inputs.get(0).isEmpty() && m.inputs.get(1).isEmpty() && m.inputs.get(2).isEmpty(), "Apparatus input quantities incorrect");
            check(m.inputs.get(3).getCount() == 1 && output(m, Items.BUCKET) == 1, "Parchment was consumed or bucket was lost");
            ItemStack result = m.outputs().get(0).getStack();
            check(result.is(Items.DIAMOND_SWORD) && result.getDamageValue() == 0 && Component.literal("Preserved name").equals(result.get(DataComponents.CUSTOM_NAME)), "Native component-preserving assembly was not used");
            check(m.sourceTank().getStored() == 250, "Apparatus Source cost differs from recipe");
            h.succeed();
        } finally { stop(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void enchantmentProducesActualOutputAndRejectsSkippedLevels(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.ENCHANTING_APPARATUS, 5, 5);
        try {
            m.cycleMode(); m.inputs.get(0).setStack(new ItemStack(Items.BOOK));
            m.inputs.get(1).setStack(new ItemStack(ItemsRegistry.ABJURATION_ESSENCE, 2));
            m.inputs.get(2).setStack(new ItemStack(Items.DIAMOND, 3));
            m.inputs.get(3).setStack(new ItemStack(BlockRegistry.SOURCE_GEM_BLOCK, 3));
            m.inputs.get(4).setStack(new ItemStack(Items.LAPIS_BLOCK, 2)); source(m, 5_500);
            check(m.setRecipeLock("ars_nouveau:unbreaking_2"), "Native enchantment unavailable");
            long before = m.energy().getEnergy(); m.onUpdateServer();
            check(m.progress() == 0 && m.energy().getEnergy() == before, "Plain book skipped directly to level two");
            m.setRecipeLock("ars_nouveau:unbreaking_1"); cycle(m);
            var enchantment = h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
            ItemStack book = m.outputs().get(0).extractItem(1, Action.EXECUTE, AutomationType.MANUAL);
            check(book.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(book).getLevel(enchantment) == 1, "Enchantment produced an empty or incorrect result");
            m.inputs.get(0).setStack(book); m.setRecipeLock("ars_nouveau:unbreaking_2"); cycle(m);
            check(EnchantmentHelper.getEnchantmentsForCrafting(m.outputs().get(0).getStack()).getLevel(enchantment) == 2, "Enchantment did not advance from level one");
            check(m.sourceTank().isEmpty() && m.inputs.stream().allMatch(s -> s.isEmpty()), "Two native enchantments consumed incorrect resources");
            h.succeed();
        } finally { stop(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void worldProgressAndAllDroppedMachineContainersSurviveRoundTrip(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.IMBUEMENT_CHAMBER, 5, 5);
        try {
            water(m, 1); source(m, 2_000);
            for (int i = 0; i < 23; i++) m.onUpdateServer();
            var restored = new SourceMachine(m.getBlockPos(), m.getBlockState()); restored.setLevel(h.getLevel());
            restored.loadAdditional(m.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            check(restored.progress() == m.progress() && restored.progress() > 0, "World save lost processing progress");
            h.getLevel().setBlockEntity(restored); stop(m); m = restored;
            for (int i = 23; i < MachineConfig.IMBUEMENT_TICKS.get(); i++) m.onUpdateServer();
            check(output(m, ItemsRegistry.WATER_ESSENCE) == 1 && m.sourceTank().isEmpty(), "Saved work did not resume with exact remaining time");
        } finally { stop(m); }
        for (MachineKind kind : MachineKind.values()) {
            SourceMachine original = machine(h, kind, 5, 5);
            try {
                if (kind.usesSource()) source(original, 1_234);
                original.getComponent().addUpgrades(Upgrade.SPEED, 2); original.getComponent().addUpgrades(Upgrade.ENERGY, 2);
                if (kind == MachineKind.IMBUEMENT_CHAMBER) water(original, 3);
                if (kind == MachineKind.ENCHANTING_APPARATUS) original.inputs.get(0).setStack(new ItemStack(Items.BOOK, 3));
                if (kind == MachineKind.SOURCE_CONVERTER) { original.cycleMode(); original.cycleArsSide(); }
                if (kind == MachineKind.ENCHANTING_APPARATUS) original.cycleMode();
                ItemStack drop = Block.getDrops(original.getBlockState(), h.getLevel(), original.getBlockPos(), original, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
                drop = ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow();
                var restored = new SourceMachine(original.getBlockPos(), original.getBlockState()); restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(drop);
                check(restored.sourceTank().getStored() == (kind.usesSource() ? 1_234 : 0) && restored.energy().getEnergy() == original.energy().getEnergy(), "Dropped " + kind + " lost Source or energy");
                check(restored.getComponent().getUpgrades(Upgrade.SPEED) == 2 && restored.getComponent().getUpgrades(Upgrade.ENERGY) == 2, "Dropped upgrades lost");
                check(restored.mode() == original.mode() && restored.arsSide() == original.arsSide() && restored.recipeLock().equals(original.recipeLock()), "Dropped settings lost");
                for (int i = 0; i < original.getInventorySlots(null).size(); i++)
                    check(ItemStack.matches(restored.getInventorySlots(null).get(i).getStack(), original.getInventorySlots(null).get(i).getStack()), "Dropped inventory order differs at slot " + i);
            } finally { stop(original); }
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void recipeLockPacketValidatesContainerDistanceAndSelectedMode(GameTestHelper h) {
        SourceMachine m = machine(h, MachineKind.IMBUEMENT_CHAMBER, 5, 5);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var oldPosition = player.position(); var oldMenu = player.containerMenu;
        try {
            player.setPos(m.getBlockPos().getCenter()); var menu = new MachineMenu(29, player.getInventory(), m); player.containerMenu = menu;
            new SetRecipeLockPayload(28, "ars_nouveau:imbuement_water_essence").handle(player);
            check(m.recipeLock().isEmpty(), "Wrong container ID applied a lock");
            var packet = new SetRecipeLockPayload(29, "ars_nouveau:imbuement_water_essence");
            var buffer = io.netty.buffer.Unpooled.buffer();
            try { SetRecipeLockPayload.STREAM_CODEC.encode(buffer, packet); SetRecipeLockPayload.STREAM_CODEC.decode(buffer).handle(player); }
            finally { buffer.release(); }
            check(m.recipeLock().equals(packet.recipe()), "Valid encoded recipe lock did not apply");
            new SetRecipeLockPayload(29, "ars_nouveau:unbreaking_1").handle(player);
            check(m.recipeLock().equals(packet.recipe()), "Unsupported recipe changed confirmed selection");
            player.setPos(m.getBlockPos().getCenter().add(9, 0, 0)); new SetRecipeLockPayload(29, "").handle(player);
            check(m.recipeLock().equals(packet.recipe()), "Distant player changed machine settings");
            h.succeed();
        } finally { player.containerMenu = oldMenu; player.setPos(oldPosition); stop(m); }
    }

    private static void configureItemSide(SourceMachine m, RelativeSide side, DataType target) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft((ServerLevel) m.getLevel());
        var context = (net.neoforged.neoforge.network.handling.IPayloadContext) java.lang.reflect.Proxy.newProxyInstance(
              MachineGameTests.class.getClassLoader(), new Class[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},
              (proxy, method, args) -> { if (method.getName().equals("player")) return player; throw new UnsupportedOperationException(method.getName()); });
        for (int i = 0; i < 12 && m.getConfig().getConfig(TransmissionType.ITEM).getDataType(side) != target; i++)
            new mekanism.common.network.to_server.configuration_update.PacketSideData(m.getBlockPos(), mekanism.common.network.MekClickType.LEFT, side, TransmissionType.ITEM).handle(context);
        check(m.getConfig().getConfig(TransmissionType.ITEM).getDataType(side) == target, "Side configuration packet did not select " + target);
    }

    @GameTest(template = "empty", batch = "ars_logistics", timeoutTicks = 370)
    public static void realCableTubeHopperAndTransporterProduceWaterEssenceInChest(GameTestHelper h) {
        SourceMachine generator = machine(h, MachineKind.SOURCE_GENERATOR, 4, 4); stop(generator);
        SourceMachine m = machine(h, MachineKind.IMBUEMENT_CHAMBER, 6, 4); stop(m); water(m, 1); m.inputs.get(0).setStack(ItemStack.EMPTY);
        configureItemSide(m, RelativeSide.TOP, DataType.EXTRA);
        configureItemSide(m, RelativeSide.fromDirections(m.getDirection(), Direction.EAST), DataType.OUTPUT);
        h.setBlock(new BlockPos(2, 1, 5), MekanismBlocks.BASIC_ENERGY_CUBE.get());
        var cube = (TileEntityEnergyCube) h.getBlockEntity(new BlockPos(2, 1, 5));
        cube.getEnergyContainers(null).getFirst().setEnergy(2_000_000);
        cube.getConfig().getConfig(TransmissionType.ENERGY).setDataType(DataType.OUTPUT, RelativeSide.fromDirections(cube.getDirection(), Direction.EAST));
        cube.getConfig().getConfig(TransmissionType.ENERGY).setEjecting(true);
        for (int x = 3; x <= 6; x++) h.setBlock(new BlockPos(x, 1, 5), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.setBlock(new BlockPos(5, 1, 4), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        h.setBlock(new BlockPos(6, 2, 4), Blocks.HOPPER);
        ((HopperBlockEntity) h.getBlockEntity(new BlockPos(6, 2, 4))).setItem(0, new ItemStack(ItemsRegistry.SOURCE_GEM));
        h.setBlock(new BlockPos(7, 1, 4), MekanismBlocks.BASIC_LOGISTICAL_TRANSPORTER.get());
        h.setBlock(new BlockPos(8, 1, 4), Blocks.CHEST);
        h.runAfterDelay(340, () -> {
            try {
                var chest = (ChestBlockEntity) h.getBlockEntity(new BlockPos(8, 1, 4)); int count = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(ItemsRegistry.WATER_ESSENCE.get())) count += chest.getItem(i).getCount();
                check(count == 1 && output(m, ItemsRegistry.WATER_ESSENCE) == 0,
                      "Real logistics did not deliver essence; status=" + m.status() + ", progress=" + m.progress() + ", energy=" + m.energy().getEnergy() + ", source=" + m.sourceTank().getStored());
                check(m.inputs.get(0).isEmpty() && m.inputs.get(1).getStack().is(Items.WATER_BUCKET), "Hopper target or retained catalyst incorrect");
                check(m.sourceTank().getStored() > 0 && cube.getEnergyContainers(null).getFirst().getEnergy() < 2_000_000, "Cable or pressurized tube did not transfer resources");
                h.succeed();
            } finally { cube.getEnergyContainers(null).getFirst().setEnergy(0); stop(m); stop(generator); }
        });
    }
}
