package dev.everyonemek.gravity;

import static dev.everyonemek.gravity.ReactorTests.check;
import java.util.*;
import dev.everyonemek.gravity.expansion.*;
import dev.everyonemek.gravity.solar.*;
import io.netty.buffer.Unpooled;
import mekanism.api.security.SecurityMode;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class LinkPanelTests {
    private static OrbitalModule place(GameTestHelper h,ModuleKind kind,BlockPos pos){h.getLevel().setBlockAndUpdate(pos,ModuleContent.BLOCK.get(kind).get().defaultBlockState());return (OrbitalModule)h.getLevel().getBlockEntity(pos);}
    private static LinkPanelMenu open(ServerPlayer p){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModuleContent.LINKER.get()));p.getMainHandItem().use(p.level(),p,InteractionHand.MAIN_HAND);check(p.containerMenu instanceof LinkPanelMenu,"Air use did not open the actual panel menu");var menu=(LinkPanelMenu)p.containerMenu;menu.broadcastChanges();return menu;}
    private static boolean action(ServerPlayer p,LinkPanelMenu menu,int op,BlockPos from,BlockPos to){var request=new LinkPanelNetwork.Action(menu.containerId,menu.session,menu.revision,op,from,to);var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),p.registryAccess());try{LinkPanelNetwork.Action.CODEC.encode(buffer,request);return menu.handle(p,LinkPanelNetwork.Action.CODEC.decode(buffer));}finally{buffer.release();}}
    private static boolean shows(LinkPanelMenu menu,BlockPos pos){return menu.devices.stream().anyMatch(d->d.pos().equals(pos));}
    private static boolean range(ServerPlayer p,LinkPanelMenu menu,int operation,int value){var request=new LinkPanelNetwork.Action(menu.containerId,menu.session,menu.revision,operation,null,null,value);var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),p.registryAccess());try{LinkPanelNetwork.Action.CODEC.encode(buffer,request);return menu.handle(p,LinkPanelNetwork.Action.CODEC.decode(buffer));}finally{buffer.release();}}

    @GameTest(template="empty",timeoutTicks=60)
    public static void panelRangesPersistAndWorldDistanceRequiresOperator(GameTestHelper h){
        var anchor=h.absolutePos(new BlockPos(8,6,8));var sender=place(h,ModuleKind.NODE,anchor.east(2));var targetPos=anchor.east(200);h.getLevel().setBlockAndUpdate(targetPos,Content.CONTROLLER.get().defaultBlockState());var target=(Controller)h.getLevel().getBlockEntity(targetPos);sender.inputs.getFirst().setStack(new ItemStack(Items.DIAMOND,32));
        int original=ModuleConfig.RANGE.get();var player=ReactorTests.player(h,anchor);var ops=player.server.getPlayerList().getOps();
        try{ModuleConfig.RANGE.set(128);var menu=open(player);check(!shows(menu,target.getBlockPos()),"Initial radius incorrectly included distant receiver");
            check(range(player,menu,8,256)&&menu.range==256&&ModuleConfig.RANGE.get()==128,"Personal scan changed the global transfer limit");
            menu=open(player);check(menu.range==256&&shows(menu,target.getBlockPos()),"Scan preference did not persist across actual panel reopen");
            check(!range(player,menu,9,256)&&ModuleConfig.RANGE.get()==128,"Non-operator changed the world's connection distance");
            check(!action(player,menu,1,target.getBlockPos(),sender.getBlockPos()),"A larger scan bypassed actual link range");
            ops.add(new net.minecraft.server.players.ServerOpListEntry(player.getGameProfile(),2,false));menu=open(player);check(menu.canEditLinkRange,"Operator permission not synchronized to the panel");
            check(range(player,menu,9,256)&&menu.linkRange==256&&ModuleConfig.RANGE.get()==256,"Operator range update was not saved/acknowledged");
            check(action(player,menu,1,target.getBlockPos(),sender.getBlockPos())&&sender.source.inRange(h.getLevel(),sender.getBlockPos()),"Expanded world limit did not permit real binding");
            check(range(player,menu,9,64)&&sender.source!=null&&!sender.source.inRange(h.getLevel(),sender.getBlockPos()),"Reduced world range deleted the link or left it operational");
            check(range(player,menu,9,256)&&sender.source.inRange(h.getLevel(),sender.getBlockPos()),"Restoring world range did not resume the existing link");
            menu=open(player);check(range(player,menu,8,16)&&!shows(menu,target.getBlockPos())&&sender.source!=null&&sender.inputs.getFirst().getCount()==32,"Scan reduction altered links or cargo");
            check(!range(player,menu,8,Integer.MAX_VALUE)&&!range(player,menu,9,-1)&&menu.range==16&&ModuleConfig.RANGE.get()==256,"Malformed range escaped bounds checking");
            ops.remove(player.getGameProfile());check(!range(player,menu,9,128)&&ModuleConfig.RANGE.get()==256,"Revoked operator status still changed settings");
        }finally{ModuleConfig.RANGE.set(original);ModuleConfig.RANGE.save();ops.remove(player.getGameProfile());ReactorTests.close(player);}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=90)
    public static void panelDragActionsBindActualTransferAndIndependentChannels(GameTestHelper h){
        var core=SolarTests.formed(h);core.enabled=core.ignited=true;core.stored=core.capacity();core.fuelRemaining=core.fuelTotal=100_000_000_000_000_000L;
        var sender=place(h,ModuleKind.NODE,core.getBlockPos().north(3));var receiver=place(h,ModuleKind.NODE,sender.getBlockPos().west(10));receiver.autoEject=false;
        UUID frequency=NodeFrequencyTests.pair(h,sender,receiver);
        List<LinkPanelNetwork.Snapshot> packets=new ArrayList<>();var player=ReactorTests.player(h,sender.getBlockPos().north(),packet->{if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof LinkPanelNetwork.Snapshot snapshot)packets.add(snapshot);});
        var menu=open(player);check(shows(menu,core.getBlockPos())&&shows(menu,sender.getBlockPos())&&shows(menu,receiver.getBlockPos()),"Discovery omitted loaded nearby core or nodes");check(!packets.isEmpty(),"Panel did not send its discovery snapshot");
        check(action(player,menu,1,core.getBlockPos(),sender.getBlockPos()),"Canvas power action failed");
        check(sender.source.pos().equals(core.getBlockPos())&&frequency.equals(((NodeModule)sender).frequency),"Canvas power binding altered selected frequency");
        check(action(player,menu,4,sender.getBlockPos(),null)&&!sender.channel(0)&&sender.channel(1),"Panel item toggle affected the wrong channel");sender.inputs.getFirst().setStack(new ItemStack(Items.DIAMOND,100));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());try{var snapshot=packets.getLast();LinkPanelNetwork.Snapshot.CODEC.encode(buffer,snapshot);check(snapshot.equals(LinkPanelNetwork.Snapshot.CODEC.decode(buffer)),"Snapshot lost device names, links or channel flags in transit");}finally{buffer.release();}
        h.startSequence().thenIdle(2).thenExecute(()->{check(sender.inputs.getFirst().getCount()==100&&receiver.outputs.getFirst().isEmpty(),"Disabled canvas channel still transferred items");check(action(player,menu,4,sender.getBlockPos(),null),"Could not re-enable item channel");})
            .thenWaitUntil(()->check(receiver.outputs.getFirst().getCount()==100&&sender.inputs.getFirst().isEmpty(),"Panel-established route did not transfer real cargo"))
            .thenExecute(()->{try{check(action(player,menu,3,sender.getBlockPos(),null)&&action(player,menu,2,sender.getBlockPos(),core.getBlockPos()),"Panel unlink controls did not reach machines");check(((NodeModule)sender).frequency==null&&sender.source==null&&receiver.outputs.getFirst().getCount()==100,"Disconnect destroyed cargo or left live links");}finally{core.enabled=false;ReactorTests.close(player);}}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void discoveryAndDelayedActionsRespectRangeIdentityAndSecurity(GameTestHelper h){
        var anchor=h.absolutePos(new BlockPos(16,0,16)).atY((h.getLevel().getMinBuildHeight()+h.getLevel().getMaxBuildHeight())/2);int range=ModuleConfig.RANGE.get();
        var privateOwner=ReactorTests.player(h,anchor.north(5));var near=place(h,ModuleKind.NODE,anchor.east(2));var privateNode=place(h,ModuleKind.NODE,anchor.west(2));privateNode.getSecurity().setOwnerUUID(privateOwner.getUUID());privateNode.getSecurity().setMode(SecurityMode.PRIVATE);
        var high=place(h,ModuleKind.NODE,anchor.above(range-4));var low=place(h,ModuleKind.NODE,anchor.below(range-4));var far=place(h,ModuleKind.NODE,anchor.above(range+2));
        var corePos=anchor.north(3);h.getLevel().setBlockAndUpdate(corePos,Content.CONTROLLER.get().defaultBlockState());var captor=place(h,ModuleKind.CAPTOR,anchor.south(3));
        var highCore=high.getBlockPos().east();h.getLevel().setBlockAndUpdate(highCore,Content.CONTROLLER.get().defaultBlockState());var player=ReactorTests.player(h,anchor);var menu=open(player);
        check(shows(menu,high.getBlockPos())&&shows(menu,low.getBlockPos())&&!shows(menu,privateNode.getBlockPos())&&!shows(menu,far.getBlockPos()),"Discovery ignored visibility range or private ownership");
        check(!menu.handle(player,new LinkPanelNetwork.Action(menu.containerId+1,menu.session,menu.revision,1,high.getBlockPos(),low.getBlockPos()))&&!menu.handle(player,new LinkPanelNetwork.Action(menu.containerId,menu.session+1,menu.revision,1,high.getBlockPos(),low.getBlockPos())),"Another menu/session could edit this network");
        check(!menu.handle(player,new LinkPanelNetwork.Action(menu.containerId,menu.session,menu.revision-1,1,high.getBlockPos(),low.getBlockPos())),"Stale drag was accepted");
        check(!action(player,menu,1,highCore,low.getBlockPos()),"Scan radius was incorrectly used as pair distance");
        check(!action(player,menu,1,corePos,captor.getBlockPos()),"Captor accepted a gravity core");
        check(!action(player,menu,1,near.getBlockPos(),near.getBlockPos()),"Self-routing was accepted");
        h.startSequence().thenIdle(1).thenExecute(()->{try{
            near.getSecurity().setOwnerUUID(privateOwner.getUUID());near.getSecurity().setMode(SecurityMode.PRIVATE);
            check(!action(player,menu,1,corePos,near.getBlockPos()),"Revoked permission was bypassed before the next scan");
            near.getSecurity().setMode(SecurityMode.PUBLIC);
            var oldPos=high.getBlockPos();h.getLevel().setBlockAndUpdate(oldPos,Blocks.AIR.defaultBlockState());var replacement=place(h,ModuleKind.NODE,oldPos);
            check(!action(player,menu,1,corePos,oldPos)&&replacement.source==null&&near.source==null,"Old snapshot authorized a replaced device");
            check(!action(player,menu,1,low.getBlockPos(),privateNode.getBlockPos()),"An undiscovered private endpoint could be addressed directly");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);check(!menu.stillValid(player)&&!action(player,menu,4,low.getBlockPos(),null),"Panel stayed writable after linker removal");
        }finally{ReactorTests.close(player);ReactorTests.close(privateOwner);}}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void machinePanelEntryAndClosedSessionsAreValidated(GameTestHelper h){
        var host=place(h,ModuleKind.NODE,h.absolutePos(new BlockPos(8,3,8)));var player=ReactorTests.player(h,host.getBlockPos().north());
        try{player.gameMode.useItemOn(player,player.level(),ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(host.getBlockPos().getCenter(),Direction.NORTH,host.getBlockPos(),false));check(player.containerMenu instanceof ModuleMenu,"Native module GUI did not open");
            check(player.containerMenu.clickMenuButton(player,7)&&player.containerMenu instanceof LinkPanelMenu,"Module panel button did not open a real panel");var menu=(LinkPanelMenu)player.containerMenu;menu.broadcastChanges();check(menu.stillValid(player)&&shows(menu,host.getBlockPos()),"Host panel incorrectly required a handheld linker");
            player.setPos(host.getBlockPos().east(12).getCenter());check(!menu.stillValid(player)&&!action(player,menu,4,host.getBlockPos(),null),"Host panel accepted a distant player");player.setPos(host.getBlockPos().north().getCenter());
            h.getLevel().setBlockAndUpdate(host.getBlockPos(),Blocks.AIR.defaultBlockState());place(h,ModuleKind.NODE,host.getBlockPos());check(!menu.stillValid(player),"Host replacement preserved the old session");
            var reopened=open(player);check(reopened.session!=menu.session&&!menu.handle(player,new LinkPanelNetwork.Action(menu.containerId,menu.session,menu.revision,0,null,null)),"Closed menu processed actions after reopening");
        }finally{ReactorTests.close(player);}h.succeed();
    }
}
