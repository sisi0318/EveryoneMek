package dev.everyonemek.oritech;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import rearth.oritech.init.*;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;

@GameTestHolder(Content.ID)
@PrefixGameTestTemplate(false)
public final class ProcessorGameTests {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static Processor place(GameTestHelper h,Profiles profile,Direction facing){var pos=new BlockPos(6,3,6);h.setBlock(pos,Content.BLOCK.get().defaultBlockState().setValue(ProcessorBlock.FACING,facing));var p=(Processor)h.getBlockEntity(pos);
        p.inventory.setItem(Processor.HOST,new ItemStack(profile.block()));p.eject=false;p.refreshEquipment();tick(p);return p;}
    private static void tick(Processor p){p.serverTick(p.getLevel(),p.getBlockPos(),p.getBlockState(),p);}
    private static void clear(Processor p){p.getLevel().removeBlock(p.getBlockPos(),false);for(var drop:p.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(p.getBlockPos()).inflate(4)))drop.discard();}
    private static ServerPlayer player(GameTestHelper h,Processor p){var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"oritech-test"),ClientInformation.createDefault());
        var connection=new Connection(PacketFlow.SERVERBOUND){private final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();@Override public io.netty.channel.Channel channel(){return channel;}};
        player.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false)){
            @Override public void send(Packet<?> packet){} @Override public void send(Packet<?> packet,PacketSendListener listener){}};
        player.setPos(p.getBlockPos().getCenter().add(0,0,3));return player;}
    private static void close(ServerPlayer p){p.closeContainer();p.connection.getConnection().channel().close();}
    private static void install(Processor p,net.minecraft.world.level.block.Block addon,int slot){p.inventory.setItem(slot,new ItemStack(addon));p.refreshEquipment();tick(p);}
    private static void supply(Processor p){
        if(p.profile()==Profiles.FURNACE){p.inventory.setItem(0,new ItemStack(Items.RAW_IRON,8));return;}
        if(p.profile()==Profiles.CENTRIFUGE)install(p,BlockContent.MACHINE_FLUID_ADDON,9);
        if(p.profile()==Profiles.REFINERY){install(p,BlockContent.REFINERY_MODULE_BLOCK,9);install(p,BlockContent.REFINERY_MODULE_BLOCK,10);}
        var type=p.profile().recipes;
        for(var holder:p.getLevel().getRecipeManager().getAllRecipesFor(type).stream().sorted(Comparator.comparingInt(r->r.value().getTime())).toList()){
            var recipe=holder.value();if(recipe.getTime()<=0||recipe.getInputs().size()>p.profile().inputs()||recipe.getResults().isEmpty())continue;
            if(p.profile()==Profiles.ATOMIC&&recipe.getTime()<=1)continue;
            if(recipe.getInputs().stream().anyMatch(i->i.getItems().length==0))continue;
            if(recipe.getFluidInput().amount()>8000)continue;
            var fluids=recipe.getFluidInput().amount()>0?recipe.getFluidInput().getFluidStacks():List.<FluidStack>of();if(recipe.getFluidInput().amount()>0&&fluids.isEmpty())continue;
            if(recipe.getFluidOutputs().stream().anyMatch(f->f.getAmount()>4000))continue;
            for(int i=0;i<4;i++)p.inventory.setItem(i,ItemStack.EMPTY);
            for(int i=0;i<recipe.getInputs().size();i++)p.inventory.setItem(i,recipe.getInputs().get(i).getItems()[0].copyWithCount(8));
            if(!fluids.isEmpty())p.fluidIn.setStack(fluids.getFirst().copyWithAmount(8000));
            if(Engine.find(p)!=null)return;
        }throw new AssertionError("No usable native recipe for "+p.profile());
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void nativeLayoutsDeployAllProfilesInFourDirections(GameTestHelper h){
        for(var profile:Profiles.values())if(profile!=Profiles.EMPTY)for(var facing:Direction.Plane.HORIZONTAL){var p=place(h,profile,facing);try{
            check(p.complete(),"Deployment failed: "+profile+" "+facing);check(p.getGuiSlots().size()==profile.metadata().getGuiSlots().size(),"Native slot count changed");
            for(int i=0;i<p.getGuiSlots().size();i++){var a=p.getGuiSlots().get(i);var b=profile.metadata().getGuiSlots().get(i);check(a.x()==b.x()&&a.y()==b.y()&&a.output()==b.output(),"Native GUI geometry changed");}
            for(var cell:p.positions())check(h.getLevel().getBlockEntity(cell) instanceof Part part&&part.controller()==p,"Part ownership invalid");
        }finally{clear(p);}}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void nativeRecipesConserveMaterialsAndPowerForAllNineProfiles(GameTestHelper h){
        for(var profile:Profiles.values())if(profile!=Profiles.EMPTY){var p=place(h,profile,Direction.NORTH);try{
            supply(p);var recipe=Engine.find(p);check(recipe!=null,"Native recipe missing: "+profile);var beforeInputs=new ArrayList<Integer>();for(int i=0;i<4;i++)beforeInputs.add(p.inventory.getItem(i).getCount());
            long beforeFluid=p.fluidIn.getAmount(),power=100_000_000;p.energyStorage.amount=power;
            long expected=profile==Profiles.ATOMIC?recipe.budget():recipe.perTick()*recipe.duration();
            int ticks=profile==Profiles.ATOMIC?(int)((recipe.budget()+recipe.perTick()-1)/recipe.perTick()):recipe.duration();check(ticks<20000,"Fixture recipe too slow");
            for(int n=0;n<ticks;n++)tick(p);
            check(power-p.energyStorage.amount==expected,"Energy mismatch for "+profile+": "+(power-p.energyStorage.amount)+" / "+expected);
            int consumed=0;for(int i=0;i<4;i++)consumed+=beforeInputs.get(i)-p.inventory.getItem(i).getCount();check(consumed==recipe.inputs().size(),"Input consumption mismatch: "+profile);
            check(beforeFluid-p.fluidIn.getAmount()==recipe.fluid().amount(),"Fluid input mismatch: "+profile);
            for(var result:recipe.items()){int actual=0;for(int i=4;i<8;i++)if(ItemStack.isSameItemSameComponents(p.inventory.getItem(i),result))actual+=p.inventory.getItem(i).getCount();check(actual==result.getCount(),"Output missing: "+profile+" "+result);}
            check(p.paid==0&&p.progress==0,"Completed job retained progress/escrow");
        }finally{clear(p);}}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void loaderInstallsNativeAndThingsAddonsWithAutomaticItemRoles(GameTestHelper h){var p=place(h,Profiles.FURNACE,Direction.WEST);var player=player(h,p);try{
        var menu=new ProcessorMenu(3,player.getInventory(),p);player.containerMenu=menu;
        check(menu.clickMenuButton(player,ProcessorMenu.OPEN_ADDONS)&&player.containerMenu instanceof AddonMenu,"Addon manager did not open from the real machine menu");
        var manager=(AddonMenu)player.containerMenu;manager.setCarried(new ItemStack(BlockContent.MACHINE_SPEED_ADDON));manager.clicked(0,0,ClickType.PICKUP,player);p.loadAddons();
        check(manager.getCarried().isEmpty()&&p.getBaseAddonData().speed()<1,"Load slot did not install/apply addon");
        check(manager.clickMenuButton(player,0)&&player.containerMenu instanceof ProcessorMenu,"Return to native machine menu failed");menu=(ProcessorMenu)player.containerMenu;
        var handler=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p.getBlockPos(),Direction.WEST);check(handler!=null&&handler.insertItem(0,new ItemStack(Items.RAW_IRON,2),false).isEmpty(),"Initial input failed");
        check(handler.insertItem(0,new ItemStack(Items.RAW_IRON,3),false).isEmpty()&&p.inventory.getItem(0).getCount()==5,"Stack refill failed");
        check(!menu.clickMenuButton(player,0),"Obsolete side-control packet was still accepted");
        for(var side:Direction.values()){var auto=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p.getBlockPos(),side);
            check(auto.insertItem(0,new ItemStack(Items.RAW_IRON),true).isEmpty()&&auto.insertItem(4,new ItemStack(Items.IRON_INGOT),true).getCount()==1&&auto.extractItem(0,1,true).isEmpty(),"Automatic input/output roles failed on "+side);
            var energy=h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,p.getBlockPos(),side);check(energy.receiveEnergy(1,true)==1,"Automatic energy input failed on "+side);
        }
        p.inventory.setItem(4,new ItemStack(Items.IRON_INGOT,2));check(handler.extractItem(4,2,true).getCount()==2&&p.inventory.getItem(4).getCount()==2,"SIMULATE mutated output");
        check(!menu.slots.get(menu.sideStart).mayPickup(player),"Host could be removed with materials present");
        var things=BuiltInRegistries.BLOCK.stream().filter(b->BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals("oritechthings")&&Processor.validAddon(new ItemStack(b))).toList();
        if(net.neoforged.fml.ModList.get().isLoaded("oritechthings"))check(things.size()==48,"Things loaded without all 6 × 8 processing addons");
        for(var b:things){p.inventory.setItem(9,new ItemStack(b));p.refreshEquipment();var a=((MachineAddonBlock)b).getAddonSettings();float expected=1/(2-a.speedMultiplier());var actual=p.getBaseAddonData();
            check(Math.abs(actual.speed()-expected)<.00001,"Higher addon speed was clamped/guessed: "+BuiltInRegistries.BLOCK.getKey(b));
            check(Math.abs(actual.efficiency()-(a.efficiencyMultiplier()>1?a.efficiencyMultiplier():1/(2-a.efficiencyMultiplier())))<.00001,"Higher addon efficiency mismatch");
            check(actual.extraChambers()==a.chamberCount()&&p.energyStorage.capacity==p.getDefaultCapacity()+a.addedCapacity()&&p.energyStorage.maxInsert==p.getDefaultInsertRate()+a.addedInsert(),"Higher addon capacity/rate/chambers mismatch");
            if(a.speedMultiplier()<1)check(Engine.find(p).duration()<100,"Higher speed addon did not shorten real recipe");
        }
        p.inventory.setItem(9,ItemStack.EMPTY);install(p,BlockContent.MACHINE_CAPACITOR_ADDON,9);long capacity=p.energyStorage.capacity;p.energyStorage.amount=capacity;p.inventory.setItem(9,ItemStack.EMPTY);p.refreshEquipment();
        check(p.energyStorage.amount==capacity&&new Ports.Energy(()->p,Direction.UP).receiveEnergy(100,false)==0,"Removing capacitor deleted excess power or accepted beyond capacity");
        for(int i=0;i<8;i++)p.inventory.setItem(i,ItemStack.EMPTY);menu.clicked(menu.sideStart,0,ClickType.PICKUP,player);
        check(!menu.slots.get(0).mayPlace(new ItemStack(Items.RAW_IRON))&&handler.insertItem(0,new ItemStack(Items.RAW_IRON),false).getCount()==1,"Old menu/handler accepted hidden materials after removing the host");menu.setCarried(ItemStack.EMPTY);
    }finally{close(player);clear(p);}h.succeed();}
    @GameTest(template="empty",timeoutTicks=50)
    public static void blockedDeploymentAndOutputAreAtomicAndStalePortsStop(GameTestHelper h){var center=new BlockPos(6,3,6);h.setBlock(center,Content.BLOCK.get());var p=(Processor)h.getBlockEntity(center);p.inventory.setItem(8,new ItemStack(Profiles.ASSEMBLER.block()));p.refreshEquipment();
        var blocked=p.positions().getFirst();h.getLevel().setBlockAndUpdate(blocked,Blocks.DIAMOND_BLOCK.defaultBlockState());tick(p);
        check(!p.complete()&&h.getLevel().getBlockState(blocked).is(Blocks.DIAMOND_BLOCK),"Deployment overwrote obstruction");h.getLevel().removeBlock(blocked,false);tick(p);check(p.complete(),"Cleared deployment did not recover");
        supply(p);p.energyStorage.amount=1_000_000;tick(p);int progress=p.progress;long stored=p.energyStorage.amount;
        for(int i=4;i<8;i++)p.inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));tick(p);check(p.progress==progress&&p.energyStorage.amount==stored,"Blocked output consumed power/reset progress");
        var part=p.positions().getFirst();var items=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,part,Direction.UP);var energy=h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,part,Direction.UP);
        check(items!=null&&energy!=null,"Part capabilities missing");h.getLevel().destroyBlock(part,true);check(h.getLevel().getBlockEntity(p.getBlockPos())==null,"Breaking a deployed part did not break its processor");
        check(items.extractItem(4,1,false).isEmpty()&&energy.receiveEnergy(100,false)==0,"Stale part handler still transfers resources");clear(p);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void saveAndDroppedItemRetainInventoryFluidsUpgradesAndPaidWork(GameTestHelper h){var p=place(h,Profiles.ATOMIC,Direction.NORTH);supply(p);p.energyStorage.amount=100_000;tick(p);check(p.paid>0,"Escrow fixture completed too early");var saved=p.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=new Processor(p.getBlockPos(),p.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);
        long paid=restored.paid;long amount=restored.energyStorage.amount;restored.refreshEquipment();check(restored.paid==paid&&restored.energyStorage.amount==amount,"Reload refunded/reset paid work");
        check(restored.identity.equals(p.identity)&&restored.complete(),"Reload lost deployed ownership");
        var drops=Block.getDrops(restored.getBlockState(),h.getLevel(),restored.getBlockPos(),restored);check(drops.size()==1&&drops.getFirst().is(Content.ITEM.get()),"Processor loot missing/duplicated");var stack=drops.getFirst();
        check(stack.has(Content.DATA.get()),"Drop omitted processor attachment");var data=stack.get(Content.DATA.get());check(data.getLong("paid")==paid&&data.getLong("oritech.machine_energy")==amount,"Drop omitted energy/paid budget");
        clear(restored);var place=new BlockPos(6,3,6);h.setBlock(place.below(),Blocks.STONE);var player=player(h,restored);try{
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);var hit=new BlockHitResult(h.absolutePos(place.below()).getCenter().add(0,.5,0),Direction.UP,h.absolutePos(place.below()),false);
            var result=((BlockItem)stack.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,hit));check(result.consumesAction(),"Saved item failed placement");
            var placed=(Processor)h.getBlockEntity(place);placed.refreshEquipment();check(placed.paid==paid&&placed.energyStorage.amount==amount,"Placed item lost escrow/power");check(!placed.identity.equals(p.identity),"Placed item reused deployment identity");clear(placed);
        }finally{close(player);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=300)
    public static void realHopperFeedsAndAutomaticOutputEjectsWhileServerTicks(GameTestHelper h){var p=place(h,Profiles.FURNACE,Direction.NORTH);p.eject=true;
        // The furnace has an upper occupied core; choose the north controller face for the chest.
        var box=p.getBlockPos().north();h.getLevel().setBlockAndUpdate(box,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(box);
        var hopperPos=p.getBlockPos().south();h.getLevel().setBlockAndUpdate(hopperPos,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.NORTH));var hopper=(HopperBlockEntity)h.getLevel().getBlockEntity(hopperPos);hopper.setItem(0,new ItemStack(Items.RAW_IRON,4));
        p.energyStorage.amount=500_000;
        h.startSequence().thenIdle(35).thenExecute(()->check(p.inventory.getItem(0).getCount()>=2,"Real hopper did not refill existing stack"))
            .thenIdle(90).thenExecute(()->{check(chest.getItem(0).is(Items.IRON_INGOT),"Auto eject did not reach chest");check(hopper.getItem(0).isEmpty(),"Hopper input did not finish refilling");for(int i=0;i<chest.getContainerSize();i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));})
            .thenIdle(110).thenExecute(()->{try{check(!p.inventory.getItem(4).isEmpty()&&hopper.getItem(0).isEmpty(),"Auto eject fed products back into the input hopper");}finally{h.getLevel().removeBlock(hopperPos,false);h.getLevel().removeBlock(box,false);clear(p);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void functionalAddonsPreserveNativeRefineryRulesAndGuardFluidClicks(GameTestHelper h){var p=place(h,Profiles.REFINERY,Direction.NORTH);try{
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(rearth.oritech.init.recipes.RecipeContent.REFINERY).stream()
            .map(RecipeHolder::value).filter(r->r.getFluidOutputs().size()>=2&&r.getInputs().size()<=1&&r.getInputs().stream().allMatch(i->i.getItems().length>0)&&r.getFluidInput().amount()<=8000&&r.getFluidInput().amount()>0&&!r.getFluidInput().getFluidStacks().isEmpty()).findFirst().orElseThrow();
        if(!recipe.getInputs().isEmpty())p.inventory.setItem(0,recipe.getInputs().getFirst().getItems()[0].copyWithCount(8));p.fluidIn.setStack(recipe.getFluidInput().getFluidStacks().getFirst().copyWithAmount(8000));
        var zero=Engine.find(p);install(p,BlockContent.REFINERY_MODULE_BLOCK,9);var one=Engine.find(p);install(p,BlockContent.REFINERY_MODULE_BLOCK,10);var two=Engine.find(p);
        check(two!=null&&two.fluids().size()>=2,"Refinery fixture needs multiple outputs");
        check(zero.fluids().size()==1&&zero.fluids().getFirst().getAmount()==two.fluids().getFirst().getAmount()*2,"Zero-module refinery did not double A");
        check(one.fluids().size()==2&&one.fluids().get(1).getAmount()==two.fluids().get(1).getAmount()*2,"One-module refinery did not double B");
        if(!two.items().isEmpty())check(zero.items().getFirst().getCount()==two.items().getFirst().getCount()*2,"Zero-module item multiplier missing");
        long input=p.fluidIn.getAmount();p.fluidA.setStack(two.fluids().getFirst().copyWithAmount(8000));check(!Engine.canOutput(p,two)&&p.fluidIn.getAmount()==input,"Full output tank consumed fluid");
    }finally{clear(p);}
        p=place(h,Profiles.CENTRIFUGE,Direction.NORTH);var player=player(h,p);try{
            var port=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,p.getBlockPos(),Direction.UP);var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
            check(port.fill(water,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==0,"Fluid mode was free");install(p,BlockContent.MACHINE_FLUID_ADDON,9);
            check(port.fill(water,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==1000,"Fluid addon did not enable real tank");
            var menu=new ProcessorMenu(5,player.getInventory(),p);player.containerMenu=menu;menu.setCarried(new ItemStack(Items.BUCKET));player.setPos(player.position().add(100,0,0));
            var packet=new rearth.oritech.client.ui.OritechScreenHandler.FluidContainerInteractionPacket(p.getBlockPos(),0,true);
            rearth.oritech.client.ui.OritechScreenHandler.handleFluidContainerInteraction(packet,player,h.getLevel().registryAccess());check(p.fluidIn.getAmount()==1000&&menu.getCarried().is(Items.BUCKET),"Out-of-range fluid packet transferred resources");
            player.setPos(p.getBlockPos().getCenter().add(0,0,3));rearth.oritech.client.ui.OritechScreenHandler.handleFluidContainerInteraction(packet,player,h.getLevel().registryAccess());
            check(p.fluidIn.getAmount()==0&&menu.getCarried().is(Items.WATER_BUCKET),"Native GUI fluid click did not use actual tank");menu.setCarried(ItemStack.EMPTY);
            var combo=new ItemStack(BlockContent.MACHINE_COMBI_ADDON);var data=new rearth.oritech.util.MachineAddonController.BaseAddonData(.2F,.4F,1234,99,2,20);
            combo.set(ComponentContent.ADDON_DATA.get(),new rearth.oritech.block.entity.interaction.ShrinkerBlockEntity.ShrunkAddonData(data,true,0,1,false,false));p.inventory.setItem(9,combo);p.refreshEquipment();
            check(p.getBaseAddonData().equals(data)&&p.fluidAddon&&p.yieldAddon,"Combined addon lost stored native parameters/features");
            check(!p.canInstallAddon(new ItemStack(BlockContent.MACHINE_SPEED_ADDON),10),"Combined addon mixed with another stat addon");
        }finally{close(player);clear(p);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void qualityUpgradesConsumeOneKeepWorkAndExpandInstalledCapacity(GameTestHelper h){var p=place(h,Profiles.FURNACE,Direction.NORTH);var player=player(h,p);try{
        p.inventory.setItem(0,new ItemStack(Items.RAW_IRON,8));p.energyStorage.amount=500_000;tick(p);int progress=p.progress;long energy=p.energyStorage.amount;var identity=p.identity;
        for(int tier=2;tier<=Processor.MAX_TIER;tier++){
            var item=Content.UPGRADES.get(tier-2).get();var held=new ItemStack(item,2);player.setItemInHand(InteractionHand.MAIN_HAND,held);
            var hit=new BlockHitResult(p.getBlockPos().getCenter(),Direction.NORTH,p.getBlockPos(),false);
            var result=p.getBlockState().useItemOn(held,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
            check(result.consumesAction()&&p.tier==tier&&p.addonSlots()==tier*9&&held.getCount()==1,"Real held upgrade failed at tier "+tier);
            p.getBlockState().useItemOn(held,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);check(held.getCount()==1,"Same-tier upgrade consumed another item");
            check(p.progress==progress&&p.energyStorage.amount==energy&&p.identity.equals(identity)&&p.complete(),"Capacity upgrade changed work/resources/deployment");
            var recipe=h.getLevel().getRecipeManager().byKey(Content.id("capacity_upgrade_"+tier));check(recipe.isPresent(),"Upgrade recipe missing");
        }
        var named=new ItemStack(BlockContent.MACHINE_SPEED_ADDON,64);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("kept-components"));
        var menu=new AddonMenu(8,player.getInventory(),p);player.containerMenu=menu;menu.setCarried(named);menu.clicked(0,0,ClickType.PICKUP,player);p.loadAddons();
        check(p.countAddons()==63&&p.inventory.getItem(Processor.LOAD_SLOT).getCount()==1,"Tier 7 did not install exactly 63 and retain remainder");
        check(p.getBaseAddonData().speed()<.04F,"Hidden installed addons did not affect native stats");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(AddonMenu.HIDDEN_START,0,ClickType.PICKUP,player);check(menu.getCarried().isEmpty()&&p.countAddons()==63,"Hidden installed slot could be picked up");
        var saved=p.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new Processor(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(copy);p=copy;
        check(p.tier==7&&p.countAddons()==63&&p.inventory.getItem(Processor.LOAD_SLOT).getCount()==1,"Expanded inventory failed NBT round trip");
        var data=Block.getDrops(p.getBlockState(),h.getLevel(),p.getBlockPos(),p).getFirst().get(Content.DATA.get());check(data.getInt("tier")==7&&data.getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND).size()==66,"Drop omitted expanded inventory/queued addon");
    }finally{close(player);clear(p);}h.succeed();}
    @GameTest(template="empty",timeoutTicks=60)
    public static void legacyNineSlotsMigrateAndUninstallPacketsConserveRealItems(GameTestHelper h){var p=place(h,Profiles.FURNACE,Direction.NORTH);var player=player(h,p);try{
        var named=new ItemStack(BlockContent.MACHINE_SPEED_ADDON);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("legacy-addon"));
        for(int i=9;i<18;i++)p.inventory.setItem(i,named.copy());p.refreshEquipment();p.energyStorage.amount=42000;p.progress=4;p.eject=false;
        var old=p.saveWithFullMetadata(h.getLevel().registryAccess());old.remove("tier");old.putIntArray("sides",new int[]{3,3,3,3,3,3});
        var restored=new Processor(p.getBlockPos(),p.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(old,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);p=restored;p.refreshEquipment();
        check(p.tier==1&&p.addonSlots()==9&&p.countAddons()==9&&p.energyStorage.amount==42000&&p.progress==4&&!p.eject,"Legacy migration changed saved resources/settings/work");
        for(var side:Direction.values())check(h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,p.getBlockPos(),side).receiveEnergy(1,true)==1,"Legacy face setting blocked automatic input");
        var menu=new AddonMenu(12,player.getInventory(),p);player.containerMenu=menu;
        var wrong=new AddonPackets.Unload(13,named.copy(),true);check(AddonPackets.handle(wrong,player)==0,"Wrong menu ID uninstalled addons");
        var mismatch=named.copy();mismatch.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("different"));check(AddonPackets.handle(new AddonPackets.Unload(12,mismatch,true),player)==0,"Component-mismatched request removed a different item");
        var packet=new AddonPackets.Unload(12,named.copy(),false);player.setPos(player.position().add(100,0,0));check(AddonPackets.handle(packet,player)==0,"Distant player uninstalled addons");player.setPos(p.getBlockPos().getCenter().add(0,0,3));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SPECTATOR);check(AddonPackets.handle(packet,player)==0,"Spectator uninstalled addons");player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var bytes=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());AddonPackets.Unload.CODEC.encode(bytes,packet);var decoded=AddonPackets.Unload.CODEC.decode(bytes);bytes.release();
        check(AddonPackets.handle(decoded,player)==1&&p.countAddons()==8&&ItemStack.isSameItemSameComponents(p.inventory.getItem(Processor.UNLOAD_SLOT),named),"Uninstall did not return exact installed item");
        p.inventory.setItem(Processor.UNLOAD_SLOT,new ItemStack(Items.COBBLESTONE));check(AddonPackets.handle(decoded,player)==0&&p.countAddons()==8,"Blocked retrieval slot deleted installed item");p.inventory.setItem(Processor.UNLOAD_SLOT,ItemStack.EMPTY);
        check(AddonPackets.handle(new AddonPackets.Unload(12,named.copy(),true),player)==8&&p.countAddons()==0&&p.inventory.getItem(Processor.UNLOAD_SLOT).getCount()==8,"Batch uninstall duplicated/lost items");
        var taken=menu.quickMoveStack(player,1);check(!taken.isEmpty()&&p.inventory.getItem(Processor.UNLOAD_SLOT).isEmpty(),"Unloaded addons could not be retrieved");
        check(p.getBaseAddonData().speed()==1,"Uninstall left stale speed multiplier");
        player.getInventory().setItem(0,new ItemStack(Content.UPGRADES.get(1).get(),2));check(!menu.quickMoveStack(player,AddonMenu.PLAYER_START+27).isEmpty()&&p.tier==3&&player.getInventory().getItem(0).getCount()==1,"Shift-click capacity upgrade failed");
    }finally{close(player);clear(p);}h.succeed();}
}
