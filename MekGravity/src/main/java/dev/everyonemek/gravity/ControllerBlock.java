package dev.everyonemek.gravity;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class ControllerBlock extends BlockTile<Controller,Machine<Controller>> {
    public ControllerBlock(Machine<Controller> type){super(type,p->p.strength(5,15));}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(p.isShiftKeyDown())return super.useItemOn(stack,state,level,pos,p,hand,hit);
        if(level.getBlockEntity(pos) instanceof Controller c)ReactorMenu.open(p,c,pos);return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level l,BlockPos pos,Player p,BlockHitResult hit){if(l.getBlockEntity(pos) instanceof Controller c)ReactorMenu.open(p,c,pos);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void onRemove(BlockState state,Level l,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&l.getBlockEntity(pos) instanceof Controller c)c.structure.detach();super.onRemove(state,l,pos,next,moving);}
}
