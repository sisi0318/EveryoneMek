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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> DATA = COMPONENTS.register("core_data",
          () -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> MACHINE = COMPONENTS.register("machine_data",
          () -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    public static final DeferredItem<CoreItem> CORE = ITEMS.register("overloaded_short_circuit_core",
          () -> new CoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    static {
        TABS.register("core", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.overloadcore"))
              .icon(() -> new ItemStack(CORE.get())).displayItems((params, output) -> output.accept(CORE)).build());
    }
    public static net.minecraft.network.chat.MutableComponent text(String key, Object... args) { return Component.translatable("overloadcore." + key, args); }
    public static void register(IEventBus bus) { ITEMS.register(bus); COMPONENTS.register(bus); TABS.register(bus); }
    private CoreContent() { }
}
