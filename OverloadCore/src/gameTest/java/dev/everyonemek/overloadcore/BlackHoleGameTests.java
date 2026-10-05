package dev.everyonemek.overloadcore;

import dev.everyonemek.overloadcore.gear.*;
import mekanism.api.*;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class BlackHoleGameTests {
    private static void check(boolean value,String text){CoreGameTests.check(value,text);}
    private static java.util.List<BlackHoleEntity> holes(GameTestHelper h){return h.getLevel().getEntitiesOfClass(BlackHoleEntity.class,h.getBounds());}
    private static Zombie mob(GameTestHelper h,BlockPos pos){
        var e=EntityType.HUSK.create(h.getLevel());e.setPos(h.absolutePos(pos).getCenter());e.setNoAi(true);e.setNoGravity(true);e.setPersistenceRequired();
        e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);e.setHealth(200);h.getLevel().addFreshEntity(e);return e;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeCubeChargesLauncherAndBothEnergyViewsPersist(GameTestHelper h){
        var pos=new BlockPos(20,4,20);h.setBlock(pos,MekanismBlocks.BASIC_ENERGY_CUBE.get());var cube=(TileEntityEnergyCube)h.getBlockEntity(pos);
        long initial=GearCombat.joules(600000);cube.getEnergyContainer().setEnergy(initial);
        var item=new ItemStack(CoreContent.BLACK_HOLE_LAUNCHER.get());
        check(cube.getInventorySlots(null).get(1).insertItem(item,Action.EXECUTE,AutomationType.MANUAL).isEmpty(),"Native charging slot rejected launcher");
        h.startSequence().thenWaitUntil(()->check(GearEnergy.stored(cube.getInventorySlots(null).get(1).getStack())>0,"Native cube has not charged launcher"))
            .thenExecute(()->{
                var stack=cube.getInventorySlots(null).get(1).getStack();long stored=GearEnergy.stored(stack);
                check(cube.getEnergyContainer().getEnergy()+stored==initial,"Native charge duplicated/lost energy");
                var fe=stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                int accepted=fe==null?0:fe.receiveEnergy(321,true);
                check(accepted>0&&accepted<=321&&GearEnergy.stored(stack)==stored,"FE simulation changed balance");
                check(fe.receiveEnergy(321,false)==accepted&&GearEnergy.stored(stack)==stored+GearCombat.joules(accepted),"FE and Mek balances differ");
                var copy=ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow();
                check(GearEnergy.stored(copy)==GearEnergy.stored(stack)&&GearEnergy.capacity(copy)==GearCombat.joules(10000000),"Launcher charge did not survive save/drop data");
                cube.getInventorySlots(null).get(1).setEmpty();h.setBlock(pos,Blocks.AIR);
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void realChargeCancelOffhandPaymentAndCooldown(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();var item=CoreContent.BLACK_HOLE_LAUNCHER.get();
        var stack=new ItemStack(item);long initial=GearCombat.joules(1000000);new GearEnergy(stack).setEnergy(initial);
        p.setItemInHand(InteractionHand.OFF_HAND,stack);p.setXRot(0);p.setYRot(0);
        item.use(h.getLevel(),p,InteractionHand.OFF_HAND);
        h.startSequence().thenIdle(2).thenExecute(()->{
            p.releaseUsingItem();check(GearEnergy.stored(stack)==initial&&holes(h).isEmpty(),"Early release fired/paid");
            item.use(h.getLevel(),p,InteractionHand.OFF_HAND);
        }).thenIdle(42).thenExecute(()->{
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }).thenIdle(2).thenExecute(()->{
            item.releaseUsing(stack,h.getLevel(),p,0);check(GearEnergy.stored(stack)==initial&&holes(h).isEmpty(),"Swapped-out stack fired");
            p.setItemInHand(InteractionHand.OFF_HAND,stack);item.use(h.getLevel(),p,InteractionHand.OFF_HAND);
        }).thenIdle(42).thenExecute(()->{try{
            p.releaseUsingItem();long left=initial-GearCombat.joules(CoreConfig.BLACK_HOLE_COST.get());
            check(GearEnergy.stored(stack)==left&&holes(h).size()==1,"Full offhand charge did not pay/spawn exactly once");
            item.releaseUsing(stack,h.getLevel(),p,0);check(GearEnergy.stored(stack)==left&&holes(h).size()==1,"Duplicate release fired twice");
            check(p.getCooldowns().isOnCooldown(item)&&item.use(h.getLevel(),p,InteractionHand.OFF_HAND).getResult()==InteractionResult.FAIL,"Cooldown bypassed");
            p.getCooldowns().removeCooldown(item);
            check(item.use(h.getLevel(),p,InteractionHand.OFF_HAND).getResult()==InteractionResult.FAIL,"Second launcher could bypass active field limit");
            holes(h).forEach(Entity::discard);new GearEnergy(stack).setEnergy(0);
            check(item.use(h.getLevel(),p,InteractionHand.OFF_HAND).getResult()==InteractionResult.FAIL&&!p.isUsingItem(),"Empty weapon began use");
        }finally{holes(h).forEach(Entity::discard);ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void actualProjectileOpensAtWallPullsOnlyVisibleEnemiesAndExpires(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();p.setNoGravity(true);
        for(int x=16;x<=24;x++)for(int y=3;y<=9;y++)h.setBlock(new BlockPos(x,y,12),Blocks.STONE);
        var exposed=mob(h,new BlockPos(22,5,15));var hidden=mob(h,new BlockPos(20,5,10));var ally=mob(h,new BlockPos(18,5,15));
        // NoAI suppresses vanilla travel entirely, including external velocity. Keep travel enabled,
        // with no walking speed, to verify real server movement instead of only a vector assignment.
        exposed.setNoAi(false);exposed.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("hole_"+p.getUUID().toString().substring(0,8));
        board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
        var drop=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),exposed.getX(),exposed.getY(),exposed.getZ(),new ItemStack(Items.DIAMOND,5));drop.setNoGravity(true);drop.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(drop);
        var hole=new BlackHoleEntity(BlackHoleEntity.TYPE.get(),h.getLevel());hole.setOwner(p);hole.setPos(p.getEyePosition());hole.setDeltaMovement(0,0,-1.25);h.getLevel().addFreshEntity(hole);
        Vec3 original=exposed.position(),allyPos=ally.position(),dropPos=drop.position();
        h.startSequence().thenWaitUntil(()->check(hole.isOpen(),"Projectile did not hit the real wall"))
            .thenExecute(()->check(hole.getZ()>h.absolutePos(new BlockPos(20,5,12)).getZ()+1,"Projectile crossed wall before opening"))
            .thenIdle(24).thenExecute(()->{
                check(exposed.getHealth()<200&&exposed.position().distanceToSqr(hole.position())<original.distanceToSqr(hole.position()),"Visible enemy was not pulled/damaged");
                check(hidden.getHealth()==200&&hidden.getDeltaMovement().lengthSqr()<.0001,"Field crossed a wall: health="+hidden.getHealth()+", velocity="+hidden.getDeltaMovement());
                check(ally.getHealth()==200&&ally.position().distanceToSqr(allyPos)<.0001,"Field pulled/damaged teammate");
                check(drop.isAlive()&&drop.getItem().getCount()==5&&drop.position().distanceToSqr(dropPos)<.0001,"Field consumed or moved loot");
                check(h.getBlockState(new BlockPos(20,5,12)).is(Blocks.STONE),"Field destroyed terrain");
                // Put the owner inside the field after normal spawn protection has elapsed.
                p.setPos(hole.position().add(0,0,2));p.setDeltaMovement(Vec3.ZERO);
            }).thenIdle(30).thenExecute(()->check(p.getHealth()==p.getMaxHealth()&&p.getDeltaMovement().lengthSqr()<.0001,"Owner affected by own field"))
            .thenWaitUntil(()->check(hole.isRemoved(),"Field outlived its server expiry"))
            .thenExecute(()->{try{check(hidden.getHealth()==200&&ally.getHealth()==200,"Field later affected excluded targets");}
                finally{hole.discard();exposed.discard();hidden.discard();ally.discard();drop.discard();board.removePlayerTeam(team);ThunderWardGameTests.close(f);}}).thenSucceed();
    }
}
