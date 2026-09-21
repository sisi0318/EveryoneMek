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
    public long inputUsed,outputUsed;
    public void clock(){if(level.getGameTime()!=ioTick){ioTick=level.getGameTime();inputUsed=outputUsed=0;}}
    public final ItemStackHandler inventory=new ItemStackHandler(18){
        @Override public boolean isItemValid(int i,ItemStack s){return kind()==PartBlock.Kind.FUEL&&FuelRecipe.find(level,s)!=null;}
        @Override protected void onContentsChanged(int i){Part.this.setChanged();}
    };
    public Part(BlockPos pos,BlockState state){super(Content.PART.get(),pos,state);}
    public PartBlock.Kind kind(){return ((PartBlock)getBlockState().getBlock()).kind;}
    public boolean output(){return getBlockState().getValue(PartBlock.OUTPUT);}
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
