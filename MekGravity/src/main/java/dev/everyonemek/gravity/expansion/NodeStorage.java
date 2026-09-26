package dev.everyonemek.gravity.expansion;
import java.util.*;
import mekanism.api.*;
import mekanism.api.chemical.*;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.energy.*;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.integration.energy.forgeenergy.ForgeEnergyIntegration;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Real independent input/output stores. Native Mek container components own serialization. */
public final class NodeStorage {
    public static final long ENERGY_CAPACITY=2_560_000_000_000_000L,CHEMICAL_CAPACITY=64_000_000L;
    public static final int FLUID_CAPACITY=16_000_000,TANKS=4;
    public final IEnergyContainer energyIn,energyOut;
    public final List<IExtendedFluidTank> fluidIn=new ArrayList<>(),fluidOut=new ArrayList<>();
    public final List<IChemicalTank> chemicalIn=new ArrayList<>(),chemicalOut=new ArrayList<>();
    public long itemsMoved,energyMoved,fluidMoved,chemicalMoved;private long tick=Long.MIN_VALUE;
    public NodeStorage(IContentsListener listener){
        energyIn=BasicEnergyContainer.create(ENERGY_CAPACITY,a->a!=AutomationType.EXTERNAL,a->true,listener);
        energyOut=BasicEnergyContainer.create(ENERGY_CAPACITY,a->true,a->a==AutomationType.INTERNAL,listener);
        for(int i=0;i<TANKS;i++){
            fluidIn.add(BasicFluidTank.create(FLUID_CAPACITY,(s,a)->a!=AutomationType.EXTERNAL,(s,a)->true,s->true,listener));
            fluidOut.add(BasicFluidTank.create(FLUID_CAPACITY,(s,a)->true,(s,a)->a==AutomationType.INTERNAL,s->true,listener));
            chemicalIn.add(BasicChemicalTank.createModern(CHEMICAL_CAPACITY,(s,a)->a!=AutomationType.EXTERNAL,(s,a)->true,s->true,ChemicalAttributeValidator.ALWAYS_ALLOW,listener));
            chemicalOut.add(BasicChemicalTank.createModern(CHEMICAL_CAPACITY,(s,a)->true,(s,a)->a==AutomationType.INTERNAL,s->true,ChemicalAttributeValidator.ALWAYS_ALLOW,listener));
        }
    }
    public void clock(long now){if(tick!=now){tick=now;itemsMoved=energyMoved=fluidMoved=chemicalMoved=0;}}
    private static long add(long a,long b){return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
    private static int fluidRoom(NodeStorage target,FluidStack stack){int n=0;for(var tank:target.fluidOut)if(tank.isEmpty()||FluidStack.isSameFluidSameComponents(tank.getFluid(),stack))n+=tank.getNeeded();return n;}
    private static long chemicalRoom(NodeStorage target,ChemicalStack stack){long n=0;for(var tank:target.chemicalOut)if(tank.isEmpty()||tank.isTypeEqual(stack))n+=tank.getNeeded();return n;}
    private static void insertFluid(NodeStorage target,FluidStack stack){var left=stack;for(int pass=0;pass<2;pass++)for(var tank:target.fluidOut){if(tank.isEmpty()!=(pass==1))continue;left=tank.insert(left,Action.EXECUTE,AutomationType.INTERNAL);if(left.isEmpty())return;}throw new IllegalStateException("Reserved fluid capacity changed");}
    private static void insertChemical(NodeStorage target,ChemicalStack stack){var left=stack;for(int pass=0;pass<2;pass++)for(var tank:target.chemicalOut){if(tank.isEmpty()!=(pass==1))continue;left=tank.insert(left,Action.EXECUTE,AutomationType.INTERNAL);if(left.isEmpty())return;}throw new IllegalStateException("Reserved chemical capacity changed");}
    /** One server transaction per source tank; no travel queue, amount loop, or separate resource copy. */
    public static boolean transfer(OrbitalModule sender,OrbitalModule receiver,FieldSource source,int channel){var from=sender.node;var to=receiver.node;boolean moved=false;
        if(channel==1&&sender.channel(1)&&receiver.receiveChannel(1)&&!from.energyIn.isEmpty()){
            long amount=Math.min(from.energyIn.getEnergy(),to.energyOut.getNeeded());
            if(amount==0)sender.status="output_full";else if(!source.spend(ModuleConfig.ENERGY_PACKET_COST.get()))sender.status="energy";
            else{sender.snapshot(1,null);receiver.snapshot(1,null);from.energyIn.extract(amount,Action.EXECUTE,AutomationType.INTERNAL);if(to.energyOut.insert(amount,Action.EXECUTE,AutomationType.INTERNAL)!=0)throw new IllegalStateException("Reserved energy capacity changed");from.energyMoved=add(from.energyMoved,amount);sender.paidEnergy+=ModuleConfig.ENERGY_PACKET_COST.get();moved=true;}
        }
        if(channel==2&&sender.channel(2)&&receiver.receiveChannel(2))for(var tank:from.fluidIn){if(tank.isEmpty())continue;int room=fluidRoom(to,tank.getFluid());long rate=ModuleConfig.FLUID_COST.get();int amount=(int)Math.min(Math.min(tank.getFluidAmount(),room),source.available()/rate);
            if(amount==0){sender.status=room==0?"output_full":"energy";continue;}long cost=amount*rate;if(!source.spend(cost)){sender.status="energy";break;}
            sender.snapshot(2,tank.getFluid());receiver.snapshot(2,tank.getFluid());insertFluid(to,tank.extract(amount,Action.EXECUTE,AutomationType.INTERNAL));from.fluidMoved+=amount;sender.paidEnergy+=cost;moved=true;
        }
        if(channel==3&&sender.channel(3)&&receiver.receiveChannel(3))for(var tank:from.chemicalIn){if(tank.isEmpty())continue;long room=chemicalRoom(to,tank.getStack()),rate=ModuleConfig.CHEMICAL_COST.get(),amount=Math.min(Math.min(tank.getStored(),room),source.available()/rate);
            if(amount==0){sender.status=room==0?"output_full":"energy";continue;}long cost=Math.multiplyExact(amount,rate);if(!source.spend(cost)){sender.status="energy";break;}
            sender.snapshot(3,tank.getStack());receiver.snapshot(3,tank.getStack());insertChemical(to,tank.extract(amount,Action.EXECUTE,AutomationType.INTERNAL));from.chemicalMoved+=amount;sender.paidEnergy+=cost;moved=true;
        }return moved;
    }
    private IStrictEnergyHandler outputHandler(){return new IStrictEnergyHandler(){
        public int getEnergyContainerCount(){return 1;}public long getEnergy(int i){return i==0?energyOut.getEnergy():0;}public long getMaxEnergy(int i){return i==0?energyOut.getMaxEnergy():0;}public long getNeededEnergy(int i){return i==0?energyOut.getNeeded():0;}public void setEnergy(int i,long n){throw new UnsupportedOperationException("Use insertion/extraction");}
        public long insertEnergy(int i,long n,Action action){return n;}public long extractEnergy(int i,long n,Action action){return i==0?energyOut.extract(n,action,AutomationType.INTERNAL):0;}
    };}
    public void eject(NodeModule owner){if(!owner.autoEject)return;var level=owner.getLevel();
        var energySides=owner.outputSides(1);
        var fluidSides=owner.outputSides(2);
        var chemicalSides=owner.outputSides(3);
        var sides=new LinkedHashSet<net.minecraft.core.Direction>();sides.addAll(energySides);sides.addAll(fluidSides);sides.addAll(chemicalSides);
        for(var direction:sides){var pos=owner.getBlockPos().relative(direction);if(!level.hasChunkAt(pos))continue;var side=direction.getOpposite();
        if(energySides.contains(direction)&&!energyOut.isEmpty()){
            var target=level.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),pos,side);
            if(target!=null){for(int pass=0;pass<64&&!energyOut.isEmpty();pass++){long offer=energyOut.getEnergy();long accepted=offer-target.insertEnergy(offer,Action.EXECUTE);if(accepted<=0)break;energyOut.extract(accepted,Action.EXECUTE,AutomationType.INTERNAL);}}
            else{var fe=level.getCapability(Capabilities.EnergyStorage.BLOCK,pos,side);if(fe!=null){var bridge=new ForgeEnergyIntegration(outputHandler());for(int pass=0;pass<64;pass++){int offer=bridge.extractEnergy(Integer.MAX_VALUE,true);if(offer==0)break;int aligned=bridge.extractEnergy(fe.receiveEnergy(offer,true),true);if(aligned==0)break;int accepted=fe.receiveEnergy(aligned,false);if(accepted<=0)break;if(bridge.extractEnergy(accepted,false)!=accepted)throw new IllegalStateException("FE output changed during transfer");}}}
        }
        var fluid=fluidSides.contains(direction)?level.getCapability(Capabilities.FluidHandler.BLOCK,pos,side):null;if(fluid!=null)for(var tank:fluidOut){if(tank.isEmpty())continue;int accepted=fluid.fill(tank.getFluid().copy(),FluidAction.EXECUTE);if(accepted>0)tank.extract(accepted,Action.EXECUTE,AutomationType.INTERNAL);}
        var chemical=chemicalSides.contains(direction)?level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),pos,side):null;if(chemical!=null)for(var tank:chemicalOut){if(tank.isEmpty())continue;long accepted=tank.getStored()-chemical.insertChemical(tank.getStack().copy(),Action.EXECUTE).getAmount();if(accepted>0)tank.extract(accepted,Action.EXECUTE,AutomationType.INTERNAL);}
        }
    }
}
