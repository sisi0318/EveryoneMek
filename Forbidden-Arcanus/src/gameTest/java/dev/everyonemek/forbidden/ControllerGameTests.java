package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import com.stal111.forbidden_arcanus.common.essence.*;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import dev.everyonemek.forbidden.mixin.ClibanoAccess;
import java.util.*;
import mekanism.api.*;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ForbiddenMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ControllerGameTests {
    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }
    private static Controller controller(GameTestHelper h, MachineKind kind, int x) {
        BlockPos pos = new BlockPos(x, 2, 12);
        h.setBlock(pos, Content.MACHINES.get(kind).defaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        Controller controller = (Controller) h.getBlockEntity(pos);
        controller.energy().setEnergy(controller.energy().getMaxEnergy());
        return controller;
    }
    private static Controller forge(GameTestHelper h, int tier) {
        String[] floor = {"***PPP***", "*PPPAPPP*", "*PAPPPAP*", "PPPPCPPPP", "PAPCACPAP", "PPPPCPPPP", "*PAPPPAP*", "*PPPAPPP*", "***PPP***"};
        for (int z = 0; z < 9; z++) for (int x = 0; x < 9; x++) {
            Block block = switch (floor[z].charAt(x)) {
                case 'P' -> ModBlocks.POLISHED_DARKSTONE.get();
                case 'A' -> ModBlocks.GILDED_CHISELED_POLISHED_DARKSTONE.get();
                case 'C' -> ModBlocks.CHISELED_ARCANE_POLISHED_DARKSTONE.get();
                default -> Blocks.AIR;
            };
            h.setBlock(new BlockPos(8 + x, 1, 8 + z), block);
        }
        Controller machine = controller(h, MachineKind.FORGE, 12);
        for (int target = 2; target <= tier; target++) check(machine.forge.upgradeTo(target), "Could not set up forge tier");
        System.arraycopy(machine.capacities, 0, machine.observed, 0, 4);
        check(machine.forge.hasPlatform(), "Native platform was not recognized");
        return machine;
    }
    private static ClibanoMainBlockEntity clibano(GameTestHelper h) {
        BlockPos center = h.absolutePos(new BlockPos(12, 2, 12));
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            int edges = Math.abs(x) + Math.abs(y) + Math.abs(z);
            Block block = edges == 0 ? ModBlocks.CLIBANO_MAIN_PART.get() : edges == 3 ? ModBlocks.CLIBANO_CORNER.get()
                  : edges == 1 ? ModBlocks.CLIBANO_CENTER.get() : y == 0 ? ModBlocks.CLIBANO_SIDE_HORIZONTAL.get() : ModBlocks.CLIBANO_SIDE_VERTICAL.get();
            var pos = center.offset(x, y, z); h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            if (h.getLevel().getBlockEntity(pos) instanceof ClibanoFrameBlockEntity frame)
                frame.setFrameData(new ClibanoFrameBlockEntity.FrameData(ModBlocks.POLISHED_DARKSTONE_BRICKS.get().defaultBlockState(), center));
        }
        var clibano = (ClibanoMainBlockEntity) h.getLevel().getBlockEntity(center);
        clibano.setFrontDirection(Direction.NORTH);
        return clibano;
    }
    private static void bind(GameTestHelper h, Controller controller, BlockPos target) {
        var player = FakePlayerFactory.getMinecraft(h.getLevel()); var old = player.position();
        try {
            player.setPos(controller.getBlockPos().getCenter());
            check(controller.binding.bind(player, target), "Native binding failed: " + controller.status);
            check(controller.binding.resolve() != null, "Binding immediately invalid");
        } finally { player.setPos(old); }
    }
    private static Ritual stock(GameTestHelper h, Controller controller, String name, int batches) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("forbidden_arcanus", name);
        Ritual ritual = h.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL).get(id);
        check(ritual != null, "Missing native ritual " + id);
        int index = 0;
        for (var ingredient : Recipes.ingredients(ritual)) {
            ItemStack item = ingredient.getItems()[0].copyWithCount(batches);
            boolean merged = false;
            for (var slot : controller.stock) if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot.getStack(), item)) {
                slot.growStack(batches, Action.EXECUTE); merged = true; break;
            }
            if (!merged) controller.stock.get(index++).setStack(item);
        }
        check(controller.setRecipe(id.toString()), "Native ritual selection failed");
        return ritual;
    }
    private static int outputs(Controller controller, net.minecraft.world.level.ItemLike item) {
        return controller.outputs.stream().filter(s -> s.getStack().is(item.asItem())).mapToInt(s -> s.getCount()).sum();
    }
    private static void runForge(Controller machine, int ticks) {
        for (int i = 0; i < ticks; i++) machine.onUpdateServer();
    }
    private static void runClibano(Controller controller, ClibanoMainBlockEntity clibano, int ticks) {
        for (int i = 0; i < ticks; i++) {
            controller.onUpdateServer();
            ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
        }
    }
    private static void stop(Controller controller) { controller.enabled = false; controller.energy().setEnergy(0); }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void internalForgeConsumesNativeMaterialsAndEssencesExactlyOnce(GameTestHelper h) {
        var machine = forge(h, 3);
        try {
            var ritual = stock(h, machine, "eternal_stella", 2);
            var costs = machine.forge.cost(ritual);
            int[] before = machine.observed.clone();
            runForge(machine, 20);
            check(machine.getActive() && machine.progress == 20 && outputs(machine, ModItems.ETERNAL_STELLA.get()) == 0, "Forge did not start internal processing");
            check(Arrays.equals(before, machine.observed), "Resources were charged before completing a batch");
            runForge(machine, ritual.duration() * 2);
            check(outputs(machine, ModItems.ETERNAL_STELLA.get()) == 2, "Two internal products missing: " + machine.status);
            for (var type : EssenceType.values()) check(before[type.ordinal()] - machine.observed[type.ordinal()] == 2 * costs.get(type), "Incorrect " + type + " cost");
            check(machine.stock.stream().allMatch(slot -> slot.isEmpty()) && machine.binding.target == null, "Internal materials or ownership incorrect");
            check(!machine.setRecipe("forbidden_arcanus:upgrade_tier_2"), "Native upgrade ritual leaked into production selection");
            h.succeed();
        } finally { stop(machine); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeNineMaterialCraftingAndRightClickInstallersPreserveMachine(GameTestHelper h) {
        var machine = forge(h, 1);
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        var oldPosition = player.position(); var oldMenu = player.containerMenu;
        var inventory = player.getInventory().save(new net.minecraft.nbt.ListTag());
        try {
            player.getInventory().clearContent(); player.setPos(machine.getBlockPos().getCenter());
            BlockPos table = machine.getBlockPos().offset(0, 0, 2);
            h.getLevel().setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
            for (int tier = 2; tier <= 5; tier++) {
                var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("forbiddenmekanism:forge_tier_" + tier + "_installer")).orElseThrow();
                check(holder.value() instanceof ForgeUpgradeRecipe, "Native-reference crafting recipe failed to load");
                var recipe = (ForgeUpgradeRecipe) holder.value();
                check(recipe.getWidth() == 3 && recipe.getHeight() == 3 && recipe.getIngredients().size() == 9, "JEI/recipe book does not have a full 3x3 recipe");
                check(recipe.ritual().unwrapKey().orElseThrow().location().toString().equals("forbidden_arcanus:upgrade_tier_" + tier), "Upgrade materials are not a native registry reference");
                var items = recipe.getIngredients().stream().map(ingredient -> ingredient.getItems()[0].copyWithCount(1)).toList();
                check(recipe.ritual().value().mainIngredient().test(items.get(4)) && recipe.matches(CraftingInput.of(3, 3, items), h.getLevel()), "Original center material missing");
                var missingCenter = new ArrayList<>(items); missingCenter.set(4, ItemStack.EMPTY);
                check(!recipe.matches(CraftingInput.of(3, 3, missingCenter), h.getLevel()), "Upgrade could be crafted without the ninth ingredient");
                var menu = new CraftingMenu(50 + tier, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), table));
                player.containerMenu = menu;
                for (int i = 0; i < 9; i++) menu.getSlot(i + 1).set(items.get(i).copy());
                check(menu.getSlot(0).getItem().is(Content.INSTALLERS.get(tier).get()), "Workbench did not produce the tier installer");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                check(menu.getCarried().is(Content.INSTALLERS.get(tier).get()), "Crafted installer not received");
                for (int i = 1; i <= 9; i++) check(menu.getSlot(i).getItem().isEmpty(), "Workbench did not consume all nine materials");
            }
            player.containerMenu = oldMenu;
            machine.stock.get(0).setStack(new ItemStack(Items.DIAMOND, 13));
            machine.enhancers.get(0).setStack(new ItemStack(ModItems.ARTISAN_RELIC.get()));
            machine.module.setStack(new ItemStack(Content.GLOW_MODULE.get(), 2));
            machine.getComponent().addUpgrades(Upgrade.SPEED, 2);
            int[] resources = machine.observed.clone();
            var hit = new net.minecraft.world.phys.BlockHitResult(machine.getBlockPos().getCenter(), Direction.UP, machine.getBlockPos(), false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Content.INSTALLERS.get(3).get()));
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            check(machine.nativeTier == 1 && player.getMainHandItem().getCount() == 1, "Installer skipped a tier");
            for (int tier = 2; tier <= 5; tier++) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Content.INSTALLERS.get(tier).get(), 2));
                player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                check(machine.nativeTier == tier && player.getMainHandItem().getCount() == 1, "Right-click failed to consume exactly one installer");
                player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                check(player.getMainHandItem().getCount() == 1, "Same-tier installer was consumed");
            }
            check(Arrays.equals(resources, machine.observed) && machine.stock.get(0).getCount() == 13 && machine.module.getCount() == 2
                  && machine.enhancers.get(0).getCount() == 1 && machine.getComponent().getUpgrades(Upgrade.SPEED) == 2, "Upgrade changed owned resources, items or upgrades");
            h.succeed();
        } finally { player.containerMenu = oldMenu; player.setPos(oldPosition); player.getInventory().load(inventory); stop(machine); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aurealModulesNeedPowerAndNativeSupplyContainersRetainRemainders(GameTestHelper h) {
        var machine = forge(h, 1);
        try {
            Arrays.fill(machine.observed, 0);
            machine.module.setStack(new ItemStack(Content.GLOW_MODULE.get(), 3));
            long energy = machine.energy().getEnergy();
            runForge(machine, 99); check(machine.observed[0] == 0, "Module generated ahead of its native interval");
            runForge(machine, 1); check(machine.observed[0] == 3 && energy - machine.energy().getEnergy() == 3 * machine.energy().getEnergyPerTick(), "Module generation rate or FE charge wrong");
            machine.energy().setEnergy(0); runForge(machine, 110); check(machine.observed[0] == 3, "Unpowered module generated Aureal");
            machine.energy().setEnergy(machine.energy().getMaxEnergy());
            machine.observed[0] = machine.capacities[0] - 1; runForge(machine, 100);
            check(machine.observed[0] == machine.capacities[0], "Aureal overflowed capacity");
            machine.module.setEmpty(); machine.supplies.get(1).setStack(new ItemStack(ModItems.SOUL.get(), 2));
            var blood = new ItemStack(ModItems.BLOOD_TEST_TUBE.get());
            blood.set(ModDataComponents.ESSENCE_STORAGE, new EssenceStorage(EssenceValue.of(EssenceType.BLOOD, 15), 100, true));
            machine.supplies.get(2).setStack(blood);
            for (var output : machine.outputs) output.setStack(new ItemStack(Items.COBBLESTONE, 64));
            runForge(machine, 3);
            check(machine.observed[1] == 2 && machine.observed[2] == 15 && machine.supplies.get(2).getStack().is(ModItems.TEST_TUBE.get()), "Resource input lost essence or empty container");
            machine.outputs.get(0).setEmpty(); runForge(machine, 1);
            check(machine.supplies.get(2).isEmpty() && outputs(machine, ModItems.TEST_TUBE.get()) == 1, "Empty native container was not recovered");
            machine.module.setStack(new ItemStack(Content.GLOW_MODULE.get())); machine.observed[0] = 0;
            machine.getComponent().addUpgrades(Upgrade.SPEED, 2); int interval = machine.forge.glowInterval();
            check(interval < 100, "Speed upgrades did not affect the module"); runForge(machine, interval);
            check(machine.observed[0] == 1, "Upgraded module interval wrong");
            h.succeed();
        } finally { stop(machine); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void forgePausesValidatesWorkAndKeepsComponentsThroughDropAndReload(GameTestHelper h) {
        var machine = forge(h, 2);
        Controller restored = null;
        try {
            stock(h, machine, "draco_arcanus_boots", 1);
            machine.enhancers.get(0).setStack(new ItemStack(ModItems.ARTISAN_RELIC.get()));
            machine.getComponent().addUpgrades(Upgrade.SPEED, 1);
            var main = machine.stock.stream().filter(slot -> slot.getStack().is(Items.NETHERITE_BOOTS)).findFirst().orElseThrow();
            ItemStack boots = main.getStack().copy(); boots.set(DataComponents.CUSTOM_NAME, Component.literal("Saved boots")); boots.setDamageValue(11); main.setStack(boots);
            runForge(machine, 30); check(machine.progress == 30, "Transmutation did not begin: " + machine.status);
            machine.enabled = false; runForge(machine, 5); check(machine.progress == 30 && !machine.getActive(), "Pause advanced work"); machine.enabled = true;
            machine.energy().setEnergy(0); runForge(machine, 5); check(machine.progress == 30, "Unpowered forge advanced work");
            machine.energy().setEnergy(machine.energy().getMaxEnergy());
            BlockPos floor = machine.getBlockPos().below(); var floorState = h.getLevel().getBlockState(floor);
            h.getLevel().setBlockAndUpdate(floor, Blocks.AIR.defaultBlockState()); runForge(machine, 5);
            check(machine.progress == 30 && machine.status == Controller.STRUCTURE, "Broken platform advanced work");
            h.getLevel().setBlockAndUpdate(floor, floorState);
            boots = main.getStack().copy(); boots.setDamageValue(12); main.setStack(boots);
            runForge(machine, 1); check(machine.progress == 1, "Changed item components retained old progress");
            runForge(machine, 30);
            var registry = h.getLevel().registryAccess();
            machine.loadAdditional(machine.saveWithoutMetadata(registry), registry);
            check(machine.progress == 31, "NBT reload lost valid progress");
            ItemStack drop = Block.getDrops(machine.getBlockState(), h.getLevel(), machine.getBlockPos(), machine, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            drop = ItemStack.parse(registry, drop.save(registry)).orElseThrow();
            int[] resources = machine.observed.clone();
            h.getLevel().removeBlock(machine.getBlockPos(), false);
            restored = controller(h, MachineKind.FORGE, 12); restored.applyComponentsFromItemStack(drop);
            check(restored.nativeTier == 2 && restored.progress == 31 && Arrays.equals(resources, restored.observed)
                  && restored.enhancers.get(0).isItemValid(new ItemStack(ModItems.ARTISAN_RELIC.get()))
                  && restored.enhancers.get(0).getCount() == 1 && restored.getComponent().getUpgrades(Upgrade.SPEED) == 1, "Drop lost tier, owned enhancer, resources or progress");
            runForge(restored, 500);
            ItemStack result = restored.outputs.stream().map(slot -> slot.getStack()).filter(stack -> stack.is(ModItems.DRACO_ARCANUS_BOOTS.get())).findFirst().orElse(ItemStack.EMPTY);
            check(!result.isEmpty() && result.getDamageValue() == 12 && Component.literal("Saved boots").equals(result.get(DataComponents.CUSTOM_NAME)), "Transmutation lost saved components");
            h.succeed();
        } finally { stop(machine); if (restored != null) stop(restored); }
    }
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void forgeSideConfigurationActuallyEjectsInternalProduct(GameTestHelper h) {
        var machine = forge(h, 3);
        try {
            stock(h, machine, "eternal_stella", 1);
            runForge(machine, 500);
            check(outputs(machine, ModItems.ETERNAL_STELLA.get()) == 1, "Internal product missing before ejection");
            var side = RelativeSide.fromDirections(machine.getDirection(), Direction.WEST);
            var player = FakePlayerFactory.getMinecraft(h.getLevel());
            var context = (net.neoforged.neoforge.network.handling.IPayloadContext) java.lang.reflect.Proxy.newProxyInstance(
                  ControllerGameTests.class.getClassLoader(), new Class[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},
                  (proxy, method, args) -> { if (method.getName().equals("player")) return player; throw new UnsupportedOperationException(method.getName()); });
            for (int i = 0; i < 12 && machine.getConfig().getConfig(TransmissionType.ITEM).getDataType(side) != DataType.OUTPUT; i++)
                new mekanism.common.network.to_server.configuration_update.PacketSideData(machine.getBlockPos(), mekanism.common.network.MekClickType.LEFT, side, TransmissionType.ITEM).handle(context);
            h.getLevel().setBlockAndUpdate(machine.getBlockPos().west(), Blocks.CHEST.defaultBlockState());
        } catch (Throwable error) { stop(machine); throw error; }
        h.runAfterDelay(30, () -> {
            try {
                var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(machine.getBlockPos().west()); int count = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(ModItems.ETERNAL_STELLA.get())) count += chest.getItem(i).getCount();
                check(count == 1 && outputs(machine, ModItems.ETERNAL_STELLA.get()) == 0, "Configured forge output did not reach the chest");
                h.succeed();
            } finally { stop(machine); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void clibanoUsesRealFuelAndBothIndependentNativeInputs(GameTestHelper h) {
        var clibano = clibano(h); var controller = controller(h, MachineKind.CLIBANO, 8);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, clibano.getBlockPos()); controller.stock.get(0).setStack(new ItemStack(Items.RAW_IRON)); controller.stock.get(1).setStack(new ItemStack(Items.RAW_COPPER));
                runClibano(controller, clibano, 30);
                check(outputs(controller, Items.IRON_INGOT) == 0 && clibano.getStack(3).isEmpty(), "FE replaced native fuel");
                controller.supplies.get(0).setStack(new ItemStack(Items.COAL)); runClibano(controller, clibano, 25);
                check(!clibano.getStack(3).isEmpty() && !clibano.getStack(4).isEmpty(), "Native independent inputs were not both fed");
                runClibano(controller, clibano, 150);
                check(outputs(controller, Items.IRON_INGOT) == 1 && outputs(controller, Items.COPPER_INGOT) == 1, "Native independent products missing: " + controller.status);
                check(clibano.getResiduesStorage().getTotalAmount() == 0, "Ordinary fire incorrectly generated residues");
                check(((ClibanoAccess) clibano).forbiddenmekanism$data().get(1) > 0, "Native fuel burn timer was replaced");
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void clibanoAlloyKeepsEnhancerSoulTimerAndNativeResidueConversion(GameTestHelper h) {
        var clibano = clibano(h); var controller = controller(h, MachineKind.CLIBANO, 8);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, clibano.getBlockPos());
                controller.setRecipe("forbidden_arcanus:clibano_combustion/obsidiansteel_ingot_from_clibano_combustion");
                controller.stock.get(0).setStack(new ItemStack(Items.RAW_IRON)); controller.stock.get(1).setStack(new ItemStack(Items.OBSIDIAN));
                controller.supplies.get(0).setStack(new ItemStack(Items.COAL)); controller.supplies.get(1).setStack(new ItemStack(ModItems.ENCHANTED_SOUL.get()));
                runClibano(controller, clibano, 20); check(clibano.getStack(3).isEmpty(), "Alloy bypassed enhancer requirement");
                clibano.setStack(0, new ItemStack(ModItems.ARTISAN_RELIC.get())); runClibano(controller, clibano, 150);
                check(outputs(controller, ModItems.OBSIDIANSTEEL_INGOT.get()) == 1, "Native alloy did not combine both inputs");
                var data = ((ClibanoAccess) clibano).forbiddenmekanism$data(); int soulTime = data.get(0), burnTime = data.get(1);
                check(data.get(7) == 2 && soulTime > 0, "Enchanted soul did not set native fire");
                controller.enabled = false; runClibano(controller, clibano, 10);
                check(data.get(0) == soulTime - 10 && data.get(1) == burnTime - 10, "Controller pause froze native fuel or souls");
                var residue = h.getLevel().registryAccess().registryOrThrow(FARegistries.RESIDUE_TYPE).getHolder(ResourceLocation.parse("forbidden_arcanus:iron")).orElseThrow();
                clibano.getResiduesStorage().increaseType(residue, residue.value().combineInfo().requiredAmount());
                controller.enabled = true; runClibano(controller, clibano, 20);
                check(outputs(controller, Items.IRON_BLOCK) == 1, "Native residue conversion did not reach controller output");
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 240)
    public static void configuredMekOutputActuallyEjectsClibanoProductIntoChest(GameTestHelper h) {
        var clibano = clibano(h); var controller = controller(h, MachineKind.CLIBANO, 8);
        h.runAfterDelay(2, () -> {
            bind(h, controller, clibano.getBlockPos());
            controller.stock.get(0).setStack(new ItemStack(Items.RAW_IRON)); controller.supplies.get(0).setStack(new ItemStack(Items.COAL));
            var side = RelativeSide.fromDirections(controller.getDirection(), Direction.WEST);
            var player = FakePlayerFactory.getMinecraft(h.getLevel());
            var context = (net.neoforged.neoforge.network.handling.IPayloadContext) java.lang.reflect.Proxy.newProxyInstance(
                  ControllerGameTests.class.getClassLoader(), new Class[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},
                  (proxy, method, args) -> { if (method.getName().equals("player")) return player; throw new UnsupportedOperationException(method.getName()); });
            for (int i = 0; i < 12 && controller.getConfig().getConfig(TransmissionType.ITEM).getDataType(side) != DataType.OUTPUT; i++)
                new mekanism.common.network.to_server.configuration_update.PacketSideData(controller.getBlockPos(), mekanism.common.network.MekClickType.LEFT, side, TransmissionType.ITEM).handle(context);
            check(controller.getConfig().getConfig(TransmissionType.ITEM).getDataType(side) == DataType.OUTPUT, "Mek side packet did not set output");
            h.getLevel().setBlockAndUpdate(controller.getBlockPos().west(), Blocks.CHEST.defaultBlockState());
        });
        h.runAfterDelay(210, () -> {
            try {
                var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(controller.getBlockPos().west()); int count = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.IRON_INGOT)) count += chest.getItem(i).getCount();
                check(count == 1 && outputs(controller, Items.IRON_INGOT) == 0, "Configured output did not eject native product into chest: " + controller.status);
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeMenuShiftClickReturnsEnhancerToPlayerAndChecksDistance(GameTestHelper h) {
        var nativeMachine = clibano(h); var controller = controller(h, MachineKind.CLIBANO, 8);
        h.runAfterDelay(2, () -> {
            var player = FakePlayerFactory.getMinecraft(h.getLevel()); var position = player.position(); var previousMenu = player.containerMenu;
            var savedInventory = player.getInventory().save(new net.minecraft.nbt.ListTag());
            try {
                bind(h, controller, nativeMachine.getBlockPos()); player.setPos(controller.getBlockPos().getCenter()); player.getInventory().clearContent();
                var menu = new MachineMenu(52, player.getInventory(), controller); player.containerMenu = menu;
                var remoteSlot = menu.slots.stream().filter(s -> s.x == 112 && s.y == 30).findFirst().orElseThrow();
                remoteSlot.set(new ItemStack(ModItems.ARTISAN_RELIC.get()));
                check(nativeMachine.getStack(0).is(ModItems.ARTISAN_RELIC.get()), "Menu insertion did not change actual native slot");
                menu.quickMoveStack(player, remoteSlot.index);
                check(nativeMachine.getStack(0).isEmpty() && player.getInventory().countItem(ModItems.ARTISAN_RELIC.get()) == 1
                      && controller.stock.stream().allMatch(s -> s.isEmpty()), "Shift-click routed native item into controller stock instead of player");
                player.setPos(controller.getBlockPos().getCenter().add(9, 0, 0));
                check(!menu.setRecipe(player, "forbidden_arcanus:clibano_combustion/iron_ingot_from_clibano_combustion") && !remoteSlot.mayPlace(new ItemStack(ModItems.ARTISAN_RELIC.get())), "Distant menu changed native inventory or recipe");
                h.succeed();
            } finally { player.containerMenu = previousMenu; player.setPos(position); player.getInventory().load(savedInventory); stop(controller); }
        });
    }
}
