package dev.everyonemek.factory;
import java.util.*;
import com.mojang.authlib.GameProfile;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.registries.*;
import mekanism.common.tile.multiblock.TileEntityInductionCell;
import mekanism.common.block.attribute.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekFactory.ID) @PrefixGameTestTemplate(false)
public final class FactoryTests {
    static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
    static Controller controller(GameTestHelper h,Grade g,int size){var pos=h.absolutePos(new BlockPos(20,4,20));h.getLevel().setBlockAndUpdate(pos,Content.CONTROLLERS.get(g).get().defaultBlockState());var c=(Controller)h.getLevel().getBlockEntity(pos);c.sizeX=c.sizeY=c.sizeZ=size;return c;}
    static Controller formed(GameTestHelper h,Grade g,int size){var c=controller(h,g,size);for(var e:Construction.plan(c).entrySet())h.getLevel().setBlockAndUpdate(e.getKey(),e.getValue());check(c.structure.validate(),"Structure failed: "+c.structure.error+" "+c.structure.errorPos);return c;}
    static Part port(Controller c,boolean output){return c.structure.ports.stream().filter(p->p.getBlockState().getValue(PartBlock.OUTPUT)==output).findFirst().orElseThrow();}
    static int count(Buffers b,Item item){return Arrays.stream(b.items).filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    static ServerPlayer player(GameTestHelper h,BlockPos pos){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"factory-test"),net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){private final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();@Override public io.netty.channel.Channel channel(){return channel;}};
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener callback){}
        };
        username(p,true);
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);h.getLevel().addNewPlayer(p);p.setPos(pos.getCenter());return p;
    }
    private static void username(ServerPlayer p,boolean add){try{
        // GameTestServer has no GameProfileCache. Mirror the real login cache only for this unique fixture UUID.
        var method=net.neoforged.neoforge.common.UsernameCache.class.getDeclaredMethod(add?"setUsername":"removeUsername",add?new Class<?>[]{UUID.class,String.class}:new Class<?>[]{UUID.class});method.setAccessible(true);
        if(add)method.invoke(null,p.getUUID(),p.getGameProfile().getName());else method.invoke(null,p.getUUID());
    }catch(ReflectiveOperationException e){throw new IllegalStateException("GameTest username cache fixture",e);}}
    static void close(ServerPlayer p){try{p.closeContainer();p.serverLevel().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);p.connection.getConnection().channel().close();}finally{username(p,false);}}

    @GameTest(template="empty",timeoutTicks=280)
    public static void nativeCrusherParallelAndReloadKeepExactEnergyAndOutputs(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);var cell=c.structure.cells.getFirst();long start=10000000;cell.getEnergyContainer().setEnergy(start);
        c.template.setStack(new ItemStack(MekanismBlocks.CRUSHER));c.inputs.insert(0,new ItemStack(Items.IRON_INGOT,8),false);
        long usage=Attribute.get(MekanismBlocks.CRUSHER.get(),AttributeEnergy.class).getUsage();var dust=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:dust_iron"));
        h.startSequence().thenIdle(60).thenExecute(()->{
            check(c.processing.reserved()==8&&count(c.outputs,dust)==0,"Processing did not reserve eight real lanes");
            check(c.template.extractItem(1,Action.SIMULATE,AutomationType.MANUAL).isEmpty(),"Running template could be removed");
            var tag=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(tag,h.getLevel().registryAccess());
            c.enabled=false; // Stop admitting work, but permit paid in-flight work to finish and unlock the template.
        }).thenWaitUntil(()->check(count(c.outputs,dust)==8,"Eight outputs not completed")).thenExecute(()->{
            c.enabled=false;check(count(c.inputs,Items.IRON_INGOT)==0&&c.processing.jobs.isEmpty(),"Reload duplicated inputs or retained completed work");
            check(cell.getEnergyContainer().getEnergy()==start-8*200*usage,"Energy did not equal eight native machine cycles");
        }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=70)
    public static void guardedPortsInvalidateImmediatelyAndShrinkingKeepsStock(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);var p=port(c,false);var side=c.structure.outward(p.getBlockPos());var items=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p.getBlockPos(),side);
        check(items!=null&&items.insertItem(0,new ItemStack(Items.IRON_INGOT,10),true).isEmpty()&&c.inputs.items[0].isEmpty(),"Simulation mutated inventory");
        check(items.insertItem(0,new ItemStack(Items.IRON_INGOT,10),false).isEmpty(),"Real input failed");
        var wrong=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p.getBlockPos(),side.getOpposite());check(wrong==null||wrong.getSlots()==0,"Interior face exposed inventory");
        var extraPos=c.structure.at(1,2,0);h.getLevel().setBlockAndUpdate(extraPos,Content.PORTS.get(Grade.ADVANCED).get().defaultBlockState());check(c.structure.valid()&&c.structure.inputSlots==27,"Port count/tier did not add capacity");
        c.inputs.insert(26,new ItemStack(Items.DIAMOND,33),false);var extra=(Part)h.getLevel().getBlockEntity(extraPos);var cached=new Ports.ItemPort(extra,c.structure.outward(extraPos));
        h.getLevel().setBlockAndUpdate(extraPos,Content.CASING.get().defaultBlockState());check(c.structure.valid()&&c.inputs.items[26].getCount()==33&&c.structure.inputSlots==9,"Shrinking discarded retained stock");check(cached.getSlots()==0,"Removed port retained access");
        var frame=c.structure.at(0,0,0);h.getLevel().setBlockAndUpdate(frame,Blocks.AIR.defaultBlockState());check(items.getSlots()==0&&c.energy().available()==0,"Broken structure kept moving resources");
        h.getLevel().setBlockAndUpdate(frame,Content.FRAMES.get(Grade.BASIC).get().defaultBlockState());check(items.getSlots()>0&&c.inputs.items[26].getCount()==33,"Repaired structure could not reuse safe handlers");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void cornerPortsFollowConfiguratorAndWholeFaceMenu(GameTestHelper h){
        var c=controller(h,Grade.BASIC,3);c.setFacing(Direction.WEST);
        for(var e:Construction.plan(c).entrySet())h.getLevel().setBlockAndUpdate(e.getKey(),e.getValue());
        var corner=c.structure.at(0,0,2);var movedProvider=c.structure.at(2,2,2);var roof=c.structure.at(1,2,1);
        var provider=h.getLevel().getBlockState(roof);
        h.getLevel().setBlockAndUpdate(roof,Content.FRAMES.get(Grade.BASIC).get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(movedProvider,provider);
        h.getLevel().setBlockAndUpdate(corner,Content.PORTS.get(Grade.BASIC).get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(c.structure.at(0,1,1),Content.PORTS.get(Grade.BASIC).get().defaultBlockState());
        check(c.structure.validate(),"Corner components or face-center frame refused: "+c.structure.error);
        var port=(Part)h.getLevel().getBlockEntity(corner);var back=RelativeSide.BACK.getDirection(c.getDirection());
        var outer=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,corner,back);
        var underside=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,corner,Direction.DOWN);
        var inner=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,corner,Direction.UP);
        check(outer!=null&&outer.getSlots()>0&&underside!=null&&underside.getSlots()>0&&(inner==null||inner.getSlots()==0),"Corner did not expose all and only outward faces");
        var p=player(h,corner.relative(back));var tool=new ItemStack(MekanismItems.CONFIGURATOR.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
        try{
            var hit=new net.minecraft.world.phys.BlockHitResult(corner.getCenter(),back,corner,false);
            p.setShiftKeyDown(true);p.gameMode.useItemOn(p,h.getLevel(),tool,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            check(port.getBlockState().getValue(PartBlock.OUTPUT),"Real crouching configurator did not switch port exactly once");
            check(!outer.insertItem(0,new ItemStack(Items.DIAMOND),false).isEmpty(),"Cached handler ignored output mode");
            p.setShiftKeyDown(false);FactoryMenu.open(p,c,corner);check(p.containerMenu instanceof FactoryMenu,"Port could not open factory");
            PortConfiguration.refresh(c);check(c.portInputs[RelativeSide.BACK.ordinal()]==1&&c.portOutputs[RelativeSide.BACK.ordinal()]==1,"Whole rotated face was not summarized");
            var menu=(FactoryMenu)p.containerMenu;check(menu.clickMenuButton(p,40+RelativeSide.BACK.ordinal()),"Side-menu action rejected");
            check(!port.getBlockState().getValue(PartBlock.OUTPUT)&&c.portInputs[RelativeSide.BACK.ordinal()]==2&&c.portOutputs[RelativeSide.BACK.ordinal()]==0,"Mixed face did not become all input");
            check(underside.insertItem(0,new ItemStack(Items.DIAMOND,3),false).isEmpty(),"Second corner face did not follow menu mode");
            check(menu.clickMenuButton(p,40+RelativeSide.BACK.ordinal())&&port.getBlockState().getValue(PartBlock.OUTPUT),"All-input face did not become all output");
            check(menu.clickMenuButton(p,46)&&!c.autoEject,"Menu did not disable automatic eject");
            var saved=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(saved,h.getLevel().registryAccess());
            check(!c.autoEject&&count(c.inputs,Items.DIAMOND)==3&&c.structure.valid(),"Port configuration reload lost state or inventory");
        }finally{close(p);}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=70)
    public static void multipleEnergyPortsShareRealCellsAndProviderBudget(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);var extraPos=c.structure.at(1,2,0);h.getLevel().setBlockAndUpdate(extraPos,Content.PORTS.get(Grade.BASIC).get().defaultBlockState());check(c.structure.valid(),"Extra port invalid");
        var ports=c.structure.ports.stream().filter(p->!p.getBlockState().getValue(PartBlock.OUTPUT)).toList();var a=new Ports.PowerPort(ports.get(0),c.structure.outward(ports.get(0).getBlockPos()));var b=new Ports.PowerPort(ports.get(1),c.structure.outward(ports.get(1).getBlockPos()));
        var cell=c.structure.cells.getFirst();cell.getEnergyContainer().setEnergy(0);long cap=c.structure.transfer;
        check(a.insertEnergy(0,cap,Action.SIMULATE)==0&&cell.getEnergyContainer().getEnergy()==0,"Simulated charging changed a cell");
        long first=cap-a.insertEnergy(0,cap,Action.EXECUTE);long second=cap-b.insertEnergy(0,cap,Action.EXECUTE);
        check(first>0&&second==0&&cell.getEnergyContainer().getEnergy()==first,"Ports duplicated the shared provider budget or battery");
        check(a.getEnergy(0)==first&&b.getEnergy(0)==first&&c.energy().getEnergy()==first,"Energy views disagree");
        var stack=new ItemStack(Content.CONTROLLERS.get(Grade.BASIC));c.saveToItem(stack,h.getLevel().registryAccess());
        check(stack.getOrDefault(MekanismDataComponents.ATTACHED_ENERGY,AttachedEnergy.EMPTY).containers().stream().allMatch(n->n==0),"Controller item copied the induction battery");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=65)
    public static void electrolysisDualOutputsAreAtomicAndRecoverable(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);c.structure.cells.getFirst().getEnergyContainer().setEnergy(10000000);c.template.setStack(new ItemStack(MekanismBlocks.ELECTROLYTIC_SEPARATOR));
        var input=port(c,false);var io=new Ports.FluidPort(input,c.structure.outward(input.getBlockPos()));
        var recipe=mekanism.common.recipe.MekanismRecipeType.SEPARATING.findFirst(h.getLevel(),r->r.test(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000)));
        check(recipe!=null,"Native water electrolysis recipe missing");int amount=recipe.getInput().getMatchingInstance(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000)).getAmount();var result=recipe.getOutput(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,amount));
        check(io.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,amount*8),IFluidHandler.FluidAction.EXECUTE)==amount*8,"Fluid input failed");
        h.startSequence().thenIdle(3).thenExecute(()->{
            check(Arrays.stream(c.outputs.chemicals).filter(s->ChemicalStack.isSameChemical(s,result.left())).mapToLong(ChemicalStack::getAmount).sum()==8*result.left().getAmount(),"Left output incorrect");
            check(Arrays.stream(c.outputs.chemicals).filter(s->ChemicalStack.isSameChemical(s,result.right())).mapToLong(ChemicalStack::getAmount).sum()==8*result.right().getAmount(),"Right output incorrect");
            for(int i=0;i<Buffers.TANKS;i++)c.outputs.chemicals[i]=result.left().copyWithAmount(c.outputs.capacity());
            io.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,amount),IFluidHandler.FluidAction.EXECUTE);
        }).thenIdle(3).thenExecute(()->{
            check(c.inputs.fluids[0].getAmount()==amount,"Blocked second output consumed water");
            var chem=new Ports.ChemPort(input,c.structure.outward(input.getBlockPos()));check(!chem.isValid(0,new ChemicalStack(MekanismChemicals.POLONIUM,100)),"Radioactive materials bypassed unsupported safety behavior");
            c.enabled=false;check(io.drain(amount,IFluidHandler.FluidAction.EXECUTE).getAmount()==amount,"Paused input could not be recovered");
        }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void buildUsesRealMaterialsAndHonorsPlaceCancellation(GameTestHelper h){
        var c=controller(h,Grade.BASIC,3);var p=player(h,c.getBlockPos().north(2));var plan=Construction.plan(c);var counts=new HashMap<Item,Integer>();plan.values().forEach(s->counts.merge(s.getBlock().asItem(),1,Integer::sum));
        counts.forEach((item,n)->p.getInventory().add(new ItemStack(item,n)));
        var blocked=plan.keySet().stream().skip(2).findFirst().orElseThrow();java.util.function.Consumer<BlockEvent.EntityPlaceEvent> listener=e->{if(e.getEntity()==p&&e.getPos().equals(blocked))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(listener);
        try {check(!Construction.build(c,p)&&h.getLevel().getBlockState(blocked).isAir(),"Builder ignored protection cancellation");}
        finally{NeoForge.EVENT_BUS.unregister(listener);}
        try {
            check(Construction.build(c,p),"Resume build failed: "+c.structure.error);check(counts.keySet().stream().allMatch(i->p.getInventory().countItem(i)==0),"Construction failed to consume exact materials");
            var corner=(Part)h.getLevel().getBlockEntity(c.structure.at(0,0,0));p.setPos(corner.getBlockPos().getCenter());corner.open(p);
            check(p.containerMenu instanceof FactoryMenu&&p.containerMenu.stillValid(p),"Corner could not open the shared GUI");
        }finally{close(p);}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=350)
    public static void compactFactoryUsesCablePowerAndNativeRoofOpensSharedMenu(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(20,4,20));h.getLevel().setBlockAndUpdate(pos,Content.CONTROLLERS.get(Grade.ULTIMATE).get().defaultBlockState());
        var c=(Controller)h.getLevel().getBlockEntity(pos);
        check(c.sizeX==3&&c.sizeY==3&&c.sizeZ==3,"New controller did not default to 3 x 3 x 3");
        var plan=Construction.plan(c);check(plan.size()==26,"Compact blueprint exceeded 27 blocks including controller");
        for(var e:plan.entrySet())h.getLevel().setBlockAndUpdate(e.getKey(),e.getValue());
        check(c.structure.validate()&&c.structure.parallel==512,"Compact tier lost its parallel capacity");
        var frame=c.structure.at(0,0,0);h.getLevel().setBlockAndUpdate(frame,Content.FRAMES.get(Grade.BASIC).get().defaultBlockState());
        check(c.structure.valid()&&c.structure.parallel==8,"Lowest frame failed to limit compact parallelism");
        h.getLevel().setBlockAndUpdate(frame,Content.FRAMES.get(Grade.ULTIMATE).get().defaultBlockState());check(c.structure.valid(),"Restoring frame did not reform");
        var roof=c.structure.at(1,2,1);var providerState=h.getLevel().getBlockState(roof);var cell=c.structure.cells.getFirst();
        check(cell.getBlockPos().equals(c.structure.at(1,1,1))&&c.structure.providers.getFirst().getBlockPos().equals(roof),"Compact induction positions incorrect");
        var input=port(c,false);var side=c.structure.outward(input.getBlockPos());
        var fe=h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,input.getBlockPos(),side);
        check(fe!=null&&fe.receiveEnergy(1000,true)==1000&&cell.getEnergyContainer().isEmpty(),"FE simulation failed or changed the real cell");
        var output=port(c,true);var outputFe=h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,output.getBlockPos(),c.structure.outward(output.getBlockPos()));
        check(outputFe!=null&&outputFe.receiveEnergy(1000,false)==0,"Output port accepted electricity");
        var p=player(h,roof.above());
        try {
            var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,roof,new net.minecraft.world.phys.BlockHitResult(roof.getCenter(),Direction.UP,roof,false));
            NeoForge.EVENT_BUS.post(event);
            check(event.isCanceled()&&p.containerMenu instanceof FactoryMenu&&p.containerMenu.stillValid(p),"Native roof provider did not open factory GUI");
            h.getLevel().setBlockAndUpdate(roof,Blocks.AIR.defaultBlockState());
            check(fe.receiveEnergy(1000,false)==0&&!p.containerMenu.stillValid(p),"Removed roof retained power or menu access");
            h.getLevel().setBlockAndUpdate(roof,providerState);check(c.structure.valid(),"Roof repair failed");
        }finally{close(p);}
        var cubePos=input.getBlockPos().relative(side,2);
        h.getLevel().setBlockAndUpdate(cubePos,MekanismBlocks.BASIC_ENERGY_CUBE.get().defaultBlockState());
        var cube=(mekanism.common.tile.TileEntityEnergyCube)h.getLevel().getBlockEntity(cubePos);
        cube.getEnergyContainers(null).getFirst().setEnergy(1000000);
        var config=cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);
        config.setDataType(mekanism.common.tile.component.config.DataType.OUTPUT,mekanism.api.RelativeSide.fromDirections(cube.getDirection(),side.getOpposite()));config.setEjecting(true);
        h.getLevel().setBlockAndUpdate(input.getBlockPos().relative(side),MekanismBlocks.BASIC_UNIVERSAL_CABLE.get().defaultBlockState());
        var dust=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:dust_iron"));
        h.startSequence().thenWaitUntil(()->check(cell.getEnergyContainer().getEnergy()>0,"Energy cube and real cable did not charge input port"))
              .thenExecute(()->{c.template.setStack(new ItemStack(MekanismBlocks.CRUSHER));c.inputs.insert(0,new ItemStack(Items.IRON_INGOT,8),false);})
              .thenWaitUntil(()->check(count(c.outputs,dust)==8,"Cable-powered compact factory did not finish work"))
              .thenExecute(()->{c.enabled=false;config.setEjecting(false);check(c.inputs.items[0].isEmpty(),"Compact work duplicated input");}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=120)
    public static void farShellMenuAndLowestFrameTierAreEnforced(GameTestHelper h){
        var c=formed(h,Grade.ULTIMATE,10);check(c.structure.parallel==512,"Ultimate volume/tier parallel limit incorrect");
        var far=c.structure.at(9,9,9);var p=player(h,far);var part=(Part)h.getLevel().getBlockEntity(far);
        try {part.open(p);check(p.distanceToSqr(c.getBlockPos().getCenter())>64&&p.containerMenu instanceof FactoryMenu&&p.containerMenu.stillValid(p),"Menu validity incorrectly used the distant controller");
            h.getLevel().setBlockAndUpdate(far,Content.FRAMES.get(Grade.BASIC).get().defaultBlockState());check(!c.structure.valid()&&c.structure.error.equals("tier"),"Low frame did not limit structure size");
            h.getLevel().setBlockAndUpdate(far,Blocks.AIR.defaultBlockState());check(!p.containerMenu.stillValid(p),"Menu survived removal of its access point");
        }finally{close(p);}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=350)
    public static void realHopperFeedsAndOutputEjectsIntoChest(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);c.structure.cells.getFirst().getEnergyContainer().setEnergy(10000000);c.template.setStack(new ItemStack(MekanismBlocks.CRUSHER));
        var input=port(c,false);var outward=c.structure.outward(input.getBlockPos());var feed=input.getBlockPos().relative(outward);
        h.getLevel().setBlockAndUpdate(feed,Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING,outward.getOpposite()));
        var hopper=(HopperBlockEntity)h.getLevel().getBlockEntity(feed);hopper.setItem(0,new ItemStack(Items.IRON_INGOT,4));hopper.setChanged();
        var output=port(c,true);var destination=output.getBlockPos().relative(c.structure.outward(output.getBlockPos()));h.getLevel().setBlockAndUpdate(destination,Blocks.CHEST.defaultBlockState());
        var dust=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:dust_iron"));
        h.startSequence().thenWaitUntil(()->{
            var target=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,destination,Direction.UP);int count=0;for(int i=0;i<target.getSlots();i++)if(target.getStackInSlot(i).is(dust))count+=target.getStackInSlot(i).getCount();
            check(count==4,"Real hopper/automatic chest output did not deliver four products");
        }).thenExecute(()->{c.enabled=false;check(hopper.getItem(0).isEmpty()&&count(c.inputs,Items.IRON_INGOT)==0,"Hopper transfer duplicated ingredients");}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=300)
    public static void smallerOutputPortDrainsPaidBatchInParts(GameTestHelper h){
        var c=formed(h,Grade.ELITE,6);c.structure.cells.getFirst().getEnergyContainer().setEnergy(1000000000);c.template.setStack(new ItemStack(MekanismBlocks.ENRICHMENT_CHAMBER));
        var recipe=mekanism.common.recipe.MekanismRecipeType.ENRICHING.findFirst(h.getLevel(),r->r.test(new ItemStack(Items.RAW_IRON_BLOCK,64)));
        check(recipe!=null,"Native raw iron block recipe missing");var result=recipe.getOutput(new ItemStack(Items.RAW_IRON_BLOCK));int total=64*result.getCount();check(total>9*64,"Fixture output does not exceed downgraded capacity");
        c.inputs.insert(0,new ItemStack(Items.RAW_IRON_BLOCK,64),false);final int[] removed={0};
        h.startSequence().thenIdle(40).thenExecute(()->{
            check(c.processing.reserved()==64,"Batch did not start");var p=port(c,true);h.getLevel().setBlockAndUpdate(p.getBlockPos(),Content.PORTS.get(Grade.BASIC).get().defaultBlockState().setValue(PartBlock.OUTPUT,true));
        }).thenWaitUntil(()->check(count(c.outputs,result.getItem())>0,"Completed batch could not partially fit smaller output"))
              .thenExecute(()->{removed[0]=count(c.outputs,result.getItem());check(!c.processing.jobs.isEmpty(),"Oversized job was lost instead of retained");for(int i=0;i<Buffers.SLOTS;i++)c.outputs.take(i,64,false);})
              .thenWaitUntil(()->check(c.processing.jobs.isEmpty(),"Remainder did not resume after extraction"))
              .thenExecute(()->{c.enabled=false;check(removed[0]+count(c.outputs,result.getItem())==total,"Partial output duplicated or discarded paid products");}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=450)
    public static void nativePrcConsumesAllThreeInputsAndKeepsBothOutputs(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);c.parallelLimit=1;c.structure.cells.getFirst().getEnergyContainer().setEnergy(100000000);
        c.template.setStack(new ItemStack(MekanismBlocks.PRESSURIZED_REACTION_CHAMBER));
        var recipe=mekanism.common.recipe.MekanismRecipeType.REACTION.findFirst(h.getLevel(),r->!r.getInputSolid().getRepresentations().isEmpty()&&!r.getInputFluid().getRepresentations().isEmpty()&&!r.getInputChemical().getRepresentations().isEmpty()&&!r.getInputChemical().getRepresentations().getFirst().isRadioactive()&&r.getDuration()<=300);
        check(recipe!=null,"No nonradioactive PRC fixture recipe");var item=recipe.getInputSolid().getRepresentations().getFirst();var fluid=recipe.getInputFluid().getRepresentations().getFirst();var chemical=recipe.getInputChemical().getRepresentations().getFirst();
        item=recipe.getInputSolid().getMatchingInstance(item);fluid=recipe.getInputFluid().getMatchingInstance(fluid);chemical=recipe.getInputChemical().getMatchingInstance(chemical);
        var result=recipe.getOutput(item,fluid,chemical);check(!result.chemical().isRadioactive(),"Prc fixture produces radioactive material");
        c.inputs.insert(0,item,false);c.inputs.insertFluid(0,fluid,false);c.inputs.insertChem(0,chemical,false);
        h.startSequence().thenWaitUntil(()->check(!c.outputs.items[0].isEmpty()||!c.outputs.chemicals[0].isEmpty(),"PRC did not complete"))
              .thenExecute(()->{c.enabled=false;check(c.inputs.items[0].isEmpty()&&c.inputs.fluids[0].isEmpty()&&c.inputs.chemicals[0].isEmpty(),"PRC did not consume exact inputs");
                  check(count(c.outputs,result.item().getItem())==result.item().getCount(),"PRC item output incorrect");check(ChemicalStack.isSameChemical(c.outputs.chemicals[0],result.chemical())&&c.outputs.chemicals[0].getAmount()==result.chemical().getAmount(),"PRC chemical output incorrect");
              }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=350)
    public static void removingLiveSpeedUpgradesRecalculatesWorkInsteadOfLendingFreeSpeed(GameTestHelper h){
        var c=formed(h,Grade.BASIC,4);var cell=c.structure.cells.getFirst();long start=100000000;cell.getEnergyContainer().setEnergy(start);c.template.setStack(new ItemStack(MekanismBlocks.CRUSHER));
        check(c.getComponent().getUpgradeSlot().insertItem(mekanism.common.util.UpgradeUtils.getStack(Upgrade.SPEED,8),Action.EXECUTE,AutomationType.MANUAL).isEmpty(),"Native upgrade input refused modules");
        final int[] fastTicks={0};final long[] fastUsage={0};long usage=Attribute.get(MekanismBlocks.CRUSHER.get(),AttributeEnergy.class).getUsage();var dust=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:dust_iron"));
        h.startSequence().thenWaitUntil(()->check(c.getComponent().getUpgrades(Upgrade.SPEED)==8,"Native upgrade installation did not tick"))
              .thenExecute(()->{fastUsage[0]=mekanism.common.util.MekanismUtils.getEnergyPerTick(c,usage);c.inputs.insert(0,new ItemStack(Items.IRON_INGOT,8),false);})
              .thenIdle(5).thenExecute(()->{check(!c.processing.jobs.isEmpty(),"Upgraded batch did not start");fastTicks[0]=c.processing.jobs.getFirst().progress;c.getComponent().removeUpgrade(Upgrade.SPEED,true);check(c.getComponent().getUpgradeOutputSlot().getCount()==8,"Uninstall lost modules");})
              .thenIdle(2).thenExecute(()->check(c.processing.jobs.getFirst().ticks==200&&c.processing.jobs.getFirst().energy==usage,"Removed upgrades still influenced reserved work"))
              .thenWaitUntil(()->check(count(c.outputs,dust)==8,"Recalculated work did not finish"))
              .thenExecute(()->{c.enabled=false;check(cell.getEnergyContainer().getEnergy()==start-8*(fastUsage[0]*fastTicks[0]+usage*(200-fastTicks[0])),"Upgrade transition charged incorrect energy");}).thenSucceed();
    }
}
