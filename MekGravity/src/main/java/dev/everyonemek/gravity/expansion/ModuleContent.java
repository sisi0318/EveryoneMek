package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.MekGravity;
import dev.everyonemek.gravity.corona.CoronalInventorySlot;
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
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModuleContent {
    private static final BlockDeferredRegister BLOCKS=new BlockDeferredRegister(MekGravity.ID);
    private static final TileEntityTypeDeferredRegister TILES=new TileEntityTypeDeferredRegister(MekGravity.ID);
    private static final ContainerTypeDeferredRegister MENUS=new ContainerTypeDeferredRegister(MekGravity.ID);
    private static final DataComponentDeferredRegister COMPONENTS=new DataComponentDeferredRegister(MekGravity.ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,MekGravity.ID);
    private static final DeferredRegister<RecipeType<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_TYPE,MekGravity.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,MekGravity.ID);
    public static final Map<ModuleKind,BlockRegistryObject<ModuleBlock,ItemBlockTooltip<ModuleBlock>>> BLOCK=new EnumMap<>(ModuleKind.class);
    public static final Map<ModuleKind,TileEntityTypeRegistryObject<OrbitalModule>> TILE=new EnumMap<>(ModuleKind.class);
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> DATA=COMPONENTS.simple("orbital_module",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> LINK=COMPONENTS.simple("field_link",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final java.util.function.Supplier<Item> LINKER=ITEMS.register("field_linker",()->new FieldLinker(new Item.Properties().stacksTo(1)));
    public static final java.util.function.Supplier<Item> FLARE=ITEMS.register("flare_cell",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<Item> ALLOY=ITEMS.register("stellar_alloy",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<RecipeType<OrbitalRecipe>> TYPE=RECIPES.register("orbital_processing",()->new RecipeType<>(){});
    public static final java.util.function.Supplier<RecipeSerializer<OrbitalRecipe>> SERIALIZER=SERIALIZERS.register("orbital_processing",OrbitalRecipe.Serializer::new);
    public static final ContainerTypeRegistryObject<ModuleMenu> MENU=MENUS.register("orbital_module",OrbitalModule.class,ModuleMenu::new);
    static{for(var kind:ModuleKind.values()){
        var type=Machine.MachineBuilder.<OrbitalModule>createMachine(()->TILE.get(kind),()->"description.mekgravity."+kind.id).withGui(()->MENU).withEnergyConfig(()->0L,()->0L).build();
        type.remove(mekanism.common.block.attribute.AttributeUpgradeSupport.class,mekanism.common.block.attribute.AttributeParticleFX.class);
        if(kind==ModuleKind.OBSERVATORY)type.remove(mekanism.common.block.attribute.Attributes.AttributeRedstone.class);
        var block=BLOCKS.registerDetails(kind.id,()->new ModuleBlock(type,kind));BLOCK.put(kind,block);
        block.forItemHolder(item->item.addAttachedContainerCapabilities(ContainerType.ITEM,()->{
            var slots=ItemSlotsBuilder.builder();if(kind.cargo)return slots.addSlots(9,(t,s,i)->CoronalInventorySlot.attached(s,i,false)).addSlots(9,(t,s,i)->CoronalInventorySlot.attached(s,i,true)).build();
            if(kind==ModuleKind.TUNER)slots.addInput(s->s.is(FLARE.get()));return slots.build();}));
        TILE.put(kind,TILES.mekBuilder(block,OrbitalModule::new).clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build());
    }}
    public static Component text(String key,Object...args){return Component.translatable("mekgravity.module."+key,args);}
    public static ModuleKind kind(net.minecraft.world.level.block.state.BlockState state){return ((ModuleBlock)state.getBlock()).kind;}
    public static void creative(CreativeModeTab.Output out){BLOCK.values().forEach(out::accept);out.accept(LINKER.get());out.accept(FLARE.get());out.accept(ALLOY.get());}
    public static void register(IEventBus bus){COMPONENTS.register(bus);BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);ITEMS.register(bus);RECIPES.register(bus);SERIALIZERS.register(bus);}
    private ModuleContent(){}
}
