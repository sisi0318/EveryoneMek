package dev.everyonemek.gravity;
import java.util.*;
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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class Content {
    public static final BlockDeferredRegister BLOCKS=new BlockDeferredRegister(MekGravity.ID);
    public static final TileEntityTypeDeferredRegister TILES=new TileEntityTypeDeferredRegister(MekGravity.ID);
    public static final ContainerTypeDeferredRegister MENUS=new ContainerTypeDeferredRegister(MekGravity.ID);
    private static final DataComponentDeferredRegister COMPONENTS=new DataComponentDeferredRegister(MekGravity.ID);
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> DATA=COMPONENTS.simple("reactor_data",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> STOCK=COMPONENTS.simple("fuel_stock",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final ContainerTypeRegistryObject<ReactorMenu> MENU=MENUS.registerMenu("reactor",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(ReactorMenu::fromNetwork));
    public static final ContainerTypeRegistryObject<FuelMenu> FUEL_MENU=MENUS.registerMenu("fuel",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(FuelMenu::fromNetwork));
    public static final BlockRegistryObject<ControllerBlock,ItemBlockTooltip<ControllerBlock>> CONTROLLER;
    public static final TileEntityTypeRegistryObject<Controller> CONTROLLER_TILE;
    public static final Map<PartBlock.Kind,BlockRegistryObject<PartBlock,BlockItem>> PARTS=new EnumMap<>(PartBlock.Kind.class);
    public static final Map<Grade,BlockRegistryObject<PartBlock,BlockItem>> COILS=new EnumMap<>(Grade.class);
    private static final DeferredRegister<BlockEntityType<?>> PART_TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MekGravity.ID);
    public static final java.util.function.Supplier<BlockEntityType<Part>> PART=PART_TYPES.register("part",()->{
        var blocks=new ArrayList<net.minecraft.world.level.block.Block>();PARTS.values().forEach(b->blocks.add(b.get()));COILS.values().forEach(b->blocks.add(b.get()));
        return BlockEntityType.Builder.of(Part::new,blocks.toArray(net.minecraft.world.level.block.Block[]::new)).build(null);
    });
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,MekGravity.ID);
    public static final java.util.function.Supplier<Item> PELLET=ITEMS.register("dense_fuel_pellet",()->new Item(new Item.Properties()));
    private static final DeferredRegister<RecipeType<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_TYPE,MekGravity.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,MekGravity.ID);
    public static final java.util.function.Supplier<RecipeType<FuelRecipe>> FUEL_TYPE=RECIPES.register("matter_fuel",()->new RecipeType<>(){});
    public static final java.util.function.Supplier<RecipeSerializer<FuelRecipe>> FUEL_SERIALIZER=SERIALIZERS.register("matter_fuel",FuelRecipe.Serializer::new);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MekGravity.ID);
    static {
        var machine=Machine.MachineBuilder.<Controller>createMachine(()->Content.CONTROLLER_TILE,()->"block.mekgravity.reactor")
              .withGui(()->MENU).withEnergyConfig(()->0L,()->0L).build();
        machine.remove(mekanism.common.block.attribute.AttributeUpgradeSupport.class,mekanism.common.block.attribute.AttributeParticleFX.class);
        CONTROLLER=BLOCKS.registerDetails("reactor",()->new ControllerBlock(machine));
        CONTROLLER_TILE=TILES.mekBuilder(CONTROLLER,Controller::new).clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build();
        for(var kind:PartBlock.Kind.values())if(kind!=PartBlock.Kind.COIL)PARTS.put(kind,BLOCKS.register(kind.id(),()->new PartBlock(kind,Grade.BASIC)));
        for(var g:Grade.values())COILS.put(g,BLOCKS.register(g.id()+"_coil",()->new PartBlock(PartBlock.Kind.COIL,g)));
        TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.mekgravity")).icon(()->new ItemStack(CONTROLLER))
              .displayItems((p,out)->{out.accept(CONTROLLER);PARTS.entrySet().stream().filter(e->e.getKey()!=PartBlock.Kind.COOLANT).forEach(e->out.accept(e.getValue()));COILS.values().forEach(out::accept);out.accept(PELLET.get());}).build());
    }
    public static Component text(String key,Object... args){return Component.translatable("mekgravity."+key,args);}
    public static void register(IEventBus b){COMPONENTS.register(b);BLOCKS.register(b);TILES.register(b);PART_TYPES.register(b);MENUS.register(b);ITEMS.register(b);RECIPES.register(b);SERIALIZERS.register(b);TABS.register(b);}
    private Content(){}
}
