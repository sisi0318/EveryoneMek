package dev.everyonemek.gravity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
public final class Part extends BlockEntity implements mekanism.api.IConfigurable {
    public BlockPos master;
    private long ioTick=Long.MIN_VALUE;
    public long inputUsed,outputUsed;private long previousInput,previousOutput;
    private int visualLoad,sentVisualLoad;
    private long visualSentAt=Long.MIN_VALUE;
    public int visualLoad(){return visualLoad;}
    public void updateVisualLoad(int percent){
        if(kind()!=PartBlock.Kind.CORE||level==null||level.isClientSide)return;
        visualLoad=Math.clamp(percent,0,100);
        long now=level.getGameTime();boolean edge=(sentVisualLoad==0)!=(visualLoad==0);
        if(visualLoad!=sentVisualLoad&&(edge||visualSentAt==Long.MIN_VALUE||now-visualSentAt>=10)){
            sentVisualLoad=visualLoad;visualSentAt=now;
            // Just a compact effect snapshot for tracking clients, never the fuel/energy inventory.
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var tag=new CompoundTag();if(kind()==PartBlock.Kind.CORE)tag.putByte("visual_load",(byte)visualLoad);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return kind()==PartBlock.Kind.CORE?net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this):null;}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider r){if(kind()==PartBlock.Kind.CORE)visualLoad=Math.clamp(tag.getByte("visual_load"),0,100);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,HolderLookup.Provider r){handleUpdateTag(packet.getTag(),r);}
    public void clock(){if(level.getGameTime()!=ioTick){previousInput=inputUsed;previousOutput=outputUsed;inputUsed=outputUsed=0;ioTick=level.getGameTime();}}
    public final ItemStackHandler inventory=new ItemStackHandler(18){
        @Override public boolean isItemValid(int i,ItemStack s){return kind()==PartBlock.Kind.FUEL&&FuelRecipe.find(level,s)!=null;}
        @Override protected void onContentsChanged(int i){Part.this.setChanged();}
    };
    public Part(BlockPos pos,BlockState state){super(Content.PART.get(),pos,state);}
    public PartBlock.Kind kind(){return ((PartBlock)getBlockState().getBlock()).kind;}
    public boolean output(){return getBlockState().getValue(PartBlock.OUTPUT);}
    public long inputRate(){long age=level==null?2:level.getGameTime()-ioTick;return age==0?(inputUsed>0?inputUsed:previousInput):age==1?inputUsed:0;}
    public long outputRate(){long age=level==null?2:level.getGameTime()-ioTick;return age==0?(outputUsed>0?outputUsed:previousOutput):age==1?outputUsed:0;}
    public Controller controller(){if(level==null||master==null||isRemoved()||!level.hasChunkAt(worldPosition)||!level.hasChunkAt(master)||level.getBlockEntity(worldPosition)!=this)return null;return level.getBlockEntity(master) instanceof Controller c&&c.structure.contains(worldPosition)?c:null;}
    public boolean canOpen(Player p){if(level==null||p.level()!=level||isRemoved()||p.distanceToSqr(worldPosition.getCenter())>64)return false;var c=controller();if(c!=null)return c.access(p);if(master==null)return true;if(!level.hasChunkAt(master))return false;return !(level.getBlockEntity(master) instanceof Controller old)||old.access(p);}
    public void open(Player p){if(!canOpen(p))return;if(kind()==PartBlock.Kind.FUEL)FuelMenu.open(p,this);else {var c=controller();if(c!=null)ReactorMenu.open(p,c,worldPosition);else if(!level.isClientSide)p.displayClientMessage(Content.text("unlinked"),true);}}
    @Override public net.minecraft.world.InteractionResult onSneakRightClick(Player p){
        if(!canOpen(p)||!p.mayInteract(level,worldPosition))return net.minecraft.world.InteractionResult.FAIL;
        if(!level.isClientSide){var state=getBlockState();if(kind()==PartBlock.Kind.ENERGY||kind()==PartBlock.Kind.COOLANT)state=state.cycle(PartBlock.OUTPUT);else if(kind()==PartBlock.Kind.COIL)state=state.cycle(PartBlock.FACING);level.setBlockAndUpdate(worldPosition,state);onRightClick(p);}
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public net.minecraft.world.InteractionResult onRightClick(Player p){if(!canOpen(p))return net.minecraft.world.InteractionResult.FAIL;if(!level.isClientSide)p.displayClientMessage(kind()==PartBlock.Kind.COIL?Content.text("facing",Content.text("direction."+getBlockState().getValue(PartBlock.FACING).getName())):Content.text(output()?"output":"input"),true);return net.minecraft.world.InteractionResult.SUCCESS;}
    private CompoundTag data(HolderLookup.Provider r){var t=new CompoundTag();if(kind()==PartBlock.Kind.FUEL)t.put("items",inventory.serializeNBT(r));t.putBoolean("output",output());return t;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(master!=null)t.putLong("master",master.asLong());t.put("stock",data(r));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);master=t.contains("master")?BlockPos.of(t.getLong("master")):null;if(kind()==PartBlock.Kind.FUEL)inventory.deserializeNBT(r,t.getCompound("stock").getCompound("items"));}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder b){super.collectImplicitComponents(b);if(kind()==PartBlock.Kind.FUEL||kind()==PartBlock.Kind.COOLANT||kind()==PartBlock.Kind.ENERGY)b.set(Content.STOCK.get(),data(level.registryAccess()));}
    @Override protected void applyImplicitComponents(DataComponentInput in){super.applyImplicitComponents(in);var t=in.get(Content.STOCK.get());if(t!=null&&kind()==PartBlock.Kind.FUEL)inventory.deserializeNBT(level.registryAccess(),t.getCompound("items"));}
}
