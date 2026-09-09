package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.items.ModItems;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import mekanism.api.Upgrade;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class BreederGameTests {
    private static void check(boolean value, String message) {
        if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(message);
    }
    private static AuraMachine machine(GameTestHelper h) {
        h.setBlock(new BlockPos(5, 1, 5), Blocks.AIR);
        h.setBlock(new BlockPos(5, 1, 5), Content.MACHINES.get(MachineKind.INDUSTRIAL_BREEDER).get());
        AuraMachine m = (AuraMachine) h.getBlockEntity(new BlockPos(5, 1, 5));
        m.area().set(SetMachineSettingPayload.AREA_X, 2);
        m.area().set(SetMachineSettingPayload.AREA_Y, 0);
        m.energy().setEnergy(m.energy().getMaxEnergy());
        return m;
    }
    private static <T extends Animal> T animal(AuraMachine m, EntityType<T> type, int offset) {
        T animal = type.create(m.getLevel());
        animal.setNoAi(true);
        animal.moveTo(m.getBlockPos().getX() + 1.5 + offset, m.getBlockPos().getY(), m.getBlockPos().getZ() - .5);
        m.getLevel().addFreshEntity(animal);
        return animal;
    }
    private static int outputs(AuraMachine m) {
        return m.getInventorySlots(null).subList(9, 13).stream().filter(s -> s.getStack().is(ModItems.BIRTH_SPIRIT)).mapToInt(s -> s.getCount()).sum();
    }
    private static void fillOutputs(AuraMachine m, boolean full) {
        m.getInventorySlots(null).subList(9, 13).forEach(s -> s.setStack(full ? new ItemStack(Items.STONE, 64) : ItemStack.EMPTY));
    }
    private static void clean(AuraMachine m) {
        m.energy().setEnergy(0);
        m.getLevel().getEntities((Entity) null, m.area().bounds(), e -> !(e instanceof Player)).forEach(Entity::discard);
    }
    private static void cycle(AuraMachine m) {
        for (int i = 0; i < MekanismUtils.getTicks(m, MachineConfig.BREEDER_TICKS.get()); i++) m.onUpdateServer();
    }

    @GameTest(template = "empty", batch = "breeder_native", timeoutTicks = 20)
    public static void nativeBirthEventCooldownSpiritCostAndSavedWork(GameTestHelper h) {
        AuraMachine m = machine(h);
        var a = animal(m, EntityType.COW, 0); var b = animal(m, EntityType.COW, 1);
        m.inputs.get(0).setStack(new ItemStack(Items.WHEAT, 4));
        int[] events = {0};
        var players = List.copyOf(h.getLevel().players());
        Consumer<BabyEntitySpawnEvent> observe = event -> {
            if (event.getParentA() == a || event.getParentA() == b) {
                check(event.getCausedByPlayer() != null, "Native event lost automated feeding attribution");
                events[0]++;
            }
        };
        NeoForge.EVENT_BUS.addListener(observe);
        try {
            check(AuraTestEnvironment.set(m, 30, 1_400_000) == 1_400_000, "Aura fixture not ready");
            for (int i = 0; i < 30; i++) m.onUpdateServer();
            check(m.progress() == .3 && m.inputs.get(0).getCount() == 4, "Food spent before breeding");
            m.loadAdditional(m.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            for (int i = 0; i < 70; i++) m.onUpdateServer();
            check(events[0] == 1 && m.inputs.get(0).getCount() == 2, "Saved work lost progress or repeated feeding");
            check(a.getAge() == 6000 && b.getAge() == 6000 && !a.isInLove() && !b.isInLove(), "Native cooldowns not retained");
            var cows = h.getLevel().getEntitiesOfClass(Cow.class, m.area().bounds());
            check(cows.size() == 3 && cows.stream().filter(Animal::isBaby).count() == 1, "Native breeding did not create exactly one baby");
            check(h.getLevel().players().equals(players), "Fake player leaked into the level player list");
            m.onUpdateServer();
            int spirits = outputs(m);
            check(spirits >= 1 && spirits <= 3, "Native birth spirits not collected");
            check(IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), 30) == 1_400_000 - 800 * spirits, "Native spirit Aura cost changed");
            check(m.energy().getMaxEnergy() - m.energy().getEnergy() == 101 * m.energy().getEnergyPerTick(), "Breeding/collection FE cost differs");
            long energy = m.energy().getEnergy();
            cycle(m);
            check(m.inputs.get(0).getCount() == 2 && outputs(m) == spirits && m.energy().getEnergy() == energy, "Cooldown animals were fed or spirits duplicated");
            h.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(observe); clean(m); }
    }

    @GameTest(template = "empty", batch = "breeder_blocked", timeoutTicks = 20)
    public static void missingResourcesCapBabiesAndBlockedSightSpendNothing(GameTestHelper h) {
        AuraMachine m = machine(h);
        var a = animal(m, EntityType.COW, 0); var b = animal(m, EntityType.COW, 1);
        try {
            m.inputs.get(0).setStack(new ItemStack(Items.WHEAT, 2));
            AuraTestEnvironment.set(m, 30, 1_000_000);
            long energy = m.energy().getEnergy();
            m.onUpdateServer(); check(m.status() == 18 && m.energy().getEnergy() == energy, "Low Aura spirit mode spent FE");
            m.breeder().setBreedOnly(true);
            a.setBaby(true); m.onUpdateServer();
            check(m.status() == 17 && m.energy().getEnergy() == energy, "Baby was treated as a parent"); a.setAge(0);
            m.inputs.get(0).setStack(new ItemStack(Items.STONE, 2)); m.onUpdateServer();
            check(m.status() == 17 && m.energy().getEnergy() == energy, "Wrong food spent FE");
            m.inputs.get(0).setStack(new ItemStack(Items.WHEAT, 2));
            m.area().set(SetMachineSettingPayload.AREA_CAP, 2); m.onUpdateServer();
            check(m.status() == 14 && m.energy().getEnergy() == energy, "Population cap spent FE");
            m.area().set(SetMachineSettingPayload.AREA_CAP, 16);
            b.setPos(b.getX() + 1, b.getY(), b.getZ());
            h.setBlock(new BlockPos(7, 1, 4), Blocks.STONE); h.setBlock(new BlockPos(7, 2, 4), Blocks.STONE);
            m.onUpdateServer(); check(m.status() == 17 && m.energy().getEnergy() == energy, "Breeding crossed a wall");
            h.setBlock(new BlockPos(7, 1, 4), Blocks.AIR); h.setBlock(new BlockPos(7, 2, 4), Blocks.AIR);
            m.energy().setEnergy(0); m.onUpdateServer();
            check(m.status() == 6 && m.inputs.get(0).getCount() == 2 && !a.isInLove(), "Unpowered feeding changed animals or food");
            m.energy().setEnergy(energy); AuraTestEnvironment.set(m, 30, 1_400_000);
            fillOutputs(m, true); m.onUpdateServer();
            check(m.status() == 3 && m.energy().getEnergy() == energy, "Full spirit output spent FE");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", batch = "breeder_cancel", timeoutTicks = 20)
    public static void cancelledBreedingKeepsVanillaFoodCooldownAndCleansAttribution(GameTestHelper h) {
        AuraMachine m = machine(h);
        var a = animal(m, EntityType.COW, 0); var b = animal(m, EntityType.COW, 1);
        m.inputs.get(0).setStack(new ItemStack(Items.WHEAT, 4));
        var players = List.copyOf(h.getLevel().players());
        Consumer<BabyEntitySpawnEvent> cancel = event -> {
            if (event.getParentA() == a || event.getParentA() == b) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancel);
        try {
            AuraTestEnvironment.set(m, 30, 1_400_000); cycle(m);
            check(a.getAge() == 6000 && b.getAge() == 6000 && m.inputs.get(0).getCount() == 2, "Cancellation changed native food/cooldown semantics");
            check(h.getLevel().getEntitiesOfClass(Cow.class, m.area().bounds()).size() == 2 && outputs(m) == 0, "Cancelled event created a child or spirit");
            check(h.getLevel().players().equals(players), "Cancelled event leaked its FakePlayer");
            check(IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), 30) == 1_400_000, "Early-cancelled event spent Aura");
            h.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(cancel); clean(m); }
    }

    @GameTest(template = "empty", batch = "breeder_eggs", timeoutTicks = 20)
    public static void turtlesAndFrogsKeepPregnancyInsteadOfSpawningLiveYoung(GameTestHelper h) {
        AuraMachine m = machine(h);
        try {
            AuraTestEnvironment.set(m, 30, 1_000_000);
            m.breeder().setBreedOnly(true);
            m.getComponent().addUpgrades(Upgrade.SPEED, 8);
            var a = animal(m, EntityType.TURTLE, 0); var b = animal(m, EntityType.TURTLE, 1);
            m.inputs.get(0).setStack(new ItemStack(Items.SEAGRASS, 2)); cycle(m);
            check(a.hasEgg() || b.hasEgg(), "Turtle native pregnancy missing");
            check(h.getLevel().getEntitiesOfClass(Turtle.class, m.area().bounds()).size() == 2, "Turtle incorrectly produced a live baby");
            a.discard(); b.discard();
            var f = animal(m, EntityType.FROG, 0); var g = animal(m, EntityType.FROG, 1);
            m.inputs.get(0).setStack(new ItemStack(Items.SLIME_BALL, 2)); cycle(m);
            check(f.getBrain().hasMemoryValue(MemoryModuleType.IS_PREGNANT) || g.getBrain().hasMemoryValue(MemoryModuleType.IS_PREGNANT), "Frog native pregnancy missing");
            check(h.getLevel().getEntitiesOfClass(Frog.class, m.area().bounds()).size() == 2 && outputs(m) == 0, "Egg breeders produced live young or invented spirits");
            check(m.inputs.get(0).isEmpty(), "Egg breeding food not consumed");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void spiritCollectionHonorsPickupDelayOwnershipFullOutputAndPower(GameTestHelper h) {
        AuraMachine m = machine(h);
        try {
            ItemEntity spirit = new ItemEntity(h.getLevel(), m.area().center().getX(), m.area().center().getY(), m.area().center().getZ(), new ItemStack(ModItems.BIRTH_SPIRIT, 5));
            h.getLevel().addFreshEntity(spirit);
            long energy = m.energy().getEnergy();
            spirit.setTarget(UUID.randomUUID()); m.onUpdateServer();
            check(spirit.isAlive() && m.energy().getEnergy() == energy, "Collected another player's reserved item");
            spirit.setTarget(null); spirit.setDefaultPickUpDelay(); m.onUpdateServer();
            check(spirit.isAlive() && m.energy().getEnergy() == energy, "Ignored pickup delay");
            spirit.setNoPickUpDelay(); fillOutputs(m, true); m.onUpdateServer();
            check(spirit.getItem().getCount() == 5 && m.energy().getEnergy() == energy, "Full output lost items or FE");
            fillOutputs(m, false); m.energy().setEnergy(0); m.onUpdateServer();
            check(spirit.isAlive(), "Unpowered collection moved an item");
            m.energy().setEnergy(energy); m.onUpdateServer(); m.onUpdateServer();
            check(!spirit.isAlive() && outputs(m) == 5 && m.energy().getEnergy() == energy - m.energy().getEnergyPerTick(), "Collection duplicated or overcharged");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void axolotlFoodReturnsWaterBucketsAndReservesContainerSpace(GameTestHelper h) {
        AuraMachine m = machine(h);
        try {
            animal(m, EntityType.AXOLOTL, 0); animal(m, EntityType.AXOLOTL, 1);
            m.breeder().setBreedOnly(true);
            AuraTestEnvironment.set(m, 30, 1_000_000);
            m.inputs.get(0).setStack(new ItemStack(Items.TROPICAL_FISH_BUCKET));
            m.inputs.get(1).setStack(new ItemStack(Items.TROPICAL_FISH_BUCKET));
            fillOutputs(m, true); m.getInventorySlots(null).get(9).setStack(ItemStack.EMPTY);
            long energy = m.energy().getEnergy();
            m.onUpdateServer(); check(m.status() == 3 && m.energy().getEnergy() == energy, "Insufficient bucket output space did not block feeding");
            fillOutputs(m, false); cycle(m);
            check(m.inputs.get(0).isEmpty() && m.inputs.get(1).isEmpty(), "Axolotl food was not consumed");
            long buckets = m.getInventorySlots(null).subList(9, 13).stream().filter(s -> s.getStack().is(Items.WATER_BUCKET)).count();
            check(buckets == 2 && m.area().count(EntityType.AXOLOTL) == 3, "Axolotl breeding lost water buckets or offspring");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void breederMenuRangeAndModeSurviveDroppedMachine(GameTestHelper h) {
        AuraMachine m = machine(h);
        try {
            m.rangeModuleSlot().setStack(new ItemStack(Content.RANGE_MODULE.get(), 4));
            var player = FakePlayerFactory.getMinecraft(h.getLevel());
            player.setPos(m.getBlockPos().getX(), m.getBlockPos().getY(), m.getBlockPos().getZ());
            var menu = new MachineMenu(82, player.getInventory(), m);
            check(menu.clickMenuButton(player, 3) && m.breeder().breedOnly(), "Breeder mode button failed");
            check(menu.applySetting(player, SetMachineSettingPayload.AREA_RADIUS, 12), "Breeder range control failed");
            ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            var restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
            restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(drop);
            check(restored.breeder().breedOnly() && restored.area().radius() == 12 && restored.rangeModules() == 4, "Dropped breeder lost its mode or area");
            player.setPos(player.getX() + 100, player.getY(), player.getZ());
            check(!menu.clickMenuButton(player, 3), "Distant player changed breeder mode");
            h.succeed();
        } finally { clean(m); }
    }
}
