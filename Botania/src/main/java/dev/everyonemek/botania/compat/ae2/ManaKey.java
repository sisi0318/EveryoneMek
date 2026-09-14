package dev.everyonemek.botania.compat.ae2;

import java.util.List;
import appeng.api.stacks.*;
import com.mojang.serialization.MapCodec;
import dev.everyonemek.botania.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ManaKey extends AEKey {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "mana");
    public static final ManaKey INSTANCE = new ManaKey();
    public static final AEKeyType TYPE = new AEKeyType(ID, ManaKey.class, Component.translatable("gui.botanicalmekanism.mana_type")) {
        @Override public MapCodec<ManaKey> codec() { return MapCodec.unit(INSTANCE); }
        @Override public AEKey readFromPacket(RegistryFriendlyByteBuf buffer) { return INSTANCE; }
        @Override public int getAmountPerOperation() { return ManaCellTier.MANA_PER_OPERATION; }
        @Override public int getAmountPerByte() { return ManaCellTier.MANA_PER_BYTE; }
        @Override public int getAmountPerUnit() { return ManaCellTier.MANA_PER_POOL; }
        @Override public String getUnitSymbol() { return "pool"; }
    };
    @Override public AEKeyType getType() { return TYPE; }
    @Override public AEKey dropSecondary() { return this; }
    @Override public CompoundTag toTag(HolderLookup.Provider registries) { return new CompoundTag(); }
    @Override public Object getPrimaryKey() { return this; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("botania", "mana"); }
    @Override public void writeToPacket(RegistryFriendlyByteBuf data) { }
    @Override protected Component computeDisplayName() { return Component.translatable("gui.botanicalmekanism.mana_type"); }
    @Override public boolean hasComponents() { return false; }
    @Override public String formatAmount(long amount, AmountFormat format) {
        if (format != AmountFormat.FULL) return super.formatAmount(amount, format);
        double pools = amount / (double) getType().getAmountPerUnit();
        return java.text.NumberFormat.getNumberInstance().format(pools) + (pools == 1 ? " pool" : " pools");
    }
    @Override public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) { if (amount > 0) drops.add(ManaStorageItem.recovery(amount)); }
    private ManaKey() { }
}
