package dev.everyonemek.ars;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import mekanism.api.Upgrade;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.chemical.ChemicalTanksBuilder;
import mekanism.common.attachments.containers.item.ItemSlotsBuilder;
import mekanism.common.block.attribute.AttributeSideConfig;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.impl.*;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class Content {
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(ArsMekanism.ID);
    public static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister(ArsMekanism.ID);
    public static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(ArsMekanism.ID);
    public static final ChemicalDeferredRegister CHEMICALS = new ChemicalDeferredRegister(ArsMekanism.ID);
    public static final DataComponentDeferredRegister COMPONENTS = new DataComponentDeferredRegister(ArsMekanism.ID);
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> SETTINGS = COMPONENTS.simple("settings",
          builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FE_ENERGY = COMPONENTS.simple("fe_energy",
          builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    private static final DeferredRegister.Blocks SOURCE_BLOCKS = DeferredRegister.createBlocks(ArsMekanism.ID);
    private static final DeferredRegister.Items SOURCE_ITEMS = DeferredRegister.createItems(ArsMekanism.ID);
    private static final DeferredRegister<BlockEntityType<?>> SOURCE_TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsMekanism.ID);
    public static final net.neoforged.neoforge.registries.DeferredBlock<FeSourcelinkBlock> FE_SOURCELINK =
          SOURCE_BLOCKS.register("fe_sourcelink", FeSourcelinkBlock::new);
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> FE_SOURCELINK_ITEM =
          SOURCE_ITEMS.registerSimpleBlockItem(FE_SOURCELINK);
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<FeSourcelinkBlockEntity>> FE_SOURCELINK_TILE =
          SOURCE_TILES.register("fe_sourcelink", () -> BlockEntityType.Builder.of(FeSourcelinkBlockEntity::new, FE_SOURCELINK.get()).build(null));
    public static final DeferredChemical<Chemical> SOURCE = CHEMICALS.register("source", () -> new Chemical(ChemicalBuilder.builder().tint(0xA866D6)));
    public static final ContainerTypeRegistryObject<MachineMenu> MENU = MENUS.register("machine", SourceMachine.class, MachineMenu::new);
    public static final Map<MachineKind, BlockRegistryObject<MachineBlock, ItemBlockTooltip<MachineBlock>>> MACHINES = new EnumMap<>(MachineKind.class);
    public static final Map<MachineKind, TileEntityTypeRegistryObject<SourceMachine>> MACHINE_TILES = new EnumMap<>(MachineKind.class);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsMekanism.ID);

    static {
        for (MachineKind kind : MachineKind.values()) {
            Machine<SourceMachine> type = Machine.MachineBuilder.<SourceMachine>createMachine(() -> MACHINE_TILES.get(kind), kind)
                  .withGui(() -> MENU)
                  .withEnergyConfig(() -> EnergyUnit.FORGE_ENERGY.convertTo(MachineConfig.baseFE(kind)),
                        () -> EnergyUnit.FORGE_ENERGY.convertTo(4_000_000L))
                  .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                  .with(AttributeSideConfig.ADVANCED_ELECTRIC_MACHINE).build();
            var block = BLOCKS.registerDetails(kind.id, () -> new MachineBlock(kind, type));
            block.forItemHolder(holder -> {
                // Block item containers must match the entity order for saved inventories and Mek tooltips.
                holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> {
                    var slots = ItemSlotsBuilder.builder();
                    if (kind.inputCount() > 0) slots.addInput(kind.inputCount());
                    if (kind.outputCount() > 0) slots.addOutput(kind.outputCount());
                    slots.addEnergy();
                    if (kind == MachineKind.GLYPH_SCRIBE) slots.addInput(1);
                    if (kind == MachineKind.DRYGMY_STATION) slots.addInput(DrygmyHarvest.JAR_SLOTS);
                    return slots.build();
                });
                if (kind.usesSource()) holder.addAttachmentOnlyContainers(ContainerType.CHEMICAL, () -> ChemicalTanksBuilder.builder()
                      .addBasic(SourceMachine.SOURCE_CAPACITY, stack -> stack.is(SOURCE)).build());
            });
            MACHINES.put(kind, block);
            var tileBuilder = TILES.mekBuilder(block, SourceMachine::new)
                  .clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer);
            if (kind.worldController()) tileBuilder.without(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK)
                  .with(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, (tile, side) -> tile.collectionCapability(side));
            MACHINE_TILES.put(kind, tileBuilder.build());
        }
        TABS.register("main", () -> CreativeModeTab.builder()
              .title(Component.translatable("itemGroup.arsmekanism"))
              .icon(() -> new ItemStack(MACHINES.get(MachineKind.SOURCE_GENERATOR)))
              .displayItems((parameters, output) -> {
                  for (MachineKind kind : MachineKind.values()) output.accept(MACHINES.get(kind));
                  output.accept(FE_SOURCELINK_ITEM);
              }).build());
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        CHEMICALS.register(bus);
        BLOCKS.register(bus);
        TILES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        SOURCE_BLOCKS.register(bus);
        SOURCE_ITEMS.register(bus);
        SOURCE_TILES.register(bus);
        bus.addListener(Content::registerSourceCapabilities);
    }

    private static void registerSourceCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
              FE_SOURCELINK_TILE.get(), (tile, side) -> tile.energy());
    }
    private Content() { }
}
