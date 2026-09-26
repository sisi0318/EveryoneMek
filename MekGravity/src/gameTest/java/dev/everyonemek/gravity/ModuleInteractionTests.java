package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import java.util.*;
import dev.everyonemek.gravity.expansion.*;
import dev.everyonemek.gravity.solar.SolarContent;
import mekanism.api.RelativeSide;
import mekanism.api.security.SecurityMode;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class ModuleInteractionTests {
    private static OrbitalModule place(GameTestHelper h,ModuleKind kind,BlockPos pos){h.getLevel().setBlockAndUpdate(pos,ModuleContent.BLOCK.get(kind).get().defaultBlockState());return (OrbitalModule)h.getLevel().getBlockEntity(pos);}
    private static ModuleMenu open(ServerPlayer p,OrbitalModule tile){p.setPos(tile.getBlockPos().north().getCenter());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(tile.getBlockPos().getCenter(),Direction.NORTH,tile.getBlockPos(),false));check(p.containerMenu instanceof ModuleMenu,"Functional block menu failed");return (ModuleMenu)p.containerMenu;}
    @GameTest(template="empty",timeoutTicks=60)
    public static void sourcePickerFiltersAndRechecksRealMenuTargets(GameTestHelper h){var origin=h.absolutePos(new BlockPos(8,4,8));var captor=place(h,ModuleKind.CAPTOR,origin);var solarPos=origin.east(6);var gravityPos=origin.west(6);var privatePos=origin.south(6);h.getLevel().setBlockAndUpdate(solarPos,SolarContent.CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(gravityPos,Content.CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(privatePos,SolarContent.CONTROLLER.get().defaultBlockState());
        var owner=ReactorTests.player(h,privatePos.north());var hidden=(dev.everyonemek.gravity.solar.SolarController)h.getLevel().getBlockEntity(privatePos);hidden.getSecurity().setOwnerUUID(owner.getUUID());hidden.getSecurity().setMode(SecurityMode.PRIVATE);var packets=new ArrayList<ModuleSourceNetwork.Snapshot>();var p=ReactorTests.player(h,origin.north(),packet->{if(packet instanceof ClientboundCustomPayloadPacket c&&c.payload() instanceof ModuleSourceNetwork.Snapshot s)packets.add(s);});
        try{var menu=open(p,captor);check(ModuleSourceNetwork.handle(p,new ModuleSourceNetwork.Action(menu.containerId,menu.nodeSession,0,null)),"Source scan failed");var list=packets.getLast().sources();check(list.stream().anyMatch(s->s.pos().equals(solarPos))&&list.stream().noneMatch(s->s.pos().equals(gravityPos)||s.pos().equals(privatePos)),"Picker leaked private/incompatible cores");
            check(!ModuleSourceNetwork.handle(p,new ModuleSourceNetwork.Action(menu.containerId,menu.nodeSession+1,1,solarPos)),"Old source menu session was accepted");check(ModuleSourceNetwork.handle(p,new ModuleSourceNetwork.Action(menu.containerId,menu.nodeSession,1,solarPos))&&captor.source.pos().equals(solarPos),"Source selection did not bind actual machine");
            h.getLevel().setBlockAndUpdate(solarPos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(solarPos,SolarContent.CONTROLLER.get().defaultBlockState());check(!ModuleSourceNetwork.handle(p,new ModuleSourceNetwork.Action(menu.containerId,menu.nodeSession,1,solarPos)),"A replacement inherited stale selection authority");
            check(ModuleSourceNetwork.handle(p,new ModuleSourceNetwork.Action(menu.containerId,menu.nodeSession,2,null))&&captor.source==null,"Unlink did not update real source");
        }finally{ReactorTests.close(p);ReactorTests.close(owner);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=70)
    public static void processSidesAndDirectObserverControlsWork(GameTestHelper h){var machine=(ProcessModule)place(h,ModuleKind.FORGE,h.absolutePos(new BlockPos(8,4,8)));var p=ReactorTests.player(h,machine.getBlockPos().north());var menu=open(p,machine);check(menu.slots.getFirst().y==78&&menu.slots.get(18).y==163,"Process GUI slots disagree with compact layout");
        var info=machine.getConfig().getConfig(TransmissionType.ITEM);for(var side:RelativeSide.values()){info.setDataType(side==RelativeSide.BACK?DataType.OUTPUT:DataType.INPUT,side);machine.getConfig().sideChanged(TransmissionType.ITEM,side);}var out=machine.getBlockPos().relative(RelativeSide.BACK.getDirection(machine.getDirection()));h.getLevel().setBlockAndUpdate(out,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(out);machine.outputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT,100));
        var observer=place(h,ModuleKind.OBSERVATORY,machine.getBlockPos().east(5));var obsMenu=open(p,observer);check(obsMenu.clickMenuButton(p,33)&&observer.alarm==3&&obsMenu.clickMenuButton(p,137)&&observer.threshold==37,"Direct observer mode/threshold ignored");check(!obsMenu.clickMenuButton(p,200),"Threshold out of range accepted");ReactorTests.close(p);
        h.startSequence().thenWaitUntil(()->check(chest.getItem(0).getCount()+chest.getItem(1).getCount()==100,"Configured rear output did not eject actual items")).thenExecute(()->{var drop=Block.getDrops(machine.getBlockState(),h.getLevel(),machine.getBlockPos(),machine).getFirst();check(drop.has(mekanism.common.registries.MekanismDataComponents.SIDE_CONFIG.get()),"Processor drop omitted side settings");}).thenSucceed();
    }
}
