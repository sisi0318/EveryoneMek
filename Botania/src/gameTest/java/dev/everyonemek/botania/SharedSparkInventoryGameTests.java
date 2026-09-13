package dev.everyonemek.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.mana.spark.ManaSparkHelper;
import vazkii.botania.common.item.*;
import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class SharedSparkInventoryGameTests {
    private static MechanicalSparkEntity spark(GameTestHelper h, BlockPos pos, boolean master) {
        pool(h, pos, 0);
        var item = new ItemStack(master ? MechanicalSparks.MASTER.get() : MechanicalSparks.SPARK.get());
        check(MechanicalSparkItem.placing(item, () -> ManaSparkItem.attachSpark(h.getLevel(), h.absolutePos(pos), item, ItemStack.EMPTY)), "Fixture spark failed to attach");
        return (MechanicalSparkEntity) ManaSparkHelper.getAttachedSpark(h.getLevel(), h.absolutePos(pos));
    }
    private static ServerGamePacketListenerImpl listener(GameTestHelper h, ServerPlayer player) {
        // FakePlayer's handler ignores container clicks. Use the real handler, suppressing only outbound network IO.
        return new ServerGamePacketListenerImpl(h.getLevel().getServer(), player.connection.getConnection(), player,
              CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, net.minecraft.network.PacketSendListener callback) { }
        };
    }
    private static ServerboundContainerClickPacket click(SparkControllerMenu menu, int slot, int button, ClickType type, ItemStack prediction) {
        var predictedSlots = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ItemStack>(); predictedSlots.put(slot, ItemStack.EMPTY);
        return new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slot, button, type, prediction, predictedSlots);
    }
    private static int inventory(ServerPlayer player, Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
    @GameTest(template = "empty", timeoutTicks = 70)
    public static void twoPlayersWithStalePredictionsCannotDuplicateSharedUpgradesOrTheirDrop(GameTestHelper h) {
        var master = spark(h, new BlockPos(20, 3, 20), true); var member = spark(h, new BlockPos(24, 3, 20), false);
        check(master.getName().equals(new ItemStack(MechanicalSparks.MASTER.get()).getHoverName())
              && member.getName().equals(new ItemStack(MechanicalSparks.SPARK.get()).getHoverName()), "Entity labels do not distinguish the master");
        master.setCustomName(net.minecraft.network.chat.Component.literal("Named master"));
        check(master.getName().getString().equals("Named master"), "Role name replaced a custom entity name"); master.setCustomName(null);
        master.modules.setItem(0, new ItemStack(MechanicalSparks.RANGE.get(), 8)); master.modules.setItem(1, new ItemStack(MechanicalSparks.EFFICIENCY.get(), 8));
        var a = player(h, "shared-spark-a"); var b = player(h, "shared-spark-b"); a.setPos(master.position()); b.setPos(member.position());
        var oldA = a.connection; var oldB = b.connection; var oldMenuA = a.containerMenu; var oldMenuB = b.containerMenu;
        var menuA = new SparkControllerMenu(103, a.getInventory(), master.getId(), master.getId());
        var menuB = new SparkControllerMenu(104, b.getInventory(), member.getId(), master.getId());
        a.containerMenu = menuA; b.containerMenu = menuB;
        try {
            var netA = listener(h, a); var netB = listener(h, b);
            check(menuA.stillValid(a) && menuB.stillValid(b), "Shared menus did not access the same active master");
            var takeA = click(menuA, 0, 0, ClickType.PICKUP, new ItemStack(MechanicalSparks.RANGE.get(), 8));
            var takeB = click(menuB, 0, 0, ClickType.PICKUP, new ItemStack(MechanicalSparks.RANGE.get(), 8));
            long tick = h.getLevel().getGameTime();
            netA.handleContainerClick(takeA); netB.handleContainerClick(takeB);
            check(h.getLevel().getGameTime() == tick && menuA.getCarried().getCount() == 8 && menuB.getCarried().isEmpty() && master.modules.getItem(0).isEmpty(),
                  "Two same-tick clicks created two copies of the shared stack");
            netA.handleContainerClick(click(menuA, 0, 0, ClickType.PICKUP, ItemStack.EMPTY));
            // Reverse arrival order while both clients again predict the full original stack.
            netB.handleContainerClick(takeB); netA.handleContainerClick(takeA);
            check(menuB.getCarried().getCount() == 8 && menuA.getCarried().isEmpty() && master.modules.getItem(0).isEmpty(), "Reverse packet order duplicated upgrades");
            netB.handleContainerClick(click(menuB, 0, 0, ClickType.PICKUP, ItemStack.EMPTY));
            var shiftA = click(menuA, 1, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY);
            var shiftB = click(menuB, 1, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY);
            netA.handleContainerClick(shiftA); netB.handleContainerClick(shiftB);
            check(inventory(a, MechanicalSparks.EFFICIENCY.get()) + inventory(b, MechanicalSparks.EFFICIENCY.get()) == 8 && master.modules.getItem(1).isEmpty(),
                  "Two shift-clicks copied a shared upgrade stack");
            member.setNetwork(DyeColor.BLUE);
            netB.handleContainerClick(takeB);
            check(menuB.getCarried().isEmpty() && master.upgrade(0) == 8, "A disconnected member retained access to controller modules");
            member.setNetwork(master.getNetwork());
            netA.handleContainerClick(click(menuA, 0, 1, ClickType.PICKUP, new ItemStack(MechanicalSparks.RANGE.get(), 4)));
            check(menuA.getCarried().getCount() == 4 && master.upgrade(0) == 4, "Right-click did not split the physical stack");
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST)); a.setShiftKeyDown(true);
            master.interact(a, InteractionHand.MAIN_HAND);
            var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, master.getBoundingBox().inflate(3));
            var drop = drops.stream().filter(item -> item.getItem().is(MechanicalSparks.MASTER.get())).findFirst().orElseThrow();
            netB.handleContainerClick(takeB); netB.handleContainerClick(shiftB);
            var saved = new MechanicalSparkEntity(MechanicalSparks.ENTITY.get(), h.getLevel()); saved.readItem(drop.getItem());
            check(menuA.getCarried().getCount() + saved.upgrade(0) == 8 && saved.upgrade(1) == 0 && menuB.getCarried().isEmpty() && master.modules.isEmpty(),
                  "Dismantle and late clicks duplicated the upgrades in the dropped master");
            check(menuA.quickMoveStack(a, 0).isEmpty() && !menuB.stillValid(b), "Removed controller remained writable");
            saved.discard(); drops.forEach(net.minecraft.world.entity.Entity::discard);
        } finally {
            a.setShiftKeyDown(false); menuA.setCarried(ItemStack.EMPTY); menuB.setCarried(ItemStack.EMPTY);
            a.getInventory().clearContent(); b.getInventory().clearContent(); a.containerMenu = oldMenuA; b.containerMenu = oldMenuB;
            a.connection = oldA; b.connection = oldB; master.discard(); member.discard();
        }
        h.succeed();
    }
}
