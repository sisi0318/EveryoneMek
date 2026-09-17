package dev.everyonemek.factory;
import mekanism.api.*;
import mekanism.api.chemical.*;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.integration.energy.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.lib.inventory.TransitRequest;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
public final class Ports {
    public static void register(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.CONFIGURABLE,Content.PART.get(),(p,s)->p.getBlockState().getBlock() instanceof PartBlock b&&b.kind==PartBlock.Kind.PORT?p:null);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,Content.PART.get(),ItemPort::new);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,Content.PART.get(),FluidPort::new);
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),Content.PART.get(),ChemPort::new);
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),Content.PART.get(),PowerPort::new);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,Content.PART.get(),(p,s)->new ForgeEnergyIntegration(new PowerPort(p,s)));
    }
    public static Controller controller(Part p,Direction side){var c=p.controller();if(c==null||side==null||!(p.getBlockState().getBlock() instanceof PartBlock b)||b.kind!=PartBlock.Kind.PORT||!c.structure.valid()||!c.structure.isOutward(p.getBlockPos(),side))return null;return c;}
    private static boolean output(Part p){return p.getBlockState().getValue(PartBlock.OUTPUT);}
    public record ItemPort(Part port,Direction side) implements IItemHandler {
        private Buffers bank(){var c=controller(port,side);return c==null?null:output(port)?c.outputs:c.inputs;}
        public int getSlots(){return bank()==null?0:Buffers.SLOTS;}
        public ItemStack getStackInSlot(int i){var b=bank();return b==null||i<0||i>=Buffers.SLOTS?ItemStack.EMPTY:b.items[i].copy();}
        public ItemStack insertItem(int i,ItemStack s,boolean sim){var b=bank();return b==null||b.output?s:b.insert(i,s,sim);}
        public ItemStack extractItem(int i,int n,boolean sim){var b=bank();return b==null||!b.output&&b.owner.enabled?ItemStack.EMPTY:b.take(i,n,sim);}
        public int getSlotLimit(int i){var b=bank();return b==null||i<0||i>=Buffers.SLOTS?0:i<b.slots()?64:b.items[i].getCount();}
        public boolean isItemValid(int i,ItemStack s){var b=bank();return b!=null&&!b.output&&i>=0&&i<b.slots();}
    }
    public record ChemPort(Part port,Direction side) implements IChemicalHandler {
        private Buffers bank(){var c=controller(port,side);return c==null?null:output(port)?c.outputs:c.inputs;}
        public int getChemicalTanks(){return bank()==null?0:Buffers.TANKS;}
        public ChemicalStack getChemicalInTank(int i){var b=bank();return b==null||i<0||i>=Buffers.TANKS?ChemicalStack.EMPTY:b.chemicals[i].copy();}
        public long getChemicalTankCapacity(int i){var b=bank();return b==null?0:b.capacity();}
        public void setChemicalInTank(int i,ChemicalStack stack){throw new UnsupportedOperationException("Use transactional insertion/extraction");}
        public boolean isValid(int i,ChemicalStack s){var b=bank();return b!=null&&!b.output&&!s.isRadioactive()&&i>=0&&i<Buffers.TANKS;}
        public ChemicalStack insertChemical(int i,ChemicalStack s,Action a){var b=bank();if(b==null||b.output)return s;long n=b.insertChem(i,s,a.simulate());return s.copyWithAmount(s.getAmount()-n);}
        public ChemicalStack extractChemical(int i,long amount,Action a){var b=bank();if(b==null||(!b.output&&b.owner.enabled)||i<0||i>=Buffers.TANKS||amount<=0)return ChemicalStack.EMPTY;long n=Math.min(amount,b.chemicals[i].getAmount());var result=b.chemicals[i].copyWithAmount(n);if(a.execute()&&n>0){b.chemicals[i]=b.chemicals[i].copyWithAmount(b.chemicals[i].getAmount()-n);b.changed();}return result;}
    }
    public record FluidPort(Part port,Direction side) implements IFluidHandler {
        private Buffers bank(){var c=controller(port,side);return c==null?null:output(port)?c.outputs:c.inputs;}
        public int getTanks(){return bank()==null?0:Buffers.TANKS;}
        public FluidStack getFluidInTank(int i){var b=bank();return b==null||i<0||i>=Buffers.TANKS?FluidStack.EMPTY:b.fluids[i].copy();}
        public int getTankCapacity(int i){var b=bank();return b==null?0:(int)b.capacity();}
        public boolean isFluidValid(int i,FluidStack s){var b=bank();return b!=null&&!b.output&&i>=0&&i<Buffers.TANKS;}
        public int fill(FluidStack s,FluidAction a){var b=bank();if(b==null||b.output||s.isEmpty())return 0;int left=s.getAmount();for(int pass=0;pass<2;pass++)for(int i=0;i<Buffers.TANKS&&left>0;i++){if(pass==0?b.fluids[i].isEmpty():!b.fluids[i].isEmpty())continue;left-=b.insertFluid(i,s.copyWithAmount(left),a.simulate());}return s.getAmount()-left;}
        public FluidStack drain(FluidStack s,FluidAction a){var b=bank();if(b==null||(!b.output&&b.owner.enabled)||s.isEmpty())return FluidStack.EMPTY;int left=s.getAmount();for(int i=0;i<Buffers.TANKS&&left>0;i++)if(FluidStack.isSameFluidSameComponents(s,b.fluids[i])){int n=Math.min(left,b.fluids[i].getAmount());if(a.execute()){b.fluids[i]=b.fluids[i].copyWithAmount(b.fluids[i].getAmount()-n);b.changed();}left-=n;}return s.copyWithAmount(s.getAmount()-left);}
        public FluidStack drain(int n,FluidAction a){var b=bank();if(b==null||(!b.output&&b.owner.enabled)||n<=0)return FluidStack.EMPTY;for(var s:b.fluids)if(!s.isEmpty())return drain(s.copyWithAmount(n),a);return FluidStack.EMPTY;}
    }
    public record PowerPort(Part port,Direction side) implements IStrictEnergyHandler {
        private Controller c(){return controller(port,side);}
        public int getEnergyContainerCount(){return c()==null?0:1;}
        public long getEnergy(int i){var c=c();return c==null||i!=0?0:c.energy().getEnergy();}
        public long getMaxEnergy(int i){var c=c();return c==null||i!=0?0:c.energy().getMaxEnergy();}
        public long getNeededEnergy(int i){return Math.max(0,getMaxEnergy(i)-getEnergy(i));}
        public void setEnergy(int i,long n){throw new UnsupportedOperationException("Energy belongs to induction cells");}
        public long insertEnergy(int i,long n,Action a){var c=c();return c==null||i!=0||output(port)?n:c.energy().insert(n,a,AutomationType.INTERNAL);}
        public long extractEnergy(int i,long n,Action a){return 0; /* Work is the only output consumer in this version. */}
    }
    public static void eject(Controller c){if(!c.autoEject||!c.structure.valid()||c.getLevel().getGameTime()%5!=0)return;for(var p:java.util.List.copyOf(c.structure.ports)){if(!output(p))continue;for(var side:Direction.values()){if(!c.structure.isOutward(p.getBlockPos(),side))continue;var next=p.getBlockPos().relative(side);var level=c.getLevel();if(!level.hasChunkAt(next))continue;
        var items=level.getCapability(Capabilities.ItemHandler.BLOCK,next,side.getOpposite());if(items!=null){var request=TransitRequest.anyItem(new ItemPort(p,side),64);if(!request.isEmpty()){var response=request.eject(p,items,0,t->null);if(!response.isEmpty())response.useAll();}}
        var fluid=level.getCapability(Capabilities.FluidHandler.BLOCK,next,side.getOpposite());if(fluid!=null){var source=new FluidPort(p,side);for(int i=0;i<Buffers.TANKS;i++){var offer=source.getFluidInTank(i);if(offer.isEmpty())continue;offer.setAmount(Math.min(offer.getAmount(),16000));int n=fluid.fill(offer,IFluidHandler.FluidAction.EXECUTE);if(n>0)source.drain(offer.copyWithAmount(n),IFluidHandler.FluidAction.EXECUTE);}}
        var chemical=level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),next,side.getOpposite());if(chemical!=null){var source=new ChemPort(p,side);for(int i=0;i<Buffers.TANKS;i++){var offer=source.getChemicalInTank(i);if(offer.isEmpty())continue;offer.setAmount(Math.min(offer.getAmount(),64000));long n=offer.getAmount()-chemical.insertChemical(offer,Action.EXECUTE).getAmount();if(n>0)source.extractChemical(i,n,Action.EXECUTE);}}
    }}}
    private Ports(){}
}
