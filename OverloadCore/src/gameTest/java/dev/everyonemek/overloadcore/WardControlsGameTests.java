package dev.everyonemek.overloadcore;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class WardControlsGameTests {
    private static void check(boolean value,String message) { CoreGameTests.check(value,message); }
    private static long price() { return EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.WARD_COST_FE.get().longValue()); }
    private static TileEntityEnergyCube cube(GameTestHelper h,BlockPos pos,ServerPlayer owner,long energy) {
        h.setBlock(pos,MekanismBlocks.ADVANCED_ENERGY_CUBE.get());var tile=(TileEntityEnergyCube)h.getBlockEntity(pos);
        DeviceScope.placed(tile,owner.getUUID());tile.getEnergyContainer().setEnergy(energy);return tile;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void modeRequestChangesRealReserveDebitAndPersists(GameTestHelper h) {
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();WardLedger.get(p).extreme(p,false);
        var power=cube(h,new BlockPos(20,4,23),p,0);var tank=power.getEnergyContainer();
        long reserve=WardSources.reserve(DeviceScope.powerDevice(power),tank.getMaxEnergy());
        h.startSequence().thenExecute(() -> {
            check(!WardLedger.get(p).extreme(p),"Default normal mode was lost");
            tank.setEnergy(reserve+price()-1);
            check(!WardPower.pay(p)&&tank.getEnergy()==reserve+price()-1,"Normal mode drained the reserve or partially paid");
            check(WardRuntime.status(p).getString("result").equals("reserved"),"Reserve failure was not distinguished from no power");
            tank.setEnergy(reserve+price());check(WardPower.pay(p)&&tank.getEnergy()==reserve,"Normal debit did not preserve the exact configured floor");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
            try {
                CorePackets.WardExtreme.CODEC.encode(buffer,new CorePackets.WardExtreme(true));
                CorePackets.handleWardExtreme(p,CorePackets.WardExtreme.CODEC.decode(buffer));
            } finally {buffer.release();}
            check(WardLedger.get(p).extreme(p),"Decoded key request did not enable extreme extraction");
            CorePackets.handleWardExtreme(p,new CorePackets.WardExtreme(false));
            check(WardLedger.get(p).extreme(p),"Packet spam bypassed the input rate limit");
            tank.setEnergy(price());check(WardPower.pay(p)&&tank.isEmpty(),"Extreme mode did not use the remaining energy");
            check(WardLedger.load(WardLedger.get(p).save(new CompoundTag(),p.registryAccess()),p.registryAccess()).extreme(p),"Mode was not persistent");
        }).thenIdle(6).thenExecute(() -> {
            try {
                CorePackets.handleWardExtreme(p,new CorePackets.WardExtreme(false));check(!WardLedger.get(p).extreme(p),"Key could not disable extreme mode");
                WardCustodyGameTests.click(p,ClickType.PICKUP,0);
                CorePackets.handleWardExtreme(p,new CorePackets.WardExtreme(true));check(!WardLedger.get(p).extreme(p),"Unequipped player could change extraction mode");
            } finally { ThunderWardGameTests.close(f); }
        }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=200)
    public static void afterguardHasFiniteHitsExpiresAndDoesNotChargeAgain(GameTestHelper h) {
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();var power=cube(h,new BlockPos(20,4,23),p,price()*8);
        h.startSequence().thenIdle(65).thenExecute(() -> {
            p.hurt(p.damageSources().generic(),40);
            check(p.getHealth()==1&&WardRuntime.hits(p)==3&&power.getEnergyContainer().getEnergy()==price()*7,"Paid rescue did not grant three guard hits");
            p.invulnerableTime=0;p.hurt(p.damageSources().generic(),0);
            check(WardRuntime.hits(p)==3,"Zero damage consumed a hit");
            for(int i=0;i<3;i++) {
                p.invulnerableTime=0;p.hurt(i==0?p.damageSources().onFire():i==1?p.damageSources().fellOutOfWorld():p.damageSources().genericKill(),2);
                check(p.getHealth()==1&&WardRuntime.hits(p)==2-i&&power.getEnergyContainer().getEnergy()==price()*7,"Guard damage consumed energy or multiple hits");
            }
            p.invulnerableTime=0;p.hurt(p.damageSources().generic(),2);
            check(p.getHealth()==1&&WardRuntime.hits(p)==3&&power.getEnergyContainer().getEnergy()==price()*6,"Exhausted shield introduced a cooldown or stacked charges");
        }).thenIdle(61).thenExecute(() -> {
            try {
                check(WardRuntime.hits(p)==0,"Shield survived beyond its duration");
                p.setHealth(1);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),2);
                check(power.getEnergyContainer().getEnergy()==price()*5,"Expired shield still blocked damage for free");
                WardCustodyGameTests.click(p,ClickType.PICKUP,0);
                check(WardRuntime.hits(p)==0,"Unequipping retained shield charges");
            } finally {ThunderWardGameTests.close(f);}
        }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void missingRecordPreservesOriginalAndRepairRejectsRetiredCopies(GameTestHelper h) throws Exception {
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();
        try {
            var slots=CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler(ThunderWardItem.SLOT).orElseThrow().getStacks();
            var original=slots.getStackInSlot(0).copy();
            WardCustody.forget(p);WardLedger.get(p).worn.remove(p.getUUID());
            check(!ThunderWard.equipped(p)&&ItemStack.matches(original,slots.getStackInSlot(0)),"Missing record deleted or silently reauthorized the original");
            var menu=WardCustodyGameTests.click(p,ClickType.PICKUP,0);menu.setCarried(ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.MAIN_HAND,original.copy());p.getInventory().tick();
            check(p.getMainHandItem().is(CoreContent.WARD),"Unknown sealed item was deleted from ordinary inventory");
            var drop=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),original.copy());
            check(h.getLevel().addFreshEntity(drop),"Unknown sealed world drop was deleted");drop.discard();
            var commands=p.server.getCommands().getDispatcher();boolean denied=false;
            try { commands.execute("overloadcore ward repair @s",p.createCommandSourceStack().withPermission(0)); }
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied=true; }
            check(denied&&!ThunderWard.equipped(p),"Unprivileged repair command could reauthorize an item");
            check(commands.execute("overloadcore ward repair @s",p.createCommandSourceStack().withPermission(2))==1
                  &&ThunderWard.equipped(p)&&p.getMainHandItem().isEmpty(),"Administrator command did not adopt the extant held item exactly once");
            check(WardCustody.release(p)&&!ThunderWard.equipped(p)&&p.getInventory().countItem(CoreContent.WARD.get())==1,"Administrator release deleted or duplicated equipment");
            var ledger=WardLedger.load(WardLedger.get(p).save(new CompoundTag(),p.registryAccess()),p.registryAccess());
            check(ledger.retired(original.get(CoreContent.WARD_SEAL)),"Released identities lost their retirement record");
            p.setItemInHand(InteractionHand.MAIN_HAND,original.copy());
            check(!WardCustody.repair(p),"Repair reauthorized an already released duplicate");
        } finally {ThunderWardGameTests.close(f);}
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void sourceConsentProhibitionAndCachedEnergyStayAuthoritative(GameTestHelper h) {
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();
        var owner=CoreGameTests.player(h,new BlockPos(21,4,20));
        var power=cube(h,new BlockPos(20,4,23),owner,price()*4);
        try {
            check(DeviceScope.share(owner,power,p.getUUID(),true),"Fixture could not grant curse sharing");
            check(!WardPower.pay(p)&&power.getEnergyContainer().getEnergy()==price()*4,"New curse consent also granted emergency extraction");
            check(WardSources.share(owner,power,p.getUUID(),true)&&WardPower.pay(p),"Separate emergency consent failed");
            check(!WardSources.enabled(p,power,false),"A shared user changed the owner's source policy");
            check(WardSources.enabled(owner,power,false)&&!WardPower.pay(p),"Extreme extraction bypassed source prohibition");
            WardSources.enabled(owner,power,true);WardSources.share(owner,power,p.getUUID(),false);
            check(!WardPower.pay(p),"Candidate cache retained revoked permission");
            WardSources.share(owner,power,p.getUUID(),true);
            power.getEnergyContainer().setEnergy(0);
            check(!WardPower.pay(p),"Candidate cache reused old stored energy");
            var newlyPlaced=cube(h,new BlockPos(23,4,20),p,price());
            check(WardPower.pay(p)&&newlyPlaced.getEnergyContainer().isEmpty(),"Failed cached probe ignored a new same-tick source");
        } finally {ThunderWardGameTests.close(f);}
        h.succeed();
    }
}
