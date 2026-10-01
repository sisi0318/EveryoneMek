package dev.everyonemek.oritech;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PartBlock extends BaseEntityBlock {
    public PartBlock(){super(Properties.of().strength(4).noOcclusion().noLootTable());}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new PartBlock());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Part(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(l.getBlockEntity(p) instanceof Part part&&part.controller() instanceof Processor main)return ProcessorBlock.open(l,main.getBlockPos(),player);return InteractionResult.SUCCESS;}
    @Override protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof ProcessorUpgrade&&level.getBlockEntity(pos) instanceof Part part&&part.controller() instanceof Processor processor)return ProcessorUpgrade.interact(stack,player,processor);
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){
        if(!l.isClientSide&&!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Part part&&part.controller() instanceof Processor main&&!main.removingParts)l.destroyBlock(main.getBlockPos(),true);
        super.onRemove(s,l,p,n,moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.PART_TILE.get(),(w,p,st,be)->{
        if((w.getGameTime()+p.asLong())%20==0&&w.hasChunkAt(be.anchor)&&be.controller()==null)w.removeBlock(p,false);
    });}
}
