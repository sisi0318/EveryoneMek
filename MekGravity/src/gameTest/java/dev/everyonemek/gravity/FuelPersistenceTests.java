package dev.everyonemek.gravity;

import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.solar.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class FuelPersistenceTests {
    @GameTest(template="empty",timeoutTicks=60)
    public static void gravityFuelClicksPersistInChunkAndDrop(GameTestHelper h){exercise(h,false);h.succeed();}
    @GameTest(template="empty",timeoutTicks=60)
    public static void solarFuelClicksPersistInChunkAndDrop(GameTestHelper h){exercise(h,true);h.succeed();}
    private static void exercise(GameTestHelper h,boolean solar){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,3,6));
        level.setBlockAndUpdate(pos,solar?SolarContent.block(SolarBlock.Kind.FUEL,0).get().defaultBlockState():Content.PARTS.get(PartBlock.Kind.FUEL).get().defaultBlockState());
        var part=level.getBlockEntity(pos);Item fuel=solar?SolarContent.FUEL.get():Content.PELLET.get();var handler=inventory(part);
        var p=ReactorTests.player(h,pos.north());
        try{
            if(solar)SolarFuelMenu.open(p,(SolarPart)part);else FuelMenu.open(p,(Part)part);var menu=p.containerMenu;
            check(menu instanceof FuelMenu||menu instanceof SolarFuelMenu,"Real fuel menu did not open");
            handler.setStackInSlot(0,new ItemStack(fuel,32));p.getInventory().setItem(9,new ItemStack(fuel,16));
            int playerSlot=-1;for(int i=18;i<menu.slots.size();i++)if(menu.slots.get(i).container==p.getInventory()&&menu.slots.get(i).getSlotIndex()==9){playerSlot=i;break;}
            check(playerSlot>=18,"Player slot missing");var chunk=level.getChunkAt(pos);boolean previousDirty=chunk.isUnsaved();
            try{
                // Mimic an already saved, stopped hatch, then shift-merge into a NONEMPTY stack.
                chunk.setUnsaved(false);menu.clicked(playerSlot,0,ClickType.QUICK_MOVE,p);
                check(handler.getStackInSlot(0).getCount()==48&&p.getInventory().countItem(fuel)==0,"Shift merge changed item totals");
                check(chunk.isUnsaved(),"Shift merge failed to mark the fuel hatch chunk for saving");savedCount(h,part,48);
                // Only eight items fit in the player's otherwise full inventory; source stays nonempty.
                for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.getInventory().setItem(9,new ItemStack(fuel,56));
                chunk.setUnsaved(false);menu.clicked(0,0,ClickType.QUICK_MOVE,p);
                check(handler.getStackInSlot(0).getCount()==40&&p.getInventory().countItem(fuel)==64,"Partial shift extraction changed item totals");
                check(chunk.isUnsaved(),"Partial shift extraction failed to mark the fuel hatch chunk for saving");savedCount(h,part,40);
                chunk.setUnsaved(false);menu.clicked(0,1,ClickType.PICKUP,p);
                check(handler.getStackInSlot(0).getCount()==20&&menu.getCarried().getCount()==20,"Right-click split lost items");
                check(chunk.isUnsaved(),"Right-click split did not save");savedCount(h,part,20);
                chunk.setUnsaved(false);menu.clicked(0,1,ClickType.PICKUP,p);
                check(handler.getStackInSlot(0).getCount()==21&&menu.getCarried().getCount()==19,"Single-item merge lost items");
                check(chunk.isUnsaved(),"Single-item merge did not save");savedCount(h,part,21);
                menu.setCarried(ItemStack.EMPTY);
                // A blocked transfer must not alter counts or mark a stopped hatch dirty.
                chunk.setUnsaved(false);menu.clicked(0,0,ClickType.QUICK_MOVE,p);
                check(handler.getStackInSlot(0).getCount()==21&&p.getInventory().countItem(fuel)==64,"Full inventory transfer consumed fuel");
                check(!chunk.isUnsaved(),"Blocked transfer marked the fuel inventory dirty");
                var drops=Block.getDrops(part.getBlockState(),level,pos,part);var tag=drops.getFirst().get(solar?SolarContent.STOCK.get():Content.STOCK.get());var recovered=new ItemStackHandler(18);recovered.deserializeNBT(level.registryAccess(),tag.getCompound("items"));
                check(recovered.getStackInSlot(0).getCount()==21,"Dropped fuel hatch used stale inventory");
            }finally{chunk.setUnsaved(previousDirty||chunk.isUnsaved());}
        }finally{p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();ReactorTests.close(p);}
    }
    private static ItemStackHandler inventory(BlockEntity p){return p instanceof Part part?part.inventory:((SolarPart)p).inventory;}
    private static void savedCount(GameTestHelper h,BlockEntity part,int count){
        var level=h.getLevel();var data=ChunkSerializer.write(level,level.getChunkAt(part.getBlockPos()));var entries=data.getList("block_entities",Tag.TAG_COMPOUND);CompoundTag saved=null;
        for(int i=0;i<entries.size();i++){var candidate=entries.getCompound(i);if(BlockEntity.getPosFromTag(candidate).equals(part.getBlockPos())){saved=candidate;break;}}
        check(saved!=null,"Chunk serializer omitted the fuel hatch");var loaded=BlockEntity.loadStatic(part.getBlockPos(),part.getBlockState(),saved,level.registryAccess());
        check(loaded!=null&&inventory(loaded).getStackInSlot(0).getCount()==count,"Saved chunk reloaded the wrong fuel count");
    }
}
