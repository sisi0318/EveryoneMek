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
        for(var slot:SolarLayout.SLOTS){if(slot.kind()==null)continue;var state=SolarContent.block(slot.kind(),0).get().defaultBlockState().setValue(SolarBlock.FACING,c.structure.direction(slot.face())).setValue(SolarBlock.OUTPUT,slot.output());result.put(c.structure.at(slot.x(),slot.y(),slot.z()),c.structure.layoutState(state,slot));}
        return result;
    }
    public static void preview(SolarController c,ServerPlayer p){c.structure.valid();if(!c.structure.formed&&c.structure.errorPos!=null)p.displayClientMessage(SolarContent.text("build_at",SolarContent.text(c.structure.error),c.structure.errorPos.toShortString()),false);var plan=plan(c);if(plan.isEmpty()){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_small"),false);return;}var needed=new LinkedHashMap<Item,Integer>();int shown=0;
        for(var e:plan.entrySet()){if(!p.serverLevel().hasChunkAt(e.getKey()))continue;if(matches(p.serverLevel().getBlockState(e.getKey()),e.getValue()))continue;needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);if(shown++<256){var v=e.getKey().getCenter();p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,v.x,v.y,v.z,1,0,0,0,0);}}
        if(needed.isEmpty())p.displayClientMessage(dev.everyonemek.gravity.Content.text(c.structure.formed?"build_complete":"adjust_parts"),false);else needed.forEach((item,n)->p.displayClientMessage(SolarContent.text("material_count",item.getDescription(),n),false));
    }
    public static boolean build(SolarController c,ServerPlayer p){if(!c.access(p)||c.enabled)return false;var plan=plan(c);if(plan.isEmpty())return false;var needed=new HashMap<Item,Integer>();
        for(int x=0;x<9;x++)for(int y=0;y<9;y++)for(int z=0;z<9;z++){
            if(!SolarLayout.footprint(x,z))continue;var pos=c.structure.at(x,y,z);
            if(!p.serverLevel().hasChunkAt(pos))return false;
            if(SolarLayout.get(x,y,z)==null&&!p.serverLevel().getBlockState(pos).isAir()){p.displayClientMessage(SolarContent.text("interior"),false);return false;}
        }
        for(var e:plan.entrySet()){var pos=e.getKey();if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().mayInteract(p,pos)){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",pos.toShortString()),false);return false;}
            if(p.serverLevel().getBlockEntity(pos) instanceof SolarPart linked&&linked.controller()!=null&&linked.controller()!=c){p.displayClientMessage(dev.everyonemek.gravity.Content.text("occupied"),false);return false;}var old=p.serverLevel().getBlockState(pos);if(matches(old,e.getValue()))continue;if(!old.canBeReplaced()||p.serverLevel().getBlockEntity(pos)!=null){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",pos.toShortString()),false);return false;}needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);}
        if(!p.getAbilities().instabuild)for(var e:needed.entrySet())if(p.getInventory().countItem(e.getKey())<e.getValue()){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_missing",e.getKey().getDescription(),e.getValue()),false);return false;}
        for(var e:plan.entrySet()){
            var pos=e.getKey();if(matches(p.serverLevel().getBlockState(pos),e.getValue()))continue;
            Item material=e.getValue().getBlock().asItem();int slot=-1;ItemStack source=new ItemStack(material);
            if(!p.getAbilities().instabuild){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(material)){slot=i;source=p.getInventory().getItem(i);break;}if(slot<0)return false;}
            var hand=p.getMainHandItem();var placing=source.copyWithCount(1);boolean success;
            try {p.setItemInHand(InteractionHand.MAIN_HAND,placing);var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));placing.useOn(context);success=matches(p.serverLevel().getBlockState(pos),e.getValue());}
            finally{p.setItemInHand(InteractionHand.MAIN_HAND,hand);}
            if(!success){p.displayClientMessage(dev.everyonemek.gravity.Content.text("build_blocked",pos.toShortString()),false);return false;}
            if(!p.getAbilities().instabuild){var held=p.getInventory().getItem(slot);held.shrink(1);p.getInventory().setChanged();}
            p.serverLevel().setBlockAndUpdate(pos,e.getValue());
        }
        c.structure.invalidate();boolean ok=c.structure.validate();p.displayClientMessage(dev.everyonemek.gravity.Content.text(ok?"build_complete":"structure"),false);return ok;
    }
    private static boolean matches(BlockState a,BlockState b){return a.getBlock() instanceof SolarBlock x&&b.getBlock() instanceof SolarBlock y&&x.kind==y.kind;}
    private SolarConstruction(){}
}
