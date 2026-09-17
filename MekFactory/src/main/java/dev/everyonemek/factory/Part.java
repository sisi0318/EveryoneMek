package dev.everyonemek.factory;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class Part extends BlockEntity {
    public BlockPos master;
    public Part(BlockPos pos,BlockState state){super(Content.PART.get(),pos,state);}
    public Controller controller(){
        if(level==null||master==null||isRemoved()||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this||!level.hasChunkAt(master))return null;
        return level.getBlockEntity(master) instanceof Controller c&&c.structure.contains(worldPosition)?c:null;
    }
    public void open(Player p){var c=controller();if(c!=null)FactoryMenu.open(p,c,worldPosition);else if(!level.isClientSide)p.displayClientMessage(Content.text("unlinked"),true);}
    public void toggle(Player player){
        var c=controller();
        if(c==null||!c.access(player)||player.distanceToSqr(worldPosition.getCenter())>64)return;
        level.setBlockAndUpdate(worldPosition,getBlockState().cycle(PartBlock.OUTPUT));c.structure.invalidate();c.structure.validate();
        player.displayClientMessage(Content.text(getBlockState().getValue(PartBlock.OUTPUT)?"port_output":"port_input"),true);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);if(master!=null)tag.putLong("master",master.asLong());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);master=tag.contains("master")?BlockPos.of(tag.getLong("master")):null;}
}
