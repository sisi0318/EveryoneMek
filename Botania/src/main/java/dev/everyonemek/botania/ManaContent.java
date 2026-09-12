package dev.everyonemek.botania;

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
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;

public final class ManaContent {
    private static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(BotanicalMekanism.ID);
    private static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister(BotanicalMekanism.ID);
    private static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(BotanicalMekanism.ID);
    private static final ChemicalDeferredRegister CHEMICALS = new ChemicalDeferredRegister(BotanicalMekanism.ID);
    private static final DataComponentDeferredRegister COMPONENTS = new DataComponentDeferredRegister(BotanicalMekanism.ID);
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> SETTINGS = COMPONENTS.simple("machine_settings",
          builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final DeferredChemical<Chemical> MANA = CHEMICALS.register("mana", () -> new Chemical(ChemicalBuilder.builder().tint(0x56C6D7)));
    public static final ContainerTypeRegistryObject<ManaMachineMenu> MENU = MENUS.register("mana_machine", ManaMachine.class, ManaMachineMenu::new);
    public static final Map<ManaMachineKind, BlockRegistryObject<ManaMachineBlock, ItemBlockTooltip<ManaMachineBlock>>> MACHINES = new EnumMap<>(ManaMachineKind.class);
    public static final Map<ManaMachineKind, TileEntityTypeRegistryObject<ManaMachine>> MACHINE_TILES = new EnumMap<>(ManaMachineKind.class);
    static {
        for (var kind : ManaMachineKind.values()) {
            Machine<ManaMachine> type = Machine.MachineBuilder.<ManaMachine>createMachine(() -> MACHINE_TILES.get(kind), () -> kind.getTranslationKey())
                  .withGui(() -> MENU).withEnergyConfig(() -> EnergyUnit.FORGE_ENERGY.convertFrom(50L), () -> EnergyUnit.FORGE_ENERGY.convertFrom(200000L))
                  .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                  .with(AttributeSideConfig.create(TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.ENERGY)).build();
            var block = BLOCKS.registerDetails(kind.id, () -> new ManaMachineBlock(kind, type));
            block.forItemHolder(holder -> {
                holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> {
                    var slots = ItemSlotsBuilder.builder();
                    if (kind.inputs + kind.extras > 0) slots.addInput(kind.inputs + kind.extras);
                    if (kind.outputs > 0) slots.addOutput(kind.outputs);
                    return slots.addEnergy().build();
                });
                if (kind.chemical) holder.addAttachmentOnlyContainers(ContainerType.CHEMICAL, () -> ChemicalTanksBuilder.builder()
                      .addBasic(ManaMachine.MANA_CAPACITY, stack -> stack.is(MANA)).build());
            });
            MACHINES.put(kind, block);
            MACHINE_TILES.put(kind, TILES.mekBuilder(block, ManaMachine::new)
                  .clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build());
        }
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); TILES.register(bus); MENUS.register(bus); CHEMICALS.register(bus); COMPONENTS.register(bus); }
    private ManaContent() { }
}
