package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.recipes.AnimalSpawnerRecipe;
import de.ellpeck.naturesaura.recipes.ModRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class SpawnerGameTests {
    private static void check(boolean value, String message) {
        if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(message);
    }
    private static AuraMachine machine(GameTestHelper h) {
        h.setBlock(new BlockPos(5, 1, 5), Blocks.AIR);
        h.setBlock(new BlockPos(5, 1, 5), Content.MACHINES.get(MachineKind.ANIMAL_SPAWNER).get());
        AuraMachine m = (AuraMachine) h.getBlockEntity(new BlockPos(5, 1, 5));
        m.area().set(SetMachineSettingPayload.AREA_X, 2);
        m.area().set(SetMachineSettingPayload.AREA_Y, 0);
        m.area().set(SetMachineSettingPayload.AREA_RADIUS, 0);
        h.setBlock(new BlockPos(7, 0, 5), Blocks.STONE);
        h.setBlock(new BlockPos(7, 1, 5), Blocks.AIR);
        h.setBlock(new BlockPos(7, 2, 5), Blocks.AIR);
        m.energy().setEnergy(m.energy().getMaxEnergy());
        m.auraTank().setStack(new ChemicalStack(Content.AURA, 100_000));
        return m;
    }
    private static void cowRecipe(AuraMachine m, int count) {
        m.inputs.get(0).setStack(new ItemStack(ModItems.BIRTH_SPIRIT, count));
        m.inputs.get(1).setStack(new ItemStack(Items.BEEF, count));
        m.inputs.get(2).setStack(new ItemStack(Items.LEATHER, count));
    }
    private static List<Cow> cows(AuraMachine m) { return m.getLevel().getEntitiesOfClass(Cow.class, m.area().bounds()); }
    private static void clean(AuraMachine m) {
        m.energy().setEnergy(0);
        m.getLevel().getEntities((Entity) null, m.area().bounds(), e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
    }

    @GameTest(template = "empty", batch = "spawner_cow", timeoutTicks = 100)
    public static void nativeCowRecipeSpawnsAtConfiguredOffsetAndPaysExactly(GameTestHelper h) {
        AuraMachine m = machine(h); cowRecipe(m, 1);
        h.runAfterDelay(70, () -> {
            check(cows(m).size() == 1 && !cows(m).getFirst().isBaby(), "Native recipe did not initialize one adult cow");
            check(m.inputs.stream().allMatch(s -> s.isEmpty()), "Cow ingredients were not consumed once");
            check(m.auraTank().getStored() == 50_000, "Cow Aura cost differs from the live recipe");
            check(m.energy().getMaxEnergy() - m.energy().getEnergy() == m.energy().getEnergyPerTick() * 60, "Cow work duration charged incorrect FE");
            clean(m); h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "spawner_blocked", timeoutTicks = 20)
    public static void noSpaceNoPowerPopulationAndCancelledSpawnPreserveMaterials(GameTestHelper h) {
        AuraMachine m = machine(h); cowRecipe(m, 2);
        try {
            long initialEnergy = m.energy().getEnergy();
            h.setBlock(new BlockPos(7, 1, 5), Blocks.STONE);
            m.onUpdateServer();
            check(m.status() == 15 && m.energy().getEnergy() == initialEnergy, "Blocked space spent FE");
            h.setBlock(new BlockPos(7, 1, 5), Blocks.AIR);
            m.energy().setEnergy(0); m.onUpdateServer();
            check(m.status() == 6 && m.auraTank().getStored() == 100_000, "Unpowered spawner consumed Aura");
            m.energy().setEnergy(initialEnergy);
            Cow blocker = EntityType.COW.create(h.getLevel());
            blocker.moveTo(m.area().center().getX() + .5, m.area().center().getY(), m.area().center().getZ() + .5);
            h.getLevel().addFreshEntity(blocker);
            m.area().set(SetMachineSettingPayload.AREA_CAP, 1);
            m.onUpdateServer();
            check(m.status() == 14 && m.energy().getEnergy() == initialEnergy, "Population cap spent energy");
            blocker.discard();
            Consumer<EntityJoinLevelEvent> cancel = event -> {
                if (event.getEntity() instanceof Cow && event.getEntity().blockPosition().closerThan(m.area().center(), 2)) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                for (int i = 0; i < 65; i++) m.onUpdateServer();
                check(cows(m).isEmpty() && m.status() == 16, "Cancelled entity was added or cancellation was hidden");
                check(m.inputs.get(0).getCount() == 2 && m.auraTank().getStored() == 100_000, "Cancelled generation consumed materials or Aura");
                check(m.energy().getEnergy() == initialEnergy - 60 * m.energy().getEnergyPerTick(), "Cancelled completion kept charging FE");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            m.onUpdateServer();
            check(cows(m).size() == 1 && m.inputs.get(0).getCount() == 1 && m.auraTank().getStored() == 50_000, "Successful retry did not commit exactly once");
            long after = m.energy().getEnergy();
            m.onUpdateServer();
            check(m.status() == 14 && m.energy().getEnergy() == after, "Filled population limit did not halt the next cycle");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", batch = "spawner_mixed", timeoutTicks = 20)
    public static void mixedAuraAndSavedProgressResumeWithoutDuplicatingWork(GameTestHelper h) {
        AuraMachine m = machine(h); cowRecipe(m, 1);
        try {
            m.auraTank().setStack(new ChemicalStack(Content.AURA, 20_000));
            check(AuraTestEnvironment.set(m, 35, 100_000) == 100_000, "Mixed fixture not ready");
            for (int i = 0; i < 20; i++) m.onUpdateServer();
            check(m.progress() > 0 && m.auraTank().getStored() == 20_000, "Spawner paid Aura before completion");
            var saved = m.saveWithoutMetadata(h.getLevel().registryAccess());
            m.loadAdditional(saved, h.getLevel().registryAccess());
            for (int i = 0; i < 40; i++) m.onUpdateServer();
            check(cows(m).size() == 1 && m.auraTank().isEmpty(), "Saved work lost progress or tank payment");
            check(IAuraChunk.getAuraInArea(h.getLevel(), m.getBlockPos(), 35) == 70_000, "Environment supplied more than the tank shortfall");
            h.succeed();
        } finally { clean(m); }
    }

    @GameTest(template = "empty", batch = "spawner_reload", timeoutTicks = 20)
    public static void reloadedCustomEntityRecipeReplacesInProgressTarget(GameTestHelper h) {
        AuraMachine m = machine(h); cowRecipe(m, 1);
        var manager = h.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            for (int i = 0; i < 10; i++) m.onUpdateServer();
            var custom = new AnimalSpawnerRecipe(ResourceLocation.withDefaultNamespace("pig"), 1234, 2,
                  List.of(Ingredient.of(ModItems.BIRTH_SPIRIT), Ingredient.of(Items.BEEF), Ingredient.of(Items.LEATHER)));
            manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "test_pig"), custom)));
            m.onUpdateServer();
            check(m.spawner().target() == EntityType.PIG && m.progress() == .5 && cows(m).isEmpty(), "Reloaded recipe reused old work or target");
            m.onUpdateServer();
            check(m.area().count(EntityType.PIG) == 1 && cows(m).isEmpty() && m.auraTank().getStored() == 98_766, "Custom recipe did not use its own entity or cost");
            h.succeed();
        } finally { manager.replaceRecipes(original); clean(m); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aquaticRecipeNeedsWaterAndHonorsSpeedUpgradeCost(GameTestHelper h) {
        AuraMachine m = machine(h);
        var manager = h.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            var recipe = new AnimalSpawnerRecipe(ResourceLocation.withDefaultNamespace("salmon"), 777, 60, List.of(Ingredient.of(Items.SALMON)));
            manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "test_salmon"), recipe)));
            m.inputs.get(0).setStack(new ItemStack(Items.SALMON));
            m.getComponent().addUpgrades(Upgrade.SPEED, 8);
            long initial = m.energy().getEnergy();
            m.onUpdateServer();
            check(m.status() == 15 && m.energy().getEnergy() == initial, "Aquatic recipe accepted a dry floor");
            h.setBlock(new BlockPos(7, 1, 5), Blocks.WATER);
            int ticks = mekanism.common.util.MekanismUtils.getTicks(m, 60);
            for (int i = 0; i < ticks; i++) m.onUpdateServer();
            check(m.area().count(EntityType.SALMON) == 1 && m.inputs.get(0).isEmpty(), "Water and upgraded duration did not permit aquatic spawning");
            check(m.auraTank().getStored() == 99_223 && initial - m.energy().getEnergy() == ticks * m.energy().getEnergyPerTick(), "Speed upgrades changed Aura cost or charged incorrect FE");
            h.succeed();
        } finally { manager.replaceRecipes(original); clean(m); h.setBlock(new BlockPos(7, 1, 5), Blocks.AIR); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void areaLimitsModulesMenuAndDroppedSettingsRemainValid(GameTestHelper h) {
        AuraMachine m = machine(h);
        m.energy().setEnergy(0);
        m.rangeModuleSlot().setStack(new ItemStack(Content.RANGE_MODULE.get(), 4));
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setPos(m.getBlockPos().getX(), m.getBlockPos().getY(), m.getBlockPos().getZ());
        var menu = new MachineMenu(81, player.getInventory(), m);
        check(menu.applySetting(player, SetMachineSettingPayload.AREA_RADIUS, 100) && m.area().radius() == 12, "Range module radius not clamped");
        menu.applySetting(player, SetMachineSettingPayload.AREA_X, -100);
        menu.applySetting(player, SetMachineSettingPayload.AREA_CAP, 0);
        check(m.area().x() == -16 && m.area().cap() == 1, "Invalid area values accepted");
        check(!menu.applySetting(player, SetMachineSettingPayload.CONTROL_LOWER, 1), "Wrong machine setting accepted");
        player.setPos(player.getX() + 100, player.getY(), player.getZ());
        check(!menu.applySetting(player, SetMachineSettingPayload.AREA_CAP, 99), "Distant player reconfigured area");
        ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
        var restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
        restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(drop);
        check(restored.area().save().equals(m.area().save()) && restored.rangeModules() == 4, "Dropped spawner lost area/module settings");
        restored.rangeModuleSlot().setStack(ItemStack.EMPTY);
        check(restored.area().radius() == 4, "Removing range modules left expanded radius active");
        h.succeed();
    }
}
