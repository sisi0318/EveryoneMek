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
    public static final DeferredBlock<PoweredPlantBlock> CLAYCONIA = BLOCKS.register("bionic_clayconia",
          () -> new PoweredPlantBlock(plant(), () -> BotaniaBlockEntities.CLAYCONIA));
    public static final DeferredBlock<PoweredPlantBlock> AGRICARNATION = BLOCKS.register("bionic_agricarnation",
          () -> new PoweredPlantBlock(plant(), () -> BotaniaBlockEntities.AGRICARNATION));
    public static final DeferredBlock<PoweredPlantBlock.Hopper> HOPPERHOCK = BLOCKS.register("bionic_hopperhock", () -> new PoweredPlantBlock.Hopper(plant()));
    public static final DeferredBlock<PoweredPlantBlock.Placer> RANNUNCARPUS = BLOCKS.register("bionic_rannuncarpus", () -> new PoweredPlantBlock.Placer(plant()));
    public static final DeferredBlock<PoweredPlantBlock> EXOFLAME = BLOCKS.register("bionic_exoflame",
          () -> new PoweredPlantBlock(plant(), () -> BotaniaBlockEntities.EXOFLAME));
    public static final DeferredBlock<NetworkPlantBlock> CORE = BLOCKS.register("resonance_flower", () -> new NetworkPlantBlock(plant(), true));
    public static final DeferredBlock<NetworkPlantBlock> NODE = BLOCKS.register("resonance_bud", () -> new NetworkPlantBlock(plant(), false));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaLotus>> LOTUS_TILE = TILES.register("mana_lotus",
          () -> BlockEntityType.Builder.of(ManaLotus::new, LOTUS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkPlant>> NETWORK_TILE = TILES.register("network_plant",
          () -> BlockEntityType.Builder.of(NetworkPlant::new, CORE.get(), NODE.get()).build(null));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> STATE = COMPONENTS.register("flower_state",
          () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SPARK_RANGE = COMPONENTS.register("spark_range",
          () -> DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
    public static final DeferredItem<Item> SPARK_AUGMENT = ITEMS.register("resonance_spark_augment",
          () -> new Item(new Item.Properties().component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(vazkii.botania.common.component.BotaniaDataComponents.AUGMENT_ICON,
                      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("botania", "item/spark_star"))));
    public static final DeferredHolder<MenuType<?>, MenuType<FlowerMenu>> MENU = MENUS.register("flower",
          () -> IMenuTypeExtension.create((id, inventory, buffer) -> new FlowerMenu(id, inventory, buffer.readBlockPos())));

    static {
        for (var block : plants())
            ITEMS.register(block.getId().getPath(), () -> new PlantItem(block.get(), new Item.Properties()));
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.botanicalmekanism"))
              .icon(() -> new ItemStack(LOTUS.get())).displayItems((parameters, output) -> {
                  output.accept(LOTUS.get());
                  for (var block : bionics()) output.accept(block.get());
                  output.accept(SPARK_AUGMENT.get());
                  output.accept(ApothecaryContent.BLOCK);
                  for (var block : ManaContent.MACHINES.values()) output.accept(block);
              }).build());
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); TILES.register(bus); MENUS.register(bus); COMPONENTS.register(bus); TABS.register(bus);
        bus.addListener(Content::capabilities);
        bus.addListener((BlockEntityTypeAddBlocksEvent event) -> {
            var blocks = bionics(); var types = bionicTypes();
            for (int i = 0; i < blocks.length; i++) event.modify(types[i], blocks[i].get());
        });
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LOTUS_TILE.get(), (tile, side) -> Flowers.energy(tile));
        for (var type : bionicTypes()) event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
              (tile, side) -> Flowers.isBionic(tile) ? Flowers.energy(tile) : null);
        event.registerBlockEntity(BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandBindable.LOOKUP), LOTUS_TILE.get(), (tile, side) -> tile);
    }
    public static DeferredBlock<?>[] bionics() { return new DeferredBlock<?>[]{AMARANTHUS, CLAYCONIA, AGRICARNATION, HOPPERHOCK, RANNUNCARPUS, EXOFLAME}; }
    public static DeferredBlock<?>[] plants() { return new DeferredBlock<?>[]{LOTUS, AMARANTHUS, CLAYCONIA, AGRICARNATION, HOPPERHOCK, RANNUNCARPUS, EXOFLAME, CORE, NODE}; }
    private static BlockEntityType<?>[] bionicTypes() { return new BlockEntityType<?>[]{BotaniaBlockEntities.JADED_AMARANTHUS, BotaniaBlockEntities.CLAYCONIA,
          BotaniaBlockEntities.AGRICARNATION, BotaniaBlockEntities.HOPPERHOCK, BotaniaBlockEntities.RANNUNCARPUS, BotaniaBlockEntities.EXOFLAME}; }
    private Content() { }
}
