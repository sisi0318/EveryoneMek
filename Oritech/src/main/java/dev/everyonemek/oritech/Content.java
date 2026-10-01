package dev.everyonemek.oritech;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.capabilities.*;

@Mod(Content.ID)
public final class Content {
    public static final String ID="oritechmekanism";
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ID,path);}
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,ID);
    public static final Supplier<ProcessorBlock> BLOCK=BLOCKS.register("universal_processor",()->new ProcessorBlock());
    public static final Supplier<PartBlock> PART=BLOCKS.register("processor_part",()->new PartBlock());
    public static final Supplier<ProcessorItem> ITEM=ITEMS.register("universal_processor",ProcessorItem::new);
    public static final Supplier<FluidCapacityAddon> FLUID_CAPACITY=ITEMS.register("fluid_capacity_addon",FluidCapacityAddon::new);
    public static final java.util.List<Supplier<ProcessorUpgrade>> UPGRADES=new java.util.ArrayList<>();
    static{
        for(int tier=Processor.BASE_TIER+1;tier<=Processor.MAX_TIER;tier++){final int target=tier;UPGRADES.add(ITEMS.register("capacity_upgrade_"+tier,()->new ProcessorUpgrade(target)));}
    }
    public static final Supplier<BlockEntityType<Processor>> TILE=TILES.register("universal_processor",()->BlockEntityType.Builder.of(Processor::new,BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<Part>> PART_TILE=TILES.register("processor_part",()->BlockEntityType.Builder.of(Part::new,PART.get()).build(null));
    public static final Supplier<MenuType<ProcessorMenu>> MENU=MENUS.register("universal_processor",()->IMenuTypeExtension.create(ProcessorMenu::new));
    public static final Supplier<MenuType<AddonMenu>> ADDON_MENU=MENUS.register("processor_addons",()->IMenuTypeExtension.create(AddonMenu::new));
    public static final Supplier<DataComponentType<CompoundTag>> DATA=COMPONENTS.register("processor",()->DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());
    public Content(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TILES.register(bus);MENUS.register(bus);COMPONENTS.register(bus);bus.addListener(Content::capabilities);bus.addListener(AddonPackets::register);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{
            if(e.getTab()==rearth.oritech.init.ItemGroups.MACHINE_GROUP||e.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){e.accept(ITEM.get());for(var upgrade:UPGRADES)e.accept(upgrade.get());e.accept(FLUID_CAPACITY.get());}
        });}
    private static void capabilities(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,TILE.get(),(p,s)->new Ports.Items(()->p,s));
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,TILE.get(),(p,s)->new Ports.Energy(()->p,s));
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,TILE.get(),(p,s)->new Ports.Fluids(()->p,s));
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,PART_TILE.get(),(p,s)->new Ports.Items(p::controller,s));
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,PART_TILE.get(),(p,s)->new Ports.Energy(p::controller,s));
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,PART_TILE.get(),(p,s)->new Ports.Fluids(p::controller,s));
    }
}
