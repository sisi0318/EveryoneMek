package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import java.util.*;
import dev.everyonemek.gravity.expansion.*;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.network.MekClickType;
import mekanism.common.network.to_server.configuration_update.PacketSideData;
import mekanism.common.registries.*;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class NodeFrequencyTests {
    static UUID pair(GameTestHelper h,OrbitalModule a,OrbitalModule b){var p=ReactorTests.player(h,a.getBlockPos().north());var data=NodeFrequencies.get(h.getLevel());var entry=data.create(p.getUUID(),"fixture-"+p.getUUID().toString().substring(0,8),false);try{check(((NodeModule)a).selectFrequency(p,entry.id())&&((NodeModule)b).selectFrequency(p,entry.id()),"Fixture frequency join failed");}finally{ReactorTests.close(p);}h.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo info){}public void testPassed(GameTestInfo info,GameTestRunner runner){data.remove(entry.id(),entry.owner());}public void testFailed(GameTestInfo info,GameTestRunner runner){data.remove(entry.id(),entry.owner());}public void testAddedForRerun(GameTestInfo old,GameTestInfo retry,GameTestRunner runner){}});return entry.id();}
    private static NodeModule node(GameTestHelper h,BlockPos p){h.getLevel().setBlockAndUpdate(p,ModuleContent.BLOCK.get(ModuleKind.NODE).get().defaultBlockState());return (NodeModule)h.getLevel().getBlockEntity(p);}
    private static ModuleMenu open(ServerPlayer p,NodeModule n){p.setPos(n.getBlockPos().north().getCenter());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(n.getBlockPos().getCenter(),Direction.NORTH,n.getBlockPos(),false));check(p.containerMenu instanceof ModuleMenu,"Node GUI unavailable");var menu=(ModuleMenu)p.containerMenu;check(menu.slots.get(18).y==163,"Node screen/menu player inventory disagree");return menu;}
    private static boolean command(ServerPlayer p,NodeModule n,int operation,UUID id,String name,boolean shared){var menu=(ModuleMenu)p.containerMenu;var packet=new NodeFrequencyNetwork.Action(menu.containerId,menu.nodeSession,n.getBlockPos(),operation,id,name,shared);var b=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());try{NodeFrequencyNetwork.Action.CODEC.encode(b,packet);return NodeFrequencyNetwork.handle(p,NodeFrequencyNetwork.Action.CODEC.decode(b));}finally{b.release();}}
    private static void sides(ServerPlayer p,NodeModule node,TransmissionType type,RelativeSide selected,DataType mode){var context=(IPayloadContext)java.lang.reflect.Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),new Class<?>[]{IPayloadContext.class},(object,method,args)->{if(method.getName().equals("player"))return p;throw new UnsupportedOperationException(method.getName());});
        for(var side:RelativeSide.values()){var target=side==selected?mode:DataType.NONE;for(int i=0;i<4&&node.getConfig().getDataType(type,side)!=target;i++)new PacketSideData(node.getBlockPos(),MekClickType.LEFT,side,type).handle(context);check(node.getConfig().getDataType(type,side)==target,"Native side packet did not update "+type+" "+side);}}

    @GameTest(template="empty",timeoutTicks=120)
    public static void sameFrequencySharesPowerAndEjectsAllResourcesThroughConfiguredSides(GameTestHelper h){var sun=SolarTests.formed(h);sun.enabled=sun.ignited=true;sun.stored=sun.capacity();sun.fuelRemaining=sun.fuelTotal=100_000_000_000_000_000L;
        var a=node(h,sun.getBlockPos().north(3));var b=node(h,a.getBlockPos().west(10));var unused=node(h,b.getBlockPos().west(5));var p=ReactorTests.player(h,a.getBlockPos().north());var catalog=NodeFrequencies.get(h.getLevel());
        open(p,a);check(command(p,a,1,null,"route-"+p.getUUID().toString().substring(0,8),false),"Frequency creation failed");UUID id=a.frequency;
        var types=NodeModule.TYPES;var inputSides=new RelativeSide[]{RelativeSide.LEFT,RelativeSide.BOTTOM,RelativeSide.TOP,RelativeSide.BACK};for(int i=0;i<4;i++)sides(p,a,types[i],inputSides[i],DataType.INPUT);
        open(p,b);check(command(p,b,2,id,"",false),"Same-frequency join failed");check(b.bind(p,FieldLink.at(h.getLevel(),sun.getBlockPos())),"Group source binding failed");for(var type:types)sides(p,b,type,RelativeSide.RIGHT,DataType.OUTPUT);open(p,unused);check(command(p,unused,2,id,"",false),"Idle member join failed");
        var side=RelativeSide.RIGHT.getDirection(b.getDirection());var machinePos=b.getBlockPos().relative(side);h.getLevel().setBlockAndUpdate(machinePos,MekanismBlocks.PRESSURIZED_REACTION_CHAMBER.get().defaultBlockState());var machine=(mekanism.common.tile.machine.TileEntityPressurizedReactionChamber)h.getLevel().getBlockEntity(machinePos);
        for(var type:types){var config=machine.getConfig().getConfig(type);for(var face:RelativeSide.values())config.setDataType(DataType.INPUT,face);config.setEjecting(false);}h.getLevel().invalidateCapabilities(machinePos);
        var item=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,a.getBlockPos(),inputSides[0].getDirection(a.getDirection()));check(item!=null&&item.insertItem(0,new ItemStack(MekanismItems.BIO_FUEL.get()),false).isEmpty(),"Configured item input rejected cargo");
        var energy=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),a.getBlockPos(),inputSides[1].getDirection(a.getDirection()));check(energy!=null&&energy.insertEnergy(8000,Action.EXECUTE)==0,"Configured energy input rejected cargo");
        var fluid=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,a.getBlockPos(),inputSides[2].getDirection(a.getDirection()));check(fluid!=null&&fluid.fill(new FluidStack(Fluids.WATER,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==1000,"Configured fluid input rejected cargo");
        var chemical=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),a.getBlockPos(),inputSides[3].getDirection(a.getDirection()));check(chemical!=null&&chemical.insertChemical(new ChemicalStack(MekanismChemicals.HYDROGEN.get(),500),Action.EXECUTE).isEmpty(),"Configured chemical input rejected cargo");
        var blocked=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,a.getBlockPos(),Direction.UP);check(blocked==null||blocked.getSlots()==0||blocked.insertItem(0,new ItemStack(Items.DIAMOND),true).getCount()==1,"Disabled side still accepted input");
        h.startSequence().thenWaitUntil(()->check(machine.inputFluidTank.getFluidAmount()==1000&&machine.inputGasTank.getStored()==500&&machine.getInventorySlots(null).stream().anyMatch(s->s.getStack().is(MekanismItems.BIO_FUEL.get()))&&machine.getEnergyContainer().getEnergy()==2000,"Frequency delivery pending: "+a.status+" / "+a.peers))
            .thenExecute(()->{try{check(a.source==null&&a.node.energyIn.isEmpty()&&b.node.energyOut.getEnergy()==6000&&a.inputs.getFirst().isEmpty(),"Frequency routing lost/duplicated cargo or required an individual sender source");check(unused.outputs.stream().allMatch(s->s.isEmpty())&&unused.node.energyOut.isEmpty()&&unused.node.fluidOut.stream().allMatch(t->t.isEmpty())&&unused.node.chemicalOut.stream().allMatch(t->t.isEmpty()),"Unconnected node captured resources intended for a real output machine");check(a.node.fluidIn.getFirst().isEmpty()&&a.node.chemicalIn.getFirst().isEmpty(),"Frequency did not empty source tanks");var tag=b.getReducedUpdateTag(h.getLevel().registryAccess());check(tag.getInt("snapshot_type")>=0&&!tag.getString("snapshot_id").isEmpty(),"Receiving node did not synchronize a resource snapshot");}finally{sun.enabled=false;catalog.remove(id,p.getUUID());ReactorTests.close(p);}}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void frequencyRightsAndNativeSideConfigurationSurviveActualDrop(GameTestHelper h){var a=node(h,h.absolutePos(new BlockPos(8,4,8)));var owner=ReactorTests.player(h,a.getBlockPos().north());var visitor=ReactorTests.player(h,a.getBlockPos().south());var data=NodeFrequencies.get(h.getLevel());UUID id=null;
        try{open(owner,a);check(command(owner,a,1,null,"private-"+owner.getUUID().toString().substring(0,8),false),"Private frequency creation failed");id=a.frequency;sides(owner,a,TransmissionType.ITEM,RelativeSide.TOP,DataType.INPUT_OUTPUT);a.inputs.getFirst().setStack(new ItemStack(Items.DIAMOND,4096));a.node.chemicalOut.get(3).setStack(new ChemicalStack(MekanismChemicals.NUCLEAR_WASTE.get(),98765));a.channels=5;
            var menu=open(visitor,a);check(!command(visitor,a,2,id,"",false)&&!command(visitor,a,4,id,"",false),"Visitor joined/deleted another player's private frequency");check(!NodeFrequencyNetwork.handle(visitor,new NodeFrequencyNetwork.Action(menu.containerId,menu.nodeSession+1,a.getBlockPos(),3,null,"",false)),"Stale frequency session changed binding");
            var copy=(NodeModule)BlockEntity.loadStatic(a.getBlockPos(),a.getBlockState(),a.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());check(copy.frequency.equals(id)&&copy.getConfig().getDataType(TransmissionType.ITEM,RelativeSide.TOP)==DataType.INPUT_OUTPUT,"World save lost frequency or native side config");
            var drop=Block.getDrops(a.getBlockState(),h.getLevel(),a.getBlockPos(),a).getFirst();drop=ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)drop.save(h.getLevel().registryAccess()));var pos=a.getBlockPos();h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());owner.setPos(pos.north().getCenter());owner.setYRot(180);owner.setItemInHand(InteractionHand.MAIN_HAND,drop);owner.getMainHandItem().useOn(new UseOnContext(owner,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
            var restored=(NodeModule)h.getLevel().getBlockEntity(pos);check(restored!=null&&id.equals(restored.frequency)&&restored.inputs.getFirst().getCount()==4096&&restored.node.chemicalOut.get(3).getStored()==98765&&restored.channels==5&&restored.getConfig().getDataType(TransmissionType.ITEM,RelativeSide.TOP)==DataType.INPUT_OUTPUT,"Actual node item replacement lost frequency, side config or cargo");
            open(owner,restored);check(command(owner,restored,3,null,"",false)&&restored.frequency==null&&restored.inputs.getFirst().getCount()==4096,"Leaving a frequency destroyed cargo");
        }finally{if(id!=null)data.remove(id,owner.getUUID());ReactorTests.close(visitor);ReactorTests.close(owner);}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void frequencyMembershipRejectsRemovedNodesAndSnapshotsExpire(GameTestHelper h){var a=node(h,h.absolutePos(new BlockPos(8,4,8)));var b=node(h,a.getBlockPos().east(5));var p=ReactorTests.player(h,a.getBlockPos().north());var data=NodeFrequencies.get(h.getLevel());open(p,a);check(command(p,a,1,null,"members-"+p.getUUID().toString().substring(0,8),true),"Public creation failed");UUID id=a.frequency;open(p,b);check(command(p,b,2,id,"",false),"Public join failed");
        h.startSequence().thenIdle(2).thenExecute(()->{try{check(NodeNetwork.members(a).contains(b),"Loaded node index missed frequency member");h.getLevel().setBlockAndUpdate(b.getBlockPos(),Blocks.AIR.defaultBlockState());var replacement=node(h,b.getBlockPos());check(!NodeNetwork.members(a).contains(b)&&!NodeNetwork.members(a).contains(replacement),"Removed/replaced device remained in the frequency");
            var snapshot=new NodeSnapshot();snapshot.record(0,0,new ItemStack(Items.IRON_INGOT));snapshot.record(0,1,null);snapshot.record(0,2,new FluidStack(Fluids.WATER,1));snapshot.record(0,3,new ChemicalStack(MekanismChemicals.HYDROGEN.get(),1));var seen=new HashSet<Integer>();for(int t=0;t<80;t+=20){for(int i=0;i<4;i++)snapshot.record(t,i,switch(i){case 0->new ItemStack(Items.IRON_INGOT);case 2->new FluidStack(Fluids.WATER,1);case 3->new ChemicalStack(MekanismChemicals.HYDROGEN.get(),1);default->null;});var tag=new net.minecraft.nbt.CompoundTag();snapshot.write(tag,t);seen.add(tag.getInt("snapshot_type"));check(!tag.getString("snapshot_id").isEmpty(),"Projection did not carry actual registry identity");}check(seen.size()==4,"Simultaneous resource projections did not rotate");var idle=new net.minecraft.nbt.CompoundTag();snapshot.write(idle,101);check(idle.getInt("snapshot_type")==-1,"Stopped resource snapshot did not expire");
        }finally{data.remove(id,p.getUUID());ReactorTests.close(p);}}).thenSucceed();
    }
}
