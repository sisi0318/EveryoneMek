package dev.everyonemek.botania;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.mana.spark.ManaSparkHelper;
import vazkii.botania.common.item.BotaniaItems;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class MechanicalSparkGameTests {
    @GameTest(template = "empty", timeoutTicks = 90)
    public static void dispenserRestoresControllerAndNativeRecessiveRespectsIsolation(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var source = pool(h, pos, 40000); var targetPool = pool(h, pos.east(8), 0);
        var stored = new MechanicalSparkEntity(MechanicalSparks.ENTITY.get(), h.getLevel()); stored.readItem(new ItemStack(MechanicalSparks.MASTER.get()));
        stored.modules.setItem(1, new ItemStack(MechanicalSparks.EFFICIENCY.get(), 2));
        h.setBlock(pos.west(), net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState()
              .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.EAST));
        var dispenser = (net.minecraft.world.level.block.entity.DispenserBlockEntity) h.getBlockEntity(pos.west()); dispenser.setItem(0, stored.dropStack());
        var target = place(h, pos.east(8), new ItemStack(MechanicalSparks.SPARK.get()));
        h.setBlock(pos.west().below(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        h.startSequence().thenWaitUntil(() -> check(ManaSparkHelper.getAttachedSpark(h.getLevel(), h.absolutePos(pos)) instanceof MechanicalSparkEntity,
              "Dispenser failed to install mechanical spark"))
              .thenExecute(() -> {
                  var master = (MechanicalSparkEntity) ManaSparkHelper.getAttachedSpark(h.getLevel(), h.absolutePos(pos));
                  check(master.isMaster() && master.upgrade(1) == 2 && dispenser.isEmpty(), "Dispenser lost controller modules or item consumption");
                  use(h, master, new ItemStack(BotaniaItems.SPARK_AUGMENT_RECESSIVE)); master.tick();
                  check(targetPool.getCurrentMana() == 3000 && source.getCurrentMana() == 37000, "Native recessive did not use shared throughput");
                  use(h, target, new ItemStack(BotaniaItems.SPARK_AUGMENT_ISOLATED)); master.tick();
                  check(targetPool.getCurrentMana() == 3000, "Native isolation accepted recessive transfer");
                  master.discard(); target.discard();
              }).thenSucceed();
    }
    private static MechanicalSparkEntity place(GameTestHelper h, BlockPos pos, ItemStack item) {
        var owner = player(h, "mechanical-spark"); var absolute = h.absolutePos(pos);
        owner.setPos(absolute.getCenter()); owner.setItemInHand(InteractionHand.MAIN_HAND, item);
        check(owner.gameMode.useItemOn(owner, h.getLevel(), item, InteractionHand.MAIN_HAND,
              new BlockHitResult(absolute.getCenter(), Direction.UP, absolute, false)).consumesAction(), "Mechanical spark placement failed");
        check(ManaSparkHelper.getAttachedSpark(h.getLevel(), absolute) instanceof MechanicalSparkEntity, "Native placement lost mechanical type");
        return (MechanicalSparkEntity) ManaSparkHelper.getAttachedSpark(h.getLevel(), absolute);
    }
    private static void use(GameTestHelper h, MechanicalSparkEntity spark, ItemStack item) {
        var owner = player(h, "mechanical-spark"); owner.setPos(spark.position()); owner.setItemInHand(InteractionHand.MAIN_HAND, item);
        check(spark.interact(owner, InteractionHand.MAIN_HAND).consumesAction(), "Spark interaction rejected");
    }
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void sharedUpgradesKeepNativeRolesAndDowngradeAfterConflictOrRemoval(GameTestHelper h) {
        var pos = new BlockPos(10, 3, 20); var source = pool(h, pos, 30000); var targetPool = pool(h, pos.east(20), 0);
        pool(h, pos.east(40), 0);
        var master = place(h, pos, new ItemStack(MechanicalSparks.MASTER.get()));
        var target = place(h, pos.east(20), new ItemStack(MechanicalSparks.SPARK.get()));
        var remote = place(h, pos.east(40), new ItemStack(MechanicalSparks.SPARK.get()));
        check(MechanicalSparkNetworks.network(target).master() == null, "Unconnected spark acquired a controller");
        master.modules.setItem(0, new ItemStack(MechanicalSparks.RANGE.get(), 2));
        use(h, remote, new ItemStack(MechanicalSparks.EFFICIENCY.get(), 2));
        check(master.upgrade(1) == 1 && remote.modules.isEmpty(), "Shared upgrade duplicated into member inventory");
        check(MechanicalSparkNetworks.network(remote).master() == master && MechanicalSparkNetworks.network(master).members() == 3,
              "Controller did not follow connected mechanical sparks");
        target.tick(); check(targetPool.getCurrentMana() == 0, "Master invented native pool transfer without a role");
        use(h, target, new ItemStack(BotaniaItems.SPARK_AUGMENT_DOMINANT)); target.tick();
        check(targetPool.getCurrentMana() == 2000 && source.getCurrentMana() == 28000, "Shared efficiency did not apply to native dominant transfer");
        use(h, target, new ItemStack(Items.RED_DYE)); int previous = targetPool.getCurrentMana(); target.tick();
        check(targetPool.getCurrentMana() == previous && MechanicalSparkNetworks.network(remote).master() == null, "Dye did not split controller path");
        use(h, target, new ItemStack(Items.WHITE_DYE));
        var conflictPos = pos.south(5); pool(h, conflictPos, 0);
        var conflict = place(h, conflictPos, new ItemStack(MechanicalSparks.MASTER.get()));
        check(MechanicalSparkNetworks.network(target).conflict() && MechanicalSparkNetworks.range(target) == 12, "Multiple masters kept shared bonuses");
        target.tick(); check(targetPool.getCurrentMana() == previous, "Conflicted controller kept a long transfer");
        conflict.discard(); check(MechanicalSparkNetworks.range(target) == 28, "Removing conflicting master did not restore upgrades");
        master.discard(); target.tick(); check(targetPool.getCurrentMana() == previous && MechanicalSparkNetworks.range(target) == 12,
              "Removed master left upgrades or long links active");
        target.discard(); remote.discard(); h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void controllerMenuAndNativeWandPreservePhysicalUpgrades(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var machine = machine(h, pos, ManaMachineKind.INFUSER); stop(machine);
        var owner = player(h, "mechanical-spark"); owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BLUE_DYE));
        var master = place(h, pos, new ItemStack(MechanicalSparks.MASTER.get()));
        check(master.isMaster() && master.getNetwork() == DyeColor.BLUE && owner.getOffhandItem().isEmpty(), "Native offhand dye or master placement changed");
        // NeoForge's ordinary FakePlayer ignores openMenu; capture the real provider and open packet.
        var viewer = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(), owner.getGameProfile()) {
            @Override public java.util.OptionalInt openMenu(MenuProvider provider, java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer) {
                containerMenu = provider.createMenu(97, getInventory(), this);
                var data = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registryAccess());
                try { writer.accept(data); check(data.readVarInt() == master.getId() && data.readVarInt() == master.getId(), "Controller open packet IDs differ from its inventory"); }
                finally { data.release(); }
                return java.util.OptionalInt.of(97);
            }
        };
        viewer.setPos(master.position()); master.interact(viewer, InteractionHand.MAIN_HAND);
        check(viewer.containerMenu instanceof SparkControllerMenu, "Empty hand did not open shared upgrade menu");
        var menu = (SparkControllerMenu) viewer.containerMenu;
        var oldSlot = owner.getInventory().getItem(9).copy();
        try {
            viewer.getInventory().setItem(9, new ItemStack(MechanicalSparks.RANGE.get(), 10)); menu.quickMoveStack(viewer, 2);
            check(master.upgrade(0) == 8 && viewer.getInventory().getItem(9).getCount() == 2, "Controller shift click exceeded eight upgrades");
            use(h, master, new ItemStack(BotaniaItems.SPARK_AUGMENT_ISOLATED));
            use(h, master, new ItemStack(BotaniaItems.PHANTOM_INK));
            check(master.getUpgrade().is(BotaniaItems.SPARK_AUGMENT_ISOLATED) && master.isInvisible(), "Native augment or ink was lost");
            var tag = new CompoundTag(); master.saveWithoutId(tag);
            var restored = new MechanicalSparkEntity(MechanicalSparks.ENTITY.get(), h.getLevel()); restored.load(tag);
            check(restored.isMaster() && restored.upgrade(0) == 8 && restored.getNetwork() == DyeColor.BLUE && restored.isInvisible()
                  && restored.getUpgrade().is(BotaniaItems.SPARK_AUGMENT_ISOLATED), "Entity save lost native state or controller inventory");
            restored.discard();
            owner.setShiftKeyDown(true);
            use(h, master, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST));
            check(master.isAlive() && master.getUpgrade().isEmpty() && master.upgrade(0) == 8, "Native wand removed controller before augment");
            use(h, master, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST));
            var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, master.getBoundingBox().inflate(3));
            var disk = drops.stream().filter(item -> item.getItem().is(MechanicalSparks.MASTER.get())).findFirst().orElseThrow();
            check(!menu.stillValid(owner) && master.modules.isEmpty(), "Removed controller retained live inventory");
            var replacement = place(h, pos, disk.getItem().copy());
            check(replacement.isMaster() && replacement.upgrade(0) == 8, "Native dismantle/reinstall lost controller modules");
            replacement.discard(); drops.forEach(net.minecraft.world.entity.Entity::discard);
        } finally { owner.setShiftKeyDown(false); owner.closeContainer(); owner.getInventory().setItem(9, oldSlot); owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); }
        h.succeed();
    }
}
