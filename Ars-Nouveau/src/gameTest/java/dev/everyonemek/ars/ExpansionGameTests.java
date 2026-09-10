package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.block.tile.*;
import com.hollingsworth.arsnouveau.common.entity.EntityDrygmy;
import com.hollingsworth.arsnouveau.common.entity.Whirlisprig;
import com.hollingsworth.arsnouveau.common.items.ExperienceGem;
import com.hollingsworth.arsnouveau.common.items.RitualTablet;
import com.hollingsworth.arsnouveau.common.ritual.RitualOvergrowth;
import com.hollingsworth.arsnouveau.common.util.PotionUtil;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ExpansionGameTests {
    private static final BlockPos MACHINE = new BlockPos(5, 1, 5);
    private static void check(boolean value, String message) { if (!value) throw new GameTestAssertException(message); }
    private static SourceMachine machine(GameTestHelper h, MachineKind kind) {
        h.setBlock(MACHINE, Content.MACHINES.get(kind).defaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var m = (SourceMachine) h.getBlockEntity(MACHINE); m.energy().setEnergy(m.energy().getMaxEnergy()); return m;
    }
    private static void source(SourceMachine m, int amount) { m.sourceTank().setStack(new ChemicalStack(Content.SOURCE, amount)); }
    private static void cycle(SourceMachine m) {
        int ticks = MekanismUtils.getTicks(m, MachineConfig.PROCESS_TICKS.get(m.kind()).get());
        for (int i = 0; i < ticks; i++) m.onUpdateServer();
    }
    private static int count(SourceMachine m, net.minecraft.world.level.ItemLike item) {
        return m.outputs().stream().filter(s -> s.getStack().is(item.asItem())).mapToInt(s -> s.getCount()).sum();
    }
    private static void clearOutput(SourceMachine m) { m.outputs().forEach(s -> s.setStack(ItemStack.EMPTY)); }
    private static PotionJarTile potionJar(GameTestHelper h, SourceMachine m, RelativeSide side) {
        BlockPos pos = m.getBlockPos().relative(side.getDirection(m.getDirection()));
        h.getLevel().setBlockAndUpdate(pos, BlockRegistry.POTION_JAR.defaultBlockState());
        return (PotionJarTile) h.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void extractionReturnsContainersAndHeatProcessingConsumesActualMaterials(GameTestHelper h) {
        var m = machine(h, MachineKind.SOURCE_EXTRACTOR);
        try {
            ItemStack stew = new ItemStack(Items.MUSHROOM_STEW); m.inputs.get(0).setStack(stew.copy());
            int expected = new MycelialSourcelinkTile(m.getBlockPos(), BlockRegistry.MYCELIAL_BLOCK.defaultBlockState()).getSourceValue(stew);
            cycle(m);
            check(m.sourceTank().getStored() == expected && count(m, Items.BOWL) == 1 && m.inputs.get(0).isEmpty(), "Food Source or bowl return differs from Ars");
            m.cycleMode(); ItemStack fuel = new ItemStack(BlockRegistry.BLAZING_LOG.asItem());
            var nativeLink = new VolcanicSourcelinkTile(m.getBlockPos(), BlockRegistry.VOLCANIC_BLOCK.defaultBlockState());
            int fuelExpected = nativeLink.getSourceValue(fuel);
            m.inputs.get(0).setStack(fuel); cycle(m);
            check(m.sourceTank().getStored() == expected + fuelExpected && m.heat() == 6, "Fuel Source or accumulated heat differs from Ars");
            m.cycleMode(); m.inputs.get(1).setStack(new ItemStack(Items.STONE)); long power = m.energy().getEnergy(); cycle(m);
            check(m.status() == SourceMachine.NEED_HEAT && m.energy().getEnergy() == power && count(m, Items.MAGMA_BLOCK) == 0, "Heat processing ran without heat");
            var tag = m.saveWithoutMetadata(h.getLevel().registryAccess()); tag.getCompound("machine_settings").putInt("heat", 350);
            m.loadAdditional(tag, h.getLevel().registryAccess()); cycle(m);
            check(count(m, Items.MAGMA_BLOCK) == 1 && m.heat() == 200, "Stone did not pay 150 heat");
            clearOutput(m); m.inputs.get(1).setStack(new ItemStack(Items.MAGMA_BLOCK)); m.inputs.get(2).setStack(new ItemStack(Items.BUCKET)); cycle(m);
            check(count(m, Items.LAVA_BUCKET) == 1 && m.heat() == 0 && m.inputs.get(1).isEmpty() && m.inputs.get(2).isEmpty(), "Lava did not consume magma, bucket and 200 heat");
            h.succeed();
        } finally { m.energy().setEnergy(0); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void crushingReservesAllPossibleProductsBeforeSpendingPowerOrInput(GameTestHelper h) {
        var m = machine(h, MachineKind.MAGIC_CRUSHER);
        try {
            check(m.setRecipeLock("arsmekanism:expansion_crush"), "Crush recipe was not registered");
            m.inputs.get(0).setStack(new ItemStack(Items.RAW_IRON));
            for (var slot : m.outputs()) slot.setStack(new ItemStack(Items.COBBLESTONE, 64));
            m.outputs().get(0).setStack(new ItemStack(Items.GOLD_NUGGET, 63));
            long power = m.energy().getEnergy(); cycle(m);
            check(m.energy().getEnergy() == power && m.inputs.get(0).getCount() == 1 && m.progress() == 0, "Blocked random recipe spent input or energy");
            clearOutput(m); cycle(m);
            check(count(m, Items.GOLD_NUGGET) >= 1 && count(m, Items.GOLD_NUGGET) <= 3 && count(m, Items.IRON_NUGGET) == 1 && m.inputs.get(0).isEmpty(), "Native random outputs or input count incorrect");
            check(h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), m.getBlockPos(), Direction.UP) == null, "FE-only crusher exposed an unused Source port");
            h.succeed();
        } finally { m.energy().setEnergy(0); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void scribingChargesXPChecksBookAndPreservesSavedExperience(GameTestHelper h) {
        var m = machine(h, MachineKind.GLYPH_SCRIBE);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var position = player.position(); var menu = player.containerMenu;
        int level = player.experienceLevel, total = player.totalExperience; float progress = player.experienceProgress;
        try {
            player.experienceLevel = 0; player.experienceProgress = 0; player.totalExperience = 0; player.giveExperiencePoints(125);
            player.setPos(m.getBlockPos().getCenter()); var machineMenu = new MachineMenu(44, player.getInventory(), m); player.containerMenu = machineMenu;
            check(machineMenu.clickMenuButton(player, 4) && m.experience() == 100 && ScribesTile.getTotalPlayerExperience(player) == 25, "Experience deposit duplicated or miscounted points");
            player.setPos(m.getBlockPos().getCenter().add(9, 0, 0));
            check(!machineMenu.clickMenuButton(player, 4) && m.experience() == 100, "Distant player deposited experience");
            player.setPos(m.getBlockPos().getCenter());
            check(m.setRecipeLock("arsmekanism:expansion_glyph"), "Glyph recipe missing");
            m.inputs.get(1).setStack(new ItemStack(Items.GOLD_INGOT, 2)); m.inputs.get(2).setStack(new ItemStack(Items.WATER_BUCKET));
            cycle(m); check(m.status() == SourceMachine.NEED_BOOK && m.experience() == 100, "Missing spellbook was ignored");
            m.inputs.get(0).setStack(new ItemStack(ItemsRegistry.NOVICE_SPELLBOOK));
            var higher = AdvancedRecipes.glyphs(h.getLevel()).stream().filter(recipe -> recipe.value().getSpellPart().getConfigTier().value > 1).findFirst().orElseThrow();
            m.setRecipeLock(higher.id().toString()); m.onUpdateServer();
            check(m.status() == SourceMachine.BOOK_TIER_LOW && m.experience() == 100, "Low-tier spellbook crafted a higher-tier glyph");
            m.setRecipeLock(""); long beforeSelection = m.energy().getEnergy(); cycle(m);
            check(m.status() == SourceMachine.NEED_SELECTION && m.experience() == 100 && m.energy().getEnergy() == beforeSelection,
                  "Scribe crafted an unselected glyph or spent resources before selection");
            var choice = RecipeChoices.available(h.getLevel(), MachineKind.GLYPH_SCRIBE, 0).stream()
                  .filter(c -> c.id().toString().equals("arsmekanism:expansion_glyph")).findFirst().orElseThrow();
            check(!choice.icon().isEmpty() && choice.experience() == 21 && choice.tier() == 1 && choice.materials().size() == 3,
                  "Glyph chooser lost its icon, book tier, XP cost or materials");
            new SetRecipeLockPayload(machineMenu.containerId, choice.id().toString()).handle(player);
            check(m.recipeLock().equals(choice.id().toString()), "Choosing a displayed glyph did not reach the server");
            m.setRecipeLock("arsmekanism:expansion_glyph"); cycle(m);
            check(m.experience() == 79 && m.inputs.get(0).getCount() == 1 && m.inputs.get(1).isEmpty() && count(m, Items.BUCKET) == 1, "Glyph resources or spellbook retention incorrect");
            var gem = new ItemStack(ItemsRegistry.EXPERIENCE_GEM);
            var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, m.getBlockPos(), Direction.UP);
            check(handler != null && handler.insertItem(0, gem, true).isEmpty() && m.experience() == 79, "Experience gem input simulation changed the buffer");
            check(handler.insertItem(0, gem, false).isEmpty(), "Experience gem port rejected input"); m.onUpdateServer();
            int expected = 79 + ((ExperienceGem) gem.getItem()).getValue();
            check(m.experience() == expected, "Experience gem conversion differs from Ars");
            ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            var restored = new SourceMachine(m.getBlockPos(), m.getBlockState()); restored.setLevel(h.getLevel());
            restored.applyComponentsFromItemStack(ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow());
            check(restored.experience() == expected && restored.inputs.get(0).getStack().is(ItemsRegistry.NOVICE_SPELLBOOK.get()), "Dropped scribe lost experience or spellbook");
            h.succeed();
        } finally {
            player.containerMenu = menu; player.setPos(position); player.experienceLevel = level; player.experienceProgress = progress; player.totalExperience = total; m.energy().setEnergy(0);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void ritualMenuAcceptsEveryRegisteredTabletAndFeedsMultipleNativeAugments(GameTestHelper h) {
        var m = machine(h, MachineKind.RITUAL_CONTROLLER);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
              new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ritual-tester"));
        player.setPos(m.getBlockPos().getCenter());
        // Make UUID lookup real without a synthetic network login (Curios requires negotiated payloads).
        h.getLevel().addNewPlayer(player);
        BlockPos pos = m.getBlockPos().north(); h.getLevel().setBlockAndUpdate(pos, BlockRegistry.RITUAL_BLOCK.defaultBlockState());
        var brazier = (RitualBrazierTile) h.getLevel().getBlockEntity(pos);
        try {
            player.setPos(m.getBlockPos().getCenter());
            var menu = new MachineMenu(71, player.getInventory(), m); player.containerMenu = menu;
            var tabletSlot = menu.slots.stream().filter(s -> s.x == 104 && s.y == 39).findFirst().orElseThrow();
            var augmentSlot = menu.slots.stream().filter(s -> s.x == 104 && s.y == 60).findFirst().orElseThrow();
            var tablets = com.hollingsworth.arsnouveau.api.registry.RitualRegistry.getRitualItemMap().values();
            check(tablets.size() > 3, "Ritual catalog was not populated");
            for (var item : tablets)
                check(tabletSlot.mayPlace(new ItemStack(item)), "Menu rejected registered tablet " + item.ritual.getRegistryName());
            var tablet = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof RitualTablet t
                  && t.ritual instanceof com.hollingsworth.arsnouveau.common.ritual.RitualWildenSummoning).findFirst().orElseThrow();
            menu.setCarried(new ItemStack(tablet));
            menu.clicked(tabletSlot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && m.inputs.getFirst().getStack().is(tablet), "Mouse placement rejected the Wilden tablet");
            m.cycleMode(); m.cycleMode(); cycle(m); cycle(m);
            check(brazier.ritual != null && !brazier.ritual.isRunning() && m.status() == SourceMachine.WAITING_START,
                  "Manual preparation started the ritual before augments");
            for (var item : new net.minecraft.world.level.ItemLike[]{ItemsRegistry.WILDEN_SPIKE, ItemsRegistry.WILDEN_HORN, ItemsRegistry.WILDEN_WING}) {
                check(augmentSlot.mayPlace(new ItemStack(item)), "Menu rejected a Wilden augment");
                menu.setCarried(new ItemStack(item)); menu.clicked(augmentSlot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                cycle(m); cycle(m);
                check(menu.getCarried().isEmpty() && m.inputs.get(1).isEmpty() && brazier.ritual.didConsumeItem(item)
                      && !brazier.ritual.isRunning(), "Native augment was not consumed exactly once before manual start");
            }
            player.setPos(m.getBlockPos().getCenter().add(9, 0, 0));
            check(!menu.clickMenuButton(player, 7), "Distant player started a ritual");
            player.setPos(m.getBlockPos().getCenter()); check(menu.clickMenuButton(player, 7), "Start button rejected prepared ritual"); cycle(m);
            check(brazier.ritual.isRunning() && brazier.ritual.getConsumedItems().size() == 3
                  && player.getUUID().equals(brazier.ritual.playerUUID), "Ritual did not retain all augments or the starting player");
            h.succeed();
        } finally {
            brazier.ritual = null; m.energy().setEnergy(0);
            h.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    @GameTest(template = "empty", batch = "ars_external_source", timeoutTicks = 260)
    public static void drygmyUsesNearbyJarWithoutInternalSourceAndStillCollects(GameTestHelper h) {
        var m = machine(h, MachineKind.DRYGMY_STATION);
        BlockPos pos = m.getBlockPos().north(); h.getLevel().setBlockAndUpdate(pos, BlockRegistry.DRYGMY_BLOCK.defaultBlockState());
        var hive = (DrygmyTile) h.getLevel().getBlockEntity(pos); hive.converted = true;
        // The player's existing supply may be beside the henge, with no jar atop the controller.
        BlockPos jarPos = pos.west(2); h.getLevel().setBlockAndUpdate(jarPos, BlockRegistry.SOURCE_JAR.defaultBlockState());
        var jar = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, jarPos, Direction.UP);
        jar.receiveSource(5_000, false);
        var drygmy = new EntityDrygmy(h.getLevel(), true); drygmy.homePos = pos; drygmy.setPos(pos.above().getCenter()); drygmy.setNoAi(true); h.getLevel().addFreshEntity(drygmy);
        var chicken = EntityType.CHICKEN.create(h.getLevel()); chicken.setPos(pos.north(2).getCenter()); chicken.setNoAi(true); h.getLevel().addFreshEntity(chicken);
        var chest = outputChest(h, m); hive.needsMana = true; hive.progress = hive.getMaxProgress();
        m.onUpdateServer();
        check(m.status() == SourceMachine.IDLE && jar.getSource() == 5_000 && m.sourceTank().isEmpty(),
              "Existing native supply was reported missing or drained by the status lookup");
        h.runAfterDelay(220, () -> {
            try {
                check(countChest(chest) > 0 && m.sourceTank().isEmpty(), "Native jar did not fund production and collection without machine Source");
                check(m.status() != SourceMachine.NEED_SOURCE && m.status() != SourceMachine.NO_CONTAINER, "Supplied henge still shows a missing-Source error");
                h.succeed();
            } finally { drygmy.discard(); chicken.discard(); m.energy().setEnergy(0); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void potionMixingUsesNativeAmountsAndRejectsIncompatibleOutputs(GameTestHelper h) {
        var m = machine(h, MachineKind.POTION_MIXER);
        try {
            var first = potionJar(h, m, RelativeSide.LEFT); var second = potionJar(h, m, RelativeSide.RIGHT); var output = potionJar(h, m, RelativeSide.TOP);
            var one = new PotionContents(Potions.HEALING); var two = new PotionContents(Potions.SWIFTNESS);
            int input = Config.MELDER_INPUT_COST.get(), generated = Config.MELDER_OUTPUT.get(), cost = Config.MELDER_SOURCE_COST.get();
            first.add(one, input * 2); second.add(two, input * 2); output.add(one, 100); source(m, cost * 2);
            long energy = m.energy().getEnergy(); cycle(m);
            check(first.getAmount() == input * 2 && m.energy().getEnergy() == energy && m.sourceTank().getStored() == cost * 2L, "Incompatible output consumed potion or energy");
            output.remove(100); cycle(m);
            check(first.getAmount() == input && second.getAmount() == input && output.getAmount() == generated && m.sourceTank().getStored() == cost, "Potion mixing did not conserve native input/output amounts");
            check(PotionUtil.arePotionContentsEqual(output.getData(), PotionUtil.merge(one, two)), "Mixing lost native potion effects");
            first.remove(input); first.add(new PotionContents(Potions.LONG_SWIFTNESS), input);
            energy = m.energy().getEnergy(); cycle(m);
            check(m.status() == SourceMachine.INVALID_POTION && m.energy().getEnergy() == energy, "Duplicate effects of different durations were mixed");
            h.succeed();
        } finally { m.energy().setEnergy(0); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void bottlingRoundTripsBottlesFlasksAndArrowsWithoutLosingPotion(GameTestHelper h) {
        var m = machine(h, MachineKind.POTION_BOTTLER);
        try {
            var jar = potionJar(h, m, RelativeSide.FRONT); var potion = new PotionContents(Potions.LONG_SWIFTNESS); jar.add(potion, 2_000);
            m.inputs.get(0).setStack(new ItemStack(Items.GLASS_BOTTLE)); cycle(m);
            ItemStack bottle = m.outputs().get(0).getStack().copy(); clearOutput(m);
            check(bottle.is(Items.POTION) && bottle.get(DataComponents.POTION_CONTENTS).equals(potion) && jar.getAmount() == 1_900, "Bottle did not receive exact potion contents");
            m.cycleMode(); m.inputs.get(0).setStack(bottle); cycle(m);
            check(jar.getAmount() == 2_000 && count(m, Items.GLASS_BOTTLE) == 1, "Potion recovery lost volume or bottle"); clearOutput(m);
            m.cycleMode(); m.inputs.get(0).setStack(new ItemStack(Items.ARROW)); cycle(m);
            check(count(m, Items.TIPPED_ARROW) == 1 && jar.getAmount() == 1_990, "Arrow did not cost ten potion units"); clearOutput(m);
            m.inputs.get(0).setStack(new ItemStack(ItemsRegistry.POTION_FLASK)); cycle(m);
            ItemStack flask = m.outputs().get(0).getStack().copy(); var data = flask.get(DataComponentRegistry.MULTI_POTION);
            check(data != null && data.charges() == data.maxUses() && data.contents().equals(potion), "Flask was not filled with preserved effects");
            check(jar.getAmount() == 1_990 - 100 * data.charges(), "Flask filling charged wrong volume"); clearOutput(m);
            m.cycleMode(); m.inputs.get(0).setStack(flask); cycle(m);
            check(jar.getAmount() == 1_990 && m.outputs().get(0).getStack().get(DataComponentRegistry.MULTI_POTION).charges() == 0, "Flask recovery duplicated or lost potion");
            h.succeed();
        } finally { m.energy().setEnergy(0); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void ritualControllerWaitsForAugmentAndNeverReplacesARunningRitual(GameTestHelper h) {
        var m = machine(h, MachineKind.RITUAL_CONTROLLER);
        try {
            BlockPos pos = m.getBlockPos().north(); h.getLevel().setBlockAndUpdate(pos, BlockRegistry.RITUAL_BLOCK.defaultBlockState());
            var brazier = (RitualBrazierTile) h.getLevel().getBlockEntity(pos);
            var tablet = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof RitualTablet ritual && ritual.ritual.getClass() == RitualOvergrowth.class).findFirst().orElseThrow();
            m.cycleMode(); m.inputs.get(0).setStack(new ItemStack(tablet, 2)); cycle(m); cycle(m);
            check(brazier.ritual != null && !brazier.ritual.isRunning() && m.status() == SourceMachine.MISSING_MATERIALS, "Augmented ritual started before its material arrived");
            m.inputs.get(1).setStack(new ItemStack(Items.BONE_BLOCK, 2)); cycle(m); cycle(m);
            check(brazier.ritual.isRunning() && brazier.ritual.didConsumeItem(Items.BONE_BLOCK) && m.inputs.get(1).getCount() == 1, "Native ritual did not consume its augment and start");
            var running = brazier.ritual; for (int i = 0; i < 3; i++) cycle(m);
            check(brazier.ritual == running && m.inputs.get(0).getCount() == 1, "Controller replaced a running ritual or wasted a tablet");
            running.setFinished(); brazier.tick(); cycle(m);
            check(m.inputs.get(0).isEmpty() && brazier.ritual != running, "Completed ritual did not accept the next tablet");
            long energy = m.energy().getEnergy(); h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); cycle(m);
            check(m.status() == SourceMachine.NO_TARGET && m.energy().getEnergy() == energy, "Missing brazier continued an automation transaction");
            h.succeed();
        } finally { m.energy().setEnergy(0); }
    }

    private static ChestBlockEntity outputChest(GameTestHelper h, SourceMachine m) {
        BlockPos pos = m.getBlockPos().relative(RelativeSide.RIGHT.getDirection(m.getDirection()));
        h.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        return (ChestBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static int countChest(ChestBlockEntity chest) {
        int total = 0; for (int i = 0; i < chest.getContainerSize(); i++) total += chest.getItem(i).getCount(); return total;
    }

    @GameTest(template = "empty", batch = "ars_drygmy", timeoutTicks = 260)
    public static void drygmyStationFeedsRealHengeAndSendsNativeLootToChest(GameTestHelper h) {
        var m = machine(h, MachineKind.DRYGMY_STATION); source(m, 5_000);
        BlockPos pos = m.getBlockPos().north(); h.getLevel().setBlockAndUpdate(pos, BlockRegistry.DRYGMY_BLOCK.defaultBlockState());
        var hive = (DrygmyTile) h.getLevel().getBlockEntity(pos); hive.converted = true;
        h.getLevel().setBlockAndUpdate(m.getBlockPos().above(), BlockRegistry.SOURCE_JAR.defaultBlockState());
        long energy = m.energy().getEnergy(); m.onUpdateServer();
        check(m.status() == SourceMachine.NO_CREATURE && m.energy().getEnergy() == energy, "Empty henge worked without a bound Drygmy");
        var drygmy = new EntityDrygmy(h.getLevel(), true); drygmy.homePos = pos; drygmy.setPos(pos.above().getCenter()); drygmy.setNoAi(true); h.getLevel().addFreshEntity(drygmy);
        var chicken = EntityType.CHICKEN.create(h.getLevel()); chicken.setPos(pos.north(2).getCenter()); chicken.setNoAi(true); h.getLevel().addFreshEntity(chicken);
        var chest = outputChest(h, m); hive.progress = hive.getMaxProgress(); hive.needsMana = true;
        h.runAfterDelay(220, () -> {
            try {
                check(countChest(chest) > 0, "Native Drygmy loot did not enter the Mek station and chest; status=" + m.status()
                      + ", inputs=" + m.inputs.stream().map(s -> s.getStack().toString()).toList() + ", outputs=" + m.outputs().stream().map(s -> s.getStack().toString()).toList());
                var jar = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, m.getBlockPos().above(), Direction.DOWN);
                long spent = jar == null ? -1 : 5_000L - m.sourceTank().getStored() - jar.getSource();
                int cost = Config.DRYGMY_MANA_COST.get();
                check(jar != null && (cost == 0 ? spent == 0 : spent >= cost && spent % cost == 0), "Drygmy Source cost was lost or duplicated");
                m.inputs.get(0).setStack(new ItemStack(Items.CHICKEN));
                check(WorldControllers.acceptsCollected(m, new ItemStack(Items.CHICKEN)) && !WorldControllers.acceptsCollected(m, new ItemStack(Items.FEATHER)), "Allow filter did not affect collection");
                m.cycleMode(); check(!WorldControllers.acceptsCollected(m, new ItemStack(Items.CHICKEN)) && WorldControllers.acceptsCollected(m, new ItemStack(Items.FEATHER)), "Exclude filter did not follow selected mode");
                h.succeed();
            } finally { drygmy.discard(); chicken.discard(); m.energy().setEnergy(0); }
        });
    }

    @GameTest(template = "empty", batch = "ars_whirlisprig", timeoutTicks = 150)
    public static void whirlisprigStationUsesRealGroveAndNativeProduction(GameTestHelper h) {
        var m = machine(h, MachineKind.WHIRLISPRIG_STATION); source(m, 5_000);
        BlockPos pos = m.getBlockPos().north(); h.getLevel().setBlockAndUpdate(pos, BlockRegistry.WHIRLISPRIG_FLOWER.defaultBlockState());
        var flower = (WhirlisprigTile) h.getLevel().getBlockEntity(pos); flower.converted = true;
        h.getLevel().setBlockAndUpdate(pos.north().below(), Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.north(), Blocks.OAK_SAPLING.defaultBlockState());
        h.getLevel().setBlockAndUpdate(m.getBlockPos().above(), BlockRegistry.SOURCE_JAR.defaultBlockState());
        var creature = new Whirlisprig(h.getLevel(), true, pos); creature.setPos(pos.above().getCenter()); creature.setNoAi(true); h.getLevel().addFreshEntity(creature);
        flower.evaluateGrove(); check(flower.moodScore > 0 && flower.diversityScore > 0, "Real plant samples did not populate the native grove");
        flower.progress = Config.WHIRLISPRIG_MAX_PROGRESS.get(); var chest = outputChest(h, m);
        h.runAfterDelay(110, () -> {
            try {
                check(countChest(chest) > 0 && m.observedPrimary() == flower.moodScore && m.observedSecondary() == flower.diversityScore, "Native grove production or environmental display failed; status=" + m.status()
                      + ", inputs=" + m.inputs.stream().map(s -> s.getStack().toString()).toList() + ", outputs=" + m.outputs().stream().map(s -> s.getStack().toString()).toList());
                var jar = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, m.getBlockPos().above(), Direction.DOWN);
                check(jar != null && 5_000L - m.sourceTank().getStored() - jar.getSource() == Config.WHIRLISPRIG_SOURCE_COST.get(), "Whirlisprig Source was duplicated or charged incorrectly");
                creature.discard(); long energy = m.energy().getEnergy(); m.onUpdateServer();
                check(m.status() == SourceMachine.NO_CREATURE && m.energy().getEnergy() == energy, "Station kept working after its bound creature disappeared");
                h.succeed();
            } finally { creature.discard(); m.energy().setEnergy(0); }
        });
    }
}
