package dev.everyonemek.overloadcore;

import dev.everyonemek.overloadcore.gear.*;
import dev.everyonemek.overloadcore.training.*;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.registries.*;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class TacticalGameTests {
    private static void check(boolean value,String text){CoreGameTests.check(value,text);}
    private static ItemStack tool(ServerPlayer p){var stack=new ItemStack(MekanismItems.MEKA_TOOL.get());ModuleHelper.get().getModuleContainer(stack).addModule(p.registryAccess(),stack,EquipmentModules.COMBAT,1);StorageUtils.getEnergyContainer(stack,0).setEnergy(GearCombat.joules(1000000));return stack;}
    private static void module(ServerPlayer p,ItemStack stack,GearUpgrade upgrade,int n){ModuleHelper.get().getModuleContainer(stack).addModule(p.registryAccess(),stack,EquipmentModules.get(upgrade),n);}
    private static void username(ServerPlayer p,boolean add){try{
        var method=net.neoforged.neoforge.common.UsernameCache.class.getDeclaredMethod(add?"setUsername":"removeUsername",add?new Class<?>[]{java.util.UUID.class,String.class}:new Class<?>[]{java.util.UUID.class});method.setAccessible(true);
        if(add)method.invoke(null,p.getUUID(),p.getGameProfile().getName());else method.invoke(null,p.getUUID());
    }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static ItemStack chest(ServerPlayer p){var stack=new ItemStack(MekanismItems.MEKASUIT_BODYARMOR.get());module(p,stack,GearUpgrade.DEFLECTOR,1);StorageUtils.getEnergyContainer(stack,0).setEnergy(GearCombat.joules(1000000));p.setItemSlot(EquipmentSlot.CHEST,stack);return stack;}
    private static Zombie mob(GameTestHelper h,BlockPos pos){var entity=EntityType.ZOMBIE.create(h.getLevel());entity.setPos(h.absolutePos(pos).getCenter());entity.setNoAi(true);entity.setPersistenceRequired();entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);entity.setHealth(200);h.getLevel().addFreshEntity(entity);return entity;}
    @GameTest(template="empty",timeoutTicks=35)
    public static void quickSwitchChangesNativeConfigCancelsChargeAndLimitsSpam(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();var stack=tool(p);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(p.isUsingItem(),"Charge did not start");
        check(TacticalCombat.switchForm(p,false)&&MekaCombat.form(stack)==CombatModule.Form.RANGED&&!p.isUsingItem(),"Quick switch did not use native config/cancel charge");
        check(!TacticalCombat.switchForm(p,false)&&MekaCombat.form(stack)==CombatModule.Form.RANGED,"Same-tick swap spam changed form");
        var off=tool(p);p.setItemInHand(InteractionHand.OFF_HAND,off);
        h.startSequence().thenIdle(5).thenExecute(()->{try{
            check(TacticalCombat.switchForm(p,true)&&MekaCombat.form(off)==CombatModule.Form.RANGED,"Offhand quick switch failed");
            var copy=ItemStack.parse(p.registryAccess(),off.save(p.registryAccess())).orElseThrow();check(MekaCombat.form(copy)==CombatModule.Form.RANGED,"Quick form was not saved");
            check(MekaCombat.energy(stack)==GearCombat.joules(1000000),"Switching consumed energy");
        }finally{ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=45)
    public static void polarizationRequiresRealDamageAndEmpowersOnePiercingShot(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();p.setPos(p.getX(),h.absolutePos(new BlockPos(20,4,20)).getY(),p.getZ());p.setYRot(0);p.setXRot(0);
        var stack=tool(p);module(p,stack,GearUpgrade.POLARIZATION,2);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var first=mob(h,new BlockPos(20,4,22));var second=mob(h,new BlockPos(20,4,25));
        try{
            check(GearCombat.shoot(p,stack)&&TacticalCombat.marks(p,first)==1,"Paid ranged hit did not polarize");
            check(GearCombat.shoot(p,stack)&&TacticalCombat.marks(p,first)==1,"Rejected invulnerability hit added marks");
            first.invulnerableTime=0;float before=first.getHealth();GearCombat.burst(p,stack);
            check(Math.abs(before-first.getHealth()-(CoreConfig.BLADE_DAMAGE.get()*1.5+6))<.01&&TacticalCombat.marks(p,first)==0,"Charged slash failed to consume/add mark damage");
            first.invulnerableTime=0;float rear=second.getHealth();GearCombat.shoot(p,stack);check(second.getHealth()<rear,"Melee combat hit did not add one penetration");
            first.invulnerableTime=second.invulnerableTime=0;rear=second.getHealth();GearCombat.shoot(p,stack);check(second.getHealth()==rear,"Extra penetration was not consumed");
            h.succeed();
        }finally{first.discard();second.discard();ThunderWardGameTests.close(f);}
    }
    @GameTest(template="empty",timeoutTicks=105)
    public static void directionalGuardPaysChestRejectsRearAndCannotRefreshPerfectWindow(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();p.setYRot(0);p.setXRot(0);var armor=chest(p);var attacker=mob(h,new BlockPos(20,4,23));
        h.startSequence().thenIdle(65).thenExecute(()->{
            check(TacticalCombat.guardInput(p,true),"Powered shield did not activate");long before=MekaCombat.energy(armor);float health=p.getHealth();
            p.hurt(p.damageSources().mobAttack(attacker),8);
            check(p.getHealth()==health&&before-MekaCombat.energy(armor)==GearCombat.joules(8L*CoreConfig.GUARD_DAMAGE_FE.get()),"Guard did not block before native armor or debit exact cost");
            check(TacticalCombat.consumeCounter(p)>1&&TacticalCombat.consumeCounter(p)==1,"Perfect counter not granted/consumed exactly once");
            TacticalCombat.guardInput(p,false);
        }).thenIdle(3).thenExecute(()->{
            check(TacticalCombat.guardInput(p,true),"Shield did not re-open");p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(attacker),4);
            check(TacticalCombat.consumeCounter(p)==1,"Rapid toggling refreshed perfect guard");
            attacker.setPos(h.absolutePos(new BlockPos(20,4,18)).getCenter());p.invulnerableTime=0;float health=p.getHealth();p.hurt(p.damageSources().mobAttack(attacker),4);
            check(p.getHealth()<health,"Rear damage was canceled by directional shield");
        }).thenExecute(()->{try{
            TacticalCombat.guardInput(p,false);StorageUtils.getEnergyContainer(armor,0).setEnergy(0);check(!TacticalCombat.guardInput(p,true),"Unpowered shield activated");
        }finally{attacker.discard();ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=105)
    public static void arrowImpactReflectsTheSameProjectile(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();p.setYRot(0);p.setXRot(0);chest(p);var attacker=mob(h,new BlockPos(20,4,25));
        var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setOwner(attacker);arrow.setNoGravity(true);
        h.startSequence().thenIdle(65).thenExecute(()->{TacticalCombat.guardInput(p,true);arrow.setPos(p.getEyePosition().add(0,0,3));arrow.setDeltaMovement(0,0,-1);h.getLevel().addFreshEntity(arrow);})
            .thenWaitUntil(()->check(arrow.getOwner()==p&&arrow.getDeltaMovement().z>0,"Arrow was not reflected by actual projectile ticks"))
            .thenExecute(()->{try{check(!arrow.isRemoved()&&p.getHealth()>0,"Reflection removed projectile or hurt player");}finally{arrow.discard();attacker.discard();ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void projectorAcceptsRealCubePowerTracksHitsAndStopsWithoutPower(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();p.setPos(p.getX(),h.absolutePos(new BlockPos(20,4,20)).getY(),p.getZ());p.setYRot(0);p.setXRot(0);
        var pos=new BlockPos(20,3,24);h.setBlock(pos,TrainingContent.BLOCK.get());var tile=(TrainingProjector)h.getBlockEntity(pos);
        h.setBlock(pos.west(),MekanismBlocks.BASIC_ENERGY_CUBE.get());var cube=(mekanism.common.tile.TileEntityEnergyCube)h.getBlockEntity(pos.west());cube.getEnergyContainer().setEnergy(cube.getEnergyContainer().getMaxEnergy());
        var config=cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);config.setDataType(mekanism.common.tile.component.config.DataType.OUTPUT,mekanism.api.RelativeSide.fromDirections(cube.getDirection(),Direction.EAST));config.setEjecting(true);cube.invalidateCapabilitiesFull();
        tile.getBlockState().useWithoutItem(h.getLevel(),p,new BlockHitResult(h.absolutePos(pos).getCenter(),Direction.NORTH,h.absolutePos(pos),false));
        check(p.containerMenu instanceof TrainingMenu&&p.containerMenu.clickMenuButton(p,0),"Projector native GUI did not open/start");
        check(p.containerMenu.slots.getFirst().y==185,"Projector inventory slots did not follow the taller screen");
        var tool=tool(p);module(p,tool,GearUpgrade.POLARIZATION,1);p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        h.startSequence().thenWaitUntil(()->check(tile.getActive()&&tile.energy.getEnergy()>0,"Cube did not power projector through its real output"))
            .thenExecute(()->{
                var targets=h.getLevel().getEntitiesOfClass(TrainingTarget.class,new AABB(h.absolutePos(pos)).inflate(3));check(targets.size()==1,"Projector did not maintain exactly one target");
                check(GearCombat.shoot(p,tool)&&tile.hits==1&&Math.abs(tile.lastDamage-CoreConfig.RAIL_DAMAGE.get())<.01,"Projection did not record resolved damage");
                check(tile.spent==GearCombat.joules(CoreConfig.RAIL_COST.get())&&TacticalCombat.consumeFlux(p,tool)==0,"Training energy accounting or reward isolation failed");
                var projection=targets.getFirst();
                check(projection.hits()==1&&Math.abs(projection.lastDamage()-tile.lastDamage)<.01&&Math.abs(projection.dps()-tile.dps)<.01&&projection.hitAge(0)==0,"Projection did not synchronize confirmed hit feedback");
                var replica=TrainingContent.TARGET.get().create(h.getLevel());replica.getEntityData().assignValues(projection.getEntityData().getNonDefaultValues());
                check(replica.hits()==1&&replica.lastDamage()==projection.lastDamage()&&replica.feedbackDamage()==projection.feedbackDamage(),"Tracking snapshot lost projection feedback");
                projection.setInvulnerable(true);check(!projection.hurt(p.damageSources().playerAttack(p),9),"Invulnerable projection accepted damage");projection.setInvulnerable(false);
                projection.recordHit(p.damageSources().playerAttack(p),Float.NaN);projection.recordHit(p.damageSources().playerAttack(p),0);
                check(projection.hits()==1&&projection.feedbackDamage()==projection.lastDamage(),"Rejected hits changed projection feedback");
                float firstDamage=projection.feedbackDamage();projection.hurt(p.damageSources().playerAttack(p),5);projection.hurt(p.damageSources().playerAttack(p),7);
                check(projection.hits()==3&&projection.lastDamage()==7&&Math.abs(projection.feedbackDamage()-firstDamage-12)<.01,"Same-tick hits did not combine the floating number while retaining the last-hit reading");
            }).thenIdle(2).thenExecute(()->{
                check(tile.dps>0,"Tile tick erased same-tick damage bucket");
                check(p.containerMenu.clickMenuButton(p,1)&&tile.hits==0&&tile.spent==0,"Native menu reset failed");
                var projection=h.getLevel().getEntitiesOfClass(TrainingTarget.class,new AABB(h.absolutePos(pos)).inflate(3)).getFirst();
                check(projection.hits()==0&&projection.dps()==0&&projection.feedbackDamage()==0&&projection.hitAge(0)>18,"Reset left a stale floating hit/readout");
                cube.getEnergyContainer().setEnergy(0);tile.energy.setEnergy(0);
            }).thenWaitUntil(()->check(!tile.getActive()&&h.getLevel().getEntitiesOfClass(TrainingTarget.class,new AABB(h.absolutePos(pos)).inflate(3)).isEmpty(),"Unpowered projection remained attackable"))
            .thenExecute(()->{tile.enabled=false;ThunderWardGameTests.close(f);}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void projectorSecurityAndBlockDropPreserveEnergyAndReadings(GameTestHelper h){
        var p=CoreGameTests.player(h,new BlockPos(20,4,20));var guest=CoreGameTests.player(h,new BlockPos(22,4,20));
        username(p,true); // GameTestServer has no profile cache; mirror this fixture's ordinary login only.
        var pos=new BlockPos(20,4,23);h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,TrainingContent.BLOCK.get());var tile=(TrainingProjector)h.getBlockEntity(pos);
        try{
            tile.getSecurity().setOwnerUUID(p.getUUID());tile.getSecurity().setMode(mekanism.api.security.SecurityMode.PRIVATE);
            check(!tile.command(guest,0)&&!tile.enabled,"Private projector accepted foreign control");
            tile.energy.setEnergy(10000);tile.recordHit(37.5);tile.recordPower(2500);
            var drops=net.minecraft.world.level.block.Block.getDrops(tile.getBlockState(),h.getLevel(),h.absolutePos(pos),tile,p,new ItemStack(Items.DIAMOND_PICKAXE));
            var drop=drops.stream().filter(s->s.is(TrainingContent.BLOCK.asItem())).findFirst().orElseThrow();
            check(p.getUUID().equals(drop.get(MekanismDataComponents.OWNER))&&drop.has(TrainingContent.DATA.get()),"Drop lost owner or readings");
            h.setBlock(pos,Blocks.AIR);p.setItemInHand(InteractionHand.MAIN_HAND,drop);
            var result=drop.useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(h.absolutePos(pos.below()).getCenter(),Direction.UP,h.absolutePos(pos.below()),false)));
            check(result.consumesAction()&&h.getBlockEntity(pos) instanceof TrainingProjector,"Dropped machine did not place");
            var restored=(TrainingProjector)h.getBlockEntity(pos);
            check(restored.energy.getEnergy()==10000&&restored.hits==1&&restored.totalDamage==37.5&&restored.spent==2500&&!restored.permitted(guest),"Placement lost energy, stats or security");h.succeed();
        }finally{h.setBlock(pos,Blocks.AIR);CoreGameTests.remove(p);CoreGameTests.remove(guest);username(p,false);}
    }
}
