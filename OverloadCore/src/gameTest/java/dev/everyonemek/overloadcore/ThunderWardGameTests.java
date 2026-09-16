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
    record Fixture(ServerPlayer player, List<Packet<?>> packets) { }
    private static void check(boolean condition, String message) { CoreGameTests.check(condition, message); }
    private static long price() { return EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.WARD_COST_FE.get().longValue()); }
    static Fixture player(GameTestHelper h, BlockPos pos) {
        return player(h, pos, null);
    }
    private static Fixture player(GameTestHelper h, BlockPos pos, Integer reusedId) {
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "ward-test"), ClientInformation.createDefault());
        if (reusedId != null) p.setId(reusedId);
        var packets = new ArrayList<Packet<?>>();
        // Respawn/attachment synchronization inspects channel attributes even though send() is intercepted below.
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            private final io.netty.channel.embedded.EmbeddedChannel localChannel = new io.netty.channel.embedded.EmbeddedChannel();
            @Override public io.netty.channel.Channel channel() { return localChannel; }
        };
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, p,
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
    static void close(Fixture f) {
        // Test teardown discards the artificial player's durable record, not a production removal bypass.
        WardCustody.forget(f.player);
        WardLedger.get(f.player).worn.remove(f.player.getUUID());
        WardLedger.get(f.player).setDirty();
        CuriosApi.getCuriosInventory(f.player).flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT))
              .ifPresent(h -> h.getStacks().setStackInSlot(0, ItemStack.EMPTY));
        ThunderWard.forget(f.player);
        f.player.serverLevel().removePlayerImmediately(f.player, Entity.RemovalReason.DISCARDED);
        f.player.connection.getConnection().channel().close();
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

    @GameTest(template="empty", timeoutTicks=90)
    public static void synchronizedHealthAndOnlineNbtCannotBypassPayment(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*4);
        var healthId=dev.everyonemek.overloadcore.mixin.WardLivingAccess.overload$healthId();
        h.startSequence().thenExecute(() -> {
            p.getEntityData().set(healthId,0F,true); alive(f);
            check(p.getEntityData().get(healthId)==1F,"Sync-data health was only visually masked");
            check(power.getEnergyContainer().getEnergy()==price()*3,"Direct synchronized health write was not paid");
        }).thenIdle(1).thenExecute(() -> {
            p.getEntityData().assignValues(List.of(net.minecraft.network.syncher.SynchedEntityData.DataValue.create(healthId,Float.NEGATIVE_INFINITY)));
            alive(f);
            check(power.getEnergyContainer().getEnergy()==price()*2,"Bulk synchronized write was not separately paid");
        }).thenIdle(1).thenExecute(() -> {
            try {
                var data=p.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                data.putFloat("Health",0);data.putShort("DeathTime",(short)19);
                p.readAdditionalSaveData(data);alive(f);
                check(p.deathTime==0&&power.getEnergyContainer().getEnergy()==price(),"Online NBT retained a death timer or escaped exact payment");
            } finally { close(f); }
        }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void rawHealthAndDeathFlagsAreRepairedBeforeDeathTicks(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*3);
        try {
            var getItem=net.minecraft.network.syncher.SynchedEntityData.class.getDeclaredMethod("getItem",net.minecraft.network.syncher.EntityDataAccessor.class);
            getItem.setAccessible(true);
            @SuppressWarnings("unchecked") var item=(net.minecraft.network.syncher.SynchedEntityData.DataItem<Float>)getItem.invoke(p.getEntityData(),dev.everyonemek.overloadcore.mixin.WardLivingAccess.overload$healthId());
            item.setValue(Float.NaN); // Bypasses both setHealth and SynchedEntityData.set.
            ((dev.everyonemek.overloadcore.mixin.WardLivingAccess)p).overload$dead(true);
            p.deathTime=19;p.setPose(net.minecraft.world.entity.Pose.DYING);
            p.tick();alive(f);
            check(item.getValue()==1F&&p.deathTime==0&&!((dev.everyonemek.overloadcore.mixin.WardLivingAccess)p).overload$dead(),"Raw death state survived the watchdog");
            check(power.getEnergyContainer().getEnergy()==price()*2,"Raw corruption did not cost exactly one rescue");
            var tickDeath=net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("tickDeath");tickDeath.setAccessible(true);tickDeath.invoke(p);
            alive(f);check(p.deathTime==0,"Direct tickDeath advanced the resisted death clock");
            check(power.getEnergyContainer().getEnergy()==price()*2,"One corruption chain was billed again by tickDeath");
        } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
        finally { close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void forgedRemovalFlagDoesNotHideAStillTrackedPlayer(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*2);
        try {
            ((dev.everyonemek.overloadcore.mixin.WardEntityAccess)p).overload$removalReason(Entity.RemovalReason.KILLED);
            check(!p.isRemoved(),"Forged removal reason bypassed resistance");alive(f);
            check(p.getRemovalReason()==null&&ThunderWard.tracked(p),"Removal flag or world membership was not restored");
            check(power.getEnergyContainer().getEnergy()==price(),"Marked-removed player could not use authorized power");
        } finally { close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void actualWorldManagerLookupAndTickListCannotEraseTheWearer(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*2);
        try {
            var access=(dev.everyonemek.overloadcore.mixin.WardServerLevelAccess)h.getLevel();
            var manager=access.overload$manager();
            var lookup=((dev.everyonemek.overloadcore.mixin.WardManagerAccess)manager).overload$lookup();
            var original=((dev.everyonemek.overloadcore.mixin.WardEntityAccess)p).overload$levelCallback();
            p.setLevelCallback(new net.minecraft.world.level.entity.EntityInLevelCallback() {
                @Override public void onMove() { original.onMove(); }
                @Override public void onRemove(Entity.RemovalReason reason) { original.onRemove(reason); }
            });
            ((dev.everyonemek.overloadcore.mixin.WardEntityAccess)p).overload$levelCallback().onRemove(Entity.RemovalReason.DISCARDED);
            var stop=net.minecraft.world.level.entity.PersistentEntitySectionManager.class.getDeclaredMethod("stopTracking",net.minecraft.world.level.entity.EntityAccess.class);
            stop.setAccessible(true);stop.invoke(manager,p);
            lookup.remove(p);access.overload$tickList().remove(p);
            var callbacks=net.minecraft.world.level.entity.PersistentEntitySectionManager.class.getDeclaredField("callbacks");callbacks.setAccessible(true);
            @SuppressWarnings("unchecked") var callback=(net.minecraft.world.level.entity.LevelCallback<Entity>)callbacks.get(manager);
            callback.onTrackingEnd(p);p.onRemovedFromLevel();alive(f);
            check(ThunderWard.tracked(p)&&h.getLevel().players().stream().anyMatch(value -> value==p),"A world index or player list lost the resisted player");
            check(p.isAddedToLevel()&&access.overload$tickList().contains(p),"The player stopped ticking or became detached");
            check(h.getLevel().getEntitiesOfClass(ServerPlayer.class,p.getBoundingBox().inflate(1)).stream().anyMatch(value -> value==p),"Delegated removal bypassed the guard and damaged the spatial section index");
            check(power.getEnergyContainer().getEnergy()==price(),"One manager-removal chain charged repeatedly");
            var temporary=new net.minecraft.world.level.entity.EntityLookup<Entity>();temporary.add(p);temporary.remove(p);
            var temporaryTicks=new net.minecraft.world.level.entity.EntityTickList();temporaryTicks.add(p);temporaryTicks.remove(p);
            check(temporary.count()==0&&!temporaryTicks.contains(p),"Unrelated temporary collections were intercepted");
        } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
        finally { close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void invalidMaximumHealthRestoresOnlyTheHealthyAttributeSnapshot(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*2);
        try {
            var attribute=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            var permanent=new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"ward_permanent"),4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            var temporary=new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"ward_temporary"),6,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            attribute.addPermanentModifier(permanent);attribute.addTransientModifier(temporary);
            ThunderWard.inspect(p);check(p.getMaxHealth()==30,"Fixture max-health bonuses missing");
            var cached=net.minecraft.world.entity.ai.attributes.AttributeInstance.class.getDeclaredField("cachedValue");cached.setAccessible(true);
            var dirty=net.minecraft.world.entity.ai.attributes.AttributeInstance.class.getDeclaredField("dirty");dirty.setAccessible(true);
            cached.setDouble(attribute,Double.NaN);dirty.setBoolean(attribute,false);
            check(p.getMaxHealth()==30,"Poisoned max health was not repaired");alive(f);
            check(attribute.hasModifier(permanent.id())&&attribute.hasModifier(temporary.id()),"Valid max-health modifiers were wiped");
            check(attribute.save().getList("modifiers",10).size()==1,"Transient modifiers were made permanent");
            check(power.getEnergyContainer().getEnergy()==price(),"Attribute recovery did not pay exactly once");
            ThunderWard.forget(p); // The same corruption must also work immediately after equipping, before a snapshot.
            cached.setDouble(attribute,0);dirty.setBoolean(attribute,false);
            check(p.getMaxHealth()==30&&attribute.getValue()==30,"A forged cached maximum survived without a prior snapshot");
            check(attribute.getModifiers().size()==2&&power.getEnergyContainer().isEmpty(),"Fresh-ward recovery lost valid modifiers or skipped payment");
        } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
        finally { close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=130)
    public static void nativeDamageFamiliesAndKillRemainPayPerFatalHit(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        var sources=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
              .holders().map(net.minecraft.world.damagesource.DamageSource::new).toList();
        h.setBlock(new BlockPos(20,4,23),MekanismBlocks.ULTIMATE_ENERGY_CUBE.get());
        var power=(TileEntityEnergyCube)h.getBlockEntity(new BlockPos(20,4,23));
        DeviceScope.placed(power,p.getUUID());power.getEnergyContainer().setEnergy(price()*(sources.size()+2));
        check(power.getEnergyContainer().getEnergy()==price()*(sources.size()+2),"Registry sweep fixture lacks power");
        h.startSequence().thenIdle(65).thenExecute(() -> {
            try {
                int paid=0;
                for(var source:sources) {
                    p.setHealth(20);p.invulnerableTime=0;p.hurt(source,100);alive(f);paid++;
                    check(power.getEnergyContainer().getEnergy()==price()*(sources.size()+2-paid),"Wrong charge for damage source "+source.getMsgId());
                }
                p.invulnerableTime=0;p.kill();alive(f);
                check(power.getEnergyContainer().getEnergy()==price(),"Native kill method was not resisted and charged");
                org.slf4j.LoggerFactory.getLogger("OverloadCore GameTest").info("Validated {} registered damage types and native kill",sources.size());
            } finally { close(f); }
        }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=90)
    public static void realRespawnAndLogoutAreNotMistakenForAttacks(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var power=cube(h,new BlockPos(20,4,23),f.player.getUUID(),price()*2);
        var list=h.getLevel().getServer().getPlayerList();ServerPlayer replacement=null;
        try {
            replacement=list.respawn(f.player,true,Entity.RemovalReason.DISCARDED);
            check(f.player.isRemoved()&&replacement!=f.player&&ThunderWard.tracked(replacement),"A real player replacement was blocked");
            check(power.getEnergyContainer().getEnergy()==price()*2,"Respawn/End-return removal spent ward power");
            list.remove(replacement);
            check(!ThunderWard.tracked(replacement)&&replacement.isRemoved(),"Normal logout was blocked");
            check(power.getEnergyContainer().getEnergy()==price()*2,"Logout spent ward power");
        } finally {
            ThunderWard.forget(f.player);
            if(replacement!=null) { ThunderWard.forget(replacement);if(!replacement.isRemoved())list.remove(replacement); }
            f.player.connection.getConnection().channel().close();
        }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void insufficientPowerAndUnequippingDisableDeepProtection(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()/2);
        try {
            p.getEntityData().set(dev.everyonemek.overloadcore.mixin.WardLivingAccess.overload$healthId(),0F,true);
            check(p.getHealth()==0&&!p.isAlive(),"Unfunded raw health was made immortal");
            check(power.getEnergyContainer().getEnergy()==price()/2,"Failed probes partially drained power");
            p.setHealth(20);
            WardCustodyGameTests.click(p, net.minecraft.world.inventory.ClickType.PICKUP, 0);
            power.getEnergyContainer().setEnergy(price()*2);
            ((dev.everyonemek.overloadcore.mixin.WardEntityAccess)p).overload$levelCallback().onRemove(Entity.RemovalReason.DISCARDED);
            check(!ThunderWard.tracked(p)&&power.getEnergyContainer().getEnergy()==price()*2,"A removed accessory kept protecting its former wearer");
        } finally { close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void completedDeathSurvivesTransientStateLossUntilRealRespawn(GameTestHelper h) {
        var f=player(h,new BlockPos(20,4,20));var p=f.player;
        try {
            p.hurt(p.damageSources().genericKill(),40);
            check(p.getPersistentData().getBoolean(ThunderWard.FINALIZED_KEY),"Committed death was not recorded for save/reload");
            ThunderWard.forget(p);
            var power=cube(h,new BlockPos(20,4,23),p.getUUID(),price()*2);
            p.getEntityData().set(dev.everyonemek.overloadcore.mixin.WardLivingAccess.overload$healthId(),0F,true);
            ThunderWard.inspect(p);
            check(!p.isAlive()&&power.getEnergyContainer().getEnergy()==price()*2,"Losing the transient cache revived an already settled death");
        } finally { close(f); }
        h.succeed();
    }
}
