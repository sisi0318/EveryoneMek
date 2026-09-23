package dev.everyonemek.gravity;

import static dev.everyonemek.gravity.ReactorTests.check;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class CoreAnimationTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void coreMotionUsesLoadAndDoesNotOverwriteInventoryOrSaveVisualState(GameTestHelper h){
        var c=ReactorTests.formed(h);var core=c.structure.core;var fuel=c.structure.fuelHatches.getFirst();
        var registry=h.getLevel().registryAccess();c.stored=12345;c.fuelRemaining=67890;
        fuel.inventory.setStackInSlot(0,new ItemStack(Content.PELLET.get(),9));
        try{
            c.gross=c.structure.grade.power()*3/4;c.structure.activity(true);
            check(core.getBlockState().getRenderShape()==RenderShape.ENTITYBLOCK_ANIMATED,"Core retained the static world mesh");
            check(core.visualLoad()==75&&core.getUpdateTag(registry).getByte("visual_load")==75,"Core effect did not track actual load");
            var packet=core.getUpdatePacket();check(packet!=null&&packet.getTag().getAllKeys().equals(java.util.Set.of("visual_load")),"Visual packet leaked resource/owner data");
            var saved=core.saveWithFullMetadata(registry);check(!saved.contains("visual_load"),"Cosmetic load was persisted as gameplay state");
            var mirror=new Part(core.getBlockPos(),core.getBlockState());mirror.setLevel(h.getLevel());mirror.handleUpdateTag(packet.getTag(),registry);
            check(mirror.visualLoad()==75,"Chunk update tag did not initialize effect load");
            var invalid=new CompoundTag();invalid.putInt("visual_load",127);mirror.handleUpdateTag(invalid,registry);check(mirror.visualLoad()==100,"Effect load was not clamped");
            invalid.putByte("visual_load",(byte)-20);mirror.handleUpdateTag(invalid,registry);check(mirror.visualLoad()==0,"Negative effect load accepted");
            var master=fuel.master;fuel.handleUpdateTag(packet.getTag(),registry);
            check(fuel.getUpdatePacket()==null&&fuel.inventory.getStackInSlot(0).getCount()==9&&master.equals(fuel.master),"Visual update overwrote hatch inventory/link");
            c.structure.activity(false);check(core.visualLoad()==0&&!core.getBlockState().getValue(PartBlock.ACTIVE),"Stop did not clear the effect immediately");
            c.gross=c.structure.grade.power();c.structure.activity(true);check(core.visualLoad()==100,"Full load animation missing");
            h.getLevel().setBlockAndUpdate(c.structure.at(0,0,0),Blocks.AIR.defaultBlockState());
            check(core.visualLoad()==0&&!core.getBlockState().getValue(PartBlock.ACTIVE),"Broken reactor retained effect load");
            check(c.stored==12345&&c.fuelRemaining==67890,"Animation changed real energy/fuel");
            // The same elapsed world time must yield the same phase at different frame rates.
            double referencePhase=Double.NaN;float referenceStrength=0;
            for(int fps:new int[]{20,60,144}){
                var motion=new CoreMotion();motion.update(0,1);
                for(int frame=1;frame<=fps*10;frame++)motion.update(frame*20D/fps,1);
                if(Double.isNaN(referencePhase)){referencePhase=motion.phase();referenceStrength=motion.strength();}
                else check(Math.abs(referencePhase-motion.phase())<1e-7&&Math.abs(referenceStrength-motion.strength())<1e-7,"Core speed depends on FPS");
                double previous=motion.phase();motion.update(200,0);check(motion.phase()==previous,"Paused game moved the core");
                for(int tick=201;tick<=400;tick++)motion.update(tick,0);
                check(motion.strength()==0,"Core never finished slowing down");
                double stopped=motion.phase();motion.update(401,0);check(motion.phase()==stopped,"Stopped core kept rotating");
            }
        }finally{c.enabled=false;c.gross=0;c.structure.activity(false);}
        h.succeed();
    }
}
