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
            runForge(machine, 1); check(machine.observed[0] == 300 && energy - machine.energy().getEnergy() == 3 * machine.energy().getEnergyPerTick(), "Module generation rate or FE charge wrong");
            machine.energy().setEnergy(0); runForge(machine, 110); check(machine.observed[0] == 300, "Unpowered module generated Aureal");
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
            machine.getComponent().addUpgrades(Upgrade.SPEED, 2); int interval = machine.forge.moduleInterval();
            check(interval < 100, "Speed upgrades did not affect the module"); runForge(machine, interval);
            check(machine.observed[0] == 100, "Upgraded module interval wrong");
            h.succeed();
        } finally { stop(machine); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void resourceModulesGenerateIndependentlyAndKeepAppendedSlotsAcrossReload(GameTestHelper h) {
        var machine = forge(h, 3);
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        try {
            Arrays.fill(machine.observed, 0);
            var menu = new MachineMenu(72, player.getInventory(), machine);
            int[] expected = {100, 2, 450, 400};
            for (int resource = 0; resource < 4; resource++) {
                var slot = machine.resourceModules.get(resource);
                check(slot.getLimit(new ItemStack(Content.resourceModule(resource))) == 8, "Module stack limit changed");
                check(!slot.isItemValid(new ItemStack(Content.resourceModule((resource + 1) % 4))), "Module accepted by the wrong resource slot");
                check(slot.createContainerSlot() == null, "Installed storage exposed a second manual upgrade slot");
                slot.setStack(new ItemStack(Content.resourceModule(resource), resource + 1));
            }
            long beforeEnergy = machine.energy().getEnergy();
            runForge(machine, 60);
            var registry = h.getLevel().registryAccess();
            machine.loadAdditional(machine.saveWithoutMetadata(registry), registry);
            runForge(machine, 40);
            for (int resource = 0; resource < 4; resource++) {
                check(machine.observed[resource] == expected[resource] && machine.resourceModuleCount(resource) == resource + 1, "Module consumed itself or restored the wrong generation timer");
                check(machine.supplies.get(resource).isEmpty(), "Generation required or fabricated a resource item");
            }
            check(beforeEnergy - machine.energy().getEnergy() == 10 * machine.energy().getEnergyPerTick(), "Resource modules did not charge FE per active module pulse");
            machine.energy().setEnergy(0); runForge(machine, 100);
            check(Arrays.stream(machine.resourceRates).allMatch(rate -> rate == 0), "Unpowered machine displayed production");
            for (int resource = 0; resource < 4; resource++) check(machine.observed[resource] == expected[resource], "Unpowered resource generation continued");
            machine.energy().setEnergy(machine.energy().getMaxEnergy());
            machine.enabled = false; runForge(machine, 100); machine.enabled = true;
            for (int resource = 0; resource < 4; resource++) check(machine.observed[resource] == expected[resource], "Paused resource generation continued");
            for (int resource = 0; resource < 4; resource++) machine.observed[resource] = machine.capacities[resource] - 1;
            runForge(machine, 100);
            for (int resource = 0; resource < 4; resource++) check(machine.observed[resource] == machine.capacities[resource], "Module exceeded resource capacity");
            beforeEnergy = machine.energy().getEnergy(); runForge(machine, 100);
            check(machine.energy().getEnergy() == beforeEnergy && Arrays.stream(machine.resourceRates).allMatch(rate -> rate == 0), "Full resource storage consumed FE or displayed production");
            machine.stock.get(0).setStack(new ItemStack(Items.DIAMOND, 13));
            machine.enhancers.get(0).setStack(new ItemStack(ModItems.ARTISAN_RELIC.get()));
            ItemStack drop = Block.getDrops(machine.getBlockState(), h.getLevel(), machine.getBlockPos(), machine, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            var restored = new Controller(machine.getBlockPos(), machine.getBlockState()); restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(drop);
            check(restored.getInventorySlots(null).size() == 26 && restored.nativeTier == 3, "Appended module slots changed the inventory layout");
            for (int resource = 0; resource < 4; resource++) check(restored.resourceModuleCount(resource) == resource + 1 && restored.observed[resource] == machine.observed[resource], "Drop lost a resource module or stored resources");
            var oldSlots = machine.getInventorySlots(null).stream().limit(23).map(slot -> slot.getStack().copy()).toList();
            ItemStack oldDrop = drop.copy();
            oldDrop.set(mekanism.common.attachments.containers.ContainerType.ITEM.getComponentType(), new mekanism.common.attachments.containers.item.AttachedItems(oldSlots));
            var from020 = new Controller(machine.getBlockPos(), machine.getBlockState()); from020.setLevel(h.getLevel()); from020.applyComponentsFromItemStack(oldDrop);
            for (int slot = 0; slot < 23; slot++) check(ItemStack.matches(from020.getInventorySlots(null).get(slot).getStack(), oldSlots.get(slot)), "0.2.0 block item lost slot " + slot);
            check(from020.resourceModules.stream().skip(1).allMatch(slot -> slot.isEmpty()) && from020.nativeTier == 3, "Old item did not gain empty new module slots");
            h.succeed();
        } finally { stop(machine); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void resourceModuleCraftingRequiresTheSelectedCoreAndAFullBloodTube(GameTestHelper h) {
        var player = FakePlayerFactory.getMinecraft(h.getLevel()); var oldMenu = player.containerMenu; var position = player.position();
        var inventory = player.getInventory().save(new net.minecraft.nbt.ListTag());
        try {
            player.getInventory().clearContent();
            BlockPos table = h.absolutePos(new BlockPos(4, 2, 4));
            h.getLevel().setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState()); player.setPos(table.getCenter());
            String[] ids = {"soul_module", "blood_module", "experience_module"};
            for (int resource = 1; resource < 4; resource++) {
                var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("forbiddenmekanism:" + ids[resource - 1])).orElseThrow();
                var recipe = (ShapelessRecipe) holder.value();
                var items = recipe.getIngredients().stream().map(ingredient -> ingredient.getItems()[0].copyWithCount(1)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                check(items.size() == 4 && items.get(2).is(ModBlocks.ARCANE_POLISHED_DARKSTONE.get().asItem()) && items.get(3).is(ModItems.MUNDABITUR_DUST.get()), "Module no longer follows the Aureal recipe materials");
                while (items.size() < 9) items.add(ItemStack.EMPTY);
                check(recipe.matches(CraftingInput.of(3, 3, items), h.getLevel()), "Module recipe is not craftable as shown in JEI");
                var core = items.getFirst();
                if (resource == 1) check(core.is(Content.SOUL_BLOCK.asItem()), "Soul core changed");
                if (resource == 3) check(core.is(Content.XPETRIFIED_BLOCK.asItem()), "Experience core is not a petrified experience block");
                if (resource == 2) {
                    check(core.is(ModItems.BLOOD_TEST_TUBE.get()) && core.get(ModDataComponents.ESSENCE_STORAGE).value().amount() == 3000, "JEI does not show a full blood tube");
                    var incomplete = new ArrayList<>(items); incomplete.set(0, new ItemStack(ModItems.TEST_TUBE.get()));
                    check(!recipe.matches(CraftingInput.of(3, 3, incomplete), h.getLevel()), "Empty tube crafted a blood module");
                    incomplete.set(0, new ItemStack(ModItems.BLOOD_TEST_TUBE.get()));
                    check(!recipe.matches(CraftingInput.of(3, 3, incomplete), h.getLevel()), "Zero-blood container crafted a blood module");
                    incomplete.get(0).set(ModDataComponents.ESSENCE_STORAGE, new EssenceStorage(EssenceValue.of(EssenceType.BLOOD, 2999), 3000, true));
                    check(!recipe.matches(CraftingInput.of(3, 3, incomplete), h.getLevel()), "Partially filled tube crafted a blood module");
                    core.set(DataComponents.CUSTOM_NAME, Component.literal("Full blood"));
                    check(recipe.matches(CraftingInput.of(3, 3, items), h.getLevel()), "Named full tube was incorrectly rejected");
                }
                var menu = new CraftingMenu(80 + resource, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), table)); player.containerMenu = menu;
                for (int i = 0; i < 9; i++) menu.getSlot(i + 1).set(items.get(i).copy());
                check(menu.getSlot(0).getItem().is(Content.resourceModule(resource)), "Actual workbench did not craft the resource module");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                check(menu.getCarried().is(Content.resourceModule(resource)), "Workbench output was not received");
                for (int i = 1; i <= 9; i++) check(menu.getSlot(i).getItem().isEmpty(), "Crafting did not consume the complete module materials");
            }
            h.succeed();
        } finally { player.containerMenu = oldMenu; player.setPos(position); player.getInventory().load(inventory); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void sharedMekUpgradeSlotsInstallUninstallAndPreservePendingItems(GameTestHelper h) {
        var machine = forge(h, 2);
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        var oldMenu = player.containerMenu; var position = player.position();
        try {
            player.setPos(machine.getBlockPos().getCenter());
            var menu = new MachineMenu(93, player.getInventory(), machine); player.containerMenu = menu;
            menu.setSelectedWindow(player.getUUID(), new mekanism.common.inventory.container.SelectedWindowData(mekanism.common.inventory.container.SelectedWindowData.WindowType.UPGRADE));
            var input = machine.getComponent().getUpgradeSlot();
            var output = machine.getComponent().getUpgradeOutputSlot();
            ItemStack modules = new ItemStack(Content.SOUL_MODULE.get(), 8);
            modules.set(DataComponents.CUSTOM_NAME, Component.literal("Stored souls"));
            machine.resourceModules.get(1).setStack(modules.copyWithCount(2));
            check(input.insertItem(modules, Action.SIMULATE, AutomationType.EXTERNAL).getCount() == 8, "External automation reached resource installation");
            check(menu.getUpgradeSlot().mayPlace(modules), "Mek shared input refused resource module");
            menu.setCarried(modules.copy()); menu.clicked(menu.getUpgradeSlot().index, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && input.getCount() == 8, "Actual upgrade slot click lost modules");
            for (int i = 0; i < 20; i++) machine.getComponent().tickServer();
            check(machine.resourceModuleCount(1) == 2 && machine.getComponent().getScaledUpgradeProgress() == 1, "Install ignored Mek progress");
            machine.getComponent().tickServer();
            check(machine.resourceModuleCount(1) == 8 && input.getCount() == 2, "Installation failed to preserve overflow");
            var registry = h.getLevel().registryAccess();
            machine.loadAdditional(machine.saveWithoutMetadata(registry), registry);
            check(machine.resourceModuleCount(1) == 8 && input.getCount() == 2, "Reload lost installed or pending modules");
            output.setStack(new ItemStack(Content.BLOOD_MODULE.get()));
            check(!menu.clickMenuButton(player, 33) && machine.resourceModuleCount(1) == 8, "Blocked output removed an installed module");
            output.setEmpty();
            check(menu.clickMenuButton(player, 33) && machine.resourceModuleCount(1) == 7 && output.getCount() == 1, "Single uninstall did not use shared output");
            check(ItemStack.isSameItemSameComponents(output.getStack(), modules), "Uninstall lost item components");
            player.setPos(machine.getBlockPos().getCenter().add(9, 0, 0));
            check(!menu.clickMenuButton(player, 37) && machine.resourceModuleCount(1) == 7, "Distant player uninstalled modules");
            player.setPos(machine.getBlockPos().getCenter());
            check(menu.clickMenuButton(player, 37) && machine.resourceModuleCount(1) == 0 && output.getCount() == 8, "Shift uninstall lost modules");
            menu.clicked(menu.getUpgradeSlot().index, 0, ClickType.PICKUP, player);
            check(menu.getCarried().getCount() == 2 && input.isEmpty(), "Pending input could not be recovered");
            menu.setCarried(new ItemStack(mekanism.common.registries.MekanismItems.SPEED_UPGRADE.get()));
            menu.clicked(menu.getUpgradeSlot().index, 0, ClickType.PICKUP, player);
            for (int i = 0; i < 21; i++) machine.getComponent().tickServer();
            check(input.isEmpty() && machine.getComponent().getUpgrades(Upgrade.SPEED) == 1, "Native Mek installation stopped working");
            h.succeed();
        } finally { player.containerMenu = oldMenu; player.setPos(position); stop(machine); }
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void compressedMaterialsCraftAndUnpackWithoutLoss(GameTestHelper h) {
        var player = FakePlayerFactory.getMinecraft(h.getLevel()); var oldMenu = player.containerMenu; var position = player.position();
        try {
            BlockPos table = h.absolutePos(new BlockPos(4, 2, 4));
            h.getLevel().setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState()); player.setPos(table.getCenter());
            Item[] materials = {ModItems.SOUL.get(), ModItems.XPETRIFIED_ORB.get()};
            Block[] blocks = {Content.SOUL_BLOCK.get(), Content.XPETRIFIED_BLOCK.get()};
            for (int material = 0; material < 2; material++) {
                var menu = new CraftingMenu(95 + material, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), table)); player.containerMenu = menu;
                for (int i = 1; i <= 9; i++) menu.getSlot(i).set(new ItemStack(materials[material]));
                check(menu.getSlot(0).getItem().is(blocks[material].asItem()), "Nine materials did not compress in workbench");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                ItemStack compressed = menu.getCarried().copy(); menu.setCarried(ItemStack.EMPTY);
                for (int i = 1; i <= 9; i++) check(menu.getSlot(i).getItem().isEmpty(), "Compression left duplicate materials");
                BlockPos pos = table.above(); h.getLevel().setBlockAndUpdate(pos, blocks[material].defaultBlockState());
                var drops = Block.getDrops(blocks[material].defaultBlockState(), h.getLevel(), pos, null, null, new ItemStack(Items.DIAMOND_PICKAXE));
                check(drops.size() == 1 && drops.getFirst().is(blocks[material].asItem()) && drops.getFirst().getCount() == 1, "Placed compression block did not drop itself");
                menu.getSlot(5).set(compressed);
                check(menu.getSlot(0).getItem().is(materials[material]) && menu.getSlot(0).getItem().getCount() == 9, "Compressed block failed to unpack");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                check(menu.getCarried().getCount() == 9 && menu.getSlot(5).getItem().isEmpty(), "Unpacking duplicated or lost materials");
            }
            h.succeed();
        } finally { player.containerMenu = oldMenu; player.setPos(position); }
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
    public static void clibanoUsesElectricHeatAndPreservesFuelAndProgress(GameTestHelper h) {
        var clibano = clibano(h); var controller = controller(h, MachineKind.CLIBANO, 8);
        h.runAfterDelay(2, () -> {
            try {
                clibano.setStack(3, new ItemStack(Items.RAW_IRON)); clibano.setStack(2, new ItemStack(Items.COAL));
                ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                var data = ((ClibanoAccess) clibano).forbiddenmekanism$data(); int savedBurn = data.get(1);
                check(savedBurn > 0 && clibano.getStack(2).isEmpty(), "Uncontrolled native furnace no longer consumes ordinary fuel");
                bind(h, controller, clibano.getBlockPos()); controller.stock.get(0).setStack(new ItemStack(Items.RAW_COPPER));
                var oldFuel = new net.minecraft.nbt.CompoundTag(); oldFuel.put("item", new ItemStack(Items.COAL, 3).save(h.getLevel().registryAccess()));
                controller.supplies.get(0).deserializeNBT(h.getLevel().registryAccess(), oldFuel);
                clibano.setStack(2, new ItemStack(Items.COAL, 2)); controller.onUpdateServer();
                check(outputs(controller, Items.COAL) == 5 && clibano.getStack(2).isEmpty() && controller.supplies.get(0).isEmpty(), "Legacy fuel was not returned intact");
                int progress = data.get(3); long energy = controller.energy().getEnergy();
                controller.enabled = false;
                ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                check(data.get(3) == progress && controller.energy().getEnergy() == energy, "Pause consumed heat or advanced processing");
                controller.enabled = true; controller.energy().setEnergy(0);
                ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                check(data.get(3) == progress && data.get(1) == savedBurn, "Power loss used stored fuel or changed progress");
                controller.energy().setEnergy(energy);
                ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                long expectedCost = mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertTo(MachineConfig.CLIBANO_HEAT_FE.get());
                check(controller.energy().getEnergy() == energy - expectedCost && data.get(3) > progress, "Electric heat did not charge once per productive tick");
                long beforeBlocked = controller.energy().getEnergy(); int blockedProgress = data.get(3);
                clibano.setStack(5, new ItemStack(Items.STONE, 64)); clibano.setStack(6, new ItemStack(Items.STONE, 64));
                try {
                    ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                    check(controller.energy().getEnergy() == beforeBlocked && data.get(3) == blockedProgress, "Full outputs consumed heat or lost progress");
                } finally { clibano.setStack(5, ItemStack.EMPTY); clibano.setStack(6, ItemStack.EMPTY); }
                runClibano(controller, clibano, 25);
                check(!clibano.getStack(3).isEmpty() && !clibano.getStack(4).isEmpty(), "Native independent inputs were not both fed");
                runClibano(controller, clibano, 150);
                check(outputs(controller, Items.IRON_INGOT) == 1 && outputs(controller, Items.COPPER_INGOT) == 1, "Native independent products missing: " + controller.status);
                check(clibano.getResiduesStorage().getTotalAmount() == 0, "Ordinary fire incorrectly generated residues");
                check(data.get(1) == savedBurn && outputs(controller, Items.COAL) == 5, "Electric processing spent the reserved combustion time or recovered fuel");
                long idleEnergy = controller.energy().getEnergy();
                ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                check(controller.energy().getEnergy() == idleEnergy, "Idle furnace consumed heating energy");
                controller.binding.release();
                ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
                check(data.get(1) == savedBurn - 1, "Removing electric control did not restore native combustion");
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
                controller.supplies.get(1).setStack(new ItemStack(ModItems.ENCHANTED_SOUL.get()));
                runClibano(controller, clibano, 20); check(clibano.getStack(3).isEmpty(), "Alloy bypassed enhancer requirement");
                clibano.setStack(0, new ItemStack(ModItems.ARTISAN_RELIC.get())); runClibano(controller, clibano, 150);
                check(outputs(controller, ModItems.OBSIDIANSTEEL_INGOT.get()) == 1, "Native alloy did not combine both inputs");
                var data = ((ClibanoAccess) clibano).forbiddenmekanism$data(); int soulTime = data.get(0), burnTime = data.get(1);
                check(data.get(7) == 2 && soulTime > 0, "Enchanted soul did not set native fire");
                int pausedProgress = data.get(3);
                controller.enabled = false; runClibano(controller, clibano, 10);
                check(data.get(0) == soulTime - 10 && data.get(1) == burnTime && data.get(3) == pausedProgress, "Pause changed native soul timing or continued electric processing");
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
            controller.stock.get(0).setStack(new ItemStack(Items.RAW_IRON));
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
