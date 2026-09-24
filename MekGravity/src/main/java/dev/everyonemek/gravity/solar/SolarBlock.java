package dev.everyonemek.gravity.solar;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
public final class SolarBlock extends BaseEntityBlock {
    public enum Kind{BASE,SUPPORT,RING,CROWN,FOCUS,SEED,COLLECTOR,FUEL,ENERGY;
        public String id(){return name().toLowerCase(java.util.Locale.ROOT);}public boolean tiered(){return this==RING||this==FOCUS||this==COLLECTOR;}}
    public static final BooleanProperty FORMED=BooleanProperty.create("formed"),ACTIVE=BooleanProperty.create("active"),OUTPUT=BooleanProperty.create("output");
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final IntegerProperty SEGMENT=IntegerProperty.create("segment",0,8);
    public final Kind kind;public final int tier;
    public SolarBlock(Kind kind,int tier){super(Properties.of().strength(6,20).noOcclusion());this.kind=kind;this.tier=tier;registerDefaultState(stateDefinition.any().setValue(FORMED,false).setValue(ACTIVE,false).setValue(OUTPUT,false).setValue(FACING,Direction.NORTH).setValue(SEGMENT,kind==Kind.RING||kind==Kind.CROWN?2:4));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new SolarBlock(kind,tier));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FORMED,ACTIVE,OUTPUT,FACING,SEGMENT);}
    @Override public RenderShape getRenderShape(BlockState s){return kind==Kind.SEED?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.MODEL;}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return kind==Kind.FOCUS?dev.everyonemek.gravity.ModelShapes.coil(s.getValue(FACING)):super.getShape(s,l,p,c);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return kind==Kind.FOCUS?dev.everyonemek.gravity.ModelShapes.coil(s.getValue(FACING)):super.getCollisionShape(s,l,p,c);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SolarPart(pos,state);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var t=c.getItemInHand().get(SolarContent.STOCK.get());var face=kind==Kind.COLLECTOR?c.getHorizontalDirection().getOpposite():c.getNearestLookingDirection().getOpposite();return SolarStructure.placement(c.getLevel(),c.getClickedPos(),defaultBlockState().setValue(FACING,face).setValue(OUTPUT,t!=null&&t.getBoolean("output")));}
    @Override protected ItemInteractionResult useItemOn(ItemStack s,BlockState state,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(s.canPerformAction(mekanism.api.MekanismItemAbilities.WRENCH_CONFIGURE))return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;if(l.getBlockEntity(pos) instanceof SolarPart part)part.open(p);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(l.getBlockEntity(pos) instanceof SolarPart part)part.open(p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public boolean hasAnalogOutputSignal(BlockState s){return kind==Kind.FUEL;}
    @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos pos){if(l.getBlockEntity(pos) instanceof SolarPart part){var c=part.controller();if(c!=null&&c.structure.valid())return c.fuelAvailable()?0:15;}return 0;}
}
