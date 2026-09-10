package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.*;
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
    private static HephaestusForgeBlockEntity forge(GameTestHelper h, HephaestusForgeLevel tier) {
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
        h.setBlock(new BlockPos(12, 2, 12), tier.getBlock().defaultBlockState().setValue(HephaestusForgeBlock.ACTIVATED, true));
        int[][] offsets = {{0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}};
        for (int[] offset : offsets) h.setBlock(new BlockPos(12 + offset[0], 2, 12 + offset[1]), ModBlocks.DARKSTONE_PEDESTAL.get());
        var forge = (HephaestusForgeBlockEntity) h.getBlockEntity(new BlockPos(12, 2, 12));
        forge.setEssences(tier.getMaxEssences());
        return forge;
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
    private static void runForge(Controller controller, HephaestusForgeBlockEntity forge, int ticks) {
        for (int i = 0; i < ticks; i++) {
            controller.onUpdateServer();
            HephaestusForgeBlockEntity.serverTick(forge.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
        }
    }
    private static void runClibano(Controller controller, ClibanoMainBlockEntity clibano, int ticks) {
        for (int i = 0; i < ticks; i++) {
            controller.onUpdateServer();
            ClibanoMainBlockEntity.serverTick(clibano.getLevel(), clibano.getBlockPos(), clibano.getBlockState(), clibano);
        }
    }
    private static void stop(Controller controller) { controller.enabled = false; controller.energy().setEnergy(0); }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void originalRitualRunsTwoBatchesWithNativeCostsAndHammerWear(GameTestHelper h) {
        var forge = forge(h, HephaestusForgeLevel.THREE); var controller = controller(h, MachineKind.FORGE, 6);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, forge.getBlockPos()); stock(h, controller, "eternal_stella", 2);
                controller.hammer.setStack(new ItemStack(ModItems.DIAMOND_BLACKSMITH_GAVEL.get()));
                int beforeAureal = forge.getEssenceManager().getAureal(), beforeBlood = forge.getEssenceManager().getBlood();
                runForge(controller, forge, 20);
                check(controller.phase == 2 && forge.getRitualManager().isRitualActive(), "Forge failed to start: " + controller.status);
                check(outputs(controller, ModItems.ETERNAL_STELLA.get()) == 0 && !forge.getStack(4).isEmpty(), "Running center was extracted");
                runForge(controller, forge, 1100);
                check(outputs(controller, ModItems.ETERNAL_STELLA.get()) == 2, "Two native outputs missing: " + controller.status + " phase=" + controller.phase);
                check(controller.hammer.getStack().getDamageValue() == 100, "Native hammer damage was not charged exactly once per batch");
                check(beforeAureal - forge.getEssenceManager().getAureal() == 164 && beforeBlood - forge.getEssenceManager().getBlood() == 2000, "Native essence costs changed");
                check(controller.stock.stream().allMatch(s -> s.isEmpty()) && ForgeAutomation.pedestals(forge).stream().noneMatch(p -> p.hasStack()), "Batch inputs were not consumed once");
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void brokenHammerFinishesCurrentRitualAndInstalledModuleStartsNext(GameTestHelper h) {
        var forge = forge(h, HephaestusForgeLevel.THREE); var controller = controller(h, MachineKind.FORGE, 6);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, forge.getBlockPos()); stock(h, controller, "eternal_stella", 2);
                ItemStack hammer = new ItemStack(ModItems.DIAMOND_BLACKSMITH_GAVEL.get()); hammer.setDamageValue(hammer.getMaxDamage() - 1);
                controller.hammer.setStack(hammer); runForge(controller, forge, 600);
                check(outputs(controller, ModItems.ETERNAL_STELLA.get()) == 1 && controller.hammer.isEmpty(), "Breaking hammer lost current batch or started another");
                controller.module.setStack(new ItemStack(Content.INFINITE_HAMMER_MODULE.get()));
                ItemStack spare = new ItemStack(ModItems.DIAMOND_BLACKSMITH_GAVEL.get()); spare.setDamageValue(7); controller.hammer.setStack(spare);
                runForge(controller, forge, 20); check(controller.phase == 2, "Module did not start next batch");
                controller.enabled = false; runForge(controller, forge, 510);
                check(!forge.getRitualManager().isRitualActive() && outputs(controller, ModItems.ETERNAL_STELLA.get()) == 1, "Pause cancelled native work or collected without permission");
                controller.enabled = true; runForge(controller, forge, 20);
                check(outputs(controller, ModItems.ETERNAL_STELLA.get()) == 2 && controller.hammer.getStack().getDamageValue() == 7 && controller.module.getCount() == 1,
                      "Infinite module consumed a tool, module or batch");
                check(controller.binding.actor().getMainHandItem().isEmpty(), "Transient activation tool leaked");
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeTierUpgradePreservesForgeInventoryAndStopsAfterOneUpgrade(GameTestHelper h) {
        var forge = forge(h, HephaestusForgeLevel.ONE); var controller = controller(h, MachineKind.FORGE, 6);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, forge.getBlockPos()); stock(h, controller, "upgrade_tier_2", 1);
                forge.setStack(0, new ItemStack(ModItems.ARTISAN_RELIC.get())); controller.module.setStack(new ItemStack(Content.INFINITE_HAMMER_MODULE.get()));
                runForge(controller, forge, 600);
                check(((HephaestusForgeBlock) forge.getBlockState().getBlock()).getLevel() == HephaestusForgeLevel.TWO, "Native tier did not upgrade: " + controller.status);
                check(h.getLevel().getBlockEntity(forge.getBlockPos()) == forge && forge.getStack(0).is(ModItems.ARTISAN_RELIC.get()), "Tier upgrade replaced native inventory");
                check(!controller.enabled && controller.phase == 0 && controller.outputs.stream().allMatch(s -> s.isEmpty()), "Tier upgrade treated as item production or repeated");
                check(forge.getEssenceManager().getAureal() == 500 && forge.getEssenceManager().getBlood() == 4000, "Tier upgrade native costs changed");
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeTransmutationKeepsItemComponentsAcrossSavedBatch(GameTestHelper h) {
        var forge = forge(h, HephaestusForgeLevel.TWO); var controller = controller(h, MachineKind.FORGE, 6);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, forge.getBlockPos()); stock(h, controller, "draco_arcanus_boots", 1);
                var main = controller.stock.stream().filter(s -> s.getStack().is(Items.NETHERITE_BOOTS)).findFirst().orElseThrow();
                ItemStack boots = main.getStack().copy(); boots.set(DataComponents.CUSTOM_NAME, Component.literal("Saved boots")); boots.setDamageValue(11); main.setStack(boots);
                controller.module.setStack(new ItemStack(Content.INFINITE_HAMMER_MODULE.get())); runForge(controller, forge, 35);
                check(controller.phase == 2, "Transmutation failed to start: " + controller.status);
                var registry = h.getLevel().registryAccess(); var savedController = controller.saveWithoutMetadata(registry); var savedForge = forge.saveWithoutMetadata(registry);
                controller.loadAdditional(savedController, registry); forge.loadWithComponents(savedForge, registry);
                ItemStack drop = Block.getDrops(controller.getBlockState(), h.getLevel(), controller.getBlockPos(), controller, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
                h.getLevel().removeBlock(controller.getBlockPos(), false);
                var moved = controller(h, MachineKind.FORGE, 18);
                try {
                    moved.applyComponentsFromItemStack(drop); bind(h, moved, forge.getBlockPos()); runForge(moved, forge, 550);
                    ItemStack result = moved.outputs.stream().map(s -> s.getStack()).filter(s -> s.is(ModItems.DRACO_ARCANUS_BOOTS.get())).findFirst().orElse(ItemStack.EMPTY);
                    check(!result.isEmpty() && result.getDamageValue() == 11 && Component.literal("Saved boots").equals(result.get(DataComponents.CUSTOM_NAME)),
                          "Native transmutation or relocated saved batch lost item components: " + moved.status);
                } finally { stop(moved); }
                h.succeed();
            } finally { stop(controller); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void staleNativeMenuCannotMoveItemsAfterStructureBreakAndDropsKeepOnlyOwnedInventory(GameTestHelper h) {
        var forge = forge(h, HephaestusForgeLevel.THREE); var controller = controller(h, MachineKind.FORGE, 6);
        h.runAfterDelay(2, () -> {
            try {
                bind(h, controller, forge.getBlockPos()); forge.setStack(0, new ItemStack(ModItems.ARTISAN_RELIC.get()));
                controller.stock.get(0).setStack(new ItemStack(Items.DIAMOND, 13)); controller.hammer.setStack(new ItemStack(ModItems.DIAMOND_BLACKSMITH_GAVEL.get()));
                controller.module.setStack(new ItemStack(Content.INFINITE_HAMMER_MODULE.get())); controller.getComponent().addUpgrades(Upgrade.SPEED, 2);
                var remote = NativeInventory.menu(controller);
                check(remote.extractItem(0, 1, true).is(ModItems.ARTISAN_RELIC.get()) && forge.getStack(0).getCount() == 1, "Native menu simulation mutated inventory");
                BlockPos damaged = forge.getBlockPos().below(); var original = h.getLevel().getBlockState(damaged);
                h.getLevel().setBlockAndUpdate(damaged, Blocks.AIR.defaultBlockState());
                check(remote.extractItem(0, 1, false).isEmpty() && forge.getStack(0).getCount() == 1, "Stale native handler extracted after structure damage");
                h.getLevel().setBlockAndUpdate(damaged, original); check(controller.binding.resolve() == forge, "Repaired structure did not recover");
                ItemStack drop = Block.getDrops(controller.getBlockState(), h.getLevel(), controller.getBlockPos(), controller, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
                drop = ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow();
                var restored = new Controller(controller.getBlockPos(), controller.getBlockState()); restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(drop);
                check(restored.stock.get(0).getCount() == 13 && restored.hammer.getCount() == 1 && restored.module.getCount() == 1 && restored.getComponent().getUpgrades(Upgrade.SPEED) == 2, "Dropped controller lost owned items or upgrades");
                check(restored.getInventorySlots(null).stream().noneMatch(s -> s.getStack().is(ModItems.ARTISAN_RELIC.get())) && forge.getStack(0).getCount() == 1, "Native enhancer duplicated into controller drop");
                var other = controller(h, MachineKind.FORGE, 18);
                try { check(!other.binding.bind(FakePlayerFactory.getMinecraft(h.getLevel()), forge.getBlockPos()), "Second controller acquired an owned forge"); }
                finally { stop(other); }
                h.succeed();
            } finally { stop(controller); }
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
        var forge = forge(h, HephaestusForgeLevel.THREE); var controller = controller(h, MachineKind.FORGE, 6);
        h.runAfterDelay(2, () -> {
            var player = FakePlayerFactory.getMinecraft(h.getLevel()); var position = player.position(); var previousMenu = player.containerMenu;
            var savedInventory = player.getInventory().save(new net.minecraft.nbt.ListTag());
            try {
                bind(h, controller, forge.getBlockPos()); player.setPos(controller.getBlockPos().getCenter()); player.getInventory().clearContent();
                var menu = new MachineMenu(52, player.getInventory(), controller); player.containerMenu = menu;
                var remoteSlot = menu.slots.stream().filter(s -> s.x == 112 && s.y == 30).findFirst().orElseThrow();
                remoteSlot.set(new ItemStack(ModItems.ARTISAN_RELIC.get()));
                check(forge.getStack(0).is(ModItems.ARTISAN_RELIC.get()), "Menu insertion did not change actual native slot");
                menu.quickMoveStack(player, remoteSlot.index);
                check(forge.getStack(0).isEmpty() && player.getInventory().countItem(ModItems.ARTISAN_RELIC.get()) == 1
                      && controller.stock.stream().allMatch(s -> s.isEmpty()), "Shift-click routed native item into controller stock instead of player");
                player.setPos(controller.getBlockPos().getCenter().add(9, 0, 0));
                check(!menu.setRecipe(player, "forbidden_arcanus:eternal_stella") && !remoteSlot.mayPlace(new ItemStack(ModItems.ARTISAN_RELIC.get())), "Distant menu changed native inventory or recipe");
                h.succeed();
            } finally { player.containerMenu = previousMenu; player.setPos(position); player.getInventory().load(savedInventory); stop(controller); }
        });
    }
}
