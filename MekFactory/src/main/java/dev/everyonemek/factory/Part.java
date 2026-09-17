package dev.everyonemek.factory;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class Part extends BlockEntity implements mekanism.api.IConfigurable {
    public BlockPos master;
    public Part(BlockPos pos,BlockState state){super(Content.PART.get(),pos,state);}
    public Controller controller(){
        if(level==null||master==null||isRemoved()||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this||!level.hasChunkAt(master))return null;
        return level.getBlockEntity(master) instanceof Controller c&&c.structure.contains(worldPosition)?c:null;
    }
    public void open(Player p){var c=controller();if(c!=null)FactoryMenu.open(p,c,worldPosition);else if(!level.isClientSide)p.displayClientMessage(Content.text("unlinked"),true);}
    public boolean setOutput(Player player,boolean output){
        if(level==null||!(getBlockState().getBlock() instanceof PartBlock b)||b.kind!=PartBlock.Kind.PORT)return false;
        var c=controller();
        if(player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>64||!player.mayInteract(level,worldPosition)||c!=null&&!c.access(player))return false;
        if(level.isClientSide)return true;
        applyOutput(output);
        if(c!=null){c.structure.validate();PortConfiguration.refresh(c);}
        player.displayClientMessage(Content.text(getBlockState().getValue(PartBlock.OUTPUT)?"port_output":"port_input"),true);
        return true;
    }
    void applyOutput(boolean output){level.setBlockAndUpdate(worldPosition,getBlockState().setValue(PartBlock.OUTPUT,output));var c=controller();if(c!=null)c.structure.invalidate();}
    public void toggle(Player player){setOutput(player,!getBlockState().getValue(PartBlock.OUTPUT));}
    @Override public net.minecraft.world.InteractionResult onSneakRightClick(Player p){return setOutput(p,!getBlockState().getValue(PartBlock.OUTPUT))?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.FAIL;}
    @Override public net.minecraft.world.InteractionResult onRightClick(Player p){if(!level.isClientSide){var c=controller();if(c!=null&&!c.access(p))return net.minecraft.world.InteractionResult.FAIL;p.displayClientMessage(Content.text(getBlockState().getValue(PartBlock.OUTPUT)?"port_output":"port_input"),true);}return net.minecraft.world.InteractionResult.SUCCESS;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);if(master!=null)tag.putLong("master",master.asLong());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);master=tag.contains("master")?BlockPos.of(tag.getLong("master")):null;}
}
