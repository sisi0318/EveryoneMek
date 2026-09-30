package dev.everyonemek.overloadcore;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CoreContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OverloadCore.ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, OverloadCore.ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OverloadCore.ID);
    public static final net.minecraft.tags.TagKey<Item> RECOVERY_CHARGEABLE = net.minecraft.tags.TagKey.create(Registries.ITEM,
          net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "recovery_chargeable"));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> DATA = COMPONENTS.register("core_data",
          () -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> MACHINE = COMPONENTS.register("machine_data",
          () -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> WARD_SEAL = COMPONENTS.register("ward_seal",
          () -> DataComponentType.<java.util.UUID>builder().persistent(net.minecraft.core.UUIDUtil.CODEC)
                .networkSynchronized(net.minecraft.core.UUIDUtil.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> GEAR_ENERGY = COMPONENTS.register("gear_energy",
          () -> DataComponentType.<Long>builder().persistent(com.mojang.serialization.Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RAIL_AMMO = COMPONENTS.register("rail_ammo",
          () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredItem<CoreItem> CORE = ITEMS.register("overloaded_short_circuit_core",
          () -> new CoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<ThunderWardItem> WARD = ITEMS.register("thunder_ward",
          () -> new ThunderWardItem(mekanism.api.gear.IModuleHelper.INSTANCE.applyModuleContainerProperties(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))));
    public static final DeferredItem<mekanism.common.item.ItemModule> COUPLING_MODULE = ITEMS.register("module_residual_coupling_unit",
          () -> new mekanism.common.item.ItemModule(() -> dev.everyonemek.overloadcore.gear.EquipmentModules.RESIDUAL_COUPLING,
                new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<mekanism.common.item.ItemModule> COMBAT_MODULE=ITEMS.register("module_combat_form",
        ()->new mekanism.common.item.ItemModule(()->dev.everyonemek.overloadcore.gear.EquipmentModules.COMBAT,new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<dev.everyonemek.overloadcore.gear.LegacyWeaponItem> RAILGUN = ITEMS.register("rail_lance", () -> new dev.everyonemek.overloadcore.gear.LegacyWeaponItem(true));
    public static final DeferredItem<dev.everyonemek.overloadcore.gear.LegacyWeaponItem> BLADE = ITEMS.register("thunder_blade", () -> new dev.everyonemek.overloadcore.gear.LegacyWeaponItem(false));
    public static final java.util.Map<dev.everyonemek.overloadcore.gear.GearUpgrade, DeferredItem<mekanism.common.item.ItemModule>> UPGRADE_ITEMS =
          new java.util.EnumMap<>(dev.everyonemek.overloadcore.gear.GearUpgrade.class);
    static {
        for (var upgrade : dev.everyonemek.overloadcore.gear.GearUpgrade.values()) UPGRADE_ITEMS.put(upgrade, ITEMS.register("module_" + upgrade.id,
              () -> new mekanism.common.item.ItemModule(() -> dev.everyonemek.overloadcore.gear.EquipmentModules.get(upgrade), new Item.Properties().rarity(Rarity.RARE))));
        TABS.register("core", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.overloadcore"))
              .icon(() -> new ItemStack(CORE.get())).displayItems((params, output) -> { output.accept(CORE); output.accept(WARD); output.accept(COUPLING_MODULE); output.accept(COMBAT_MODULE); UPGRADE_ITEMS.forEach((kind,item)->{if(kind.targets!=0)output.accept(item);}); }).build());
    }
    public static net.minecraft.network.chat.MutableComponent text(String key, Object... args) { return Component.translatable("overloadcore." + key, args); }
    public static void register(IEventBus bus) { ITEMS.register(bus); COMPONENTS.register(bus); TABS.register(bus); }
    private CoreContent() { }
}
