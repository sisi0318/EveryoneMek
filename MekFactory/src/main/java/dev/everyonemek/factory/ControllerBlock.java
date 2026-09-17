package dev.everyonemek.factory;
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
    public final Grade grade;
    public ControllerBlock(Grade grade,Machine<Controller> type){super(type,p->p.strength(4,12));this.grade=grade;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown())return super.useItemOn(stack,state,level,pos,player,hand,hit);
        if(level.getBlockEntity(pos) instanceof Controller c)FactoryMenu.open(player,c,pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof Controller c)FactoryMenu.open(player,c,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&level.getBlockEntity(pos) instanceof Controller c)c.structure.detach();
        super.onRemove(state,level,pos,replacement,moving);
    }
}
