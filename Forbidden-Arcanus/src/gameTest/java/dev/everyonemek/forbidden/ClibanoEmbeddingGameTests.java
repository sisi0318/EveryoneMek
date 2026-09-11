package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.item.mundabitur.MundabiturInteractions;
import com.stal111.forbidden_arcanus.common.item.mundabitur.TransformPatternInteraction.TransformPatternContext;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import java.util.ArrayList;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(ForbiddenMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ClibanoEmbeddingGameTests {
    private static final com.mojang.authlib.GameProfile PROFILE = new com.mojang.authlib.GameProfile(
          java.util.UUID.fromString("11799e71-9b94-4d99-86a0-6b78610ee219"), "[ClibanoTest]");
    private static void stop(Controller controller) {
        controller.enabled = false; controller.energy().setEnergy(0);
        var frequency = controller.getSecurity().getFrequency();
        controller.getSecurity().setOwnerUUID(null);
        if (frequency != null) mekanism.common.lib.frequency.FrequencyType.SECURITY.getFrequencyManager(frequency).remove(frequency.getKey(), frequency.getOwner());
    }
    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }
    private static void base(GameTestHelper h, BlockPos center, Direction front) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            var block = pos.equals(center) ? Blocks.AIR : pos.equals(center.relative(front)) ? ModBlocks.CLIBANO_CORE.get()
                  : pos.distManhattan(center) == 3 ? ModBlocks.POLISHED_DARKSTONE.get() : ModBlocks.POLISHED_DARKSTONE_BRICKS.get();
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        }
    }
    private static BlockHitResult hit(BlockPos pos, Direction side) { return new BlockHitResult(pos.getCenter(), side, pos, false); }
    private static void withUsername(ServerPlayer player, Runnable action) {
        // Headless GameTestServer has no profile service; seed only this fake player's name for real Mek placement.
        try {
            var field = net.neoforged.neoforge.common.UsernameCache.class.getDeclaredField("map"); field.setAccessible(true);
            @SuppressWarnings("unchecked") var names = (java.util.Map<java.util.UUID, String>) field.get(null);
            String previous = names.put(player.getUUID(), player.getGameProfile().getName());
            try { action.run(); }
            finally { if (previous == null) names.remove(player.getUUID()); else names.put(player.getUUID(), previous); }
        } catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
    }
    private static void place(ServerPlayer player, BlockPos pos, Direction side, ItemStack stack) {
        player.setPos(pos.relative(side, 2).getCenter()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        withUsername(player, () -> check(((BlockItem) stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit(pos, side))).consumesAction(), "Part placement failed"));
    }
    private static void install(ServerPlayer player, BlockPos pos, Direction side, ItemStack stack) {
        var previousHand = player.getMainHandItem().copy();
        player.setPos(pos.relative(side, 2).getCenter()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (!previousHand.isEmpty()) player.getInventory().add(previousHand);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit(pos, side));
        withUsername(player, () -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event));
        check(event.isCanceled() && stack.isEmpty() && ClibanoEmbedding.isPart(player.level().getBlockState(pos)), "Right-click did not install exactly one part");
    }
    private static ClibanoMainBlockEntity form(GameTestHelper h, ServerPlayer player, BlockPos center, Direction front) {
        BlockPos core = center.relative(front);
        player.setPos(core.relative(front, 2).getCenter());
        var dust = new ItemStack(ModItems.MUNDABITUR_DUST.get()); player.setItemInHand(InteractionHand.MAIN_HAND, dust);
        check(dust.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(core, front))).consumesAction() && dust.isEmpty(), "Native dust did not form furnace");
        check(h.getLevel().getBlockEntity(center) instanceof ClibanoMainBlockEntity, "Native center entity missing");
        return (ClibanoMainBlockEntity) h.getLevel().getBlockEntity(center);
    }
    private static void configure(ServerPlayer player, Controller controller, Direction side, DataType type) {
        player.setPos(controller.getBlockPos().relative(controller.getDirection(), 2).getCenter());
        var relative = RelativeSide.fromDirections(controller.getDirection(), side);
        var context = (net.neoforged.neoforge.network.handling.IPayloadContext) java.lang.reflect.Proxy.newProxyInstance(
              ClibanoEmbeddingGameTests.class.getClassLoader(), new Class[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},
              (proxy, method, args) -> { if (method.getName().equals("player")) return player; throw new UnsupportedOperationException(method.getName()); });
        for (int i = 0; i < 16 && controller.getConfig().getConfig(TransmissionType.ITEM).getDataType(relative) != type; i++)
            new mekanism.common.network.to_server.configuration_update.PacketSideData(controller.getBlockPos(), mekanism.common.network.MekClickType.LEFT, relative, TransmissionType.ITEM).handle(context);
        check(controller.getConfig().getConfig(TransmissionType.ITEM).getDataType(relative) == type, "Side packet did not apply " + type);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeDustFormsEmbeddedControllersInEveryOrientationAndRejectsDuplicates(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), PROFILE); var position = player.position();
        var saved = player.getInventory().save(new ListTag()); boolean creative = player.getAbilities().instabuild;
        var controllers = new ArrayList<Controller>();
        try {
            player.getInventory().clearContent(); player.getAbilities().instabuild = false;
            int index = 0;
            for (Direction front : Direction.Plane.HORIZONTAL) {
                BlockPos center = h.absolutePos(new BlockPos(7 + index % 2 * 16, 2, 7 + index / 2 * 16)); index++;
                base(h, center, front);
                Direction side = front.getClockWise(); BlockPos wall = center.relative(side);
                h.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
                place(player, wall, side, new ItemStack(Content.MACHINES.get(MachineKind.CLIBANO)));
                var controller = (Controller) h.getLevel().getBlockEntity(wall); controllers.add(controller);
                controller.stock.getFirst().setStack(new ItemStack(Items.DIAMOND, 3));
                for (Direction vertical : new Direction[] {Direction.UP, Direction.DOWN}) {
                    BlockPos port = center.relative(vertical); h.getLevel().setBlockAndUpdate(port, Blocks.AIR.defaultBlockState());
                    place(player, port, vertical, new ItemStack(Content.CLIBANO_PORT));
                }
                BlockPos duplicate = center.relative(side.getOpposite());
                h.getLevel().setBlockAndUpdate(duplicate, Content.MACHINES.get(MachineKind.CLIBANO).defaultState());
                check(!MundabiturInteractions.CREATE_CLIBANO.get().canInteract(new TransformPatternContext(h.getLevel(), center.relative(front), InteractionHand.MAIN_HAND, front)), "Duplicate controllers allowed furnace formation");
                h.getLevel().setBlockAndUpdate(duplicate, ModBlocks.POLISHED_DARKSTONE_BRICKS.get().defaultBlockState());
                var main = form(h, player, center, front);
                controller.onUpdateServer();
                check(h.getLevel().getBlockEntity(wall) == controller && controller.binding.embedded && controller.binding.resolve() == main,
                      "Embedded controller did not automatically connect for " + front);
                check(controller.getDirection() == side && controller.stock.getFirst().getCount() == 3, "Formation changed facing or inventory");
                controller.enabled = false;
            }
            BlockPos immediateCenter = h.absolutePos(new BlockPos(15, 2, 15)), immediateWall = immediateCenter.east();
            base(h, immediateCenter, Direction.NORTH);
            h.getLevel().setBlockAndUpdate(immediateWall, Blocks.AIR.defaultBlockState());
            place(player, immediateWall, Direction.EAST, new ItemStack(Content.MACHINES.get(MachineKind.CLIBANO)));
            var immediateController = (Controller) h.getLevel().getBlockEntity(immediateWall); controllers.add(immediateController);
            immediateController.stock.getFirst().setStack(new ItemStack(Items.DIAMOND, 3));
            var immediateMain = form(h, player, immediateCenter, Direction.NORTH);
            immediateMain.setStack(0, new ItemStack(ModItems.ARTISAN_RELIC.get()));
            // This is the real dust -> break operation before any controller server tick.
            h.getLevel().destroyBlock(immediateWall, true);
            check(h.getLevel().getBlockEntity(immediateCenter) == null, "Immediate controller removal left the original center running");
            var dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                  new net.minecraft.world.phys.AABB(immediateCenter).inflate(3));
            try {
                check(dropped.stream().filter(e -> e.getItem().is(ModItems.ARTISAN_RELIC.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1,
                      "Removing controller lost or duplicated native inventory");
                var controllerDrop = dropped.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                      .filter(s -> s.is(Content.MACHINES.get(MachineKind.CLIBANO).asItem())).findFirst().orElseThrow();
                var attached = controllerDrop.get(mekanism.common.attachments.containers.ContainerType.ITEM.getComponentType());
                check(attached != null && attached.containers().getFirst().is(Items.DIAMOND) && attached.containers().getFirst().getCount() == 3,
                      "Dismantling lost the controller's own item components");
            } finally { dropped.forEach(net.minecraft.world.entity.Entity::discard); }
            h.succeed();
        } finally {
            controllers.forEach(ClibanoEmbeddingGameTests::stop);
            player.setPos(position); player.getInventory().load(saved); player.getAbilities().instabuild = creative;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void installedPortsRoutePowerAndItemsThenStopAndReconnectAfterRepair(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), PROFILE); var position = player.position();
        var saved = player.getInventory().save(new ListTag()); boolean creative = player.getAbilities().instabuild;
        BlockPos center = h.absolutePos(new BlockPos(12, 2, 12));
        player.getInventory().clearContent(); player.getAbilities().instabuild = false;
        ClibanoMainBlockEntity main;
        try { base(h, center, Direction.NORTH); main = form(h, player, center, Direction.NORTH); }
        finally { player.setPos(position); player.getInventory().load(saved); player.getAbilities().instabuild = creative; }
        h.runAfterDelay(2, () -> {
            var savedNow = player.getInventory().save(new ListTag()); var positionNow = player.position(); boolean creativeNow = player.getAbilities().instabuild;
            try {
                player.getInventory().clearContent(); player.getAbilities().instabuild = false;
                main.setStack(0, new ItemStack(ModItems.ARTISAN_RELIC.get()));
                install(player, center.east(), Direction.EAST, new ItemStack(Content.MACHINES.get(MachineKind.CLIBANO)));
                var controller = (Controller) h.getLevel().getBlockEntity(center.east());
                check(controller.binding.embedded && controller.binding.resolve() == main && main.getStack(0).is(ModItems.ARTISAN_RELIC.get()), "Installing controller replaced native entity or inventory");
                for (Direction side : new Direction[] {Direction.UP, Direction.DOWN, Direction.WEST, Direction.SOUTH})
                    install(player, center.relative(side), side, new ItemStack(Content.CLIBANO_PORT));
                check(player.getInventory().countItem(ModBlocks.POLISHED_DARKSTONE_BRICKS.get().asItem()) == 5, "Installed parts did not return exactly five original bricks");
                player.setPos(center.north(3).getCenter()); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.CLIBANO_PORT));
                check(!ClibanoEmbedding.install(player, InteractionHand.MAIN_HAND, hit(center.north(), Direction.NORTH)) && main == h.getLevel().getBlockEntity(center), "Front core could be replaced");
                configure(player, controller, Direction.WEST, DataType.INPUT);
                configure(player, controller, Direction.UP, DataType.EXTRA);
                configure(player, controller, Direction.SOUTH, DataType.OUTPUT);
                configure(player, controller, Direction.DOWN, DataType.INPUT_2);
                var input = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, center.west(), Direction.WEST);
                var fuel = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, center.above(), Direction.UP);
                var souls = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, center.below(), Direction.DOWN);
                var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, center.above(), Direction.UP);
                check(input != null && fuel != null && souls != null && energy != null, "Formed port capabilities missing");
                check(energy.receiveEnergy(5000, true) == 5000 && controller.energy().getEnergy() == 0, "FE simulation mutated energy");
                check(energy.receiveEnergy(5000, false) == 5000, "Top port did not accept real FE");
                check(ItemHandlerHelper.insertItemStacked(input, new ItemStack(Items.RAW_IRON), true).isEmpty()
                      && controller.stock.stream().allMatch(s -> s.isEmpty()), "Item simulation mutated stock");
                check(ItemHandlerHelper.insertItemStacked(input, new ItemStack(Items.RAW_IRON), false).isEmpty(), "West port did not feed materials");
                check(ItemHandlerHelper.insertItemStacked(input, new ItemStack(Items.RAW_COPPER), false).isEmpty(), "Second material rejected");
                check(ItemHandlerHelper.insertItemStacked(fuel, new ItemStack(Items.COAL), false).isEmpty(), "Top port did not feed fuel");
                check(!ItemHandlerHelper.insertItemStacked(souls, new ItemStack(Items.COAL), false).isEmpty(), "Soul port accepted coal");
                h.getLevel().setBlockAndUpdate(center.south(2), mekanism.common.registries.MekanismBlocks.BASIC_LOGISTICAL_TRANSPORTER.defaultState());
                h.getLevel().setBlockAndUpdate(center.south(3), mekanism.common.registries.MekanismBlocks.BASIC_LOGISTICAL_TRANSPORTER.defaultState());
                h.getLevel().setBlockAndUpdate(center.south(4), Blocks.CHEST.defaultBlockState());
                h.runAfterDelay(230, () -> {
                    var inventoryLater = player.getInventory().save(new ListTag()); var positionLater = player.position(); boolean creativeLater = player.getAbilities().instabuild;
                    try {
                        var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(center.south(4));
                        check(chest.countItem(Items.IRON_INGOT) == 1 && chest.countItem(Items.COPPER_INGOT) == 1, "Native products did not eject through configured port into chest: " + controller.status);
                        controller.enabled = false; controller.stock.getFirst().setStack(new ItemStack(Items.DIAMOND, 3));
                        var output = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, center.south(), Direction.SOUTH);
                        controller.outputs.getFirst().setStack(new ItemStack(Items.GOLD_INGOT));
                        check(output.extractItem(0, 1, true).is(Items.GOLD_INGOT), "Output port could not extract");
                        player.getAbilities().instabuild = false;
                        BlockPos broken = center.offset(-1, 1, -1);
                        h.getLevel().destroyBlock(broken, true);
                        check(h.getLevel().getBlockEntity(center) == null && controller.binding.resolve() == null, "Broken frame left furnace connected");
                        check(energy.receiveEnergy(1, false) == 0 && input.insertItem(0, new ItemStack(Items.COAL), false).getCount() == 1
                              && output.extractItem(0, 1, false).isEmpty(), "Cached port handlers transferred through broken structure");
                        h.getLevel().setBlockAndUpdate(broken, ModBlocks.POLISHED_DARKSTONE.get().defaultBlockState());
                        var repaired = form(h, player, center, Direction.NORTH); controller.onUpdateServer();
                        check(repaired != main && controller.binding.resolve() == repaired && controller.stock.getFirst().getCount() == 3
                              && controller.outputs.getFirst().getStack().is(Items.GOLD_INGOT), "Repair lost controller inventory or failed automatic reconnection");
                        check(energy.receiveEnergy(100, false) == 100, "Cached energy handler did not resume after repair");
                        check(output.extractItem(0, 1, true).is(Items.GOLD_INGOT), "Cached item handler did not resume after repair");
                        h.getLevel().destroyBlock(center.below(), true);
                        check(h.getLevel().getBlockEntity(center) == null && output.extractItem(0, 1, false).isEmpty(), "Removing a port failed to dismantle furnace");
                        h.succeed();
                    } finally {
                        stop(controller);
                        player.getInventory().load(inventoryLater); player.setPos(positionLater); player.getAbilities().instabuild = creativeLater;
                        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(center).inflate(4)).forEach(net.minecraft.world.entity.Entity::discard);
                    }
                });
            } finally { player.getInventory().load(savedNow); player.setPos(positionNow); player.getAbilities().instabuild = creativeNow; }
        });
    }
}
