package dev.everyonemek.factory;

import static dev.everyonemek.factory.FactoryTests.*;
import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import dev.everyonemek.factory.compat.Compat;
import java.util.Map;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekFactory.ID) @PrefixGameTestTemplate(false)
public final class BulkStorageTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void largeStacksSurviveMenusWireAndBlockDrops(GameTestHelper h){
        var c=formed(h,Grade.ULTIMATE,3);c.enabled=false;c.autoEject=false;var part=port(c,false);var side=c.structure.outward(part.getBlockPos());var itemHandler=new Ports.ItemPort(part,side);
        var named=new ItemStack(Items.IRON_INGOT,3000);named.set(DataComponents.CUSTOM_NAME,Component.literal("Stored alloy"));
        check(itemHandler.getSlotLimit(0)==32768&&itemHandler.insertItem(0,named,true).isEmpty()&&part.storage().item(0).isEmpty(),"Capacity query or insertion simulation failed");
        check(itemHandler.insertItem(0,named,false).isEmpty(),"Real large insertion failed");part.storage().insert(53,new ItemStack(Items.DIAMOND,32000),false);
        check(itemHandler.extractItem(0,99999,true).getCount()==64&&part.storage().item(0).getCount()==3000,"External handler exposed an oversized extraction");
        var tag=part.saveWithFullMetadata(h.getLevel().registryAccess());part.loadWithComponents(tag,h.getLevel().registryAccess());
        check(ItemStack.matches(part.storage().item(0),named)&&part.storage().item(53).getCount()==32000,"NBT truncated bulk stacks or item components");
        var wire=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{ItemStack.OPTIONAL_STREAM_CODEC.encode(wire,named);check(ItemStack.matches(ItemStack.OPTIONAL_STREAM_CODEC.decode(wire),named),"Menu wire codec truncated a large stack");}finally{wire.release();}
        var p=player(h,part.getBlockPos().relative(side));try{
            part.open(p);var menu=(WarehouseMenu)p.containerMenu;
            menu.clicked(0,1,ClickType.PICKUP,p);check(menu.getCarried().getCount()==32&&part.storage().item(0).getCount()==2968,"Right pickup did not take half a normal stack");
            menu.clicked(0,0,ClickType.PICKUP,p);check(menu.getCarried().isEmpty()&&part.storage().item(0).getCount()==3000,"Click could not top up an overstacked slot");
            menu.clicked(0,2,ClickType.SWAP,p);check(p.getInventory().getItem(2).getCount()==64&&part.storage().item(0).getCount()==2936,"Hotbar extraction moved an oversized stack");
            menu.setCarried(p.getInventory().removeItemNoUpdate(2));menu.clicked(0,0,ClickType.PICKUP,p);
            p.getInventory().setItem(2,new ItemStack(Items.EMERALD,16));menu.clicked(0,2,ClickType.SWAP,p);
            check(p.getInventory().getItem(2).is(Items.EMERALD)&&part.storage().item(0).getCount()==3000,"Hotbar swap replaced and lost a bulk stack");
            menu.clicked(0,0,ClickType.QUICK_MOVE,p);check(p.getInventory().countItem(Items.IRON_INGOT)==2240&&part.storage().item(0).getCount()==760,"Shift transfer failed to debit exactly what fit in the backpack");
            for(int i=36;i<41;i++)check(p.getInventory().getItem(i).isEmpty(),"Shift transfer filled armor or offhand slots");
            check(part.setOutput(p,true),"Could not switch to output");part.open(p);menu=(WarehouseMenu)p.containerMenu;
            menu.clicked(0,0,ClickType.PICKUP,p);check(menu.getCarried().getCount()==64&&part.storage().item(0).getCount()==696,"Output pickup could not remove a normal stack");
            part.storage().insert(0,menu.getCarried(),false);menu.setCarried(ItemStack.EMPTY);
            var drops=Block.getDrops(part.getBlockState(),h.getLevel(),part.getBlockPos(),part);check(drops.size()==1,"Warehouse drop missing");
            var pos=part.getBlockPos();var held=p.getMainHandItem().copy();h.getLevel().destroyBlock(pos,false);p.setItemInHand(InteractionHand.MAIN_HAND,drops.getFirst());
            try{p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));}finally{p.setItemInHand(InteractionHand.MAIN_HAND,held);}
            var restored=(Part)h.getLevel().getBlockEntity(pos);check(restored.storage().item(0).getCount()==760&&ItemStack.isSameItemSameComponents(restored.storage().item(0),named)&&restored.storage().item(53).getCount()==32000&&restored.getBlockState().getValue(PartBlock.OUTPUT),"Block drop/placement lost large contents or mode");
        }finally{close(p);}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void bulkEjectionRetainsRemainderWhenChestIsFull(GameTestHelper h){
        var c=formed(h,Grade.ULTIMATE,3);c.enabled=false;c.autoEject=false;var part=port(c,true);var destination=part.getBlockPos().relative(c.structure.outward(part.getBlockPos()));
        h.getLevel().setBlockAndUpdate(destination,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(destination);chest.setItem(0,new ItemStack(Items.IRON_INGOT,63));chest.setChanged();
        part.storage().insert(0,new ItemStack(Items.IRON_INGOT,32768),false);
        h.startSequence().thenExecute(()->c.autoEject=true).thenIdle(3).thenExecute(()->{
            int total=0;for(int i=0;i<chest.getContainerSize();i++)total+=chest.getItem(i).getCount();check(total==1728&&part.storage().item(0).getCount()==32768-(1728-63),"Full chest transfer lost or duplicated its accepted amount");
        }).thenIdle(3).thenExecute(()->{
            check(part.storage().item(0).getCount()==32768-(1728-63),"Full receiver kept consuming source items");chest.setItem(0,ItemStack.EMPTY);chest.setChanged();
        }).thenIdle(2).thenExecute(()->{check(chest.getItem(0).getCount()==64&&part.storage().item(0).getCount()==32768-(1728-63)-64,"Partial room was not refilled accurately");c.autoEject=false;}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void oneHatchFeedsFourCreativeInfiniteFactorySteps(GameTestHelper h){
        if(!Compat.EXTRAS){h.succeed();return;}
        var c=formed(h,Grade.ULTIMATE,3);c.autoEject=false;var template=new ItemStack(ExtraBlocks.getExtraFactory(ExtraFactoryTier.INFINITE,FactoryType.CRUSHING));
        template.set(MekanismDataComponents.UPGRADES,new UpgradeAware(Map.of(ExtraUpgrade.STACK,8,ExtraUpgrade.CREATIVE,1),ItemStack.EMPTY,ItemStack.EMPTY));c.template.setStack(template);
        var input=port(c,false).storage();input.insert(0,new ItemStack(Items.IRON_INGOT,32768),false);input.insert(1,new ItemStack(Items.IRON_INGOT,32768),false);
        c.processing.tick(c);int output=0;var bank=c.outputBank();for(int i=0;i<bank.itemSlots();i++)output+=bank.item(i).getCount();
        check(output==17*256*4&&count(input,Items.IRON_INGOT)==65536-output&&c.running==17&&c.powerUsed==0,"One upgraded warehouse still bottlenecked the four-step creative factory");c.enabled=false;h.succeed();
    }
}
