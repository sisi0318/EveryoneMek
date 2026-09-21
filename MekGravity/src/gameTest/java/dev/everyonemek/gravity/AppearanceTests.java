package dev.everyonemek.gravity;

import static dev.everyonemek.gravity.ReactorTests.check;
import java.util.*;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class AppearanceTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void assembledGeometryOrientsReloadsAndResetsWithoutChangingStock(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(20,4,20));
        for(var facing:Direction.Plane.HORIZONTAL){
            level.setBlockAndUpdate(origin,Attribute.setFacing(Content.CONTROLLER.get().defaultBlockState(),facing));
            var c=(Controller)level.getBlockEntity(origin);var plan=Construction.plan(c);
            try{
                plan.forEach(level::setBlockAndUpdate);check(c.structure.valid(),"Could not form facing "+facing+": "+c.structure.error);
                c.autoEject=false;c.stored=123456;c.structure.fuelHatches.getFirst().inventory.setStackInSlot(0,new ItemStack(Content.PELLET.get(),7));
                int corners=0,vertical=0,xBeams=0,zBeams=0;
                for(var pos:plan.keySet()){
                    var state=level.getBlockState(pos);var kind=((PartBlock)state.getBlock()).kind;
                    check(state.getValue(PartBlock.FORMED),"Part not assembled: "+pos);
                    if(kind==PartBlock.Kind.FRAME)switch(state.getValue(PartBlock.FACING)){
                        case DOWN->corners++;case UP->vertical++;case EAST->xBeams++;case NORTH->zBeams++;
                        default->throw new GameTestAssertException("Invalid beam axis");
                    }
                    if(kind==PartBlock.Kind.GLASS||kind==PartBlock.Kind.ENERGY||kind==PartBlock.Kind.FUEL)
                        check(c.structure.outward(pos,state.getValue(PartBlock.FACING)),"Panel/port points into the chamber: "+kind+" "+pos);
                }
                check(corners==8&&vertical==20&&xBeams==20&&zBeams==20,"Wrong joints/beam axes facing "+facing);
                check(level.getBlockState(c.structure.at(3,0,3)).getValue(PartBlock.FACING)==Direction.UP,"Floor recess faces outside");
                check(level.getBlockState(c.structure.at(3,6,3)).getValue(PartBlock.FACING)==Direction.DOWN,"Roof recess faces outside");
                var saved=c.saveWithFullMetadata(level.registryAccess());c.loadWithComponents(saved,level.registryAccess());
                check(c.structure.valid()&&c.stored==123456&&c.structure.fuelHatches.getFirst().inventory.getStackInSlot(0).getCount()==7,"Reload changed resources or assembly");
                check(c.structure.energyInputs==1&&c.structure.energyOutputs==4,"Cosmetic direction changed port modes");
                c.structure.activity(true);var broken=c.structure.at(0,0,0);level.setBlockAndUpdate(broken,Blocks.AIR.defaultBlockState());
                check(!level.getBlockState(c.structure.at(0,1,0)).getValue(PartBlock.FORMED),"Broken frame kept assembled appearance");
                check(!level.getBlockState(c.structure.at(3,3,3)).getValue(PartBlock.ACTIVE),"Broken structure kept core effects active");
                level.setBlockAndUpdate(broken,plan.get(broken));check(c.structure.valid(),"Repair did not re-form");
                var coil=c.structure.coils.getFirst();var aim=coil.getBlockState();
                level.setBlockAndUpdate(coil.getBlockPos(),aim.setValue(PartBlock.FACING,aim.getValue(PartBlock.FACING).getOpposite()));
                check(!c.structure.valid()&&c.structure.error.equals("coil_facing"),"Cosmetic update filter swallowed a real coil rotation");
                level.setBlockAndUpdate(coil.getBlockPos(),aim);check(c.structure.valid(),"Restored coil aim did not recover");
                level.setBlockAndUpdate(origin,Attribute.setFacing(c.getBlockState(),facing.getClockWise()));
                for(var pos:plan.keySet())check(!level.getBlockState(pos).getValue(PartBlock.FORMED),"Turned controller left an old assembled part outside new bounds");
            }finally{
                c.structure.detach();for(var pos:plan.keySet())level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(origin,Blocks.AIR.defaultBlockState());
            }
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void coilOutlineAndCollisionHitTheExtendedNoseOnAllSixFaces(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(8,5,8));
        for(var direction:Direction.values()){
            var state=Content.COILS.get(Grade.BASIC).get().defaultBlockState().setValue(PartBlock.FACING,direction);
            level.setBlockAndUpdate(pos,state);var normal=Vec3.atLowerCornerOf(direction.getNormal());
            var tangent=Vec3.atLowerCornerOf((direction.getAxis()==Direction.Axis.X?Direction.UP:Direction.EAST).getNormal()).scale(.25);
            var start=pos.getCenter().add(normal.scale(2)).add(tangent);var end=pos.getCenter().add(normal.scale(-.45)).add(tangent);
            for(var mode:List.of(ClipContext.Block.OUTLINE,ClipContext.Block.COLLIDER)){
                var hit=level.clip(new ClipContext(start,end,mode,ClipContext.Fluid.NONE,CollisionContext.empty()));
                check(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos),"Could not target emitter nose: "+direction+" "+mode);
                check(Math.abs(hit.getLocation().subtract(pos.getCenter()).dot(normal)-.75)<1e-6,"Ray hit old cube instead of the extended nose: "+direction+" "+mode);
            }
        }
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.succeed();
    }
}
