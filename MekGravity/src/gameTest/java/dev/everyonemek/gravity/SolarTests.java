package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.solar.*;
import java.util.*;
import mekanism.api.*;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.registries.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class SolarTests {
    static SolarController create(GameTestHelper h){var pos=h.absolutePos(new BlockPos(20,4,20));h.getLevel().setBlockAndUpdate(pos,SolarContent.CONTROLLER.get().defaultBlockState());return (SolarController)h.getLevel().getBlockEntity(pos);}
    static SolarController formed(GameTestHelper h){var c=create(h);SolarConstruction.plan(c).forEach(h.getLevel()::setBlockAndUpdate);check(c.structure.valid(),"Solar structure: "+c.structure.error+" "+c.structure.errorPos);c.autoEject=false;return c;}
    static Direction side(SolarController c,SolarPart p){return Arrays.stream(Direction.values()).filter(d->c.structure.outward(p.getBlockPos(),d)).findFirst().orElseThrow();}
    static void tier(GameTestHelper h,SolarController c,int grade){for(var p:List.copyOf(c.structure.parts))if(p.kind().tiered())h.getLevel().setBlockAndUpdate(p.getBlockPos(),SolarContent.block(p.kind(),grade).get().defaultBlockState().setValue(SolarBlock.FACING,p.getBlockState().getValue(SolarBlock.FACING)));check(c.structure.valid(),"Upgraded structure failed: "+c.structure.error);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void solarFuelLastsTwoHoursAndCapsuleRecoveryIsAtomic(GameTestHelper h){var c=formed(h);tier(h,c,3);var hatch=c.structure.fuelHatches.getFirst();var player=ReactorTests.player(h,c.getBlockPos().north());
        try{
            check(SolarLayout.SLOTS.size()==220&&SolarConstruction.plan(c).size()==219,"Solar design size changed");
            var recipe=SolarFuelRecipe.find(h.getLevel(),new ItemStack(SolarContent.FUEL.get()));check(recipe!=null&&recipe.energy()==737_280_000_000_000_000L,"Stellar recipe energy mismatch");
            hatch.inventory.setStackInSlot(0,new ItemStack(SolarContent.FUEL.get(),64));c.stored=c.startup()+c.reserve();c.enabled=true;c.automatic=false;c.react();
            check(c.ignited&&hatch.inventory.getStackInSlot(0).getCount()==63&&c.fuelRemaining==recipe.energy()-c.gross,"Preloading 64 fuels overflowed or duplicated energy");
            long left=c.fuelRemaining,total=c.fuelTotal,stored=c.stored;var tag=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(tag,h.getLevel().registryAccess());
            check(c.fuelRemaining==left&&c.fuelTotal==total&&c.stored==stored&&c.ignited,"Reload lost stellar budget");
            c.enabled=false;check(c.recover(player)&&c.fuelRemaining==0,"Could not recover paused stellar fuel");check(!c.recover(player),"Recovered one budget twice");
            var capsule=player.getInventory().getItem(0).copy();check(capsule.is(SolarContent.CAPSULE.get())&&SolarFuelRecipe.fuel(h.getLevel(),capsule).remaining()==left,"Capsule refilled instead of preserving budget");
            hatch.inventory.setStackInSlot(0,capsule);player.getInventory().setItem(0,ItemStack.EMPTY);c.enabled=true;c.stored=c.reserve();c.react();check(c.fuelRemaining==left-c.gross&&hatch.inventory.getStackInSlot(0).isEmpty(),"Capsule insertion cloned or reset its budget");
            c.structure.valid();c.fuelRemaining=recipe.energy();c.fuelTotal=recipe.energy();c.gross=c.structure.power();hatch.inventory.setStackInSlot(0,ItemStack.EMPTY);long produced=0,used=0;
            for(int i=0;i<144000;i++){c.stored=c.reserve();c.react();check(c.gross==5_120_000_000_000L,"Full-load stellar fuel ran out before two hours");produced+=c.gross-c.selfUse;used+=c.selfUse;}
            check(c.fuelRemaining==0&&produced+used==recipe.energy(),"Two-hour budget did not conserve total energy");
            check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"stellar_fuel")).isPresent(),"Native PRC fuel recipe failed to load");
        }finally{c.enabled=false;ReactorTests.close(player);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void solarLimitsMixedWingsAndStopsAtFullBuffer(GameTestHelper h){var c=formed(h);
        check(c.structure.power()==640_000_000_000L,"Basic solar power wrong");
        for(var p:List.copyOf(c.structure.parts))if(p.kind()==SolarBlock.Kind.RING||p.kind()==SolarBlock.Kind.FOCUS||p.kind()==SolarBlock.Kind.COLLECTOR&&c.structure.outward(p.getBlockPos(),c.getDirection().getClockWise()))
            h.getLevel().setBlockAndUpdate(p.getBlockPos(),SolarContent.block(p.kind(),3).get().defaultBlockState().setValue(SolarBlock.FACING,p.getBlockState().getValue(SolarBlock.FACING)));
        check(c.structure.valid()&&c.structure.collectorLimit()==1_760_000_000_000L&&c.structure.power()==c.structure.collectorLimit(),"Mixed four-wing tiers not summed once per wing");
        var wing=c.structure.parts.stream().filter(p->p.kind()==SolarBlock.Kind.COLLECTOR).findFirst().orElseThrow();var old=wing.getBlockState();h.getLevel().setBlockAndUpdate(wing.getBlockPos(),SolarContent.block(SolarBlock.Kind.COLLECTOR,1).get().defaultBlockState().setValue(SolarBlock.FACING,old.getValue(SolarBlock.FACING)));check(!c.structure.valid()&&c.structure.error.equals("wing_mixed"),"Mixed tiles within one wing accepted");h.getLevel().setBlockAndUpdate(wing.getBlockPos(),old);check(c.structure.valid(),"Wing repair failed");
        c.structure.fuelHatches.getFirst().inventory.setStackInSlot(0,new ItemStack(SolarContent.FUEL.get()));c.enabled=c.ignited=true;c.stored=c.reserve();c.react();long left=c.fuelRemaining;
        c.stored=c.capacity();c.react();check(c.fuelRemaining==left&&c.gross==0,"Full solar kept consuming fuel");c.stored=c.reserve();c.react();check(c.gross>0,"Automatic load failed to resume");
        var out=c.structure.ports.stream().filter(p->p.kind()==SolarBlock.Kind.ENERGY&&p.output()).findFirst().orElseThrow();var energy=new SolarPorts.Energy(out,side(c,out));
        c.stored=c.reserve()+SolarConfig.PORT_RATE.get();long before=c.stored;check(energy.extractEnergy(0,Long.MAX_VALUE,Action.SIMULATE)==SolarConfig.PORT_RATE.get()&&c.stored==before,"Simulated solar extraction changed energy");
        check(energy.extractEnergy(0,Long.MAX_VALUE,Action.EXECUTE)==SolarConfig.PORT_RATE.get()&&energy.extractEnergy(0,Long.MAX_VALUE,Action.EXECUTE)==0,"Per-port solar limit bypassed");
        h.getLevel().setBlockAndUpdate(c.structure.at(1,1,1),Blocks.AIR.defaultBlockState());check(!c.structure.valid()&&energy.getEnergyContainerCount()==0,"Broken solar kept capability access");c.enabled=false;h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void solarBuildRespectsProtectionAndFeedsNativeCube(GameTestHelper h){var c=create(h);var p=ReactorTests.player(h,c.getBlockPos().north());var plan=SolarConstruction.plan(c);var counts=new HashMap<Item,Integer>();plan.values().forEach(s->counts.merge(s.getBlock().asItem(),1,Integer::sum));counts.forEach((item,n)->p.getInventory().add(new ItemStack(item,n)));
        var blocked=plan.keySet().stream().skip(4).findFirst().orElseThrow();java.util.function.Consumer<BlockEvent.EntityPlaceEvent> deny=e->{if(e.getEntity()==p&&e.getPos().equals(blocked))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(deny);
        try{check(!SolarConstruction.build(c,p)&&h.getLevel().getBlockState(blocked).isAir(),"Solar builder bypassed protection");}finally{NeoForge.EVENT_BUS.unregister(deny);}
        try{check(SolarConstruction.build(c,p),"Solar builder failed to resume: "+c.structure.error);check(counts.keySet().stream().allMatch(i->p.getInventory().countItem(i)==0),"Solar construction did not debit exact materials");
            SolarMenu.open(p,c,c.getBlockPos());check(p.containerMenu instanceof SolarMenu&&p.containerMenu.stillValid(p),"Solar menu inaccessible");
            var menu=(SolarMenu)p.containerMenu;check(menu.clickMenuButton(p,150)&&c.load==50&&!menu.clickMenuButton(p,201),"Solar numeric load validation failed");
            var remote=c.structure.at(7,7,7);p.setPos(remote.above().getCenter());((SolarPart)h.getLevel().getBlockEntity(remote)).open(p);check(p.containerMenu instanceof SolarMenu&&p.containerMenu.stillValid(p),"Far solar support could not keep the main GUI open");
        }finally{ReactorTests.close(p);}
        var in=c.structure.ports.stream().filter(part->part.kind()==SolarBlock.Kind.ENERGY&&!part.output()).findFirst().orElseThrow();var input=new SolarPorts.Energy(in,side(c,in));long offered=1_000_000;
        check(input.insertEnergy(0,offered,Action.SIMULATE)==0&&c.stored==0,"Solar input simulation changed buffer");check(input.insertEnergy(0,offered,Action.EXECUTE)==0&&c.stored==offered,"Real solar input failed");
        var out=c.structure.ports.stream().filter(part->part.kind()==SolarBlock.Kind.ENERGY&&part.output()).findFirst().orElseThrow();var target=out.getBlockPos().relative(side(c,out));h.getLevel().setBlockAndUpdate(target,MekanismBlocks.BASIC_ENERGY_CUBE.get().defaultBlockState());
        var cube=(mekanism.common.tile.TileEntityEnergyCube)h.getLevel().getBlockEntity(target);var config=cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);for(var face:RelativeSide.values())config.setDataType(mekanism.common.tile.component.config.DataType.INPUT,face);config.setEjecting(false);h.getLevel().invalidateCapabilities(target);
        c.ignited=true;c.stored=c.reserve()+offered;c.autoEject=true;
        h.startSequence().thenWaitUntil(()->check(cube.getEnergyContainers(null).getFirst().getEnergy()==offered&&c.stored==c.reserve(),"Solar did not feed real energy cube without loss")).thenExecute(()->c.autoEject=false).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void nativePressurizedChamberProducesStellarFuel(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(6,3,6));h.getLevel().setBlockAndUpdate(pos,MekanismBlocks.PRESSURIZED_REACTION_CHAMBER.get().defaultBlockState());
        var machine=(mekanism.common.tile.machine.TileEntityPressurizedReactionChamber)h.getLevel().getBlockEntity(pos);
        machine.getComponent().addUpgrades(Upgrade.SPEED,8);machine.getComponent().addUpgrades(Upgrade.ENERGY,8);
        var input=machine.getInventorySlots(null).stream().filter(s->s instanceof mekanism.common.inventory.slot.InputInventorySlot).findFirst().orElseThrow();
        input.setStack(new ItemStack(SolarContent.PREFORM.get()));machine.inputFluidTank.setStack(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
        var chemical=mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanismgenerators","fusion_fuel"));machine.inputGasTank.setStack(new mekanism.api.chemical.ChemicalStack(chemical,8000));
        h.onEachTick(()->machine.getEnergyContainer().setEnergy(machine.getEnergyContainer().getMaxEnergy()));
        h.startSequence().thenWaitUntil(()->check(machine.getInventorySlots(null).stream().anyMatch(s->s.getStack().is(SolarContent.FUEL.get())),"Native PRC pending: "+machine.getOperatingTicks()+"/"+machine.getTicksRequired()+", energy "+machine.getEnergyContainer().getEnergy()+", recipe "+(machine.getRecipe(0)!=null)))
          .thenExecute(()->{check(input.isEmpty()&&machine.inputFluidTank.isEmpty()&&machine.inputGasTank.isEmpty(),"Native fuel recipe consumed incorrect ingredient quantities");h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void solarCollectorsSnapAtEveryRotationAndRefreshIncompleteLayouts(GameTestHelper h){
        SolarController current=null;
        for(var facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var c=create(h);h.getLevel().setBlockAndUpdate(c.getBlockPos(),Attribute.setFacing(c.getBlockState(),facing));c=(SolarController)h.getLevel().getBlockEntity(c.getBlockPos());
            check(!c.structure.valid(),"Empty solar unexpectedly formed");var p=ReactorTests.player(h,c.getBlockPos().above(12));
            try{
                for(var slot:SolarLayout.SLOTS)if(slot.kind()==SolarBlock.Kind.COLLECTOR){var pos=c.structure.at(slot.x(),slot.y(),slot.z());
                    p.setPos(pos.above(2).getCenter());p.setXRot(slot.y()%2==0?89:-89);p.setYRot(137);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SolarContent.block(SolarBlock.Kind.COLLECTOR,0).get()));
                    p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
                    var state=h.getLevel().getBlockState(pos);check(state.getBlock() instanceof SolarBlock,"Real collector placement failed");
                    check(state.getValue(SolarBlock.FACING)==c.structure.direction(slot.face()),"Collector used player pitch instead of its wing position");
                    check(state==c.structure.layoutState(state,slot),"Collector segment was not assigned on placement");
                }
                var outside=c.getBlockPos().above(12);p.setPos(outside.above(2).getCenter());p.setXRot(89);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SolarContent.block(SolarBlock.Kind.COLLECTOR,0).get()));
                p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(outside.getCenter(),Direction.UP,outside,false)));
                check(h.getLevel().getBlockState(outside).getValue(SolarBlock.FACING).getAxis().isHorizontal(),"Standalone collector tilted vertically");h.getLevel().setBlockAndUpdate(outside,Blocks.AIR.defaultBlockState());
            }finally{ReactorTests.close(p);}
            if(facing!=Direction.WEST){for(var pos:SolarConstruction.plan(c).keySet())h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(c.getBlockPos(),Blocks.AIR.defaultBlockState());}else current=c;
        }
        var c=current;var far=c.structure.at(8,3,4);var missing=c.structure.at(0,0,2);
        // No manual validate() after these changes: real controller ticks must refresh even
        // while an earlier, unrelated missing base stops the structure scan.
        h.getLevel().setBlockAndUpdate(far,Blocks.AIR.defaultBlockState());check(!c.structure.valid(),"Unfinished sun formed");
        h.getLevel().setBlockAndUpdate(far,SolarContent.block(SolarBlock.Kind.COLLECTOR,0).get().defaultBlockState().setValue(SolarBlock.FACING,Direction.UP));
        h.startSequence().thenIdle(2).thenExecute(()->{
            var part=(SolarPart)h.getLevel().getBlockEntity(far);check(c.getBlockPos().equals(part.master)&&part.getBlockState().getValue(SolarBlock.FACING).getAxis().isHorizontal(),"Unwatched far wing waited for periodic rescan");
            SolarConstruction.plan(c).forEach((pos,state)->{if(!pos.equals(missing))h.getLevel().setBlockAndUpdate(pos,state);});
        }).thenIdle(2).thenExecute(()->{
            check(!c.structure.formed,"Missing base accepted");h.getLevel().setBlockAndUpdate(missing,SolarContent.block(SolarBlock.Kind.BASE,0).get().defaultBlockState());
        }).thenIdle(2).thenExecute(()->{
            check(c.structure.formed,"Last block did not form on the next controller tick");
            for(var part:c.structure.parts)check(part.getBlockState().getValue(SolarBlock.FORMED),"Part appearance lagged behind formation");
            h.getLevel().setBlockAndUpdate(missing,Blocks.AIR.defaultBlockState());check(!c.structure.formed,"Broken solar appearance was not invalidated immediately");
        }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void solarPortConfiguratorKeepsModeThroughDropAndPlacement(GameTestHelper h){var c=formed(h);var p=ReactorTests.player(h,c.getBlockPos().north());
        try{
            var port=c.structure.ports.stream().filter(part->part.kind()==SolarBlock.Kind.ENERGY&&part.output()).findFirst().orElseThrow();var pos=port.getBlockPos();var face=side(c,port);var cached=new SolarPorts.Energy(port,face);
            c.ignited=true;c.stored=c.reserve()+1_000_000;p.setPos(pos.relative(face).getCenter());p.setShiftKeyDown(true);var tool=new ItemStack(MekanismItems.CONFIGURATOR.get());p.setItemInHand(InteractionHand.MAIN_HAND,tool);
            p.gameMode.useItemOn(p,h.getLevel(),tool,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),face,pos,false));
            check(!port.output()&&cached.extractEnergy(0,100,Action.EXECUTE)==0,"Native configurator did not change the real solar output to input");
            check(cached.insertEnergy(0,100,Action.EXECUTE)==0,"Reconfigured port failed to accept energy");
            p.gameMode.useItemOn(p,h.getLevel(),tool,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),face,pos,false));
            check(port.output()&&cached.extractEnergy(0,100,Action.EXECUTE)==100,"Port output state and real transfer disagreed");
            var drop=net.minecraft.world.level.block.Block.getDrops(port.getBlockState(),h.getLevel(),pos,port).getFirst();check(drop.get(SolarContent.STOCK.get()).getBoolean("output"),"Output item lost its mode/model property");
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,drop);
            drop.useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
            var restored=(SolarPart)h.getLevel().getBlockEntity(pos);check(restored!=null&&restored.output()&&restored.getBlockState().getValue(SolarBlock.FACING)==face&&c.structure.valid(),"Dropped port did not restore mode, appearance and outward connection");
        }finally{c.enabled=false;ReactorTests.close(p);}h.succeed();
    }
}
