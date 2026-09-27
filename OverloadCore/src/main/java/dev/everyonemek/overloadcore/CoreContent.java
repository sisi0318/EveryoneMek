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
    public static final DeferredItem<CoreItem> CORE = ITEMS.register("overloaded_short_circuit_core",
          () -> new CoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<ThunderWardItem> WARD = ITEMS.register("thunder_ward",
          () -> new ThunderWardItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<mekanism.common.item.ItemModule> COUPLING_MODULE = ITEMS.register("module_residual_coupling_unit",
          () -> new mekanism.common.item.ItemModule(() -> dev.everyonemek.overloadcore.gear.EquipmentModules.RESIDUAL_COUPLING,
                new Item.Properties().rarity(Rarity.UNCOMMON)));
    static {
        TABS.register("core", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.overloadcore"))
              .icon(() -> new ItemStack(CORE.get())).displayItems((params, output) -> { output.accept(CORE); output.accept(WARD); output.accept(COUPLING_MODULE); }).build());
    }
    public static net.minecraft.network.chat.MutableComponent text(String key, Object... args) { return Component.translatable("overloadcore." + key, args); }
    public static void register(IEventBus bus) { ITEMS.register(bus); COMPONENTS.register(bus); TABS.register(bus); }
    private CoreContent() { }
}
