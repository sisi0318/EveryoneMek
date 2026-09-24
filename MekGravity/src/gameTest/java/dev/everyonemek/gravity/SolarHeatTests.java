package dev.everyonemek.gravity;

import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.solar.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class SolarHeatTests {
    private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
        private boolean done;
        private void finish(){if(!done){done=true;action.run();}}
        public void testStructureLoaded(GameTestInfo test){}
        public void testPassed(GameTestInfo test,GameTestRunner runner){finish();}
        public void testFailed(GameTestInfo test,GameTestRunner runner){finish();}
        public void testAddedForRerun(GameTestInfo oldTest,GameTestInfo newTest,GameTestRunner runner){}
    });}
    private static SolarController hot(GameTestHelper h){var c=SolarTests.formed(h);c.enabled=c.ignited=true;c.stored=c.capacity();c.fuelRemaining=c.fuelTotal=1_000_000_000_000L;return c;}
    @GameTest(template="empty",timeoutTicks=100)
    public static void solarHeatWarnsAtFullBufferAndStopsAfterShutdown(GameTestHelper h){
        var c=hot(h);var center=c.structure.seed.getBlockPos().getCenter();var warnings=new ArrayList<String>();int[] sounds={0};
        var p=ReactorTests.player(h,c.getBlockPos().north(),packet->{
            if(packet instanceof ClientboundSystemChatPacket chat&&chat.overlay()&&chat.content().getContents() instanceof TranslatableContents text&&text.getKey().startsWith("mekgravity.solar.heat_"))warnings.add(text.getKey());
            if(packet instanceof ClientboundSoundPacket)sounds[0]++;
        });cleanup(h,()->{try{c.enabled=false;}finally{ReactorTests.close(p);}});p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);p.setNoGravity(true);p.noPhysics=true;p.setPos(center.add(3.8,-.9,0));
        h.startSequence().thenIdle(6).thenExecute(()->{
            check(c.gross==0&&c.isCoreHot()&&c.structure.seed.hot(),"Full buffer incorrectly extinguished stellar heat");
            check(warnings.equals(List.of("mekgravity.solar.heat_warning"))&&sounds[0]==1,"Approach did not send one actionbar warning and one private alarm");
            check(p.getHealth()==20&&!p.isOnFire(),"Creative inspection caused burning damage");
            var tag=c.structure.seed.getUpdateTag(h.getLevel().registryAccess());check(tag.getAllKeys().equals(Set.of("visual","hot"))&&tag.getBoolean("hot")&&tag.getByte("visual")==0,"Hot standby was not synced separately from generation");
            var fuel=c.structure.fuelHatches.getFirst();var master=fuel.master;fuel.handleUpdateTag(tag,h.getLevel().registryAccess());check(master.equals(fuel.master),"Hot-state packet overwrote hatch linkage");
            var saved=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(saved,h.getLevel().registryAccess());check(c.structure.valid()&&c.isCoreHot(),"Reload lost the hot standby state");
            p.setPos(center.add(1,-.9,0));
        }).thenIdle(6).thenExecute(()->{
            check(warnings.size()==2&&warnings.getLast().equals("mekgravity.solar.heat_contact"),"Moving closer did not escalate immediately without chat spam");
            check(c.stored==c.capacity()&&c.fuelRemaining==1_000_000_000_000L,"Heat warnings consumed generation resources");
            c.enabled=false;
        }).thenIdle(6).thenExecute(()->{
            check(!c.isCoreHot()&&!c.structure.seed.hot()&&warnings.size()==2,"Manual shutdown left a hot core or continued alarms");
            c.enabled=true;c.fuelRemaining=c.fuelTotal=0;
        }).thenIdle(6).thenExecute(()->{
            check(!c.isCoreHot()&&warnings.size()==2,"Historical ignition flag burned an exhausted core");
            c.fuelRemaining=c.fuelTotal=1_000_000_000_000L;check(c.isCoreHot(),"Restored fuel did not restore heat");h.getLevel().setBlockAndUpdate(c.structure.at(0,0,2),Blocks.AIR.defaultBlockState());
            check(!c.isCoreHot()&&!c.structure.seed.hot(),"Structure safety shutdown retained a hot core");
        }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void solarBurningIncreasesTowardThePhotosphereAndRespectsFireResistance(GameTestHelper h){
        var c=hot(h);var center=c.structure.seed.getBlockPos().getCenter();var mobs=new ArrayList<Cow>();var positions=new ArrayList<Vec3>();
        cleanup(h,()->{try{c.enabled=false;}finally{mobs.forEach(Entity::discard);}});
        for(double offset:new double[]{4.2,3.1,2.1,1.2,-1.2}){
            var cow=EntityType.COW.create(h.getLevel());cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);cow.setHealth(200);cow.setNoAi(true);cow.setNoGravity(true);cow.noPhysics=true;
            var pos=center.add(offset,-.7,0);cow.setPos(pos);h.getLevel().addFreshEntity(cow);mobs.add(cow);positions.add(pos);
        }
        mobs.getLast().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200));
        h.onEachTick(()->{for(int i=0;i<mobs.size();i++){mobs.get(i).setPos(positions.get(i));mobs.get(i).setDeltaMovement(Vec3.ZERO);}});
        float[] stopped=new float[mobs.size()];
        h.startSequence().thenIdle(40).thenExecute(()->{
            float outer=200-mobs.get(1).getHealth(),inner=200-mobs.get(2).getHealth(),contact=200-mobs.get(3).getHealth();
            check(mobs.getFirst().getHealth()==200&&!mobs.getFirst().isOnFire(),"Warning band dealt damage");
            check(outer>=4&&inner>outer*2&&contact>inner*2,"Burning frequency/damage did not increase toward contact: "+outer+", "+inner+", "+contact);
            check(mobs.get(3).isOnFire(),"Stellar contact did not ignite a vulnerable entity");
            check(mobs.getLast().getHealth()==200&&!mobs.getLast().isOnFire(),"Fire resistance was bypassed");
            c.enabled=false;for(int i=0;i<mobs.size();i++){mobs.get(i).clearFire();stopped[i]=mobs.get(i).getHealth();}
        }).thenIdle(10).thenExecute(()->{
            for(int i=0;i<mobs.size();i++)check(mobs.get(i).getHealth()==stopped[i],"Extinguished core continued dealing heat damage");
        }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void stellarInnerCoreDealsLethalDamageOnlyInsideItsCenter(GameTestHelper h){
        var c=hot(h);var center=c.structure.seed.getBlockPos().getCenter();var entities=new ArrayList<Cow>();
        cleanup(h,()->{try{c.enabled=false;}finally{entities.forEach(Entity::discard);}});
        for(double x:new double[]{.1,1.2,-.1}){
            var cow=EntityType.COW.create(h.getLevel());cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);cow.setHealth(200);cow.setNoAi(true);cow.setNoGravity(true);cow.noPhysics=true;cow.setPos(center.add(x,-.7,0));h.getLevel().addFreshEntity(cow);entities.add(cow);
        }
        entities.getLast().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100));
        check(c.structure.seed.getBlockState().getCollisionShape(h.getLevel(),c.structure.seed.getBlockPos()).isEmpty(),"Invisible solid cube prevented core contact");
        h.startSequence().thenIdle(6).thenExecute(()->{
            check(!entities.getFirst().isAlive(),"Inner core did not kill a 200-health unprotected entity on contact");
            check(entities.get(1).isAlive()&&entities.get(1).getHealth()>180,"Lethal center damage escaped into the outer photosphere");
            check(entities.getLast().getHealth()==200,"Inner core bypassed normal fire resistance");
            c.enabled=false;entities.getLast().removeEffect(MobEffects.FIRE_RESISTANCE);entities.getLast().clearFire();
        }).thenIdle(6).thenExecute(()->check(entities.getLast().getHealth()==200,"Cold inner core remained lethal")).thenSucceed();
    }
}
