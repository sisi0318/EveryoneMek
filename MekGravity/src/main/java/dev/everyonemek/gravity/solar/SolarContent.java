package dev.everyonemek.gravity.solar;
import dev.everyonemek.gravity.MekGravity;
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
public final class SolarContent {
    private static final BlockDeferredRegister BLOCKS=new BlockDeferredRegister(MekGravity.ID);
    private static final TileEntityTypeDeferredRegister TILES=new TileEntityTypeDeferredRegister(MekGravity.ID);
    private static final ContainerTypeDeferredRegister MENUS=new ContainerTypeDeferredRegister(MekGravity.ID);
    private static final DataComponentDeferredRegister COMPONENTS=new DataComponentDeferredRegister(MekGravity.ID);
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> DATA=COMPONENTS.simple("solar_data",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> STOCK=COMPONENTS.simple("solar_stock",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> FUEL_DATA=COMPONENTS.simple("stellar_reserve",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final ContainerTypeRegistryObject<SolarMenu> MENU=MENUS.registerMenu("solar_controller",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(SolarMenu::fromNetwork));
    public static final ContainerTypeRegistryObject<SolarFuelMenu> FUEL_MENU=MENUS.registerMenu("solar_fuel",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(SolarFuelMenu::fromNetwork));
    public static final BlockRegistryObject<SolarControllerBlock,ItemBlockTooltip<SolarControllerBlock>> CONTROLLER;
    public static final TileEntityTypeRegistryObject<SolarController> CONTROLLER_TILE;
    public static final Map<String,BlockRegistryObject<SolarBlock,BlockItem>> PARTS=new LinkedHashMap<>();
    private static final DeferredRegister<BlockEntityType<?>> PART_TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MekGravity.ID);
    public static final java.util.function.Supplier<BlockEntityType<SolarPart>> PART=PART_TYPES.register("solar_part",()->BlockEntityType.Builder.of(SolarPart::new,PARTS.values().stream().map(b->b.get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,MekGravity.ID);
    public static final java.util.function.Supplier<Item> COMPRESSED=ITEMS.register("compressed_stellar_matter",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<Item> PREFORM=ITEMS.register("stellar_fuel_preform",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<Item> FUEL=ITEMS.register("stellar_fuel",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<Item> CAPSULE=ITEMS.register("stellar_fuel_capsule",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> lines,TooltipFlag f){var t=stack.get(FUEL_DATA.get());if(t!=null&&SolarFuelRecipe.validReserve(t))lines.add(text("capsule_left",Math.round(t.getLong("remaining")*100D/t.getLong("total"))));}
    });
    private static final DeferredRegister<RecipeType<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_TYPE,MekGravity.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,MekGravity.ID);
    public static final java.util.function.Supplier<RecipeType<SolarFuelRecipe>> FUEL_TYPE=RECIPES.register("stellar_fuel",()->new RecipeType<>(){});
    public static final java.util.function.Supplier<RecipeSerializer<SolarFuelRecipe>> SERIALIZER=SERIALIZERS.register("stellar_fuel",SolarFuelRecipe.Serializer::new);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MekGravity.ID);
    static{
        var machine=Machine.MachineBuilder.<SolarController>createMachine(()->SolarContent.CONTROLLER_TILE,()->"block.mekgravity.solar_controller").withGui(()->MENU).withEnergyConfig(()->0L,()->0L).build();
        machine.remove(mekanism.common.block.attribute.AttributeUpgradeSupport.class,mekanism.common.block.attribute.AttributeParticleFX.class);
        CONTROLLER=BLOCKS.registerDetails("solar_controller",()->new SolarControllerBlock(machine));
        CONTROLLER_TILE=TILES.mekBuilder(CONTROLLER,SolarController::new).clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build();
        for(var kind:SolarBlock.Kind.values())for(int tier=0;tier<(kind.tiered()?4:1);tier++){
            int grade=tier;String id=kind.id()+(kind.tiered()?"_"+List.of("basic","advanced","elite","ultimate").get(tier):"");
            PARTS.put(id,BLOCKS.register("solar_"+id,()->new SolarBlock(kind,grade)));
        }
        TABS.register("solar",()->CreativeModeTab.builder().title(text("tab")).icon(()->new ItemStack(CONTROLLER)).displayItems((p,out)->{out.accept(CONTROLLER);PARTS.values().forEach(out::accept);out.accept(dev.everyonemek.gravity.corona.CoronalContent.BLOCK);out.accept(COMPRESSED.get());out.accept(PREFORM.get());out.accept(FUEL.get());}).build());
    }
    public static BlockRegistryObject<SolarBlock,BlockItem> block(SolarBlock.Kind kind,int tier){return PARTS.get(kind.id()+(kind.tiered()?"_"+List.of("basic","advanced","elite","ultimate").get(tier):""));}
    public static Component text(String key,Object...args){return Component.translatable("mekgravity.solar."+key,args);}
    public static void register(IEventBus bus){COMPONENTS.register(bus);BLOCKS.register(bus);TILES.register(bus);PART_TYPES.register(bus);MENUS.register(bus);ITEMS.register(bus);RECIPES.register(bus);SERIALIZERS.register(bus);TABS.register(bus);}
    private SolarContent(){}
}
