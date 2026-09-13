package dev.everyonemek.botania;

import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import vazkii.botania.common.entity.ManaSparkEntity;

/** Inherits all native transfer roles, dye, ink, animation and wand operations. */
public final class MechanicalSparkEntity extends ManaSparkEntity {
    private static final EntityDataAccessor<Boolean> MASTER = SynchedEntityData.defineId(MechanicalSparkEntity.class, EntityDataSerializers.BOOLEAN);
    public final SimpleContainer modules = new SimpleContainer(2);
    private int lastGeneration = -1;
    private boolean dropping;
    public MechanicalSparkEntity(EntityType<ManaSparkEntity> type, Level level) {
        super(type, level); modules.addListener(container -> MechanicalSparkNetworks.invalidate(level));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data) { super.defineSynchedData(data); data.define(MASTER, false); }
    public boolean isMaster() { return entityData.get(MASTER); }
    public int upgrade(int slot) { return modules.getItem(slot).is(slot == 0 ? MechanicalSparks.RANGE.get() : MechanicalSparks.EFFICIENCY.get()) ? Math.min(8, modules.getItem(slot).getCount()) : 0; }
    public boolean live() { return !dropping && isAlive() && level().hasChunkAt(getAttachPos()) && getAttachedTile() != null && getAttachedManaReceiver() != null; }
    public boolean accessible(Player player) {
        var pos = getAttachPos(); return live() && IBlockSecurityUtils.INSTANCE.canAccess(player, level(), pos, level().getBlockEntity(pos));
    }
    @Override protected Item getSparkItem() { return isMaster() ? MechanicalSparks.MASTER.get() : MechanicalSparks.SPARK.get(); }
    @Override public void tick() {
        if (!level().isClientSide) {
            MechanicalSparkNetworks.add(this);
            int generation = MechanicalSparkNetworks.generation(level());
            if (generation != lastGeneration) { lastGeneration = generation; updateTransfers(); }
        }
        super.tick();
    }
    public void readItem(ItemStack stack) {
        entityData.set(MASTER, stack.getItem() instanceof MechanicalSparkItem item && item.master);
        if (isMaster()) readModules(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("spark_controller"));
    }
    private void readModules(CompoundTag tag) {
        for (int slot = 0; slot < 2; slot++) modules.setItem(slot, ItemStack.parseOptional(registryAccess(), tag.getCompound("slot" + slot)));
    }
    private CompoundTag saveModules() {
        var tag = new CompoundTag(); for (int slot = 0; slot < 2; slot++) if (!modules.getItem(slot).isEmpty()) tag.put("slot" + slot, modules.getItem(slot).save(registryAccess())); return tag;
    }
    public ItemStack dropStack() {
        var stack = new ItemStack(getSparkItem());
        if (isMaster() && !modules.isEmpty()) { var tag = new CompoundTag(); tag.put("spark_controller", saveModules()); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); }
        dropping = true; modules.clearContent();
        return stack;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("mechanical_master", isMaster()); tag.put("spark_controller", saveModules()); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MASTER, tag.getBoolean("mechanical_master")); super.readAdditionalSaveData(tag); readModules(tag.getCompound("spark_controller"));
    }
    @Override public void setNetwork(DyeColor color) { MechanicalSparkNetworks.invalidate(level()); super.setNetwork(color); }
    @Override public void remove(RemovalReason reason) { MechanicalSparkNetworks.remove(this); super.remove(reason); }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (!accessible(player)) return InteractionResult.FAIL;
        var stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.is(MechanicalSparks.RANGE.get()) || stack.is(MechanicalSparks.EFFICIENCY.get())) {
            if (!level().isClientSide) {
                var network = MechanicalSparkNetworks.network(this); var master = isMaster() ? this : network.master();
                if (master == null) { player.displayClientMessage(Component.translatable("gui.botanicalmekanism.spark." + network.status()), true); }
                else if (!master.accessible(player)) return InteractionResult.FAIL;
                else if (!stack.isEmpty()) {
                    int slot = stack.is(MechanicalSparks.RANGE.get()) ? 0 : 1; var old = master.modules.getItem(slot);
                    if ((old.isEmpty() || ItemStack.isSameItemSameComponents(old, stack)) && old.getCount() < 8) {
                        master.modules.setItem(slot, stack.copyWithCount(old.getCount() + 1)); if (!player.isCreative()) stack.shrink(1);
                    }
                } else if (player instanceof ServerPlayer server) {
                    server.openMenu(new SimpleMenuProvider((id, inventory, unused) -> new SparkControllerMenu(id, inventory, getId(), master.getId()),
                          Component.translatable("gui.botanicalmekanism.spark.title")), data -> { data.writeVarInt(getId()); data.writeVarInt(master.getId()); });
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.interact(player, hand);
    }
}
