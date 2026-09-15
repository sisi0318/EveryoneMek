package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.*;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.*;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityEnrichmentChamber;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }
    static ServerPlayer player(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos.below(), Blocks.STONE);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "overload-test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.getLevel().addNewPlayer(player);
        h.onEachTick(() -> { if (!player.isRemoved()) player.doTick(); });
        player.setPos(h.absolutePos(pos).getX()+.5, h.absolutePos(pos).getY(), h.absolutePos(pos).getZ()+.5); return player;
    }
    static void bind(ServerPlayer player) { check(CoreBinding.bind(player, new ItemStack(CoreContent.CORE.get())), "Core binding failed: Curios slot unavailable"); }
    static void remove(ServerPlayer player) { player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
    static List<IInventorySlot> slots(TileEntityConfigurableMachine tile, DataType type) { return ((InventorySlotInfo)tile.getConfig().getConfig(TransmissionType.ITEM).getSlotInfo(type)).getSlots(); }
    static TileEntityEnrichmentChamber enrichment(GameTestHelper h, BlockPos pos, UUID owner) {
        h.setBlock(pos, MekanismBlocks.ENRICHMENT_CHAMBER.get()); var tile=(TileEntityEnrichmentChamber)h.getBlockEntity(pos);
        DeviceScope.placed(tile, owner);
        for(var side:RelativeSide.values())tile.getConfig().getConfig(TransmissionType.ENERGY).setDataType(DataType.INPUT,side);
        tile.getEnergyContainer().setEnergy(tile.getEnergyContainer().getMaxEnergy()); return tile;
    }
    static long stored(TileEntityEnrichmentChamber tile) { return tile.getEnergyContainer().getEnergy(); }
    static int output(TileEntityConfigurableMachine tile) { return slots(tile,DataType.OUTPUT).stream().mapToInt(IInventorySlot::getCount).sum(); }

    @GameTest(template="empty", timeoutTicks=100)
    public static void explicitHoldEquipsOnePendantAndSurvivalCannotRemoveIt(GameTestHelper h) {
        var player=player(h,new BlockPos(20,4,20)); var stack=new ItemStack(CoreContent.CORE.get()); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var context=new SlotContext(CoreBinding.SLOT,player,0,false,true);
        check(!CoreContent.CORE.get().canEquip(context,stack) && !CoreBinding.bound(player),"Unconfirmed drag could bind the pendant");
        CoreContent.CORE.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.startSequence().thenIdle(10).thenExecute(()->check(!CoreBinding.bound(player),"Core bound before the hold completed"))
              .thenWaitUntil(()->check(CoreBinding.bound(player),"Holding use did not equip the pendant"))
              .thenExecute(()->{
                  var equipped=CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler(CoreBinding.SLOT).orElseThrow().getStacks().getStackInSlot(0);
                  check(CoreBinding.matches(player,equipped) && equipped.getCount()==1 && player.getMainHandItem().isEmpty(),"Equip duplicated or lost the physical pendant");
                  check(!CoreContent.CORE.get().canUnequip(context,equipped),"Survival wearer could remove the core");
                  check(CoreContent.CORE.get().getDropRule(context,player.damageSources().generic(),true,equipped)==top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_KEEP,"Core was allowed to drop on death");
                  var saved=CoreBinding.data(player).copy(); CoreBinding.restore(player); CoreBinding.restore(player);
                  check(saved.getUUID("instance").equals(CoreBinding.data(player).getUUID("instance")),"Restoration changed the bound instance");
                  remove(player);
              }).thenSucceed();
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void rangeOwnershipAndExplicitShareDoNotStackOrAffectNeighbours(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20)); bind(wearer);
        var owner=player(h,new BlockPos(25,4,20)); var tile=enrichment(h,new BlockPos(20,4,26),owner.getUUID());
        try {
            check(DeviceScope.bearer(tile)==null,"Public/unshared neighbour was cursed");
            check(!DeviceScope.share(wearer,tile,wearer.getUUID(),true),"Visitor could authorize the owner's device");
            check(DeviceScope.share(owner,tile,wearer.getUUID(),true) && DeviceScope.bearer(tile)==wearer,"Owner's explicit consent was ignored");
            bind(owner); check(DeviceScope.bearer(tile)!=null,"Overlapping bearers lost the device");
            wearer.setPos(h.absolutePos(new BlockPos(75,4,20)).getCenter()); owner.setPos(h.absolutePos(new BlockPos(75,4,25)).getCenter());
            check(DeviceScope.bearer(tile)==null,"A stale scope remained active outside 32 blocks");
            check(DeviceScope.share(owner,tile,wearer.getUUID(),false),"Owner could not revoke consent");
            h.succeed();
        } finally { remove(wearer);remove(owner); }
    }
    @GameTest(template="empty", timeoutTicks=330)
    public static void nativeEnrichmentPaysTwiceAndProducesTwiceWithoutMutatingRecipes(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20)); bind(wearer);
        var cursed=enrichment(h,new BlockPos(20,4,26),wearer.getUUID()); var normal=enrichment(h,new BlockPos(28,4,26),UUID.randomUUID());
        long start=stored(cursed),normalStart=stored(normal);
        slots(cursed,DataType.INPUT).getFirst().setStack(new ItemStack(Items.IRON_ORE));
        slots(normal,DataType.INPUT).getFirst().setStack(new ItemStack(Items.IRON_ORE));
        h.startSequence().thenWaitUntil(()->check(output(cursed)==4 && output(normal)==2,"Native enrichment did not produce base and cursed outputs"))
              .thenExecute(()->{
                  check(start-stored(cursed)==2*(normalStart-stored(normal)),"Working energy was not exactly doubled");
                  check(CoreBinding.data(wearer).getLong("energy")== (normalStart-stored(normal))/4,"Recovered energy was not 25% of the actual surcharge");
                  check(slots(cursed,DataType.INPUT).getFirst().isEmpty(),"Cursed production did not consume its input");
                  var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("mekanism:processing/iron/dust/from_ore")).orElseThrow().value();
                  check(recipe.getResultItem(h.getLevel().registryAccess()).getCount()==2,"Global recipe output was modified");
                  remove(wearer);
              }).thenSucceed();
    }
    @GameTest(template="empty", timeoutTicks=360)
    public static void bonusSpaceIsReservedBeforeAnyPowerAndLateEntryCannotClaimTheBatch(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20)); bind(wearer);
        var blocked=enrichment(h,new BlockPos(20,4,26),wearer.getUUID()); var late=enrichment(h,new BlockPos(64,4,26),wearer.getUUID());
        var dust=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("mekanism:dust_iron")));
        slots(blocked,DataType.OUTPUT).getFirst().setStack(dust.copyWithCount(61));
        slots(blocked,DataType.INPUT).getFirst().setStack(new ItemStack(Items.IRON_ORE)); slots(late,DataType.INPUT).getFirst().setStack(new ItemStack(Items.IRON_ORE));
        long energy=stored(blocked);
        h.startSequence().thenIdle(25).thenExecute(()->{
            check(stored(blocked)==energy && slots(blocked,DataType.INPUT).getFirst().getCount()==1,"Blocked doubled output still spent resources");
            slots(blocked,DataType.OUTPUT).getFirst().setStack(dust.copyWithCount(60));
            wearer.setPos(h.absolutePos(new BlockPos(43,4,20)).getCenter());
        }).thenWaitUntil(()->check(output(blocked)==64 && output(late)==2,"Output reservation or late-entry qualification failed"))
              .thenExecute(()->remove(wearer)).thenSucceed();
    }
    @GameTest(template="empty", timeoutTicks=70)
    public static void metalThresholdRecoversAndStoredPendantEnergyChargesRealMekaTool(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20)); bind(wearer);
        for(int i=0;i<6;i++) wearer.getInventory().setItem(i,new ItemStack(Items.IRON_INGOT,64));
        h.startSequence().thenIdle(12).thenExecute(()->{
            check(MetalLoad.blocksSprint(wearer),"Six stacks of metal did not prevent sprinting");
            wearer.setSprinting(true); check(!wearer.isSprinting(),"Server accepted sprint while overloaded");
            wearer.getInventory().setItem(0,new ItemStack(MekanismItems.MEKA_TOOL.get()));
            CoreBinding.recover(wearer,10000);
        }).thenIdle(12).thenExecute(()->{
            check(!MetalLoad.blocksSprint(wearer),"Unloading metal did not restore sprinting");
            var tool=wearer.getInventory().getItem(0); var handler=tool.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            check(handler!=null && handler.getEnergyStored()>0 && CoreBinding.data(wearer).getLong("energy")<2500,"Recovered energy did not charge the actual tool");
            remove(wearer);
        }).thenSucceed();
    }
    @GameTest(template="empty", timeoutTicks=35)
    public static void recipeCyclesStayExcludedAndMachineConsentSurvivesSerialization(GameTestHelper h) {
        var player=player(h,new BlockPos(20,4,20)); bind(player);
        var tile=enrichment(h,new BlockPos(20,4,26),player.getUUID());
        try {
            var badId=net.minecraft.resources.ResourceLocation.parse("mekanism:processing/iron/dust/from_ingot");
            var bad=h.getLevel().getRecipeManager().byKey(badId).orElseThrow().value();
            check(!BonusRecipes.allowed(h.getLevel(),bad,badId.toString()),"Ingot/dust recycling was given bonus output");
            var save=tile.saveWithFullMetadata(h.getLevel().registryAccess());
            var copy=BlockEntity.loadStatic(tile.getBlockPos(),tile.getBlockState(),save,h.getLevel().registryAccess());
            check(copy!=null && DeviceScope.data(copy).getUUID("placed_by").equals(player.getUUID()),"Machine ownership was not saved");
            var before=CoreBinding.data(player).getLong("energy");
            var handler=Arrays.stream(Direction.values()).map(side -> h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,tile.getBlockPos(),side))
                  .filter(Objects::nonNull).findFirst().orElseThrow(() -> new GameTestAssertException("No energy capability; live="+DeviceScope.live(tile)+", same="+(h.getLevel().getBlockEntity(tile.getBlockPos())==tile)+", config="+tile.getConfig().getConfig(TransmissionType.ENERGY)));
            handler.receiveEnergy(1000,true);handler.extractEnergy(1000,true);
            check(CoreBinding.data(player).getLong("energy")==before,"Simulated IO generated recovery credit");
            h.succeed();
        } finally { remove(player); }
    }
}
