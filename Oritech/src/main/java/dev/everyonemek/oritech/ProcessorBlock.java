package dev.everyonemek.oritech;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class ProcessorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty DEPLOYED=BooleanProperty.create("deployed");
    public ProcessorBlock(){super(Properties.of().strength(4).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(DEPLOYED,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new ProcessorBlock());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,DEPLOYED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Processor(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return s.getValue(DEPLOYED)?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,Content.TILE.get(),(w,p,st,be)->be.tick(w,p,st,be));}
    static InteractionResult open(Level l,BlockPos p,Player player){if(!l.isClientSide&&l.getBlockEntity(p) instanceof Processor be&&player instanceof ServerPlayer sp)
        dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(sp,be);return InteractionResult.SUCCESS;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return open(l,p,player);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Processor be)be.removeParts();super.onRemove(s,l,p,n,moving);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
}
