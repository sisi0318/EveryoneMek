package dev.everyonemek.natures;

import java.util.EnumMap;
import java.util.Map;
import mekanism.api.Upgrade;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import mekanism.common.block.attribute.AttributeSideConfig;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.*;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public final class Content {
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(NaturesMekanism.ID);
    public static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister(NaturesMekanism.ID);
    public static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(NaturesMekanism.ID);
    public static final ChemicalDeferredRegister CHEMICALS = new ChemicalDeferredRegister(NaturesMekanism.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NaturesMekanism.ID);
    public static final DeferredItem<InfiniteGoldModuleItem> INFINITE_GOLD_MODULE = ITEMS.register("infinite_gold_module",
          () -> new InfiniteGoldModuleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DataComponentDeferredRegister COMPONENTS = new DataComponentDeferredRegister(NaturesMekanism.ID);
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ENVIRONMENT_OUTPUT = COMPONENTS.registerBoolean("environment_output");
    public static final DeferredChemical<Chemical> AURA = CHEMICALS.register("aura", () -> new Chemical(ChemicalBuilder.builder().tint(0x89CC37)));
    public static final ContainerTypeRegistryObject<MachineMenu> MENU = MENUS.register("machine", AuraMachine.class, MachineMenu::new);
    public static final Map<MachineKind, BlockRegistryObject<MachineBlock, ItemBlockTooltip<MachineBlock>>> MACHINES = new EnumMap<>(MachineKind.class);
    public static final Map<MachineKind, TileEntityTypeRegistryObject<AuraMachine>> MACHINE_TILES = new EnumMap<>(MachineKind.class);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NaturesMekanism.ID);

    static {
        for (MachineKind kind : MachineKind.values()) {
            Machine<AuraMachine> type = Machine.MachineBuilder.<AuraMachine>createMachine(() -> MACHINE_TILES.get(kind), kind)
                  .withGui(() -> MENU)
                  .withEnergyConfig(() -> EnergyUnit.FORGE_ENERGY.convertTo(MachineConfig.baseFE(kind)),
                        () -> EnergyUnit.FORGE_ENERGY.convertTo(4_000_000L))
                  .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                  .with(kind == MachineKind.AURA_GENERATOR || kind == MachineKind.NATURAL_ALTAR
                        ? AttributeSideConfig.ADVANCED_ELECTRIC_MACHINE : AttributeSideConfig.ELECTRIC_MACHINE)
                  .build();
            var block = BLOCKS.registerDetails(kind.id, () -> new MachineBlock(kind, type));
            MACHINES.put(kind, block);
            MACHINE_TILES.put(kind, TILES.mekBuilder(block, AuraMachine::new)
                  .clientTicker(TileEntityMekanism::tickClient)
                  .serverTicker(TileEntityMekanism::tickServer).build());
        }
        TABS.register("main", () -> CreativeModeTab.builder()
              .title(Component.translatable("itemGroup.naturesmekanism"))
              .icon(() -> new ItemStack(MACHINES.get(MachineKind.AURA_GENERATOR).asItem()))
              .displayItems((parameters, output) -> {
                  for (MachineKind kind : MachineKind.values()) output.accept(MACHINES.get(kind).asItem());
                  output.accept(INFINITE_GOLD_MODULE);
              }).build());
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ITEMS.register(bus);
        CHEMICALS.register(bus);
        BLOCKS.register(bus);
        TILES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }

    private Content() { }
}
