package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.expansion.*;
import dev.everyonemek.gravity.solar.*;
import mekanism.api.security.SecurityMode;
import mekanism.common.registries.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class OrbitalTests{
    private static OrbitalModule place(GameTestHelper h,ModuleKind kind,BlockPos pos){h.getLevel().setBlockAndUpdate(pos,ModuleContent.BLOCK.get(kind).get().defaultBlockState());return (OrbitalModule)h.getLevel().getBlockEntity(pos);}
    private static void hot(SolarController s){s.enabled=s.ignited=true;s.stored=s.capacity();s.fuelRemaining=s.fuelTotal=100_000_000_000_000_000L;}
    private static void hot(Controller c){c.enabled=c.ignited=true;c.stored=c.capacity();c.fuelRemaining=c.fuelTotal=100_000_000_000_000L;}
    private static void use(ServerPlayer p,BlockPos pos,boolean sneak){p.setPos(pos.north().getCenter());p.setShiftKeyDown(sneak);p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false));}
    private static void link(GameTestHelper h,OrbitalModule module,BlockPos source){var p=ReactorTests.player(h,source.north());try{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModuleContent.LINKER.get()));use(p,source,true);check(p.getMainHandItem().has(ModuleContent.LINK.get()),"Real linker selection was swallowed by block GUI");use(p,module.getBlockPos(),false);check(module.source!=null&&module.source.pos().equals(source),"Real linker did not bind source");}finally{ReactorTests.close(p);}}
    @GameTest(template="empty",timeoutTicks=120)
    public static void flareCaptureUsesRealBindingAndOutput(GameTestHelper h){var s=SolarTests.formed(h);hot(s);var m=place(h,ModuleKind.CAPTOR,s.getBlockPos().north(3));link(h,m,s.getBlockPos());
        var target=m.getBlockPos().relative(m.getDirection());h.getLevel().setBlockAndUpdate(target,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(target);
        var input=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,m.getBlockPos(),Direction.UP);input.insertItem(0,new ItemStack(MekanismItems.ATOMIC_ALLOY.get(),16),false);input.insertItem(1,new ItemStack(Items.QUARTZ,16),false);
        h.startSequence().thenWaitUntil(()->check(m.remaining==4,"Capture batch did not begin: "+m.status)).thenExecute(()->{check(m.paidEnergy==200_000_000_000L&&m.inputs.stream().allMatch(slot->slot.isEmpty()),"Capture failed exact payment/material debit");s.enabled=false;})
            .thenIdle(3).thenExecute(()->{check(m.remaining==4&&m.progress<m.duration,"Paused sun completed a capture");var copy=(OrbitalModule)BlockEntity.loadStatic(m.getBlockPos(),m.getBlockState(),m.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());check(copy.remaining==4&&copy.paidEnergy==m.paidEnergy,"Capture paid work failed reload");s.enabled=true;})
            .thenWaitUntil(()->check(chest.getItem(0).is(ModuleContent.FLARE.get())&&chest.getItem(0).getCount()==4,"Captor failed real chest ejection"))
            .thenExecute(()->s.enabled=false).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=120)
    public static void gravityForgeKeepsPaidCargoThroughRealReplacement(GameTestHelper h){var c=ReactorTests.formed(h);hot(c);var m=place(h,ModuleKind.FORGE,c.getBlockPos().north(3));m.autoEject=false;link(h,m,c.getBlockPos());
        m.inputs.get(0).setStack(new ItemStack(MekanismItems.ATOMIC_ALLOY.get(),16));m.inputs.get(1).setStack(new ItemStack(MekanismItems.HDPE_SHEET.get(),4));m.inputs.get(2).setStack(new ItemStack(ModuleContent.FLARE.get(),4));
        h.startSequence().thenWaitUntil(()->check(m.remaining==16,"Forge batch did not begin: "+m.status)).thenExecute(()->{c.enabled=false;var item=Block.getDrops(m.getBlockState(),h.getLevel(),m.getBlockPos(),m).getFirst();check(item.get(ModuleContent.DATA.get()).getInt("remaining")==16,"Forge drop lost paid output");var pos=m.getBlockPos();h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            var p=ReactorTests.player(h,pos.north());try{p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)item.save(h.getLevel().registryAccess())));p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));}finally{ReactorTests.close(p);}
            var restored=(OrbitalModule)h.getLevel().getBlockEntity(pos);check(restored.remaining==16&&restored.paidEnergy==8_000_000_000L&&restored.source.pos().equals(c.getBlockPos()),"Replacement altered paid state or binding");hot(c);
        }).thenWaitUntil(()->{var restored=(OrbitalModule)h.getLevel().getBlockEntity(m.getBlockPos());check(restored.outputs.getFirst().getStack().is(ModuleContent.ALLOY.get())&&restored.outputs.getFirst().getCount()==16,"Forge failed to resume prepaid batch");})
        .thenExecute(()->{check(c.stored==c.capacity(),"Forge resume debited energy twice");c.enabled=false;}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=140)
    public static void nodesConserveCargoAndStopAtFullReceiver(GameTestHelper h){var s=SolarTests.formed(h);hot(s);var sender=place(h,ModuleKind.NODE,s.getBlockPos().north(3));var receiver=place(h,ModuleKind.NODE,s.getBlockPos().west(9).north(3));link(h,sender,s.getBlockPos());
        var p=ReactorTests.player(h,sender.getBlockPos().north());try{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModuleContent.LINKER.get()));use(p,receiver.getBlockPos(),true);use(p,sender.getBlockPos(),false);check(sender.peer!=null&&sender.peer.pos().equals(receiver.getBlockPos()),"Real node pairing failed");}finally{ReactorTests.close(p);}
        var target=receiver.getBlockPos().relative(receiver.getDirection());h.getLevel().setBlockAndUpdate(target,MekanismBlocks.ULTIMATE_BIN.get().defaultBlockState());var bin=(mekanism.common.tile.TileEntityBin)h.getLevel().getBlockEntity(target);receiver.autoEject=false;
        for(var slot:receiver.outputs)slot.setStack(new ItemStack(Items.COBBLESTONE,4096));sender.inputs.get(0).setStack(new ItemStack(Items.IRON_INGOT,4096));sender.inputs.get(1).setStack(new ItemStack(Items.IRON_INGOT,4096));sender.inputs.get(2).setStack(new ItemStack(Items.IRON_INGOT,1808));
        h.startSequence().thenIdle(3).thenExecute(()->{check(sender.transferred==0&&sender.inputs.stream().mapToInt(slot->slot.getCount()).sum()==10000&&s.stored==s.capacity(),"Blocked receiver consumed cargo or energy");receiver.outputs.forEach(slot->slot.setStack(ItemStack.EMPTY));receiver.autoEject=true;})
            .thenWaitUntil(()->check(bin.getBinSlot().getCount()==10000,"Node output pending: "+sender.status))
            .thenExecute(()->{check(sender.transferred==10000&&sender.inputs.stream().allMatch(slot->slot.isEmpty())&&receiver.outputs.stream().allMatch(slot->slot.isEmpty()),"Node transfer duplicated cargo");s.enabled=false;}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void moduleLinksRecheckPrivateOwnersAndBounds(GameTestHelper h){var s=SolarTests.formed(h);hot(s);var m=place(h,ModuleKind.CAPTOR,s.getBlockPos().north(3));link(h,m,s.getBlockPos());m.inputs.get(0).setStack(new ItemStack(MekanismItems.ATOMIC_ALLOY.get(),4));m.inputs.get(1).setStack(new ItemStack(Items.QUARTZ,4));
        var owner=ReactorTests.player(h,s.getBlockPos().north());var visitor=ReactorTests.player(h,m.getBlockPos().north());try{s.getSecurity().setOwnerUUID(owner.getUUID());s.getSecurity().setMode(SecurityMode.PRIVATE);check(!m.bind(visitor,FieldLink.at(h.getLevel(),s.getBlockPos()),false),"Binding bypassed source security");check(!m.bind(visitor,new FieldLink("minecraft:the_nether",s.getBlockPos()),false),"Cross-dimension link accepted");check(!m.bind(visitor,FieldLink.at(h.getLevel(),m.getBlockPos().east(10000)),false),"Out-of-range link accepted");}finally{ReactorTests.close(visitor);ReactorTests.close(owner);}
        h.startSequence().thenIdle(3).thenExecute(()->{check(m.status.equals("access")&&m.remaining==0&&m.inputs.getFirst().getCount()==4,"Revoked permission did not stop automatic source use");s.enabled=false;}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void tuningConservesFuelAndPersistsBurstCooldown(GameTestHelper h){var c=ReactorTests.formed(h);hot(c);
        for(int mode=0;mode<3;mode++){c.tuning.profile=mode;c.gross=c.tuning.power(c.structure.grade.power());c.stored=c.reserve();long before=c.fuelRemaining;c.react();check(c.gross==c.tuning.power(c.structure.grade.power())&&before-c.fuelRemaining==c.tuning.cost(c.gross)&&c.stored-c.reserve()==c.gross-c.selfUse,"Tuning fuel/net accounting incorrect");}
        for(int budget=1;budget<=160;budget++){c.tuning.profile=2;c.fuelRemaining=c.fuelTotal=budget;c.stored=c.reserve();c.gross=c.structure.grade.power();c.react();if(c.fuelRemaining>0)c.react();check(c.fuelRemaining==0&&c.stored>=c.reserve()&&c.stored-c.reserve()<=budget,"Overclock stranded/created final fuel joules: "+budget);}
        hot(c);var m=place(h,ModuleKind.TUNER,c.getBlockPos().north(3));link(h,m,c.getBlockPos());m.inputs.getFirst().setStack(new ItemStack(ModuleContent.FLARE.get(),2));var p=ReactorTests.player(h,m.getBlockPos().north());
        try{p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);use(p,m.getBlockPos(),false);check(p.containerMenu instanceof ModuleMenu,"Tuner menu inaccessible");check(p.containerMenu.clickMenuButton(p,11)&&c.tuning.profile==1,"Tuning button did not reach real core");check(!p.containerMenu.clickMenuButton(p,13)&&m.inputs.getFirst().getCount()==2,"Full buffer wasted a flare cell");c.stored=c.reserve()+1000000;check(p.containerMenu.clickMenuButton(p,13)&&m.inputs.getFirst().getCount()==1&&c.tuning.burst==400,"Burst did not consume exactly one flare cell");check(!p.containerMenu.clickMenuButton(p,13)&&m.inputs.getFirst().getCount()==1,"A second console action bypassed cooldown");
            var loaded=(Controller)BlockEntity.loadStatic(c.getBlockPos(),c.getBlockState(),c.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());check(loaded.tuning.profile==1&&loaded.tuning.burst==400&&loaded.tuning.cooldown==1600,"Core save dropped burst/cooldown");var data=Block.getDrops(c.getBlockState(),h.getLevel(),c.getBlockPos(),c).getFirst().get(Content.DATA.get());check(data.getCompound("tuning").getInt("cooldown")==1600,"Core drop reset cooldown");
        }finally{ReactorTests.close(p);}c.enabled=false;h.startSequence().thenIdle(2).thenExecute(()->check(c.tuning.burst==0&&c.tuning.cooldown>1500,"Shutdown failed to stop burst or erased cooldown")).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void observatoryDrivesRealRedstoneAndReportsBrokenSource(GameTestHelper h){var s=SolarTests.formed(h);s.stored=s.capacity()/2;var m=place(h,ModuleKind.OBSERVATORY,s.getBlockPos().north(3));link(h,m,s.getBlockPos());var lamp=m.getBlockPos().north();h.getLevel().setBlockAndUpdate(lamp,Blocks.REDSTONE_LAMP.defaultBlockState());
        h.startSequence().thenIdle(6).thenExecute(()->{check(m.signal==8&&!m.supportsRedstone()&&h.getLevel().getSignal(m.getBlockPos(),Direction.NORTH)==0&&h.getLevel().getBlockState(lamp).getValue(RedstoneLampBlock.LIT),"Observer did not drive real redstone from stored energy");var p=ReactorTests.player(h,m.getBlockPos().west());try{p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);use(p,m.getBlockPos(),false);check(p.containerMenu instanceof ModuleMenu&&p.containerMenu.clickMenuButton(p,3)&&m.alarm==1,"Alarm mode GUI not wired");}finally{ReactorTests.close(p);}h.getLevel().setBlockAndUpdate(s.structure.at(1,1,1),Blocks.AIR.defaultBlockState());})
            .thenIdle(6).thenExecute(()->check(m.signal==15&&!m.sourceFormed&&m.status.equals("structure"),"Broken reactor was not reported as a live structural alarm")).thenSucceed();
    }
}
