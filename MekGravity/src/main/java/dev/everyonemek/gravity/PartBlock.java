package dev.everyonemek.gravity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
public final class PartBlock extends BaseEntityBlock {
    public enum Kind {FRAME,CASING,GLASS,FUEL,COOLANT,ENERGY,COIL,CORE;public String id(){return name().toLowerCase(java.util.Locale.ROOT);}}
    public static final BooleanProperty OUTPUT=BooleanProperty.create("output"),ACTIVE=BooleanProperty.create("active");
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public final Kind kind;public final Grade grade;
    public PartBlock(Kind kind,Grade grade){super(Properties.of().strength(5,15).noOcclusion());this.kind=kind;this.grade=grade;registerDefaultState(stateDefinition.any().setValue(OUTPUT,false).setValue(ACTIVE,false).setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new PartBlock(kind,grade));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OUTPUT,ACTIVE,FACING);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Part(p,s);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var data=c.getItemInHand().get(Content.STOCK.get());return defaultBlockState().setValue(FACING,c.getNearestLookingDirection().getOpposite()).setValue(OUTPUT,data!=null&&data.getBoolean("output"));}
    @Override protected ItemInteractionResult useItemOn(ItemStack s,BlockState state,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(s.canPerformAction(mekanism.api.MekanismItemAbilities.WRENCH_CONFIGURE))return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if(l.getBlockEntity(pos) instanceof Part part)part.open(p);return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level l,BlockPos pos,Player p,BlockHitResult hit){if(l.getBlockEntity(pos) instanceof Part part)part.open(p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void appendHoverText(ItemStack s,net.minecraft.world.item.Item.TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag f){if(kind==Kind.ENERGY||kind==Kind.COOLANT)lines.add(Content.text("port_hint"));if(kind==Kind.COIL)lines.add(Content.text("coil_hint"));if(kind==Kind.FUEL)lines.add(Content.text("fuel_hint"));}
}
