package dev.everyonemek.overloadcore;

import com.mojang.authlib.GameProfile;
import java.util.*;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class ThunderWardGameTests {
    private record Fixture(ServerPlayer player, List<Packet<?>> packets) { }
    private static void check(boolean condition, String message) { CoreGameTests.check(condition, message); }
    private static long price() { return EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.WARD_COST_FE.get().longValue()); }
    private static Fixture player(GameTestHelper h, BlockPos pos) {
        return player(h, pos, null);
    }
    private static Fixture player(GameTestHelper h, BlockPos pos, Integer reusedId) {
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "ward-test"), ClientInformation.createDefault());
        if (reusedId != null) p.setId(reusedId);
        var packets = new ArrayList<Packet<?>>();
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), p,
              CommonListenerCookie.createInitial(p.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) { packets.add(packet); }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { packets.add(packet); }
        };
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        h.setBlock(pos.below(), Blocks.STONE);
        p.setPos(h.absolutePos(pos).getCenter());
        h.getLevel().addNewPlayer(p);
        h.onEachTick(() -> { if (!p.isRemoved()) p.doTick(); });
        var slots = CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler(ThunderWardItem.SLOT).orElseThrow().getStacks();
        var ward = new ItemStack(CoreContent.WARD.get());
        check(slots.isItemValid(0, ward), "Ward could not be placed in its real Curios slot");
        check(slots.insertItem(0, ward.copy(), true).isEmpty() && slots.getStackInSlot(0).isEmpty(), "Simulated equip mutated inventory");
        check(slots.insertItem(0, ward, false).isEmpty() && ThunderWard.equipped(p), "Ward did not equip through Curios");
        return new Fixture(p, packets);
    }
    private static TileEntityEnergyCube cube(GameTestHelper h, BlockPos pos, UUID owner, long amount) {
        h.setBlock(pos, MekanismBlocks.ADVANCED_ENERGY_CUBE.get());
        var cube = (TileEntityEnergyCube)h.getBlockEntity(pos);
        DeviceScope.placed(cube, owner);
        cube.getEnergyContainer().setEnergy(amount);
        check(cube.getEnergyContainer().getEnergy() == amount, "Fixture cube capacity too small");
        return cube;
    }
    private static void close(Fixture f) {
        CuriosApi.getCuriosInventory(f.player).flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT))
              .ifPresent(h -> h.getStacks().setStackInSlot(0, ItemStack.EMPTY));
        ThunderWard.forget(f.player);
        f.player.serverLevel().removePlayerImmediately(f.player, Entity.RemovalReason.DISCARDED);
    }
    private static void alive(Fixture f) {
        check(f.player.isAlive() && !f.player.isRemoved() && f.player.getHealth() == 1F, "Ward failed to retain one real health point");
        check(f.packets.stream().noneMatch(p -> p instanceof ClientboundPlayerCombatKillPacket), "A death-screen packet escaped the guard");
    }

    @GameTest(template="empty", timeoutTicks=120)
    public static void pooledFatalHitsHaveNoCooldownAndNoInventoryLoss(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20)); var p=f.player;
        CoreGameTests.bind(p);
        var a=cube(h,new BlockPos(20,4,23),p.getUUID(),price());
        var b=cube(h,new BlockPos(23,4,20),p.getUUID(),price()*3);
        var tiny=cube(h,new BlockPos(19,4,23),p.getUUID(),1);
        p.getInventory().setItem(1,new ItemStack(Items.DIAMOND,7));
        h.startSequence().thenIdle(65).thenExecute(() -> {
            try {
                check(p.hurt(p.damageSources().generic(),40),"Real ordinary damage did not reach the player");
                alive(f);
                check(a.getEnergyContainer().getEnergy()<price()&&b.getEnergyContainer().getEnergy()<price()*3,"Only one machine was charged");
                check(tiny.getEnergyContainer().isEmpty(),"A small nearby reservoir never participated in the shared price");
                check(a.getEnergyContainer().getEnergy()+b.getEnergyContainer().getEnergy()==price()*3+1,"First rescue did not debit exactly the pooled price");
                p.invulnerableTime=0;
                p.hurt(p.damageSources().genericKill(),40);
                alive(f);
                check(a.getEnergyContainer().getEnergy()+b.getEnergyContainer().getEnergy()==price()*2+1,"Second hit in the same tick was free or blocked by a cooldown");
                check(p.getInventory().getItem(1).getCount()==7,"Inventory was lost on resisted death");
                check(CoreBinding.data(p).getLong("energy")==0,"The cursed core recycled or multiplied the ward's emergency payment");
            } finally { close(f); }
        }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=120)
    public static void insufficientOrUnownedPowerDoesNotChargeAndCompletedDeathStaysFinal(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var own=cube(h,new BlockPos(20,4,23),p.getUUID(),price()/2);
        var foreign=cube(h,new BlockPos(23,4,20),UUID.randomUUID(),price()*3);
        var far=cube(h,new BlockPos(70,4,20),p.getUUID(),price()*3);
        h.startSequence().thenIdle(65).thenExecute(() -> {
            try {
                p.hurt(p.damageSources().generic(),40);
                check(!p.isAlive()&&f.packets.stream().anyMatch(packet -> packet instanceof ClientboundPlayerCombatKillPacket),"Unfunded damage was secretly made nonfatal");
                check(own.getEnergyContainer().getEnergy()==price()/2&&foreign.getEnergyContainer().getEnergy()==price()*3&&far.getEnergyContainer().getEnergy()==price()*3,"Failed rescue stole or partially charged power");
                own.getEnergyContainer().setEnergy(price());
                p.discard();
                check(p.isRemoved()&&own.getEnergyContainer().getEnergy()==price(),"Finalized death was resurrected after drops/packet settlement");
            } finally { close(f); }
        }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void chainedInfiniteDamageClearHealthDieAndDiscardCostsOnce(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*3);
        try {
            p.hurt(p.damageSources().genericKill(),Float.POSITIVE_INFINITY);
            p.setHealth(0);
            p.getCombatTracker().recordDamage(p.damageSources().genericKill(),Float.POSITIVE_INFINITY);
            p.die(p.damageSources().genericKill());
            p.deathTime=19;
            p.discard();
            alive(f);
            check(p.deathTime==0&&Float.isFinite(p.getAbsorptionAmount()),"Forced-kill residue remained on the player");
            check(power.getEnergyContainer().getEnergy()==price()*2,"A single forced-kill chain was charged repeatedly");
            check(p.getAbsorptionAmount()==0,"Resistance granted an unintended absorption shield");
        } finally { close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void directEntryPointsAndSharedPowerWorkWithoutCursedCore(GameTestHelper h) {
        var owner=CoreGameTests.player(h,new BlockPos(22,4,20));
        var power=cube(h,new BlockPos(20,4,23),owner.getUUID(),price()*4);
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        try {
            check(!CoreBinding.bound(p),"Ward unexpectedly needed a permanently bound core");
            check(DeviceScope.share(owner,power,p.getUUID(),true),"Owner could not grant power consent");
            p.setHealth(0); alive(f);
            check(power.getEnergyContainer().getEnergy()==price()*3,"Direct health clearing was not resisted");
            ThunderWard.forget(p);
            p.die(p.damageSources().genericKill()); alive(f);
            check(power.getEnergyContainer().getEnergy()==price()*2,"Direct die was not resisted");
            ThunderWard.forget(p);
            p.setRemoved(Entity.RemovalReason.DISCARDED); alive(f);
            check(power.getEnergyContainer().getEnergy()==price(),"Direct setRemoved escaped resistance");
            ThunderWard.forget(p);
            p.discard(); alive(f);
            check(power.getEnergyContainer().isEmpty(),"Direct discard was not independently paid");
        } finally { close(f); CoreGameTests.remove(owner); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=120)
    public static void totemWinsAndRoutineUnloadingDoesNotSpendPower(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*3);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        h.startSequence().thenIdle(65).thenExecute(() -> {
            try {
                p.hurt(p.damageSources().generic(),40);
                check(p.isAlive()&&p.getOffhandItem().isEmpty(),"Native totem priority was stolen");
                check(power.getEnergyContainer().getEnergy()==price()*3,"Ward charged for a totem-protected hit");
                p.setHealth(15); p.invulnerableTime=0;
                p.hurt(p.damageSources().genericKill(),1);
                check(power.getEnergyContainer().getEnergy()==price()*3,"Nonfatal injury or healing spent power");
                p.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                check(p.isRemoved()&&power.getEnergyContainer().getEnergy()==price()*3,"Normal dimension departure was blocked or charged");
            } finally { close(f); }
        }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=140)
    public static void assembledMatrixPortsAndCellsAreChargedOnlyOnce(GameTestHelper h) {
        var start = new BlockPos(10,2,10);
        for (int x=0;x<4;x++) for (int y=0;y<4;y++) for (int z=0;z<4;z++) {
            if (x==0||x==3||y==0||y==3||z==0||z==3) h.setBlock(start.offset(x,y,z),MekanismBlocks.INDUCTION_CASING.get());
        }
        h.setBlock(start.offset(1,1,0),MekanismBlocks.INDUCTION_PORT.get());
        h.setBlock(start.offset(2,1,0),MekanismBlocks.INDUCTION_PORT.get());
        h.setBlock(start.offset(1,1,1),MekanismBlocks.BASIC_INDUCTION_CELL.get());
        h.setBlock(start.offset(2,1,1),MekanismBlocks.BASIC_INDUCTION_PROVIDER.get());
        var cell=(mekanism.common.tile.multiblock.TileEntityInductionCell)h.getBlockEntity(start.offset(1,1,1));
        cell.getEnergyContainer().setEnergy(price()*4);
        var port=(mekanism.common.tile.multiblock.TileEntityInductionPort)h.getBlockEntity(start.offset(1,1,0));
        var f=player(h,new BlockPos(16,4,12));
        h.startSequence().thenWaitUntil(() -> check(port.getMultiblock().isFormed(),"Induction matrix did not form"))
              .thenExecute(() -> {
                  DeviceScope.claim(port,f.player.getUUID());
                  // Even individually owned cells must not be charged separately from the aggregate matrix.
                  DeviceScope.placed(cell,f.player.getUUID());
                  check(port.getMultiblock().getEnergy()==price()*4,"Matrix fixture energy was not cached");
                  f.player.setHealth(0); alive(f);
                  check(port.getMultiblock().getEnergy()==price()*3,"Matrix ports were double-counted or native extraction failed");
              }).thenIdle(2).thenExecute(() -> {
                  try { check(cell.getEnergyContainer().getEnergy()==price()*3,"Matrix cache and actual cell storage diverged"); }
                  finally { close(f); }
              }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=80)
    public static void respawnedPlayerWithReusedEntityIdDoesNotInheritFinalDeath(GameTestHelper h) {
        var previous=player(h,new BlockPos(20,4,20)); Fixture replacement=null;
        try {
            previous.player.hurt(previous.player.damageSources().genericKill(),40);
            check(!previous.player.isAlive(),"Unfunded previous life did not end");
            int id=previous.player.getId();
            h.getLevel().removePlayerImmediately(previous.player,Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            replacement=player(h,new BlockPos(20,4,20),id);
            var power=cube(h,new BlockPos(20,4,23),replacement.player.getUUID(),price()*2);
            replacement.player.hurt(replacement.player.damageSources().genericKill(),40);
            alive(replacement);
            check(power.getEnergyContainer().getEnergy()==price(),"Replacement player's new life could not pay for resistance");
        } finally {
            ThunderWard.forget(previous.player);
            if (replacement != null) close(replacement);
        }
        h.succeed();
    }
}
