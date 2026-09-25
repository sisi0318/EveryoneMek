package dev.everyonemek.gravity.solar;
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
public final class SolarConstruction {
    public static Map<BlockPos,BlockState> plan(SolarController c){
        var result=new LinkedHashMap<BlockPos,BlockState>();
        for(var slot:SolarLayout.SLOTS){if(slot.kind()==null)continue;var state=SolarContent.block(slot.kind(),slot.kind().tiered()?c.buildTier:0).get().defaultBlockState().setValue(SolarBlock.FACING,c.structure.direction(slot.face())).setValue(SolarBlock.OUTPUT,slot.output());result.put(c.structure.at(slot.x(),slot.y(),slot.z()),c.structure.layoutState(state,slot));}
        return result;
    }
    public static void preview(SolarController c,ServerPlayer p){c.structure.valid();if(!c.structure.formed&&c.structure.errorPos!=null)p.displayClientMessage(SolarContent.text("build_at",SolarContent.text(c.structure.error),c.structure.errorPos.toShortString()),false);var plan=plan(c);if(plan.isEmpty()){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_small"),false);return;}var needed=new LinkedHashMap<Item,Integer>();int shown=0;
        for(var e:plan.entrySet()){if(!p.serverLevel().hasChunkAt(e.getKey()))continue;if(matches(p.serverLevel().getBlockState(e.getKey()),e.getValue()))continue;needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);if(shown++<256){var v=e.getKey().getCenter();p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,v.x,v.y,v.z,1,0,0,0,0);}}
        if(needed.isEmpty())p.displayClientMessage(dev.everyonemek.gravity.Content.text(c.structure.formed?"build_complete":"adjust_parts"),false);else needed.forEach((item,n)->p.displayClientMessage(SolarContent.text("material_count",item.getDescription(),n),false));
    }
    public static boolean build(SolarController c,ServerPlayer p){if(!c.access(p))return false;if(c.enabled){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_stop"),false);return false;}var plan=plan(c);if(plan.isEmpty())return false;var needed=new HashMap<Item,Integer>();
        for(int x=0;x<9;x++)for(int y=0;y<9;y++)for(int z=0;z<9;z++){
            if(!SolarLayout.footprint(x,z))continue;var pos=c.structure.at(x,y,z);
            if(!p.serverLevel().hasChunkAt(pos))return false;
            if(SolarLayout.get(x,y,z)==null&&!p.serverLevel().getBlockState(pos).isAir()){p.displayClientMessage(SolarContent.text("interior"),false);return false;}
        }
        for(var e:plan.entrySet()){var pos=e.getKey();if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().mayInteract(p,pos)){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",pos.toShortString()),false);return false;}
            if(p.serverLevel().getBlockEntity(pos) instanceof SolarPart linked&&linked.controller()!=null&&linked.controller()!=c){p.displayClientMessage(dev.everyonemek.gravity.Content.text("occupied"),false);return false;}var old=p.serverLevel().getBlockState(pos);if(matches(old,e.getValue()))continue;if(!old.canBeReplaced()||p.serverLevel().getBlockEntity(pos)!=null){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",pos.toShortString()),false);return false;}needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);}
        int placed=0,missing=0;
        for(var e:plan.entrySet()){
            if(matches(p.serverLevel().getBlockState(e.getKey()),e.getValue()))continue;
            if(!dev.everyonemek.gravity.AssemblyBuild.available(p,e.getValue())){missing++;continue;}
            if(!dev.everyonemek.gravity.AssemblyBuild.place(p,e.getKey(),e.getValue(),false)){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",e.getKey().toShortString()),false);return false;}placed++;
        }
        if(missing>0)p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_partial",placed,missing),false);
        c.structure.invalidate();boolean ok=c.structure.validate();p.displayClientMessage(dev.everyonemek.gravity.Content.text(ok?"build_complete":"structure"),false);return ok;
    }
    public static boolean upgrade(SolarController c,ServerPlayer p){
        if(!c.access(p))return false;if(c.enabled){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_stop"),false);return false;}var changes=new LinkedHashMap<BlockPos,BlockState>();var needed=new HashMap<Item,Integer>();
        for(var pos:plan(c).keySet()){if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().mayInteract(p,pos))return false;
            var state=p.serverLevel().getBlockState(pos);
            if(state.getBlock() instanceof SolarBlock block&&block.kind.tiered()&&block.tier<c.buildTier){
                var be=p.serverLevel().getBlockEntity(pos);
                if(be instanceof SolarPart part&&part.master!=null&&!part.master.equals(c.getBlockPos()))return false;
                var desired=SolarContent.block(block.kind,c.buildTier).get().defaultBlockState().setValue(SolarBlock.FACING,state.getValue(SolarBlock.FACING));
                changes.put(pos,desired);needed.merge(desired.getBlock().asItem(),1,Integer::sum);
            }
        }
        if(!p.getAbilities().instabuild)for(var e:needed.entrySet())if(p.getInventory().countItem(e.getKey())<e.getValue()){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_missing",e.getKey().getDescription(),e.getValue()),false);return false;}
        for(var e:changes.entrySet())if(!dev.everyonemek.gravity.AssemblyBuild.place(p,e.getKey(),e.getValue(),true)){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",e.getKey().toShortString()),false);return false;}
        c.structure.invalidate();c.structure.valid();p.displayClientMessage(dev.everyonemek.gravity.Content.text("upgrade_complete",changes.size()),false);return true;
    }
    private static boolean matches(BlockState a,BlockState b){return a.getBlock() instanceof SolarBlock x&&b.getBlock() instanceof SolarBlock y&&x.kind==y.kind;}
    private SolarConstruction(){}
}
