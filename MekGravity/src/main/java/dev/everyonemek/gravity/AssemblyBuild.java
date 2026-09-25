package dev.everyonemek.gravity;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

/** One paid placement or protected in-place grade replacement, never a second inventory. */
public final class AssemblyBuild {
    public static int materialSlot(ServerPlayer p,Item item){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))return i;return -1;}
    public static boolean available(ServerPlayer p,BlockState desired){return p.getAbilities().instabuild||materialSlot(p,desired.getBlock().asItem())>=0;}
    public static boolean place(ServerPlayer p,BlockPos pos,BlockState desired,boolean upgrade){
        var level=p.serverLevel();if(!level.hasChunkAt(pos)||!level.mayInteract(p,pos))return false;
        int slot=materialSlot(p,desired.getBlock().asItem());if(!p.getAbilities().instabuild&&slot<0)return false;
        var source=slot<0?new ItemStack(desired.getBlock()):p.getInventory().getItem(slot);var hand=p.getMainHandItem();var placing=source.copyWithCount(1);
        if(!p.mayUseItemAt(pos,Direction.UP,placing))return false;
        boolean success=false;java.util.List<ItemStack> refunds=java.util.List.of();
        try{
            p.setItemInHand(InteractionHand.MAIN_HAND,placing);
            if(upgrade){
                var old=level.getBlockState(pos);var be=level.getBlockEntity(pos);
                if(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,old,p)).isCanceled())return false;
                var snapshot=BlockSnapshot.create(level.dimension(),level,pos);var tag=be==null?null:be.saveWithFullMetadata(level.registryAccess());
                refunds=Block.getDrops(old,level,pos,be);
                try{
                    if(!level.setBlock(pos,desired,2))return false;
                    if(EventHooks.onBlockPlace(p,snapshot,Direction.UP)||!level.getBlockState(pos).is(desired.getBlock()))return false;
                    if(tag!=null&&level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).loadWithComponents(tag,level.registryAccess());
                    success=true;level.sendBlockUpdated(pos,old,desired,3);level.updateNeighborsAt(pos,desired.getBlock());
                }finally{if(!success){boolean restoring=level.restoringBlockSnapshots;try{level.restoringBlockSnapshots=true;snapshot.restore();}finally{level.restoringBlockSnapshots=restoring;}}}
            }else{
                placing.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
                success=level.getBlockState(pos).is(desired.getBlock());
                if(success)level.setBlockAndUpdate(pos,desired);
            }
        }finally{p.setItemInHand(InteractionHand.MAIN_HAND,hand);}
        // Return the real hand before inventory accounting: refunds must never merge into
        // the temporary placement stack that is about to be discarded.
        if(success&&!p.getAbilities().instabuild){source.shrink(1);p.getInventory().setChanged();for(var refund:refunds)if(!p.getInventory().add(refund))p.drop(refund,false);}
        return success;
    }
    private AssemblyBuild(){}
}
