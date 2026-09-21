package dev.everyonemek.factory;
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
    public static Map<BlockPos,BlockState> plan(Controller c){var plan=new LinkedHashMap<BlockPos,BlockState>();var g=c.grade();var inside=new ArrayList<BlockPos>();
        for(int x=0;x<c.sizeX;x++)for(int y=0;y<c.sizeY;y++)for(int z=0;z<c.sizeZ;z++){
            var pos=c.structure.at(x,y,z);if(pos.equals(c.getBlockPos()))continue;int edges=(x==0||x==c.sizeX-1?1:0)+(y==0||y==c.sizeY-1?1:0)+(z==0||z==c.sizeZ-1?1:0);
            if(edges>=2)plan.put(pos,Content.FRAMES.get(g).get().defaultBlockState());else if(edges==1)plan.put(pos,Content.CASING.get().defaultBlockState());else inside.add(pos);
        }
        plan.put(c.structure.at(1,1,c.sizeZ-1),Content.PORTS.get(g).get().defaultBlockState());
        plan.put(c.structure.at(c.sizeX-1,1,1),Content.PORTS.get(g).get().defaultBlockState().setValue(PartBlock.OUTPUT,true));
        if(inside.isEmpty())return Map.of();
        Block cell=switch(g){case BASIC->MekanismBlocks.BASIC_INDUCTION_CELL.get();case ADVANCED->MekanismBlocks.ADVANCED_INDUCTION_CELL.get();case ELITE->MekanismBlocks.ELITE_INDUCTION_CELL.get();case ULTIMATE->MekanismBlocks.ULTIMATE_INDUCTION_CELL.get();};
        Block provider=switch(g){case BASIC->MekanismBlocks.BASIC_INDUCTION_PROVIDER.get();case ADVANCED->MekanismBlocks.ADVANCED_INDUCTION_PROVIDER.get();case ELITE->MekanismBlocks.ELITE_INDUCTION_PROVIDER.get();case ULTIMATE->MekanismBlocks.ULTIMATE_INDUCTION_PROVIDER.get();};
        // A compact cube stores energy in its center; its provider replaces the top center casing.
        // Larger saved factories keep their original internal cell/provider placement.
        var ordered=new LinkedHashMap<BlockPos,BlockState>();ordered.put(inside.get(0),cell.defaultBlockState());
        if(inside.size()==1){var top=c.structure.at(1,c.sizeY-1,1);plan.remove(top);ordered.put(top,provider.defaultBlockState());}
        else ordered.put(inside.get(1),provider.defaultBlockState());
        ordered.putAll(plan);return ordered;
    }
    public static void preview(Controller c,ServerPlayer p){var plan=plan(c);if(plan.isEmpty()){p.displayClientMessage(Content.text("build_small"),false);return;}var needed=new LinkedHashMap<Item,Integer>();int shown=0;
        for(var e:plan.entrySet()){if(!p.serverLevel().hasChunkAt(e.getKey()))continue;if(p.serverLevel().getBlockState(e.getKey()).is(e.getValue().getBlock()))continue;needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);if(shown++<256){var v=e.getKey().getCenter();p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,v.x,v.y,v.z,1,0,0,0,0);}}
        if(needed.isEmpty())p.displayClientMessage(Content.text("build_complete"),false);else needed.forEach((item,n)->p.displayClientMessage(item.getDescription().copy().append(" × "+n),false));
    }
    public static boolean build(Controller c,ServerPlayer p){if(!c.access(p)||!c.processing.jobs.isEmpty())return false;var plan=plan(c);if(plan.isEmpty())return false;var needed=new HashMap<Item,Integer>();
        for(int x=1;x<c.sizeX-1;x++)for(int y=1;y<c.sizeY-1;y++)for(int z=1;z<c.sizeZ-1;z++){
            var pos=c.structure.at(x,y,z);if(!p.serverLevel().hasChunkAt(pos))return false;
            if(!plan.containsKey(pos)&&!p.serverLevel().getBlockState(pos).isAir()){
                var be=p.serverLevel().getBlockEntity(pos);if(!InductionAccess.part(be)){
                    p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;
                }
            }
        }
        for(var e:plan.entrySet()){var pos=e.getKey();if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().mayInteract(p,pos)){p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;}
            var old=p.serverLevel().getBlockState(pos);if(old.is(e.getValue().getBlock()))continue;if(!old.canBeReplaced()||p.serverLevel().getBlockEntity(pos)!=null){p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;}needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);}
        if(!p.getAbilities().instabuild)for(var e:needed.entrySet())if(p.getInventory().countItem(e.getKey())<e.getValue()){p.displayClientMessage(Content.text("build_missing",e.getKey().getDescription(),e.getValue()),false);return false;}
        for(var e:plan.entrySet()){
            var pos=e.getKey();if(p.serverLevel().getBlockState(pos).is(e.getValue().getBlock()))continue;
            Item material=e.getValue().getBlock().asItem();int slot=-1;ItemStack source=new ItemStack(material);
            if(!p.getAbilities().instabuild){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(material)){slot=i;source=p.getInventory().getItem(i);break;}if(slot<0)return false;}
            var hand=p.getMainHandItem();var placing=source.copyWithCount(1);boolean success;
            try {p.setItemInHand(InteractionHand.MAIN_HAND,placing);var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));placing.useOn(context);success=p.serverLevel().getBlockState(pos).is(e.getValue().getBlock());}
            finally{p.setItemInHand(InteractionHand.MAIN_HAND,hand);}
            if(!success){p.displayClientMessage(Content.text("build_blocked",pos.toShortString()),false);return false;}
            if(!p.getAbilities().instabuild){var held=p.getInventory().getItem(slot);held.shrink(1);p.getInventory().setChanged();}
            if(e.getValue().hasProperty(PartBlock.OUTPUT)&&e.getValue().getValue(PartBlock.OUTPUT))p.serverLevel().setBlockAndUpdate(pos,p.serverLevel().getBlockState(pos).setValue(PartBlock.OUTPUT,true));
        }
        c.structure.invalidate();boolean ok=c.structure.validate();p.displayClientMessage(Content.text(ok?"build_complete":c.structure.error),false);return ok;
    }
    private Construction(){}
}
