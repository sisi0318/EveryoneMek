package dev.everyonemek.gravity.solar;
import mekanism.api.*;
import mekanism.api.chemical.*;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.integration.energy.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.IItemHandler;
public final class SolarPorts {
    public static void register(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.CONFIGURABLE,SolarContent.PART.get(),(p,s)->p);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,SolarContent.PART.get(),(p,s)->p.kind()==SolarBlock.Kind.FUEL?new Items(p,s):null);
        e.registerBlockEntity(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),SolarContent.PART.get(),(p,s)->p.kind()==SolarBlock.Kind.ENERGY?new Energy(p,s):null);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,SolarContent.PART.get(),(p,s)->p.kind()==SolarBlock.Kind.ENERGY?new ForgeEnergyIntegration(new Energy(p,s)):null);
    }
    public static SolarController controller(SolarPart p,Direction side){var c=p.controller();return c!=null&&c.structure.valid()&&c.structure.outward(p.getBlockPos(),side)?c:null;}
    public record Items(SolarPart part,Direction side) implements IItemHandler {
        private boolean live(){return part.kind()==SolarBlock.Kind.FUEL&&controller(part,side)!=null;}
        public int getSlots(){return live()?18:0;}
        public ItemStack getStackInSlot(int i){return live()&&i>=0&&i<18?part.inventory.getStackInSlot(i).copy():ItemStack.EMPTY;}
        public int getSlotLimit(int i){return live()&&i>=0&&i<18?64:0;}
        public boolean isItemValid(int i,ItemStack s){return live()&&i>=0&&i<18&&part.inventory.isItemValid(i,s);}
        public ItemStack insertItem(int i,ItemStack s,boolean sim){return isItemValid(i,s)?part.inventory.insertItem(i,s,sim):s;}
        public ItemStack extractItem(int i,int n,boolean sim){var c=controller(part,side);return c!=null&&!c.enabled&&i>=0&&i<18?part.inventory.extractItem(i,n,sim):ItemStack.EMPTY;}
    }
    public record Energy(SolarPart part,Direction side) implements IStrictEnergyHandler {
        private SolarController c(){return part.kind()==SolarBlock.Kind.ENERGY?controller(part,side):null;}
        public int getEnergyContainerCount(){return c()==null?0:1;}
        public long getEnergy(int i){var c=c();return c==null||i!=0?0:c.stored;}
        public long getMaxEnergy(int i){var c=c();return c==null||i!=0?0:c.capacity();}
        public long getNeededEnergy(int i){return Math.max(0,getMaxEnergy(i)-getEnergy(i));}
        public void setEnergy(int i,long n){throw new UnsupportedOperationException("Shared reactor energy");}
        public long insertEnergy(int i,long n,Action a){var c=c();if(c==null||i!=0||n<=0||part.output())return n;part.clock();long accepted=c.accept(Math.min(n,Math.max(0,SolarConfig.PORT_RATE.get()-part.inUsed)),a.simulate());if(a.execute())part.inUsed+=accepted;return n-accepted;}
        public long extractEnergy(int i,long n,Action a){var c=c();if(c==null||i!=0||n<=0||!part.output())return 0;part.clock();long taken=c.extract(Math.min(n,Math.max(0,SolarConfig.PORT_RATE.get()-part.outUsed)),a.simulate());if(a.execute())part.outUsed+=taken;return taken;}
    }
    // Some long handlers bridge to FE internally, so both paths must allow more than one accepted chunk.
    static void emit(Energy source,IStrictEnergyHandler target){
        for(int pass=0;pass<64;pass++){
            long offer=source.extractEnergy(0,Long.MAX_VALUE,Action.SIMULATE);if(offer<=0)break;
            long accepted=offer-target.insertEnergy(offer,Action.EXECUTE);
            if(accepted<=0)break;
            if(source.extractEnergy(0,accepted,Action.EXECUTE)!=accepted)throw new IllegalStateException("Energy output changed during transfer");
        }
    }
    static void emit(Energy source,net.neoforged.neoforge.energy.IEnergyStorage target){
        var bridge=new ForgeEnergyIntegration(source);
        for(int pass=0;pass<64;pass++){
            int offer=bridge.extractEnergy(Integer.MAX_VALUE,true);if(offer<=0)break;
            int aligned=bridge.extractEnergy(target.receiveEnergy(offer,true),true);if(aligned<=0)break;
            int accepted=target.receiveEnergy(aligned,false);if(accepted<=0)break;
            if(bridge.extractEnergy(accepted,false)!=accepted)throw new IllegalStateException("FE output changed during transfer");
        }
    }
    public static void eject(SolarController c){if(!c.autoEject||!c.structure.valid())return;
        var ordered=new java.util.ArrayList<>(c.structure.ports.stream().filter(p->p.output()&&p.kind()==SolarBlock.Kind.ENERGY).toList());
        if(!ordered.isEmpty()){java.util.Collections.rotate(ordered,-Math.floorMod(c.outputCursor++,ordered.size()));}
        for(var p:ordered)for(var side:Direction.values()){
            if(controller(p,side)!=c)continue;var next=p.getBlockPos().relative(side);if(!c.getLevel().hasChunkAt(next))continue;
            if(p.kind()==SolarBlock.Kind.ENERGY){
                var source=new Energy(p,side);long offer=source.extractEnergy(0,Long.MAX_VALUE,Action.SIMULATE);if(offer<=0)continue;
                var target=c.getLevel().getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),next,side.getOpposite());
                if(target!=null)emit(source,target);
                else {var fe=c.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,next,side.getOpposite());if(fe!=null)emit(source,fe);}
            }
        }
    }
    private SolarPorts(){}
}