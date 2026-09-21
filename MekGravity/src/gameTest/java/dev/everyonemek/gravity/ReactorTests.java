package dev.everyonemek.gravity;
import java.util.*;
import com.mojang.authlib.GameProfile;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.registries.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class ReactorTests {
    static void check(boolean ok,String text){if(!ok)throw new GameTestAssertException(text);}
    static Controller create(GameTestHelper h){var pos=h.absolutePos(new BlockPos(20,4,20));h.getLevel().setBlockAndUpdate(pos,Content.CONTROLLER.get().defaultBlockState());return (Controller)h.getLevel().getBlockEntity(pos);}
    static Controller formed(GameTestHelper h){var c=create(h);for(var e:Construction.plan(c).entrySet())h.getLevel().setBlockAndUpdate(e.getKey(),e.getValue());check(c.structure.validate(),"Structure: "+c.structure.error+" at "+c.structure.errorPos);c.autoEject=false;return c;}
    static Part port(Controller c,PartBlock.Kind kind,boolean output){return c.structure.ports.stream().filter(p->p.kind()==kind&&p.output()==output).findFirst().orElseThrow();}
    static Direction side(Controller c,Part p){return Arrays.stream(Direction.values()).filter(d->c.structure.outward(p.getBlockPos(),d)).findFirst().orElseThrow();}
    static void supply(Controller c,int count){c.stored=c.startup()+c.reserve();c.cold=new ChemicalStack(MekanismChemicals.SODIUM,1000000);c.structure.fuelHatches.getFirst().inventory.setStackInSlot(0,new ItemStack(Content.PELLET.get(),count));c.enabled=true;}
    static ServerPlayer player(GameTestHelper h,BlockPos pos){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"gravity-test"),net.minecraft.server.level.ClientInformation.createDefault());
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

    @GameTest(template="empty",timeoutTicks=80)
    public static void fuelHeatStartupAndReloadConserveResources(GameTestHelper h){
        var c=formed(h);supply(c,3);var recipe=FuelRecipe.find(h.getLevel(),new ItemStack(Content.PELLET.get()));check(recipe!=null,"Data fuel recipe not registered");
        long initialCold=c.cold.getAmount(),cost=c.startup();c.react();
        check(c.ignited&&c.gross>0&&c.stored==c.reserve()+c.gross-c.selfUse,"Startup or gross/net energy wrong");
        check(recipe.energy()-c.fuelRemaining==c.gross+c.heatSpent,"Cooling heat duplicated fuel energy");
        check(c.hot.is(MekanismChemicals.SUPERHEATED_SODIUM)&&initialCold-c.cold.getAmount()==c.hot.getAmount(),"Sodium conversion lost matter");
        long stored=c.stored,remaining=c.fuelRemaining,cold=c.cold.getAmount(),hot=c.hot.getAmount();var tag=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(tag,h.getLevel().registryAccess());
        check(c.stored==stored&&c.fuelRemaining==remaining&&c.cold.getAmount()==cold&&c.hot.getAmount()==hot&&c.ignited,"Reload reset the paid fuel or field");
        c.enabled=false;c.react();check(c.stored==stored&&c.fuelRemaining==remaining&&c.gross==0,"Stopped reactor consumed fuel");c.enabled=true;c.react();
        check(c.stored==stored+c.gross-c.selfUse&&remaining-c.fuelRemaining==c.gross+c.heatSpent,"Restart charged startup twice or repeated fuel");
        var drop=Block.getDrops(c.getBlockState(),h.getLevel(),c.getBlockPos(),c).getFirst();check(drop.has(Content.DATA.get())&&drop.get(Content.DATA.get()).getLong("energy")==c.stored,"Controller drop lost stored resources");
        check(cost>0&&c.structure.fuelHatches.getFirst().inventory.getStackInSlot(0).getCount()==2,"A reaction consumed more than one pellet");c.enabled=false;h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void starvationAndCachedPortsStopImmediately(GameTestHelper h){
        var c=formed(h);supply(c,2);var input=port(c,PartBlock.Kind.COOLANT,false);var face=side(c,input);var chemicals=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),input.getBlockPos(),face);
        long power=c.stored;c.cold=ChemicalStack.EMPTY;c.react();check(!c.ignited&&c.stored==power&&c.fuelRemaining==0,"Missing sodium consumed startup or fuel");
        var sodium=new ChemicalStack(MekanismChemicals.SODIUM,1000000);check(chemicals.insertChemical(sodium,Action.SIMULATE).isEmpty()&&c.cold.isEmpty(),"Simulated sodium insertion mutated state");chemicals.insertChemical(sodium,Action.EXECUTE);
        c.hot=new ChemicalStack(MekanismChemicals.SUPERHEATED_SODIUM,c.tankCapacity());c.react();check(c.stored==power&&c.fuelRemaining==0&&c.status.equals("hot_blocked"),"Blocked cooling consumed fuel");
        c.hot=ChemicalStack.EMPTY;c.react();long fuel=c.fuelRemaining;c.stored=c.capacity();c.react();check(c.gross==0&&c.fuelRemaining==fuel,"Full energy buffer kept burning fuel");
        var frame=c.structure.at(0,0,0);var original=h.getLevel().getBlockState(frame);h.getLevel().setBlockAndUpdate(frame,Blocks.AIR.defaultBlockState());
        check(chemicals.getChemicalTanks()==0&&!chemicals.insertChemical(sodium,Action.EXECUTE).isEmpty(),"Broken structure retained cached transfer");c.react();check(c.fuelRemaining==fuel,"Broken reactor consumed fuel");
        h.getLevel().setBlockAndUpdate(frame,original);check(c.structure.valid()&&chemicals.getChemicalTanks()==1,"Repair did not restore transfer");c.enabled=false;h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void fourEnergyPortsShareLongBudgetAndProtectReserve(GameTestHelper h){
        var c=formed(h);for(var coil:List.copyOf(c.structure.coils))h.getLevel().setBlockAndUpdate(coil.getBlockPos(),Content.COILS.get(Grade.ULTIMATE).get().defaultBlockState().setValue(PartBlock.FACING,coil.getBlockState().getValue(PartBlock.FACING)));
        check(c.structure.valid()&&c.structure.grade==Grade.ULTIMATE,"Six high tier coils did not upgrade");c.ignited=true;c.stored=c.reserve()+c.structure.grade.power();
        long initial=c.stored,total=0;for(var p:c.structure.ports)if(p.kind()==PartBlock.Kind.ENERGY&&p.output()){
            var handler=h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,p.getBlockPos(),side(c,p));check(handler.receiveEnergy(100,false)==0,"Output port accepted power");
            check(handler.extractEnergy(Integer.MAX_VALUE,true)>0&&c.stored==initial-total,"Energy simulation mutated the buffer");
            int fe=handler.extractEnergy(Integer.MAX_VALUE,false);long joules=(long)mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertFrom(fe);total+=joules;
            check(handler.extractEnergy(Integer.MAX_VALUE,false)==0,"The same port bypassed its tick budget");
        }
        check(total==c.structure.grade.power()&&total>Integer.MAX_VALUE&&c.stored==c.reserve(),"Multiple ports duplicated energy, overflowed int, or drained reserve");
        var lowest=c.structure.coils.getFirst();h.getLevel().setBlockAndUpdate(lowest.getBlockPos(),Content.COILS.get(Grade.BASIC).get().defaultBlockState().setValue(PartBlock.FACING,lowest.getBlockState().getValue(PartBlock.FACING)));check(c.structure.valid()&&c.structure.grade==Grade.BASIC,"Lowest coil tier ignored");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=140)
    public static void actualCableChargesAndOutputPortFeedsNativeCube(GameTestHelper h){
        var c=formed(h);var input=port(c,PartBlock.Kind.ENERGY,false);var side=side(c,input);var cubePos=input.getBlockPos().relative(side,2);
        h.getLevel().setBlockAndUpdate(cubePos,MekanismBlocks.BASIC_ENERGY_CUBE.get().defaultBlockState());var cube=(mekanism.common.tile.TileEntityEnergyCube)h.getLevel().getBlockEntity(cubePos);cube.getEnergyContainers(null).getFirst().setEnergy(1000000);
        var config=cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);config.setDataType(mekanism.common.tile.component.config.DataType.OUTPUT,RelativeSide.fromDirections(cube.getDirection(),side.getOpposite()));config.setEjecting(true);
        h.getLevel().setBlockAndUpdate(input.getBlockPos().relative(side),MekanismBlocks.BASIC_UNIVERSAL_CABLE.get().defaultBlockState());
        var out=port(c,PartBlock.Kind.ENERGY,true);var targetPos=out.getBlockPos().relative(side(c,out));
        h.startSequence().thenWaitUntil(()->check(c.stored>0,"Native cable did not charge excitation input")).thenExecute(()->{
            check(!c.ignited&&c.gross==0,"Unstarted reactor generated power");h.getLevel().destroyBlock(cubePos,false);h.getLevel().destroyBlock(input.getBlockPos().relative(side),false);
            h.getLevel().setBlockAndUpdate(targetPos,MekanismBlocks.BASIC_ENERGY_CUBE.get().defaultBlockState());var target=(mekanism.common.tile.TileEntityEnergyCube)h.getLevel().getBlockEntity(targetPos);
            var targetConfig=target.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);for(var face:RelativeSide.values())targetConfig.setDataType(mekanism.common.tile.component.config.DataType.INPUT,face);targetConfig.setEjecting(false);h.getLevel().invalidateCapabilities(targetPos);
            c.ignited=true;c.stored=c.reserve()+1000000;c.autoEject=true;
        }).thenWaitUntil(()->{var target=(mekanism.common.tile.TileEntityEnergyCube)h.getLevel().getBlockEntity(targetPos);check(target.getEnergyContainers(null).getFirst().getEnergy()==1000000&&c.stored==c.reserve(),"Automatic output did not conserve energy at the real receiving cube");}).thenExecute(()->c.autoEject=false).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void constructionConfiguratorMenusAndFuelDropsWork(GameTestHelper h){
        for(var name:List.of("reactor","frame","casing","glass","fuel","coolant","energy","core","basic_coil","advanced_coil","elite_coil","ultimate_coil","dense_fuel_pellet","matter_fuel/dense"))check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MekGravity.ID,name)).isPresent(),"Recipe failed to load: "+name);
        var c=create(h);var p=player(h,c.getBlockPos().north(2));var plan=Construction.plan(c);check(plan.size()==224,"Blueprint no longer matches approved 225-block design");var counts=new HashMap<Item,Integer>();plan.values().forEach(s->counts.merge(s.getBlock().asItem(),1,Integer::sum));counts.forEach((item,n)->p.getInventory().add(new ItemStack(item,n)));
        var blocked=plan.keySet().stream().skip(2).findFirst().orElseThrow();java.util.function.Consumer<BlockEvent.EntityPlaceEvent> listener=e->{if(e.getEntity()==p&&e.getPos().equals(blocked))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(listener);
        try{check(!Construction.build(c,p)&&h.getLevel().getBlockState(blocked).isAir(),"Construction bypassed block-place cancellation");}finally{NeoForge.EVENT_BUS.unregister(listener);}
        try{
            check(Construction.build(c,p),"Construction could not resume: "+c.structure.error);check(counts.keySet().stream().allMatch(i->p.getInventory().countItem(i)==0),"Construction did not debit exactly placed materials");
            var fuel=c.structure.fuelHatches.getFirst();p.setPos(fuel.getBlockPos().north().getCenter());fuel.open(p);check(p.containerMenu instanceof FuelMenu&&p.containerMenu.stillValid(p),"Fuel hatch did not open its own menu");
            fuel.inventory.setStackInSlot(0,new ItemStack(Content.PELLET.get(),13));var drops=Block.getDrops(fuel.getBlockState(),h.getLevel(),fuel.getBlockPos(),fuel);check(drops.getFirst().has(Content.STOCK.get()),"Fuel drop missing inventory");var pos=fuel.getBlockPos();var cached=new Ports.Items(fuel,side(c,fuel));h.getLevel().destroyBlock(pos,false);check(cached.getSlots()==0&&!p.containerMenu.stillValid(p),"Removed fuel hatch kept inventory access");
            p.setItemInHand(InteractionHand.MAIN_HAND,drops.getFirst());p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
            var restored=(Part)h.getLevel().getBlockEntity(pos);check(restored!=null&&restored.inventory.getStackInSlot(0).getCount()==13&&c.structure.valid(),"Fuel inventory did not survive real drop/place");
            var inlet=port(c,PartBlock.Kind.ENERGY,false);var face=side(c,inlet);p.setPos(inlet.getBlockPos().relative(face).getCenter());p.setShiftKeyDown(true);var tool=new ItemStack(MekanismItems.CONFIGURATOR.get());p.setItemInHand(InteractionHand.MAIN_HAND,tool);p.gameMode.useItemOn(p,h.getLevel(),tool,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(inlet.getBlockPos().getCenter(),face,inlet.getBlockPos(),false));check(inlet.output(),"Native Configurator did not change energy port mode");
            p.setShiftKeyDown(false);var corner=(Part)h.getLevel().getBlockEntity(c.structure.at(0,0,0));p.setPos(corner.getBlockPos().north().getCenter());corner.open(p);check(p.containerMenu instanceof ReactorMenu&&p.containerMenu.stillValid(p),"Corner did not open reactor menu");
            var menu=(ReactorMenu)p.containerMenu;check(menu.clickMenuButton(p,150)&&c.load==50&&!menu.clickMenuButton(p,201),"Load setting was not validated");
        }finally{close(p);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void nativeBoilerRecoversReactorHotSodium(GameTestHelper h){
        var c=formed(h);supply(c,1);c.react();c.enabled=false;c.autoEject=true;long supplied=c.hot.getAmount();check(supplied>0,"No reactor sodium produced");
        var hotPort=port(c,PartBlock.Kind.COOLANT,true);var exit=side(c,hotPort);var valvePos=hotPort.getBlockPos().relative(exit,2);var base=valvePos.offset(-2,-1,-1);
        for(int x=0;x<3;x++)for(int y=0;y<5;y++)for(int z=0;z<3;z++)if(x==0||x==2||y==0||y==4||z==0||z==2)h.getLevel().setBlockAndUpdate(base.offset(x,y,z),MekanismBlocks.BOILER_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.offset(1,1,1),MekanismBlocks.SUPERHEATING_ELEMENT.get().defaultBlockState());h.getLevel().setBlockAndUpdate(base.offset(1,2,1),MekanismBlocks.PRESSURE_DISPERSER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(valvePos,MekanismBlocks.BOILER_VALVE.get().defaultBlockState());
        var valve=(mekanism.common.tile.multiblock.TileEntityBoilerValve)h.getLevel().getBlockEntity(valvePos);
        h.startSequence().thenWaitUntil(()->check(valve.getMultiblock().isFormed(),"Native boiler fixture did not form"))
              .thenExecute(()->h.getLevel().setBlockAndUpdate(hotPort.getBlockPos().relative(exit),MekanismBlocks.ULTIMATE_PRESSURIZED_TUBE.get().defaultBlockState()))
              .thenWaitUntil(()->check(valve.getMultiblock().cooledCoolantTank.getStored()>0,"Native boiler did not cool reactor sodium"))
              .thenExecute(()->{check(valve.getMultiblock().cooledCoolantTank.getStack().is(MekanismChemicals.SODIUM),"Boiler produced wrong coolant");check(valve.getMultiblock().cooledCoolantTank.getStored()+valve.getMultiblock().superheatedCoolantTank.getStored()<=supplied,"Cooling loop duplicated chemical");c.autoEject=false;}).thenSucceed();
    }
}
