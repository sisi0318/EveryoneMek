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
    public static int resourceModuleIndex(ItemStack stack) {
        if (!stack.isEmpty()) for (int i = 0; i < 4; i++) if (stack.is(resourceModule(i))) return i;
        return -1;
    }
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(ForbiddenMekanism.ID);
    public static final BlockRegistryObject<ClibanoPortBlock, net.minecraft.world.item.BlockItem> CLIBANO_PORT = BLOCKS.register("clibano_port", ClibanoPortBlock::new);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> PORT_TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForbiddenMekanism.ID);
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ClibanoPort>> CLIBANO_PORT_TILE = PORT_TILES.register("clibano_port",
          () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(ClibanoPort::new, CLIBANO_PORT.get()).build(null));
    public static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister(ForbiddenMekanism.ID);
    public static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(ForbiddenMekanism.ID);
    public static final DataComponentDeferredRegister COMPONENTS = new DataComponentDeferredRegister(ForbiddenMekanism.ID);
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> SETTINGS = COMPONENTS.simple("settings",
          builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForbiddenMekanism.ID);
    public static final BlockRegistryObject<net.minecraft.world.level.block.Block, net.minecraft.world.item.BlockItem> SOUL_BLOCK = BLOCKS.register("soul_block",
          net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3, 6).sound(net.minecraft.world.level.block.SoundType.AMETHYST));
    public static final BlockRegistryObject<net.minecraft.world.level.block.Block, net.minecraft.world.item.BlockItem> XPETRIFIED_BLOCK = BLOCKS.register("xpetrified_block",
          net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3, 6).sound(net.minecraft.world.level.block.SoundType.STONE));
    public static final net.neoforged.neoforge.registries.DeferredItem<ResourceModuleItem> GLOW_MODULE = ITEMS.register("glow_module", () -> new ResourceModuleItem("glow_module"));
    public static final net.neoforged.neoforge.registries.DeferredItem<ResourceModuleItem> SOUL_MODULE = ITEMS.register("soul_module", () -> new ResourceModuleItem("soul_module"));
    public static final net.neoforged.neoforge.registries.DeferredItem<ResourceModuleItem> BLOOD_MODULE = ITEMS.register("blood_module", () -> new ResourceModuleItem("blood_module"));
    public static final net.neoforged.neoforge.registries.DeferredItem<ResourceModuleItem> EXPERIENCE_MODULE = ITEMS.register("experience_module", () -> new ResourceModuleItem("experience_module"));
    public static final Map<Integer, net.neoforged.neoforge.registries.DeferredItem<ForgeTierInstallerItem>> INSTALLERS = new java.util.LinkedHashMap<>();
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ForbiddenMekanism.ID);
    public static final java.util.function.Supplier<ForgeUpgradeRecipe.Serializer> FORGE_UPGRADE_RECIPE = SERIALIZERS.register("forge_upgrade_crafting", ForgeUpgradeRecipe.Serializer::new);
    public static final ContainerTypeRegistryObject<MachineMenu> MENU = MENUS.register("controller", Controller.class, MachineMenu::new);
    public static final Map<MachineKind, BlockRegistryObject<MachineBlock, ItemBlockTooltip<MachineBlock>>> MACHINES = new EnumMap<>(MachineKind.class);
    public static final Map<MachineKind, TileEntityTypeRegistryObject<Controller>> MACHINE_TILES = new EnumMap<>(MachineKind.class);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForbiddenMekanism.ID);

    static {
        for (int tier = 2; tier <= 5; tier++) {
            final int target = tier;
            INSTALLERS.put(tier, ITEMS.register("forge_tier_" + tier + "_installer", () -> new ForgeTierInstallerItem(target)));
        }
        for (MachineKind kind : MachineKind.values()) {
            Machine<Controller> type = Machine.MachineBuilder.<Controller>createMachine(() -> MACHINE_TILES.get(kind), kind)
                  .withGui(() -> MENU)
                  .withEnergyConfig(() -> EnergyUnit.FORGE_ENERGY.convertTo((kind.forge() ? MachineConfig.FORGE_FE : MachineConfig.OPERATION_FE).get()),
                        () -> EnergyUnit.FORGE_ENERGY.convertTo(1_000_000L))
                  .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                  .with(AttributeSideConfig.ADVANCED_ELECTRIC_MACHINE).build();
            var block = BLOCKS.registerDetails(kind.id, () -> kind.forge() ? new MachineBlock(kind, type) : new ClibanoControllerBlock(type));
            block.forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> {
                var slots = ItemSlotsBuilder.builder().addInput(9).addOutput(4).addInput(kind.supplies()).addEnergy();
                if (kind.forge()) slots.addInput(8);
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
                  output.accept(CLIBANO_PORT);
                  for (int resource = 0; resource < 4; resource++) output.accept(resourceModule(resource));
                  INSTALLERS.values().forEach(output::accept);
                  output.accept(SOUL_BLOCK); output.accept(XPETRIFIED_BLOCK);
              }).build());
    }
    public static void register(IEventBus bus) {
        COMPONENTS.register(bus); ITEMS.register(bus); BLOCKS.register(bus); TILES.register(bus); MENUS.register(bus); TABS.register(bus); SERIALIZERS.register(bus);
        PORT_TILES.register(bus);
    }
    public static Item resourceModule(int resource) {
        return switch (resource) {
            case 0 -> GLOW_MODULE.get();
            case 1 -> SOUL_MODULE.get();
            case 2 -> BLOOD_MODULE.get();
            case 3 -> EXPERIENCE_MODULE.get();
            default -> throw new IllegalArgumentException("Unknown forge resource: " + resource);
        };
    }
    private Content() { }
}
