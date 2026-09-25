package dev.everyonemek.gravity.solar;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
public final class SolarPart extends BlockEntity implements mekanism.api.IConfigurable {
    public BlockPos master;private long clock=Long.MIN_VALUE,visualSent=Long.MIN_VALUE;
    public long inUsed,outUsed;private long previousInput,previousOutput;private int visual,sentVisual;private boolean hot,sentHot;
    public final ItemStackHandler inventory=new ItemStackHandler(18){
        @Override public boolean isItemValid(int slot,ItemStack stack){return kind()==SolarBlock.Kind.FUEL&&SolarFuelRecipe.fuel(level,stack)!=null;}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    public SolarPart(BlockPos pos,BlockState s){super(SolarContent.PART.get(),pos,s);}
    public SolarBlock.Kind kind(){return ((SolarBlock)getBlockState().getBlock()).kind;}
    public int tier(){return ((SolarBlock)getBlockState().getBlock()).tier;}
    public boolean output(){return getBlockState().getValue(SolarBlock.OUTPUT);}
    public void clock(){if(clock!=level.getGameTime()){clock=level.getGameTime();previousInput=inUsed;previousOutput=outUsed;inUsed=outUsed=0;}}
    public long inputRate(){long age=level==null?2:level.getGameTime()-clock;return age==0?(inUsed>0?inUsed:previousInput):age==1?inUsed:0;}
    public long outputRate(){long age=level==null?2:level.getGameTime()-clock;return age==0?(outUsed>0?outUsed:previousOutput):age==1?outUsed:0;}
    public SolarController controller(){if(level==null||master==null||isRemoved()||!level.hasChunkAt(worldPosition)||!level.hasChunkAt(master)||level.getBlockEntity(worldPosition)!=this)return null;return level.getBlockEntity(master) instanceof SolarController c&&c.structure.contains(worldPosition)?c:null;}
    public boolean canOpen(Player p){if(level==null||p.level()!=level||isRemoved()||p.distanceToSqr(worldPosition.getCenter())>64)return false;var c=controller();if(c!=null)return c.access(p);if(master==null)return true;if(!level.hasChunkAt(master))return false;return !(level.getBlockEntity(master) instanceof SolarController old)||old.access(p);}
    public void open(Player p){if(!canOpen(p))return;if(kind()==SolarBlock.Kind.FUEL)SolarFuelMenu.open(p,this);else{var c=controller();if(c!=null)SolarMenu.open(p,c,worldPosition);else if(!level.isClientSide)p.displayClientMessage(SolarContent.text("unlinked"),true);}}
    @Override public InteractionResult onSneakRightClick(Player p){if(!canOpen(p)||!p.mayInteract(level,worldPosition))return InteractionResult.FAIL;if(!level.isClientSide){var s=getBlockState();if(kind()==SolarBlock.Kind.ENERGY)s=s.cycle(SolarBlock.OUTPUT);else if(kind()==SolarBlock.Kind.FOCUS)s=s.cycle(SolarBlock.FACING);else if(kind()==SolarBlock.Kind.COLLECTOR){var face=s.getValue(SolarBlock.FACING);s=SolarStructure.placement(level,worldPosition,s.setValue(SolarBlock.FACING,face.getAxis().isHorizontal()?face.getClockWise():Direction.NORTH));}level.setBlockAndUpdate(worldPosition,s);onRightClick(p);}return InteractionResult.SUCCESS;}
    @Override public InteractionResult onRightClick(Player p){if(!canOpen(p))return InteractionResult.FAIL;if(!level.isClientSide)p.displayClientMessage(kind()==SolarBlock.Kind.ENERGY?dev.everyonemek.gravity.Content.text(output()?"output":"input"):dev.everyonemek.gravity.Content.text("facing",dev.everyonemek.gravity.Content.text("direction."+getBlockState().getValue(SolarBlock.FACING).getName())),true);return InteractionResult.SUCCESS;}
    public int visual(){return visual;}
    public boolean hot(){return hot;}
    public void visual(int value,boolean hot){if(kind()!=SolarBlock.Kind.SEED||level==null||level.isClientSide)return;visual=Math.clamp(value,0,100);this.hot=hot;long t=level.getGameTime();if(hot!=sentHot||visual!=sentVisual&&((visual==0)!=(sentVisual==0)||visualSent==Long.MIN_VALUE||t-visualSent>=10)){sentVisual=visual;sentHot=hot;visualSent=t;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();if(kind()==SolarBlock.Kind.SEED){t.putByte("visual",(byte)visual);t.putBoolean("hot",hot);}return t;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return kind()==SolarBlock.Kind.SEED?net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this):null;}
    @Override public void handleUpdateTag(CompoundTag t,HolderLookup.Provider r){if(kind()==SolarBlock.Kind.SEED){visual=Math.clamp(t.getByte("visual"),0,100);hot=t.getBoolean("hot");}}
    @Override public void onDataPacket(net.minecraft.network.Connection n,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket p,HolderLookup.Provider r){handleUpdateTag(p.getTag(),r);}
    private CompoundTag stock(HolderLookup.Provider r){var t=new CompoundTag();if(kind()==SolarBlock.Kind.FUEL)t.put("items",inventory.serializeNBT(r));t.putBoolean("output",output());return t;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(master!=null)t.putLong("master",master.asLong());t.put("stock",stock(r));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);master=t.contains("master")?BlockPos.of(t.getLong("master")):null;if(kind()==SolarBlock.Kind.FUEL)inventory.deserializeNBT(r,t.getCompound("stock").getCompound("items"));}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder b){super.collectImplicitComponents(b);if(kind()==SolarBlock.Kind.FUEL||kind()==SolarBlock.Kind.ENERGY)b.set(SolarContent.STOCK.get(),stock(level.registryAccess()));}
    @Override protected void applyImplicitComponents(DataComponentInput in){super.applyImplicitComponents(in);var t=in.get(SolarContent.STOCK.get());if(t!=null&&kind()==SolarBlock.Kind.FUEL)inventory.deserializeNBT(level.registryAccess(),t.getCompound("items"));}
}
