package dev.everyonemek.gravity.corona;
import dev.everyonemek.gravity.MekGravity;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.item.ItemSlotsBuilder;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoronalContent {
    private static final BlockDeferredRegister BLOCKS=new BlockDeferredRegister(MekGravity.ID);
    private static final TileEntityTypeDeferredRegister TILES=new TileEntityTypeDeferredRegister(MekGravity.ID);
    private static final ContainerTypeDeferredRegister MENUS=new ContainerTypeDeferredRegister(MekGravity.ID);
    private static final DataComponentDeferredRegister COMPONENTS=new DataComponentDeferredRegister(MekGravity.ID);
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> WORK=COMPONENTS.simple("coronal_work",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final ContainerTypeRegistryObject<CoronalMenu> MENU=MENUS.register("coronal_chamber",CoronalMachine.class,CoronalMenu::new);
    public static final BlockRegistryObject<BlockTile<CoronalMachine,Machine<CoronalMachine>>,ItemBlockTooltip<BlockTile<CoronalMachine,Machine<CoronalMachine>>>> BLOCK;
    public static final TileEntityTypeRegistryObject<CoronalMachine> TILE;
    private static final DeferredRegister<RecipeType<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_TYPE,MekGravity.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,MekGravity.ID);
    public static final java.util.function.Supplier<RecipeType<CoronalRecipe>> TYPE=RECIPES.register("coronal_processing",()->new RecipeType<>(){});
    public static final java.util.function.Supplier<RecipeSerializer<CoronalRecipe>> SERIALIZER=SERIALIZERS.register("coronal_processing",CoronalRecipe.Serializer::new);
    static{
        var machine=Machine.MachineBuilder.<CoronalMachine>createMachine(()->CoronalContent.TILE,()->"description.mekgravity.coronal_chamber").withGui(()->MENU).withEnergyConfig(()->0L,()->0L).build();
        machine.remove(mekanism.common.block.attribute.AttributeUpgradeSupport.class,mekanism.common.block.attribute.AttributeParticleFX.class);
        BLOCK=BLOCKS.registerDetails("coronal_chamber",()->new BlockTile<>(machine,p->p.strength(6,20).noOcclusion()));
        BLOCK.forItemHolder(item->item.addAttachedContainerCapabilities(ContainerType.ITEM,()->ItemSlotsBuilder.builder().addInput(9).addOutput(9).build()));
        TILE=TILES.mekBuilder(BLOCK,CoronalMachine::new).clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build();
    }
    public static Component text(String key,Object...args){return Component.translatable("mekgravity.corona."+key,args);}
    public static void register(IEventBus bus){COMPONENTS.register(bus);BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);RECIPES.register(bus);SERIALIZERS.register(bus);}
    private CoronalContent(){}
}
