package dev.everyonemek.forbidden;

import java.util.EnumMap;
import java.util.Map;
import mekanism.api.Upgrade;
import mekanism.common.attachments.containers.ContainerType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Content {
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(ForbiddenMekanism.ID);
    public static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister(ForbiddenMekanism.ID);
    public static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(ForbiddenMekanism.ID);
    public static final DataComponentDeferredRegister COMPONENTS = new DataComponentDeferredRegister(ForbiddenMekanism.ID);
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> SETTINGS = COMPONENTS.simple("settings",
          builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForbiddenMekanism.ID);
    public static final net.neoforged.neoforge.registries.DeferredItem<InfiniteHammerModuleItem> INFINITE_HAMMER_MODULE = ITEMS.register("infinite_hammer_module", InfiniteHammerModuleItem::new);
    public static final ContainerTypeRegistryObject<MachineMenu> MENU = MENUS.register("controller", Controller.class, MachineMenu::new);
    public static final Map<MachineKind, BlockRegistryObject<MachineBlock, ItemBlockTooltip<MachineBlock>>> MACHINES = new EnumMap<>(MachineKind.class);
    public static final Map<MachineKind, TileEntityTypeRegistryObject<Controller>> MACHINE_TILES = new EnumMap<>(MachineKind.class);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForbiddenMekanism.ID);

    static {
        for (MachineKind kind : MachineKind.values()) {
            Machine<Controller> type = Machine.MachineBuilder.<Controller>createMachine(() -> MACHINE_TILES.get(kind), kind)
                  .withGui(() -> MENU)
                  .withEnergyConfig(() -> EnergyUnit.FORGE_ENERGY.convertTo(MachineConfig.OPERATION_FE.get()),
                        () -> EnergyUnit.FORGE_ENERGY.convertTo(1_000_000L))
                  .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                  .with(AttributeSideConfig.ADVANCED_ELECTRIC_MACHINE).build();
            var block = BLOCKS.registerDetails(kind.id, () -> new MachineBlock(kind, type));
            block.forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> {
                var slots = ItemSlotsBuilder.builder().addInput(9).addOutput(4).addInput(kind.supplies()).addEnergy();
                if (kind.forge()) slots.addInput(2);
                return slots.build();
            }));
            MACHINES.put(kind, block);
            MACHINE_TILES.put(kind, TILES.mekBuilder(block, Controller::new)
                  .clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build());
        }
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.forbiddenmekanism"))
              .icon(() -> new ItemStack(MACHINES.get(MachineKind.FORGE)))
              .displayItems((parameters, output) -> {
                  for (MachineKind kind : MachineKind.values()) output.accept(MACHINES.get(kind));
                  output.accept(INFINITE_HAMMER_MODULE);
              }).build());
    }
    public static void register(IEventBus bus) {
        COMPONENTS.register(bus); ITEMS.register(bus); BLOCKS.register(bus); TILES.register(bus); MENUS.register(bus); TABS.register(bus);
    }
    private Content() { }
}
