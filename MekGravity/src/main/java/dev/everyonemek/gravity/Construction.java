package dev.everyonemek.gravity;
import java.util.*;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** Each placement uses ItemStack.useOn so NeoForge place events and protection cancellation apply. */
public final class Construction {
    public static Map<BlockPos,BlockState> plan(Controller c){
        var result=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++){
            var pos=c.structure.at(x,y,z);if(pos.equals(c.getBlockPos()))continue;
            int edges=(x==0||x==6?1:0)+(y==0||y==6?1:0)+(z==0||z==6?1:0);
            if(edges>0){var kind=edges>=2?PartBlock.Kind.FRAME:y==0||y==6?PartBlock.Kind.CASING:PartBlock.Kind.GLASS;result.put(pos,Content.PARTS.get(kind).get().defaultBlockState());}
            else if(x==3&&y==3&&z==3)result.put(pos,Content.PARTS.get(PartBlock.Kind.CORE).get().defaultBlockState());
            else if(Structure.coilPosition(x,y,z)){
                var center=c.structure.at(3,3,3);var direction=Direction.values()[0];for(var d:Direction.values())if(pos.relative(d,2).equals(center)){direction=d;break;}
                result.put(pos,Content.COILS.get(Grade.values()[c.buildTier]).get().defaultBlockState().setValue(PartBlock.FACING,direction));
            }
        }
        result.put(c.structure.at(1,1,0),Content.PARTS.get(PartBlock.Kind.FUEL).get().defaultBlockState());
        result.put(c.structure.at(5,1,0),Content.PARTS.get(PartBlock.Kind.ENERGY).get().defaultBlockState());
        for(int x:new int[]{1,2,4,5})result.put(c.structure.at(x,1,6),Content.PARTS.get(PartBlock.Kind.ENERGY).get().defaultBlockState().setValue(PartBlock.OUTPUT,true));
        return result;
    }
    public static void preview(Controller c,ServerPlayer p){c.structure.valid();if(!c.structure.formed&&c.structure.errorPos!=null)p.displayClientMessage(Content.text(c.structure.error).copy().append(" · "+c.structure.errorPos.toShortString()),false);var plan=plan(c);if(plan.isEmpty()){p.displayClientMessage(Content.text("build_small"),false);return;}var needed=new LinkedHashMap<Item,Integer>();int shown=0;
        for(var e:plan.entrySet()){if(!p.serverLevel().hasChunkAt(e.getKey()))continue;if(matches(p.serverLevel().getBlockState(e.getKey()),e.getValue()))continue;needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);if(shown++<256){var v=e.getKey().getCenter();p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,v.x,v.y,v.z,1,0,0,0,0);}}
        if(needed.isEmpty())p.displayClientMessage(Content.text(c.structure.formed?"build_complete":"adjust_parts"),false);else needed.forEach((item,n)->p.displayClientMessage(item.getDescription().copy().append(" × "+n),false));
    }
    public static boolean build(Controller c,ServerPlayer p){if(!c.access(p))return false;if(c.enabled){p.displayClientMessage(Content.text("build_stop"),false);return false;}var plan=plan(c);if(plan.isEmpty())return false;var needed=new HashMap<Item,Integer>();
        for(int x=1;x<7-1;x++)for(int y=1;y<7-1;y++)for(int z=1;z<7-1;z++){
            var pos=c.structure.at(x,y,z);if(!p.serverLevel().hasChunkAt(pos))return false;
            if(!plan.containsKey(pos)&&!p.serverLevel().getBlockState(pos).isAir()){
                {
                    p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;
                }
            }
        }
        for(var e:plan.entrySet()){var pos=e.getKey();if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().mayInteract(p,pos)){p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;}
            if(p.serverLevel().getBlockEntity(pos) instanceof Part linked&&linked.controller()!=null&&linked.controller()!=c){p.displayClientMessage(Content.text("occupied"),false);return false;}var old=p.serverLevel().getBlockState(pos);if(matches(old,e.getValue()))continue;if(!old.canBeReplaced()||p.serverLevel().getBlockEntity(pos)!=null){p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;}needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);}
        int placed=0,missing=0;
        for(var e:plan.entrySet()){
            if(matches(p.serverLevel().getBlockState(e.getKey()),e.getValue()))continue;
            if(!dev.everyonemek.gravity.AssemblyBuild.available(p,e.getValue())){missing++;continue;}
            if(!dev.everyonemek.gravity.AssemblyBuild.place(p,e.getKey(),e.getValue(),false)){p.displayClientMessage(Content.text("build_blocked",e.getKey().toShortString()),false);return false;}placed++;
        }
        if(missing>0)p.displayClientMessage(Content.text("build_partial",placed,missing),false);
        c.structure.invalidate();boolean ok=c.structure.validate();p.displayClientMessage(Content.text(ok?"build_complete":c.structure.error),false);return ok;
    }
    public static boolean upgrade(Controller c,ServerPlayer p){
        if(!c.access(p))return false;if(c.enabled){p.displayClientMessage(Content.text("build_stop"),false);return false;}var changes=new LinkedHashMap<BlockPos,BlockState>();var needed=new HashMap<Item,Integer>();
        for(var pos:plan(c).keySet()){if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().mayInteract(p,pos))return false;
            var state=p.serverLevel().getBlockState(pos);
            if(state.getBlock() instanceof PartBlock block&&block.kind==PartBlock.Kind.COIL&&block.grade.ordinal()<c.buildTier){
                var be=p.serverLevel().getBlockEntity(pos);
                if(be instanceof Part part&&part.master!=null&&!part.master.equals(c.getBlockPos()))return false;
                var desired=Content.COILS.get(Grade.values()[c.buildTier]).get().defaultBlockState().setValue(PartBlock.FACING,state.getValue(PartBlock.FACING));
                changes.put(pos,desired);needed.merge(desired.getBlock().asItem(),1,Integer::sum);
            }
        }
        if(!p.getAbilities().instabuild)for(var e:needed.entrySet())if(p.getInventory().countItem(e.getKey())<e.getValue()){p.displayClientMessage(Content.text("build_missing",e.getKey().getDescription(),e.getValue()),false);return false;}
        for(var e:changes.entrySet())if(!dev.everyonemek.gravity.AssemblyBuild.place(p,e.getKey(),e.getValue(),true)){p.displayClientMessage(Content.text("build_blocked",e.getKey().toShortString()),false);return false;}
        c.structure.invalidate();c.structure.valid();p.displayClientMessage(Content.text("upgrade_complete",changes.size()),false);return true;
    }
    private static boolean matches(BlockState a,BlockState b){return a.is(b.getBlock())||a.getBlock() instanceof PartBlock x&&b.getBlock() instanceof PartBlock y&&x.kind==PartBlock.Kind.COIL&&y.kind==PartBlock.Kind.COIL;}
    private Construction(){}
}
