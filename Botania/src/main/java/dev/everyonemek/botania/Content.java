package dev.everyonemek.botania;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import vazkii.botania.api.block.WandBindable;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;
import vazkii.botania.common.block.block_entity.BotaniaBlockEntities;

public final class Content {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BotanicalMekanism.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BotanicalMekanism.ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BotanicalMekanism.ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BotanicalMekanism.ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BotanicalMekanism.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BotanicalMekanism.ID);

    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS);
    }
    public static final DeferredBlock<PoweredPlantBlock> LOTUS = BLOCKS.register("mana_lotus",
          () -> new PoweredPlantBlock(plant(), () -> Content.LOTUS_TILE.get()));
    public static final DeferredBlock<PoweredPlantBlock> AMARANTHUS = BLOCKS.register("bionic_amaranthus",
          () -> new PoweredPlantBlock(plant(), () -> BotaniaBlockEntities.JADED_AMARANTHUS));
    public static final DeferredBlock<NetworkPlantBlock> CORE = BLOCKS.register("resonance_flower", () -> new NetworkPlantBlock(plant(), true));
    public static final DeferredBlock<NetworkPlantBlock> NODE = BLOCKS.register("resonance_bud", () -> new NetworkPlantBlock(plant(), false));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaLotus>> LOTUS_TILE = TILES.register("mana_lotus",
          () -> BlockEntityType.Builder.of(ManaLotus::new, LOTUS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkPlant>> NETWORK_TILE = TILES.register("network_plant",
          () -> BlockEntityType.Builder.of(NetworkPlant::new, CORE.get(), NODE.get()).build(null));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> STATE = COMPONENTS.register("flower_state",
          () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());
    public static final DeferredHolder<MenuType<?>, MenuType<FlowerMenu>> MENU = MENUS.register("flower",
          () -> IMenuTypeExtension.create((id, inventory, buffer) -> new FlowerMenu(id, inventory, buffer.readBlockPos())));

    static {
        for (var block : new DeferredBlock<?>[]{LOTUS, AMARANTHUS, CORE, NODE})
            ITEMS.register(block.getId().getPath(), () -> new PlantItem(block.get(), new Item.Properties()));
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.botanicalmekanism"))
              .icon(() -> new ItemStack(LOTUS.get())).displayItems((parameters, output) -> {
                  for (var block : new DeferredBlock<?>[]{LOTUS, AMARANTHUS, CORE, NODE}) output.accept(block.get());
                  output.accept(ApothecaryContent.BLOCK);
              }).build());
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); TILES.register(bus); MENUS.register(bus); COMPONENTS.register(bus); TABS.register(bus);
        bus.addListener(Content::capabilities);
        bus.addListener((BlockEntityTypeAddBlocksEvent event) -> event.modify(BotaniaBlockEntities.JADED_AMARANTHUS, AMARANTHUS.get()));
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LOTUS_TILE.get(), (tile, side) -> Flowers.energy(tile));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BotaniaBlockEntities.JADED_AMARANTHUS,
              (tile, side) -> Flowers.isAmaranthus(tile) ? Flowers.energy(tile) : null);
        event.registerBlockEntity(BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandBindable.LOOKUP), LOTUS_TILE.get(), (tile, side) -> tile);
    }
    private Content() { }
}
