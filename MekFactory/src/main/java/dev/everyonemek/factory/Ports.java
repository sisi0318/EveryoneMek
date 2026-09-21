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
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.CONFIGURABLE,Content.PART.get(),(p,s)->p.hasStorage()?p:null);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,Content.PART.get(),ItemPort::new);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,Content.PART.get(),(p,s)->p.isConverter()?null:new FluidPort(p,s));
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),Content.PART.get(),ChemPort::new);
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),Content.PART.get(),(p,s)->p.isPort()?new PowerPort(p,s):null);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,Content.PART.get(),(p,s)->p.isPort()?new ForgeEnergyIntegration(new PowerPort(p,s)):null);
    }
    public static Controller controller(Part p,Direction side){var c=p.controller();if(c==null||side==null||!p.hasStorage()||!c.structure.valid()||!c.structure.isOutward(p.getBlockPos(),side))return null;return c;}
    private static boolean output(Part p){return p.isOutput();}
    private static boolean chemicalOutput(Part p){return p.isConverter()||output(p);}
    public record ItemPort(Part port,Direction side) implements IItemHandler {
        private Buffers bank(){var c=controller(port,side);return c==null?null:port.storage();}
        public int getSlots(){return bank()==null?0:bank().itemSlots();}
        public ItemStack getStackInSlot(int i){var b=bank();return b==null||i<0||i>=b.itemSlots()?ItemStack.EMPTY:b.items[i].copy();}
        public ItemStack insertItem(int i,ItemStack s,boolean sim){var b=bank();return b==null||output(port)?s:b.insert(i,s,sim);}
        public ItemStack extractItem(int i,int n,boolean sim){var b=bank();return b==null||i<0||i>=b.itemSlots()||!output(port)&&controller(port,side).enabled?ItemStack.EMPTY:b.take(i,Math.min(n,b.item(i).getMaxStackSize()),sim);}
        public int getSlotLimit(int i){var b=bank();return b==null||i<0||i>=b.itemSlots()?0:b.itemLimit(i);}
        public boolean isItemValid(int i,ItemStack s){var b=bank();return b!=null&&!output(port)&&i>=0&&i<b.slots()&&b.acceptsItem(s);}
    }
    public record ChemPort(Part port,Direction side) implements IChemicalHandler {
        private Buffers bank(){var c=controller(port,side);return c==null?null:port.storage();}
        public int getChemicalTanks(){return bank()==null?0:Buffers.TANKS;}
        public ChemicalStack getChemicalInTank(int i){var b=bank();return b==null||i<0||i>=Buffers.TANKS?ChemicalStack.EMPTY:b.chemicals[i].copy();}
        public long getChemicalTankCapacity(int i){var b=bank();return b==null?0:b.capacity();}
        public void setChemicalInTank(int i,ChemicalStack stack){throw new UnsupportedOperationException("Use transactional insertion/extraction");}
        public boolean isValid(int i,ChemicalStack s){var b=bank();return b!=null&&!chemicalOutput(port)&&!s.isRadioactive()&&i>=0&&i<Buffers.TANKS;}
        public ChemicalStack insertChemical(int i,ChemicalStack s,Action a){var b=bank();if(b==null||chemicalOutput(port))return s;long n=b.insertChem(i,s,a.simulate());return s.copyWithAmount(s.getAmount()-n);}
        public ChemicalStack extractChemical(int i,long amount,Action a){var b=bank();if(b==null||(!chemicalOutput(port)&&controller(port,side).enabled)||i<0||i>=Buffers.TANKS||amount<=0)return ChemicalStack.EMPTY;long n=Math.min(amount,b.chemicals[i].getAmount());var result=b.chemicals[i].copyWithAmount(n);if(a.execute()&&n>0){b.chemicals[i]=b.chemicals[i].copyWithAmount(b.chemicals[i].getAmount()-n);b.changed();}return result;}
    }
    public record FluidPort(Part port,Direction side) implements IFluidHandler {
        private Buffers bank(){var c=controller(port,side);return c==null||port.isConverter()?null:port.storage();}
        public int getTanks(){return bank()==null?0:Buffers.TANKS;}
        public FluidStack getFluidInTank(int i){var b=bank();return b==null||i<0||i>=Buffers.TANKS?FluidStack.EMPTY:b.fluids[i].copy();}
        public int getTankCapacity(int i){var b=bank();return b==null?0:(int)b.capacity();}
        public boolean isFluidValid(int i,FluidStack s){var b=bank();return b!=null&&!output(port)&&i>=0&&i<Buffers.TANKS;}
        public int fill(FluidStack s,FluidAction a){var b=bank();if(b==null||output(port)||s.isEmpty())return 0;int left=s.getAmount();for(int pass=0;pass<2;pass++)for(int i=0;i<Buffers.TANKS&&left>0;i++){if(pass==0?b.fluids[i].isEmpty():!b.fluids[i].isEmpty())continue;left-=b.insertFluid(i,s.copyWithAmount(left),a.simulate());}return s.getAmount()-left;}
        public FluidStack drain(FluidStack s,FluidAction a){var b=bank();if(b==null||(!output(port)&&controller(port,side).enabled)||s.isEmpty())return FluidStack.EMPTY;int left=s.getAmount();for(int i=0;i<Buffers.TANKS&&left>0;i++)if(FluidStack.isSameFluidSameComponents(s,b.fluids[i])){int n=Math.min(left,b.fluids[i].getAmount());if(a.execute()){b.fluids[i]=b.fluids[i].copyWithAmount(b.fluids[i].getAmount()-n);b.changed();}left-=n;}return s.copyWithAmount(s.getAmount()-left);}
        public FluidStack drain(int n,FluidAction a){var b=bank();if(b==null||(!output(port)&&controller(port,side).enabled)||n<=0)return FluidStack.EMPTY;for(var s:b.fluids)if(!s.isEmpty())return drain(s.copyWithAmount(n),a);return FluidStack.EMPTY;}
    }
    public record PowerPort(Part port,Direction side) implements IStrictEnergyHandler {
        private Controller c(){return port.isPort()?controller(port,side):null;}
        public int getEnergyContainerCount(){return c()==null?0:1;}
        public long getEnergy(int i){var c=c();return c==null||i!=0?0:c.energy().getEnergy();}
        public long getMaxEnergy(int i){var c=c();return c==null||i!=0?0:c.energy().getMaxEnergy();}
        public long getNeededEnergy(int i){return Math.max(0,getMaxEnergy(i)-getEnergy(i));}
        public void setEnergy(int i,long n){throw new UnsupportedOperationException("Energy belongs to induction cells");}
        public long insertEnergy(int i,long n,Action a){var c=c();return c==null||i!=0||output(port)?n:c.energy().insert(n,a,AutomationType.INTERNAL);}
        public long extractEnergy(int i,long n,Action a){return 0; /* Work is the only output consumer in this version. */}
    }
    /** Private native transit bookkeeping may consume a grouped batch; external extraction remains stack-sized. */
    private static IItemHandler bulkEjection(Part port,Direction side){
        var exposed=new ItemPort(port,side);
        return new IItemHandler(){
            public int getSlots(){return exposed.getSlots();}
            public ItemStack getStackInSlot(int i){return exposed.getStackInSlot(i);}
            public ItemStack insertItem(int i,ItemStack stack,boolean simulate){return stack;}
            public ItemStack extractItem(int i,int amount,boolean simulate){var bank=exposed.bank();return bank==null||!output(port)?ItemStack.EMPTY:bank.take(i,amount,simulate);}
            public int getSlotLimit(int i){return exposed.getSlotLimit(i);}
            public boolean isItemValid(int i,ItemStack stack){return false;}
        };
    }
    public static void eject(Controller c){
        if(!c.autoEject||!c.structure.valid())return;
        for(var p:java.util.List.copyOf(c.structure.ports)){
            if(!output(p)||!p.storage().hasContents())continue;
            for(var side:Direction.values()){
                if(controller(p,side)!=c)continue;
                var next=p.getBlockPos().relative(side);var level=c.getLevel();if(!level.hasChunkAt(next))continue;
                var bank=p.storage();
                if(java.util.Arrays.stream(bank.items).anyMatch(s->!s.isEmpty())){
                    var target=level.getCapability(Capabilities.ItemHandler.BLOCK,next,side.getOpposite());
                    if(target!=null){
                        // Native responses debit their slot map after every success. Reuse the request,
                        // including Mek transporter routing, instead of rescanning all slots for each type.
                        var request=TransitRequest.anyItem(bulkEjection(p,side),Integer.MAX_VALUE);
                        for(int pass=0;pass<bank.itemSlots()&&!request.isEmpty();pass++){
                            var response=request.eject(p,target,0,t->null);
                            if(response.isEmpty())break;
                            response.useAll();
                        }
                    }
                }
                if(java.util.Arrays.stream(bank.fluids).anyMatch(s->!s.isEmpty())){
                    var target=level.getCapability(Capabilities.FluidHandler.BLOCK,next,side.getOpposite());
                    if(target!=null){var source=new FluidPort(p,side);for(int i=0;i<Buffers.TANKS;i++){
                        var offer=source.getFluidInTank(i);if(offer.isEmpty())continue;
                        int accepted=target.fill(offer.copy(),IFluidHandler.FluidAction.EXECUTE);
                        if(accepted>0)source.drain(offer.copyWithAmount(Math.min(accepted,offer.getAmount())),IFluidHandler.FluidAction.EXECUTE);
                    }}
                }
                if(java.util.Arrays.stream(bank.chemicals).anyMatch(s->!s.isEmpty())){
                    var target=level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),next,side.getOpposite());
                    if(target!=null){var source=new ChemPort(p,side);for(int i=0;i<Buffers.TANKS;i++){
                        var offer=source.getChemicalInTank(i);if(offer.isEmpty())continue;
                        long accepted=offer.getAmount()-target.insertChemical(offer.copy(),Action.EXECUTE).getAmount();
                        if(accepted>0)source.extractChemical(i,accepted,Action.EXECUTE);
                    }}
                }
            }
        }
    }
    /** Export only after processing has finished using its input snapshot for this world tick. */
    public static void ejectConverters(Controller c){
        if(!c.autoEject||!c.structure.valid())return;
        for(var p:java.util.List.copyOf(c.structure.converters))for(var side:Direction.values()){
            if(controller(p,side)!=c)continue;
            var next=p.getBlockPos().relative(side);if(!c.getLevel().hasChunkAt(next))continue;
            if(java.util.Arrays.stream(p.storage().chemicals).allMatch(s->s.isEmpty()))break;
            var target=c.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),next,side.getOpposite());
            if(target==null)continue;var source=new ChemPort(p,side);
            for(int i=0;i<Buffers.TANKS;i++){
                var offer=source.getChemicalInTank(i);if(offer.isEmpty())continue;
                long accepted=offer.getAmount()-target.insertChemical(offer.copy(),Action.EXECUTE).getAmount();
                if(accepted>0)source.extractChemical(i,accepted,Action.EXECUTE);
            }
        }
    }
    private Ports(){}
}
