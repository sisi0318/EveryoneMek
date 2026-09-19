package dev.everyonemek.factory;
import java.util.*;
import mekanism.api.Upgrade;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.item.ItemSlotsBuilder;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class Content {
    public static final BlockDeferredRegister BLOCKS=new BlockDeferredRegister(MekFactory.ID);
    public static final TileEntityTypeDeferredRegister TILES=new TileEntityTypeDeferredRegister(MekFactory.ID);
    public static final ContainerTypeDeferredRegister MENUS=new ContainerTypeDeferredRegister(MekFactory.ID);
    private static final DataComponentDeferredRegister COMPONENTS=new DataComponentDeferredRegister(MekFactory.ID);
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> DATA=COMPONENTS.simple("factory_data",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> PORT_DATA=COMPONENTS.simple("port_data",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final ContainerTypeRegistryObject<FactoryMenu> MENU=MENUS.registerMenu("factory",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(FactoryMenu::fromNetwork));
    public static final ContainerTypeRegistryObject<WarehouseMenu> WAREHOUSE_MENU=MENUS.registerMenu("warehouse",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(WarehouseMenu::fromNetwork));
    public static final Map<Grade,BlockRegistryObject<ControllerBlock,ItemBlockTooltip<ControllerBlock>>> CONTROLLERS=new EnumMap<>(Grade.class);
    public static final Map<Grade,TileEntityTypeRegistryObject<Controller>> CONTROLLER_TILES=new EnumMap<>(Grade.class);
    public static final Map<Grade,BlockRegistryObject<PartBlock,BlockItem>> FRAMES=new EnumMap<>(Grade.class),PORTS=new EnumMap<>(Grade.class);
    public static final BlockRegistryObject<PartBlock,BlockItem> CASING=BLOCKS.register("casing",()->new PartBlock(PartBlock.Kind.CASING,Grade.BASIC));
    public static final BlockRegistryObject<PartBlock,BlockItem> GLASS=BLOCKS.register("glass",()->new PartBlock(PartBlock.Kind.GLASS,Grade.BASIC));
    private static final DeferredRegister<BlockEntityType<?>> PART_TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MekFactory.ID);
    public static final java.util.function.Supplier<BlockEntityType<Part>> PART=PART_TYPES.register("part",()->{
        var blocks=new ArrayList<net.minecraft.world.level.block.Block>();blocks.add(CASING.get());blocks.add(GLASS.get());
        FRAMES.values().forEach(b->blocks.add(b.get()));PORTS.values().forEach(b->blocks.add(b.get()));
        return BlockEntityType.Builder.of(Part::new,blocks.toArray(net.minecraft.world.level.block.Block[]::new)).build(null);
    });
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MekFactory.ID);
    static {
        for(var g:Grade.values()) {
            var machine=Machine.MachineBuilder.<Controller>createMachine(()->CONTROLLER_TILES.get(g),g).withGui(()->MENU)
                  .withEnergyConfig(()->0L,()->0L).withSupportedUpgrades(Upgrade.SPEED,Upgrade.ENERGY,Upgrade.CHEMICAL).build();
            var block=BLOCKS.registerDetails(g.id+"_controller",()->new ControllerBlock(g,machine));
            block.forItemHolder(h->h.addAttachmentOnlyContainers(ContainerType.ITEM,()->ItemSlotsBuilder.builder().addInput(1).build()));
            CONTROLLERS.put(g,block);CONTROLLER_TILES.put(g,TILES.mekBuilder(block,Controller::new).clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build());
            FRAMES.put(g,BLOCKS.register(g.id+"_frame",()->new PartBlock(PartBlock.Kind.FRAME,g)));
            PORTS.put(g,BLOCKS.register(g.id+"_port",()->new PartBlock(PartBlock.Kind.PORT,g)));
        }
        TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.mekfactory")).icon(()->new ItemStack(CONTROLLERS.get(Grade.BASIC)))
              .displayItems((p,out)->{for(var g:Grade.values()){out.accept(CONTROLLERS.get(g));out.accept(FRAMES.get(g));out.accept(PORTS.get(g));}out.accept(CASING);out.accept(GLASS);}).build());
    }
    public static Component text(String key,Object... args){return Component.translatable("mekfactory."+key,args);}
    public static void register(IEventBus bus){COMPONENTS.register(bus);BLOCKS.register(bus);TILES.register(bus);PART_TYPES.register(bus);MENUS.register(bus);TABS.register(bus);}
    private Content(){}
}
