package dev.everyonemek.factory;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class Part extends BlockEntity implements mekanism.api.IConfigurable {
    public BlockPos master;
    private Buffers storage;
    public Part(BlockPos pos,BlockState state){super(Content.PART.get(),pos,state);}
    public Grade grade(){return ((PartBlock)getBlockState().getBlock()).grade;}
    public boolean isPort(){return getBlockState().getBlock() instanceof PartBlock b&&b.kind==PartBlock.Kind.PORT;}
    public Buffers storage(){if(storage==null)storage=new Buffers(this);return storage;}
    public boolean canOpen(Player p){
        if(level==null||isRemoved()||p.level()!=level||p.distanceToSqr(worldPosition.getCenter())>64)return false;
        var c=controller();if(c!=null)return c.access(p);if(master==null)return true;if(!level.hasChunkAt(master))return false;
        return !(level.getBlockEntity(master) instanceof Controller previous)||previous.access(p);
    }
    public Controller controller(){
        if(level==null||master==null||isRemoved()||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this||!level.hasChunkAt(master))return null;
        return level.getBlockEntity(master) instanceof Controller c&&c.structure.contains(worldPosition)?c:null;
    }
    public void open(Player p){if(isPort()){WarehouseMenu.open(p,this);return;}var c=controller();if(c!=null)FactoryMenu.open(p,c,worldPosition);else if(!level.isClientSide)p.displayClientMessage(Content.text("unlinked"),true);}
    public boolean setOutput(Player player,boolean output){
        if(level==null||!(getBlockState().getBlock() instanceof PartBlock b)||b.kind!=PartBlock.Kind.PORT)return false;
        var c=controller();
        if(!canOpen(player)||!player.mayInteract(level,worldPosition))return false;
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
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);if(master!=null)tag.putLong("master",master.asLong());if(isPort())tag.put("warehouse",storage().save(r));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);master=tag.contains("master")?BlockPos.of(tag.getLong("master")):null;if(isPort())storage().load(tag.getCompound("warehouse"),r);}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder){super.collectImplicitComponents(builder);if(isPort()){var tag=storage().save(level.registryAccess());tag.putBoolean("output",getBlockState().getValue(PartBlock.OUTPUT));builder.set(Content.PORT_DATA.get(),tag);}}
    @Override protected void applyImplicitComponents(DataComponentInput input){super.applyImplicitComponents(input);var data=input.get(Content.PORT_DATA.get());if(isPort()&&data!=null)storage().load(data,level.registryAccess());}
}
