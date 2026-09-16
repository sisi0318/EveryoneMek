package dev.everyonemek.overloadcore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.inventory.*;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class WardCustodyGameTests {
    private static void check(boolean condition, String message) { CoreGameTests.check(condition, message); }
    private static IDynamicStackHandler slots(ServerPlayer p) {
        return CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler(ThunderWardItem.SLOT).orElseThrow().getStacks();
    }
    static CuriosContainer click(ServerPlayer p, ClickType type, int button) {
        p.closeContainer();
        var menu = new CuriosContainer(41, p.getInventory());
        p.containerMenu = menu;
        int index = -1;
        for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i) instanceof CurioSlot s
              && s.getIdentifier().equals(ThunderWardItem.SLOT) && !s.isCosmetic()) { index = i; break; }
        check(index >= 0, "Real Curios menu has no Ward slot");
        // Exercise vanilla packet checks and the actual Curios menu, not a direct production permission flag.
        p.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(),
              index, button, type, ItemStack.EMPTY, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
        return menu;
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void equippedWardRejectsMutationAndRecoversBypassedStorage(GameTestHelper h) throws Exception {
        var f = ThunderWardGameTests.player(h, new BlockPos(20,4,20)); var p = f.player();
        try {
            var stack = slots(p).getStackInSlot(0); var token = stack.get(CoreContent.WARD_SEAL);
            check(token != null, "First real insert did not establish custody");
            slots(p).setStackInSlot(0, ItemStack.EMPTY);
            check(slots(p).extractItem(0,1,true).isEmpty() && slots(p).extractItem(0,1,false).isEmpty(), "API extraction bypassed custody");
            stack.setCount(0); stack.remove(CoreContent.WARD_SEAL); stack.set(DataComponents.CUSTOM_NAME, Component.literal("forged"));
            stack.applyComponents(net.minecraft.core.component.DataComponentPatch.builder().remove(CoreContent.WARD_SEAL.get()).build());
            check(stack.getCount()==1 && token.equals(stack.get(CoreContent.WARD_SEAL)) && !stack.has(DataComponents.CUSTOM_NAME), "An equipped stack accepted count/component tampering");
            var count = ItemStack.class.getDeclaredField("count"); count.setAccessible(true); count.setInt(stack,0);
            p.getPersistentData().getAllKeys().clear();
            check(ThunderWard.equipped(p) && token.equals(slots(p).getStackInSlot(0).get(CoreContent.WARD_SEAL)), "Raw count or player NBT clearing erased custody");
            var components=ItemStack.class.getDeclaredField("components");components.setAccessible(true);
            var raw=(net.minecraft.core.component.PatchedDataComponentMap)components.get(slots(p).getStackInSlot(0));
            raw.remove(CoreContent.WARD_SEAL.get());raw.set(DataComponents.CUSTOM_NAME,Component.literal("raw forgery"));
            check(ThunderWard.equipped(p) && token.equals(slots(p).getStackInSlot(0).get(CoreContent.WARD_SEAL))
                  && !slots(p).getStackInSlot(0).has(DataComponents.CUSTOM_NAME), "Bypassed component writes corrupted the independent snapshot");
            var strippedCopy=slots(p).getStackInSlot(0).copy();strippedCopy.remove(CoreContent.WARD_SEAL);
            var forgedInventory=new net.neoforged.neoforge.items.ItemStackHandler(1);
            forgedInventory.setStackInSlot(0,strippedCopy);
            ((DynamicStackHandler)slots(p)).deserializeNBT(p.registryAccess(),forgedInventory.serializeNBT(p.registryAccess()));
            check(ThunderWard.equipped(p) && p.getInventory().countItem(CoreContent.WARD.get())==0,
                  "Repair returned an identity-stripped copy to inventory and duplicated the original");
            forgedInventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));
            ((DynamicStackHandler)slots(p)).deserializeNBT(p.registryAccess(),forgedInventory.serializeNBT(p.registryAccess()));
            check(ThunderWard.equipped(p) && p.getInventory().countItem(Items.DIAMOND)==3, "Repair deleted unrelated replacement items");
            var handler = CuriosApi.getCuriosInventory(p).orElseThrow();
            handler.setSlotActive(ThunderWardItem.SLOT,0,false);
            check(ThunderWard.equipped(p) && handler.isSlotActive(ThunderWardItem.SLOT,0), "Forced deactivation disabled the ward");
            ((DynamicStackHandler)slots(p)).deserializeNBT(p.registryAccess(),new CompoundTag());
            check(ThunderWard.equipped(p), "Direct handler NBT replacement erased the ward");
            var before = new java.util.HashMap<>(handler.getCurios());
            var stripped = new java.util.HashMap<>(before); stripped.remove(ThunderWardItem.SLOT); handler.setCurios(stripped);
            check(ThunderWard.equipped(p) && token.equals(slots(p).getStackInSlot(0).get(CoreContent.WARD_SEAL)), "Missing slot was not reconstructed");
            check(handler.getStacksHandler(CoreBinding.SLOT).orElseThrow()==before.get(CoreBinding.SLOT), "Repair replaced unrelated accessory slots");
        } finally { ThunderWardGameTests.close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void realMenuPickupShiftSwapAndFullInventoryDoNotDuplicate(GameTestHelper h) {
        var f = ThunderWardGameTests.player(h,new BlockPos(20,4,20)); var p=f.player();
        try {
            var menu=click(p,ClickType.PICKUP,0);
            check(menu.getCarried().is(CoreContent.WARD) && !menu.getCarried().has(CoreContent.WARD_SEAL)
                  && slots(p).getStackInSlot(0).isEmpty() && !ThunderWard.equipped(p), "Manual pickup failed to release custody");
            var customized=menu.getCarried(); customized.set(DataComponents.CUSTOM_NAME,Component.literal("My ward"));
            menu.setCarried(ItemStack.EMPTY);
            slots(p).insertItem(0,customized,false);
            click(p,ClickType.QUICK_MOVE,0);
            check(!ThunderWard.equipped(p) && p.getInventory().countItem(CoreContent.WARD.get())==1, "Shift extraction duplicated or restored the ward");
            for(int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).is(CoreContent.WARD)) {
                var moved=p.getInventory().removeItemNoUpdate(i);slots(p).insertItem(0,moved,false);break;
            }
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            click(p,ClickType.QUICK_MOVE,0);
            check(ThunderWard.equipped(p) && slots(p).getStackInSlot(0).getHoverName().getString().equals("My ward"), "Failed shift move lost custody or custom components");
            p.getInventory().setItem(0,ItemStack.EMPTY);
            click(p,ClickType.SWAP,0);
            check(!ThunderWard.equipped(p) && p.getInventory().getItem(0).is(CoreContent.WARD)
                  && !p.getInventory().getItem(0).has(CoreContent.WARD_SEAL), "Number-key extraction did not release exactly one item");
            p.closeContainer(); WardCustody.ensure(p);
            check(slots(p).getStackInSlot(0).isEmpty(), "Closing the menu restored a deliberately removed ward");
        } finally { ThunderWardGameTests.close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void forcedCopiesCannotEquipOrDropAndManualThrowWorks(GameTestHelper h) {
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20)); var p=f.player();
        try {
            var stale=slots(p).getStackInSlot(0).copy();
            p.getInventory().setItem(1,stale.copy());WardCustody.ensure(p);
            check(p.getInventory().getItem(1).isEmpty(), "A forced inventory copy survived repair");
            var forced=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),stale.copy());
            check(!h.getLevel().addFreshEntity(forced), "Forced world drop created a second physical ward");
            click(p,ClickType.THROW,1);
            check(!ThunderWard.equipped(p), "Deliberate Q-drop could not release custody");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4),e->e.getItem().is(CoreContent.WARD));
            check(drops.size()==1 && !drops.getFirst().getItem().has(CoreContent.WARD_SEAL), "Q-drop did not create exactly one ordinary ward");
            check(!slots(p).isItemValid(0,stale), "Old copy became equipable after legitimate release");
            p.getInventory().setItem(2,stale);p.getInventory().tick();
            check(p.getInventory().getItem(2).isEmpty(), "Stale copy survived in ordinary inventory");
            drops.forEach(Entity::discard);
        } finally { ThunderWardGameTests.close(f); }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void durableSnapshotAndRealDeathRespawnKeepExactlyOneWard(GameTestHelper h) {
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20)); var p=f.player(); ServerPlayer replacement=null;
        try {
            var token=slots(p).getStackInSlot(0).get(CoreContent.WARD_SEAL);
            var encoded=WardLedger.get(p).save(new CompoundTag(),p.registryAccess());
            var decoded=WardLedger.load(encoded,p.registryAccess());
            check(decoded.worn.get(p.getUUID()).token.equals(token), "Independent world record lost its identity in save/load");
            WardCustody.forget(p);
            WardLedger.get(p).worn.put(p.getUUID(),decoded.worn.get(p.getUUID()));
            ((DynamicStackHandler)slots(p)).setSize(1);
            check(ThunderWard.equipped(p) && token.equals(slots(p).getStackInSlot(0).get(CoreContent.WARD_SEAL)), "Loaded world record could not recover missing player inventory");
            p.hurt(p.damageSources().genericKill(),100);
            check(p.getPersistentData().getBoolean(ThunderWard.FINALIZED_KEY) && !p.isAlive(), "No-power death was incorrectly prevented");
            check(slots(p).getStackInSlot(0).is(CoreContent.WARD), "Unfunded death dropped the ward");
            replacement=p.server.getPlayerList().respawn(p,false,Entity.RemovalReason.KILLED);
            // The normal respawn packet handler assigns the return value to its connection after PlayerList returns.
            replacement.connection.player=replacement;
            check(ThunderWard.equipped(replacement) && token.equals(slots(replacement).getStackInSlot(0).get(CoreContent.WARD_SEAL)), "Real respawn lost or reminted the ward");
            check(replacement.getInventory().countItem(CoreContent.WARD.get())==0, "Respawn duplicated a ward into inventory");
            var old=slots(p).getStackInSlot(0).copy();
            click(replacement,ClickType.PICKUP,0);
            check(!ThunderWard.equipped(replacement), "Respawned player's real menu did not release custody");
            check(!slots(replacement).isItemValid(0,old), "Old-life copy remained valid after respawn/manual release");
        } finally {
            if(replacement!=null) { replacement.server.getPlayerList().remove(replacement); ThunderWard.forget(replacement); }
            ThunderWardGameTests.close(f);
        }
        h.succeed();
    }
}
