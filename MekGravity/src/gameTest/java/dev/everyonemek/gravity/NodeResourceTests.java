package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.expansion.*;
import dev.everyonemek.gravity.solar.*;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.security.SecurityMode;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class NodeResourceTests {
    private static OrbitalModule node(GameTestHelper h,BlockPos pos){h.getLevel().setBlockAndUpdate(pos,ModuleContent.BLOCK.get(ModuleKind.NODE).get().defaultBlockState());var n=(OrbitalModule)h.getLevel().getBlockEntity(pos);n.autoEject=false;return n;}
    private static void use(ServerPlayer p,BlockPos pos){p.setPos(pos.north().getCenter());p.setShiftKeyDown(false);p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false));}
    private static void pair(GameTestHelper h,SolarController core,OrbitalModule a,OrbitalModule b){var p=ReactorTests.player(h,core.getBlockPos().north());try{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModuleContent.LINKER.get()));use(p,core.getBlockPos());use(p,a.getBlockPos());check(a.source!=null&&a.source.pos().equals(core.getBlockPos()),"Source binding failed");p.setShiftKeyDown(true);p.getMainHandItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(!p.getMainHandItem().has(ModuleContent.LINK.get())&&a.source!=null,"Clear selection broke the world power link");}finally{ReactorTests.close(p);}NodeFrequencyTests.pair(h,a,b);}

    private static void hot(SolarController c){c.enabled=c.ignited=true;c.stored=c.capacity();c.fuelRemaining=c.fuelTotal=100_000_000_000_000_000L;}
    private static ChemicalStack chemical(String id,long amount){return new ChemicalStack(mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.parse(id)),amount);}
    @GameTest(template="empty",timeoutTicks=80)
    public static void nodeTransfersAllResourceBuffersInOneTick(GameTestHelper h){var c=SolarTests.formed(h);var a=node(h,c.getBlockPos().north(3));var b=node(h,c.getBlockPos().east(20).north(3));pair(h,c,a,b);var level=h.getLevel();
        var energy=level.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),a.getBlockPos(),Direction.UP);var fluid=level.getCapability(Capabilities.FluidHandler.BLOCK,a.getBlockPos(),Direction.UP);var gas=level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),a.getBlockPos(),Direction.UP);
        check(energy!=null&&fluid!=null&&gas!=null&&energy.getEnergyContainerCount()==1&&fluid.getTanks()==4&&gas.getChemicalTanks()==4,"Native resource capabilities or side tank counts missing");
        check(energy.insertEnergy(0,100_000_000_000L,Action.SIMULATE)==0&&a.node.energyIn.isEmpty(),"Energy simulation mutated storage");energy.insertEnergy(0,100_000_000_000L,Action.EXECUTE);
        var fe=level.getCapability(Capabilities.EnergyStorage.BLOCK,a.getBlockPos(),Direction.UP);check(fe.receiveEnergy(1000,false)>0&&fe.extractEnergy(1000,false)==0,"FE input bridge or direction missing");long energyPayload=a.node.energyIn.getEnergy();
        var water=new FluidStack(Fluids.WATER,2_000_000);water.set(DataComponents.CUSTOM_NAME,Component.literal("tagged payload"));check(fluid.fill(water,FluidAction.SIMULATE)==2_000_000&&a.node.fluidIn.stream().allMatch(t->t.isEmpty()),"Fluid simulation changed storage");fluid.fill(water,FluidAction.EXECUTE);fluid.fill(new FluidStack(Fluids.LAVA,500_000),FluidAction.EXECUTE);
        var hydrogen=chemical("mekanism:hydrogen",65_536);var redstone=chemical("mekanism:redstone",123_456);check(gas.insertChemical(hydrogen,Action.SIMULATE).isEmpty()&&a.node.chemicalIn.stream().allMatch(t->t.isEmpty()),"Chemical simulation changed storage");gas.insertChemical(hydrogen,Action.EXECUTE);gas.insertChemical(redstone,Action.EXECUTE);
        a.inputs.get(0).setStack(new ItemStack(Items.IRON_INGOT,4096));a.inputs.get(1).setStack(new ItemStack(Items.IRON_INGOT,1904));long[] start={0};
        h.startSequence().thenIdle(2).thenExecute(()->{check(a.transferred==0&&b.node.energyOut.isEmpty(),"Cold source allowed transfer");hot(c);start[0]=level.getGameTime();})
        .thenWaitUntil(()->check(b.node.energyOut.getEnergy()==energyPayload,"Waiting for energy payload"))
        .thenExecute(()->{
            check(level.getGameTime()-start[0]<=1,"Resource travel introduced a delay");check(a.inputs.stream().allMatch(s->s.isEmpty())&&b.outputs.stream().mapToInt(s->s.getCount()).sum()==6000,"Items were throttled below the complete payload");
            check(a.node.energyIn.isEmpty()&&a.node.fluidIn.stream().allMatch(t->t.isEmpty())&&a.node.chemicalIn.stream().allMatch(t->t.isEmpty()),"Resource buffers did not move in the same tick");
            check(b.node.fluidOut.stream().anyMatch(t->FluidStack.isSameFluidSameComponents(t.getFluid(),water)&&t.getFluidAmount()==2_000_000),"Fluid components or amount changed");check(b.node.fluidOut.stream().mapToInt(t->t.getFluidAmount()).sum()==2_500_000,"Mixed fluids merged or vanished");
            check(b.node.chemicalOut.stream().anyMatch(t->t.isTypeEqual(hydrogen)&&t.getStored()==65_536)&&b.node.chemicalOut.stream().anyMatch(t->t.isTypeEqual(redstone)&&t.getStored()==123_456),"Gas/infusion cargo mixed or changed");
            long fee=6000*ModuleConfig.ITEM_COST.get()+ModuleConfig.ENERGY_PACKET_COST.get()+2_500_000*ModuleConfig.FLUID_COST.get()+188_992*ModuleConfig.CHEMICAL_COST.get();check(a.paidEnergy==fee,"Operating power did not use exact accepted quantities");
            var output=level.getCapability(Capabilities.EnergyStorage.BLOCK,b.getBlockPos(),b.getDirection());check(output.receiveEnergy(1000,false)==0&&output.extractEnergy(1000,true)>0&&b.node.energyOut.getEnergy()==energyPayload,"Output FE simulation/direction changed energy");c.enabled=false;
        }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void nodeSavesAllResourcesInDroppedItemAndReplacement(GameTestHelper h){var pos=h.absolutePos(new BlockPos(8,3,8));var a=node(h,pos);a.node.energyIn.setEnergy(1_099_511_627_777L);a.node.energyOut.setEnergy(9999);a.node.fluidIn.get(0).setStack(new FluidStack(Fluids.WATER,12345));a.node.fluidOut.get(2).setStack(new FluidStack(Fluids.LAVA,54321));a.node.chemicalIn.get(1).setStack(chemical("mekanism:nuclear_waste",1_234_567));a.node.chemicalOut.get(3).setStack(chemical("mekanism:hydrogen",7654321));a.channels=5;a.inputs.getFirst().setStack(new ItemStack(Items.DIAMOND,4096));
        var registry=h.getLevel().registryAccess();var copy=(OrbitalModule)BlockEntity.loadStatic(pos,a.getBlockState(),a.saveWithFullMetadata(registry),registry);checkSaved(copy);
        var drop=Block.getDrops(a.getBlockState(),h.getLevel(),pos,a).getFirst();drop=ItemStack.parseOptional(registry,(net.minecraft.nbt.CompoundTag)drop.save(registry));check(!drop.isEmpty(),"Resource-bearing item failed native component serialization");
        var p=ReactorTests.player(h,pos.north());try{h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,drop);p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));checkSaved((OrbitalModule)h.getLevel().getBlockEntity(pos));}finally{ReactorTests.close(p);}h.succeed();
    }
    private static void checkSaved(OrbitalModule a){check(a!=null&&a.node.energyIn.getEnergy()==1_099_511_627_777L&&a.node.energyOut.getEnergy()==9999&&a.node.fluidIn.get(0).getFluidAmount()==12345&&a.node.fluidOut.get(2).getFluidAmount()==54321&&a.node.chemicalIn.get(1).getStored()==1_234_567&&a.node.chemicalOut.get(3).getStored()==7654321&&a.inputs.getFirst().getCount()==4096&&a.channels==5,"Saved resources: "+a.node.energyIn.getEnergy()+" / "+a.node.energyOut.getEnergy()+", fluids "+a.node.fluidIn.get(0).getFluidAmount()+" / "+a.node.fluidOut.get(2).getFluidAmount()+", chemicals "+a.node.chemicalIn.get(1).getStored()+" / "+a.node.chemicalOut.get(3).getStored()+", items "+a.inputs.getFirst().getCount()+", channels "+a.channels);}
    @GameTest(template="empty",timeoutTicks=90)
    public static void nodeEjectsAllFourResourcesIntoNativeMekMachine(GameTestHelper h){var a=node(h,h.absolutePos(new BlockPos(8,3,8)));a.autoEject=true;var target=a.getBlockPos().relative(a.getDirection());h.getLevel().setBlockAndUpdate(target,MekanismBlocks.PRESSURIZED_REACTION_CHAMBER.get().defaultBlockState());var machine=(mekanism.common.tile.machine.TileEntityPressurizedReactionChamber)h.getLevel().getBlockEntity(target);
        for(var type:new TransmissionType[]{TransmissionType.ITEM,TransmissionType.ENERGY,TransmissionType.FLUID,TransmissionType.CHEMICAL}){var config=machine.getConfig().getConfig(type);for(var side:RelativeSide.values())config.setDataType(DataType.INPUT,side);config.setEjecting(false);}h.getLevel().invalidateCapabilities(target);
        a.outputs.getFirst().setStack(new ItemStack(mekanism.common.registries.MekanismItems.BIO_FUEL.get()));a.node.energyOut.setEnergy(12345);a.node.fluidOut.get(0).setStack(new FluidStack(Fluids.WATER,1000));a.node.chemicalOut.get(0).setStack(chemical("mekanism:hydrogen",500));
        h.startSequence().thenWaitUntil(()->check(machine.inputFluidTank.getFluidAmount()==1000&&machine.inputGasTank.getStored()==500&&machine.getEnergyContainer().getEnergy()==machine.getEnergyContainer().getMaxEnergy()&&machine.getInventorySlots(null).stream().anyMatch(s->s.getStack().is(mekanism.common.registries.MekanismItems.BIO_FUEL.get())),"Actual Mek receipt: fluid="+machine.inputFluidTank.getFluidAmount()+", chemical="+machine.inputGasTank.getStored()+", energy="+machine.getEnergyContainer().getEnergy()+", items="+machine.getInventorySlots(null).stream().map(slot->slot.getStack().toString()).toList()+"; node="+a.node.energyOut.getEnergy()+"/"+a.node.fluidOut.get(0).getFluidAmount()+"/"+a.node.chemicalOut.get(0).getStored()))
        .thenExecute(()->check(a.outputs.getFirst().isEmpty()&&a.node.energyOut.getEnergy()+machine.getEnergyContainer().getEnergy()==12345&&a.node.fluidOut.stream().allMatch(t->t.isEmpty())&&a.node.chemicalOut.stream().allMatch(t->t.isEmpty()),"Native ejection duplicated stock or discarded energy beyond receiver capacity")).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nodeResourceSwitchesAndFullReceiversPreserveStock(GameTestHelper h){var c=SolarTests.formed(h);hot(c);var a=node(h,c.getBlockPos().north(3));var b=node(h,c.getBlockPos().west(9).north(3));pair(h,c,a,b);a.node.fluidIn.get(0).setStack(new FluidStack(Fluids.WATER,1000));a.node.energyIn.setEnergy(123456);a.node.chemicalIn.get(0).setStack(chemical("mekanism:hydrogen",250));b.node.energyOut.setEnergy(NodeStorage.ENERGY_CAPACITY);for(var t:b.node.fluidOut)t.setStack(new FluidStack(Fluids.LAVA,NodeStorage.FLUID_CAPACITY));for(var t:b.node.chemicalOut)t.setStack(chemical("mekanism:redstone",NodeStorage.CHEMICAL_CAPACITY));
        h.startSequence().thenIdle(3).thenExecute(()->{check(a.node.energyIn.getEnergy()==123456&&a.node.fluidIn.get(0).getFluidAmount()==1000&&a.node.chemicalIn.get(0).getStored()==250&&c.stored==c.capacity(),"Full receiver charged or destroyed resources");
            var p=ReactorTests.player(h,b.getBlockPos().north());try{p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);use(p,b.getBlockPos());check(p.containerMenu instanceof ModuleMenu&&p.containerMenu.clickMenuButton(p,22)&&!b.channel(2),"Fluid toggle did not change server behaviour");}finally{ReactorTests.close(p);}b.node.fluidOut.forEach(t->t.setStack(FluidStack.EMPTY));b.node.chemicalOut.forEach(t->t.setStack(ChemicalStack.EMPTY));b.node.energyOut.setEnergy(0);
        }).thenIdle(2).thenExecute(()->{check(a.node.fluidIn.get(0).getFluidAmount()==1000&&b.node.fluidOut.stream().allMatch(t->t.isEmpty())&&a.node.energyIn.isEmpty()&&a.node.chemicalIn.get(0).isEmpty(),"Fluid disable affected other channels or was ignored");b.channels|=4;})
        .thenWaitUntil(()->check(a.node.fluidIn.get(0).isEmpty()&&b.node.fluidOut.get(0).getFluidAmount()==1000,"Re-enabled fluid transfer did not resume"))
        .thenExecute(()->c.enabled=false).thenSucceed();
    }
}
