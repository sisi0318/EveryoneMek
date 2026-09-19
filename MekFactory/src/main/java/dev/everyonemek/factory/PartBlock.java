package dev.everyonemek.factory;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class PartBlock extends BaseEntityBlock {
    public enum Kind {FRAME,CASING,GLASS,PORT}
    public static final BooleanProperty OUTPUT=BooleanProperty.create("output");
    public final Kind kind;public final Grade grade;
    public PartBlock(Kind kind,Grade grade){super(Properties.of().strength(4,12).noOcclusion());this.kind=kind;this.grade=grade;registerDefaultState(stateDefinition.any().setValue(OUTPUT,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new PartBlock(kind,grade));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(OUTPUT);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Part(pos,state);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){var state=defaultBlockState();var data=context.getItemInHand().get(Content.PORT_DATA.get());return data==null||kind!=Kind.PORT?state:state.setValue(OUTPUT,data.getBoolean("output"));}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag){
        if(kind==Kind.PORT){tooltip.add(Content.text("port_power_hint"));tooltip.add(Content.text("port_mode_hint"));}
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(kind==Kind.PORT&&stack.canPerformAction(mekanism.api.MekanismItemAbilities.WRENCH_CONFIGURE))return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if(level.getBlockEntity(pos) instanceof Part p)p.open(player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof Part p)p.open(player);return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
