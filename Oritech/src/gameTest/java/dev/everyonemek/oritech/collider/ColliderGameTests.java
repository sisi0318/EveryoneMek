package dev.everyonemek.oritech.collider;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.everyonemek.oritech.Content;
import mekanism.api.RelativeSide;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import rearth.oritech.init.BlockContent;

@GameTestHolder(Content.ID)
@PrefixGameTestTemplate(false)
public final class ColliderGameTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static Collider place(GameTestHelper h,Direction facing){var pos=new BlockPos(5,3,5);h.setBlock(pos,Content.COLLIDER.get().defaultBlockState().setValue(ColliderBlock.FACING,facing));return (Collider)h.getBlockEntity(pos);}
    private static ServerPlayer player(GameTestHelper h,Collider p){var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"collider-test"),ClientInformation.createDefault());
        var connection=new Connection(PacketFlow.SERVERBOUND){private final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();@Override public io.netty.channel.Channel channel(){return channel;}};
        player.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false)){
            @Override public void send(Packet<?> packet){} @Override public void send(Packet<?> packet,PacketSendListener listener){}};
        player.setPos(p.getBlockPos().getCenter().add(0,0,3));player.containerMenu=new ColliderMenu(21,player.getInventory(),p);return player;
    }
    private static void close(ServerPlayer player){player.closeContainer();player.connection.getConnection().channel().close();}
    private static void clear(Collider p){p.getLevel().removeBlock(p.getBlockPos(),false);for(var drop:p.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(p.getBlockPos()).inflate(4)))drop.discard();}
    private static ColliderPackets.Control control(Collider p,int action,int cell,int direction,int bend,String recipe){return new ColliderPackets.Control(21,action,p.revision,cell,direction,bend,recipe);}
    private static Map<Integer,Track.Node> loop(){return loop(3,60);}
    private static Map<Integer,Track.Node> loop(int low,int high){var cells=new TreeMap<Integer,Track.Node>();
        for(int n=low+2;n<=high-2;n++){cells.put(Track.cell(n,low),new Track.Node(Track.MOTOR,0,0));cells.put(Track.cell(high,n),new Track.Node(Track.MOTOR,2,0));cells.put(Track.cell(n,high),new Track.Node(Track.MOTOR,4,0));cells.put(Track.cell(low,n),new Track.Node(Track.MOTOR,6,0));}
        int[][] turns={{high-2,low,0,2},{high-1,low+1,1,0},{high,low+2,6,1},{high,high-2,2,2},{high-1,high-1,3,0},{high-2,high,0,1},
            {low+2,high,4,2},{low+1,high-1,5,0},{low,high-2,2,1},{low,low+2,6,2},{low+1,low+1,7,0},{low+2,low,4,1}};
        for(var c:turns)cells.put(Track.cell(c[0],c[1]),new Track.Node(Track.RING,c[2],c[3]));
        cells.put(Track.cell(low+4,low),new Track.Node(Track.A,0,0));cells.put(Track.cell(high-4,low),new Track.Node(Track.B,4,0));return cells;
    }
    private static void build(Collider p,ServerPlayer player){build(p,player,loop());}
    private static void build(Collider p,ServerPlayer player,Map<Integer,Track.Node> cells){
        for(var entry:cells.entrySet())if(entry.getValue().kind()==Track.A||entry.getValue().kind()==Track.B)check(ColliderPackets.handle(control(p,entry.getValue().kind()==Track.A?6:7,entry.getKey(),entry.getValue().direction(),0,""),player),"Emitter move rejected");
        for(var entry:cells.entrySet()){var n=entry.getValue();if(n.kind()>=Track.A)continue;var item=new ItemStack(n.kind()==Track.MOTOR?BlockContent.ACCELERATOR_MOTOR:BlockContent.ACCELERATOR_RING);item.set(DataComponents.CUSTOM_NAME,Component.literal("kept-part"));p.inventory.setItem(Collider.PART_IN,item);
            check(ColliderPackets.paint(new ColliderPackets.Paint(21,p.revision,new int[]{entry.getKey()},n.direction(),n.bend(),false),player)==1,"Real packet did not install part");check(p.inventory.getItem(Collider.PART_IN).isEmpty(),"Placement failed to consume part");}
        check(p.plan().valid(),"Fixture did not form a valid real beamline: "+p.plan().fault());
    }
    private static ItemStack ingredient(Collider p,String name,int input,int amount){return p.recipes().stream().filter(r->r.id().equals(ResourceLocation.fromNamespaceAndPath("oritech","particle/"+name))).findFirst().orElseThrow().value().getInputs().get(input).getItems()[0].copyWithCount(amount);}

    @GameTest(template="empty",timeoutTicks=60)
    public static void editorLockingAndSixSidePacketsControlActualCapabilities(GameTestHelper h){
        for(var facing:Direction.Plane.HORIZONTAL){var p=place(h,facing);var player=player(h,p);try{
            var menu=(ColliderMenu)player.containerMenu;check(menu.getSlot(0).x==ColliderMenu.INPUT_A_X&&menu.getSlot(0).y==ColliderMenu.INPUT_Y&&menu.getSlot(ColliderMenu.PLAYER_START).y==ColliderMenu.INVENTORY_Y,"Screen/inventory coordinates disagree");
            check(p.parts.isEmpty()&&p.nodes().size()==2,"New collider came with free track parts");int pos=Track.cell(20,20);
            var ring=new ItemStack(BlockContent.ACCELERATOR_RING,3);ring.set(DataComponents.CUSTOM_NAME,Component.literal("named-ring"));menu.setCarried(ring.copy());menu.clicked(3,0,ClickType.PICKUP,player);
            var paint=new ColliderPackets.Paint(21,p.revision,new int[]{pos,pos,pos+1},0,0,false);check(ColliderPackets.paint(paint,player)==2&&p.inventory.getItem(Collider.PART_IN).getCount()==1,"Brush duplicated components or consumed duplicates twice");
            check(ColliderPackets.paint(paint,player)==0,"Stale edit revision was accepted");
            check(ColliderPackets.handle(control(p,5,pos,0,0,""),player)&&ItemStack.isSameItemSameComponents(p.inventory.getItem(Collider.PART_OUT),ring),"Removed component lost identity");
            check(!ColliderPackets.handle(control(p,5,p.emitterA,0,0,""),player)&&p.nodes().values().stream().filter(n->n.kind()==Track.A).count()==1,"Built-in emitter could be removed");
            var left=RelativeSide.LEFT.getDirection(facing);var port=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p.getBlockPos(),left);
            p.inventory.setItem(Collider.INPUT_A,ingredient(p,"enderic_compound",1,1));p.inventory.setItem(Collider.INPUT_B,ingredient(p,"enderic_compound",0,1));
            check(ColliderPackets.handle(control(p,10,0,0,0,""),player)&&p.lockedSwapped,"Lock detected did not preserve reversed A/B assignment");
            check(!port.isItemValid(0,ingredient(p,"enderic_compound",0,1))&&port.isItemValid(0,ingredient(p,"enderic_compound",1,1)),"Locked input accepts wrong ingredient");
            check(ColliderPackets.handle(control(p,3,RelativeSide.LEFT.ordinal(),Collider.CLOSED,0,""),player),"Side packet rejected");
            check(port.insertItem(0,ingredient(p,"enderic_compound",1,1),false).getCount()==1,"Cached handler ignored closed side");
            check(ColliderPackets.handle(control(p,3,RelativeSide.LEFT.ordinal(),Collider.FEED_BOTH,0,""),player),"Both-input side failed");
            p.inventory.setItem(0,ItemStack.EMPTY);p.inventory.setItem(1,ItemStack.EMPTY);check(ColliderPackets.handle(control(p,9,0,0,0,""),player),"Unlock failed");
            var coal=ingredient(p,"diamond",0,2);check(ItemHandlerHelper.insertItemStacked(port,coal.copy(),true).isEmpty()&&p.inventory.getItem(0).isEmpty()&&p.inventory.getItem(1).isEmpty(),"Simulated supply changed the inventory");
            check(ItemHandlerHelper.insertItemStacked(port,coal.copy(),false).isEmpty()&&p.inventory.getItem(0).getCount()==1&&p.inventory.getItem(1).getCount()==1,"Identical recipe ingredients all went into A");
            check(ItemHandlerHelper.insertItemStacked(port,coal.copyWithCount(4),false).isEmpty()&&p.inventory.getItem(0).getCount()==3&&p.inventory.getItem(1).getCount()==3,"Existing stacks did not refill evenly");
            var allowed=control(p,0,0,0,0,"");player.setPos(player.position().add(100,0,0));check(!ColliderPackets.handle(allowed,player),"Distant packet changed machine");player.setPos(p.getBlockPos().getCenter().add(0,0,3));
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SPECTATOR);check(!ColliderPackets.handle(allowed,player),"Spectator changed machine");player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var snapshot=p.view(true);var bytes=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{ColliderPackets.Snapshot.CODEC.encode(bytes,new ColliderPackets.Snapshot(21,snapshot));var restored=ColliderPackets.Snapshot.CODEC.decode(bytes);check(restored.data().equals(snapshot),"Client snapshot failed codec round-trip");}finally{bytes.release();}
            h.getLevel().destroyBlock(p.getBlockPos(),true);check(port.insertItem(0,coal.copy(),false).getCount()==2,"Removed machine's old capability still accepts items");
        }finally{close(player);clear(p);}}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void activeBeamAndRealComponentsSurviveSaveAndDroppedPlacement(GameTestHelper h){var p=place(h,Direction.NORTH);var player=player(h,p);try{
        build(p,player);var coal=ingredient(p,"diamond",0,1);p.inventory.setItem(0,coal.copy());p.inventory.setItem(1,coal.copy());p.energy=Collider.CAPACITY;
        check(ColliderPackets.handle(control(p,0,0,0,0,""),player),"Start packet failed");for(int i=0;i<25;i++)p.tick();check(p.busy()&&p.beam!=null&&p.collisionTicks==0,"Expected an in-flight beam: status="+p.status+", speed="+(p.beam==null?0:p.beam.speed)+", result="+p.inventory.getItem(Collider.OUTPUT));
        check(!ColliderPackets.handle(control(p,4,Track.cell(20,20),0,0,""),player),"Active beamline could be edited");long energy=p.energy,spent=p.spent,speed=p.beam.speed;double bend=p.beam.bendDistance;int count=p.parts.size();
        var saved=p.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new Collider(p.getBlockPos(),p.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);p=restored;
        check(p.busy()&&p.energy==energy&&p.spent==spent&&p.beam.speed==speed&&p.beam.bendDistance==bend&&p.parts.size()==count,"World reload changed paid work, beam or components");
        var drop=Block.getDrops(p.getBlockState(),h.getLevel(),p.getBlockPos(),p).getFirst();check(drop.has(Content.COLLIDER_DATA.get()),"Dropped collider has no persistent attachment");clear(p);
        var pos=new BlockPos(5,3,5);h.setBlock(pos.below(),Blocks.STONE);player.setItemInHand(InteractionHand.MAIN_HAND,drop);var hit=new BlockHitResult(h.absolutePos(pos.below()).getCenter().add(0,.5,0),Direction.UP,h.absolutePos(pos.below()),false);
        check(((BlockItem)drop.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,drop,hit)).consumesAction(),"Saved collider failed actual placement");p=(Collider)h.getBlockEntity(pos);
        check(p.parts.size()==count&&p.parts.values().stream().allMatch(part->part.item().has(DataComponents.CUSTOM_NAME))&&p.beam.speed==speed&&p.energy==energy&&p.spent==spent,"Item placement lost component data or resumed beam");
        for(int i=0;i<1600&&p.inventory.getItem(Collider.OUTPUT).isEmpty();i++){p.energy=Math.min(Collider.CAPACITY,p.energy+Collider.RECEIVE);p.tick();}
        check(p.inventory.getItem(Collider.OUTPUT).is(Items.DIAMOND)&&p.inventory.getItem(Collider.OUTPUT).getCount()==1&&!p.busy(),"Resumed physical collision did not produce exactly one native result");
        check(p.inventory.getItem(Collider.WORK_A).isEmpty()&&p.inventory.getItem(Collider.WORK_B).isEmpty()&&p.inventory.getItem(0).isEmpty()&&p.inventory.getItem(1).isEmpty(),"Completed collision duplicated reserved ingredients");
    }finally{close(player);clear(p);}h.succeed();}

    @GameTest(template="empty",timeoutTicks=1600)
    public static void realHoppersFeedDistinctInputsAndConfiguredOutputReachesChest(GameTestHelper h){var p=place(h,Direction.NORTH);var player=player(h,p);build(p,player);
        var a=p.getBlockPos().relative(RelativeSide.LEFT.getDirection(p.facing()));var b=p.getBlockPos().relative(RelativeSide.RIGHT.getDirection(p.facing()));var out=p.getBlockPos().north();
        h.getLevel().setBlockAndUpdate(a,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,RelativeSide.LEFT.getDirection(p.facing()).getOpposite()));
        h.getLevel().setBlockAndUpdate(b,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,RelativeSide.RIGHT.getDirection(p.facing()).getOpposite()));h.getLevel().setBlockAndUpdate(out,Blocks.CHEST.defaultBlockState());
        var hopperA=(HopperBlockEntity)h.getLevel().getBlockEntity(a);var hopperB=(HopperBlockEntity)h.getLevel().getBlockEntity(b);var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(out);
        hopperA.setItem(0,ingredient(p,"enderic_compound",0,4));hopperB.setItem(0,ingredient(p,"enderic_compound",1,4));p.energy=Collider.CAPACITY;
        check(ColliderPackets.handle(control(p,8,0,0,0,"oritech:particle/enderic_compound"),player),"Lock recipe packet failed");check(ColliderPackets.handle(control(p,0,0,0,0,""),player),"Start failed");close(player);
        h.startSequence().thenIdle(36).thenExecute(()->check(p.busy()&&p.inventory.getItem(0).getCount()>=2&&p.inventory.getItem(1).getCount()>=2,"Real hopper ticks did not refill existing A/B stacks"))
            .thenWaitUntil(()->{h.assertTrue(chest.getItem(0).getCount()>=2,"Waiting for two real collisions and automatic output; status="+p.status+", speed="+(p.beam==null?0:p.beam.speed));})
            .thenExecute(()->{try{var result=p.recipes().stream().filter(r->r.id().toString().equals("oritech:particle/enderic_compound")).findFirst().orElseThrow().value().getResults().getFirst();
                check(chest.getItem(0).is(result.getItem()),"Native collision result did not reach configured output");check(p.energy<Collider.CAPACITY,"Accelerator produced results without consuming energy");
            }finally{h.getLevel().removeBlock(a,false);h.getLevel().removeBlock(b,false);h.getLevel().removeBlock(out,false);clear(p);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void smartStrokePacketsAreAtomicAndKeepOldPartsAndEmitters(GameTestHelper h){var p=place(h,Direction.NORTH);var player=player(h,p);try{
        int[] stroke={Track.cell(2,2),Track.cell(3,2),Track.cell(4,2),Track.cell(4,3),Track.cell(4,4)};
        var material=new ItemStack(BlockContent.ACCELERATOR_RING,3);material.set(DataComponents.CUSTOM_NAME,Component.literal("smart-part"));p.inventory.setItem(Collider.PART_IN,material.copy());
        var request=new ColliderPackets.Paint(21,p.revision,stroke,0,0,false,true);var bytes=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        ColliderPackets.Paint decoded;try{ColliderPackets.Paint.CODEC.encode(bytes,request);decoded=ColliderPackets.Paint.CODEC.decode(bytes);}finally{bytes.release();}
        check(ColliderPackets.paint(decoded,player)==0&&p.parts.isEmpty()&&p.inventory.getItem(Collider.PART_IN).getCount()==3,"Insufficient smart stroke partially consumed materials");
        check(((ColliderMenu)player.containerMenu).editFailure==SmartTrack.Failure.NOT_ENOUGH_PARTS.ordinal(),"Rejected brush has no actionable response");
        p.inventory.setItem(Collider.PART_IN,material.copyWithCount(5));int a=p.emitterA,b=p.emitterB;
        check(ColliderPackets.paint(new ColliderPackets.Paint(21,p.revision,stroke,0,0,false,true),player)==4&&p.parts.size()==4&&p.inventory.getItem(Collider.PART_IN).getCount()==1,"Smart bevel did not commit exactly four paid cells");
        check(p.emitterA==a&&p.emitterB==b&&p.parts.values().stream().allMatch(part->part.item().has(DataComponents.CUSTOM_NAME)),"Smart brush changed emitters or lost components");
        check(!p.parts.containsKey(Track.cell(4,2))&&p.parts.get(Track.cell(3,2)).node().exit(0)==1&&p.parts.get(Track.cell(4,3)).node().exit(1)==2,"Smart brush produced an illegal sharp corner");
        var before=p.saveWithFullMetadata(h.getLevel().registryAccess());check(ColliderPackets.paint(decoded,player)==0&&p.saveWithFullMetadata(h.getLevel().registryAccess()).equals(before),"Stale smart packet changed the machine");
        var loaded=new Collider(p.getBlockPos(),p.getBlockState());loaded.setLevel(h.getLevel());loaded.loadWithComponents(before,h.getLevel().registryAccess());check(loaded.parts.size()==4&&loaded.emitterA==a,"Smart layout failed NBT round-trip");
    }finally{close(player);clear(p);}h.succeed();}

    @GameTest(template="empty",timeoutTicks=80)
    public static void optionalThingsMagnetUsesNativeUpgradesAndItsItemsStoredEnergy(GameTestHelper h){var p=place(h,Direction.NORTH);var player=player(h,p);try{
        if(!MagnetSupport.loaded()){
            check(p.magnet().equals(MagnetSupport.Settings.NONE)&&p.magneticEnergy()==0,"Absent optional mod enabled magnetic assistance");
            check(!ColliderPackets.handle(control(p,11,3,0,0,""),player),"Unavailable magnet page could be opened");h.succeed();return;
        }
        var field=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(MagnetSupport::isField).findFirst().orElseThrow();field.set(DataComponents.CUSTOM_NAME,Component.literal("saved-magnet"));MagnetSupport.energy(field,123456);
        var menu=(ColliderMenu)player.containerMenu;menu.setCarried(field.copy());menu.clicked(ColliderMenu.MAGNET_MENU,0,ClickType.PICKUP,player);
        check(p.inventory.getItem(Collider.MAGNET_SLOT).isEmpty()&&ItemStack.matches(menu.getCarried(),field),"Hidden magnet slot was interactive");menu.setCarried(ItemStack.EMPTY);
        check(ColliderPackets.handle(control(p,11,3,0,0,""),player),"Magnet page selection failed");menu.setCarried(field.copyWithCount(2));menu.clicked(ColliderMenu.MAGNET_MENU,0,ClickType.PICKUP,player);
        check(p.inventory.getItem(Collider.MAGNET_SLOT).getCount()==1&&menu.getCarried().getCount()==1,"More than one field was installed");menu.setCarried(ItemStack.EMPTY);
        check(!menu.getSlot(ColliderMenu.UPGRADE_MENU).mayPlace(new ItemStack(BlockContent.MACHINE_SPEED_ADDON)),"Field accepted an unsupported speed addon");
        menu.setCarried(new ItemStack(BlockContent.MACHINE_EFFICIENCY_ADDON));menu.clicked(ColliderMenu.UPGRADE_MENU,0,ClickType.PICKUP,player);
        menu.setCarried(new ItemStack(BlockContent.MACHINE_CAPACITOR_ADDON));menu.clicked(ColliderMenu.UPGRADE_MENU+1,0,ClickType.PICKUP,player);
        var advanced=net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream().filter(block->net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("oritechthings"))
            .filter(block->block instanceof rearth.oritech.block.blocks.addons.MachineAddonBlock addon&&addon.getAddonSettings().efficiencyMultiplier()!=1&&MagnetSupport.isUpgrade(new ItemStack(block))).findFirst().orElseThrow();
        menu.setCarried(new ItemStack(advanced));menu.clicked(ColliderMenu.UPGRADE_MENU+2,0,ClickType.PICKUP,player);
        var settings=p.magnet();check(settings.enabled()&&settings.efficiency()<1&&settings.capacity()>500000,"Native or Things efficiency/capacitor parameters were ignored");
        p.energy=1_000_000;long before=p.energy+p.magneticEnergy();p.tick();check(p.magneticEnergy()>123456&&p.energy+p.magneticEnergy()==before,"Charging created or deleted field energy");
        check(ColliderPackets.handle(control(p,11,0,0,0,""),player),"Could not return to beamline");build(p,player,loop(3,12));
        var noField=Track.preflight(p.plan(),Collider.rules(),500);check(!noField.valid()&&Track.preflight(p.plan(),Collider.rules(),500,p.magnet().physics()).valid(),"Native field did not make the tight ring viable");
        p.inventory.setItem(0,ingredient(p,"diamond",0,1));p.inventory.setItem(1,ingredient(p,"diamond",0,1));p.energy=Collider.CAPACITY;before=p.energy+p.magneticEnergy();
        check(ColliderPackets.handle(control(p,0,0,0,0,""),player),"Magnetic run did not start");
        for(int i=0;i<1500&&p.magneticSpent==0;i++)p.tick();check(p.busy()&&p.magneticSpent>0,"Magnet gave no paid assistance");
        check(before-p.energy-p.magneticEnergy()==p.spent,"Field charging was counted as consumption or assistance was free");
        var saved=p.saveWithFullMetadata(h.getLevel().registryAccess());long stored=p.magneticEnergy(),spent=p.spent;var restored=new Collider(p.getBlockPos(),p.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);p=restored;
        check(p.magneticEnergy()==stored&&p.spent==spent&&p.inventory.getItem(Collider.MAGNET_SLOT).has(DataComponents.CUSTOM_NAME)&&p.magnet().equals(settings),"Reload lost field charge, native upgrades or work");
        for(int i=0;i<1500&&p.inventory.getItem(Collider.OUTPUT).isEmpty();i++)p.tick();check(p.inventory.getItem(Collider.OUTPUT).is(Items.DIAMOND)&&!p.busy(),"Compact magnetic ring did not complete its native collision");
        menu=new ColliderMenu(21,player.getInventory(),p);player.containerMenu=menu;check(ColliderPackets.handle(control(p,0,0,0,0,""),player),"Could not stop collider");check(ColliderPackets.handle(control(p,11,3,0,0,""),player),"Could not reopen magnet page");
        stored=p.magneticEnergy();long mainEnergy=p.energy;var removed=menu.quickMoveStack(player,ColliderMenu.MAGNET_MENU);
        check(MagnetSupport.isField(removed)&&MagnetSupport.energy(removed)==stored&&removed.has(DataComponents.CUSTOM_NAME)&&p.magneticEnergy()==0&&p.energy==mainEnergy,"Uninstall duplicated or lost the field's own energy");
    }finally{close(player);clear(p);}h.succeed();}
}
