package dev.everyonemek.overloadcore.training;

import dev.everyonemek.overloadcore.OverloadCore;
import dev.everyonemek.overloadcore.gear.GearCombat;
import mekanism.common.registration.impl.*;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TrainingContent {
    private static final BlockDeferredRegister BLOCKS=new BlockDeferredRegister(OverloadCore.ID);
    private static final TileEntityTypeDeferredRegister TILES=new TileEntityTypeDeferredRegister(OverloadCore.ID);
    private static final ContainerTypeDeferredRegister MENUS=new ContainerTypeDeferredRegister(OverloadCore.ID);
    private static final DataComponentDeferredRegister COMPONENTS=new DataComponentDeferredRegister(OverloadCore.ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,OverloadCore.ID);
    public static final java.util.function.Supplier<DataComponentType<CompoundTag>> DATA=COMPONENTS.simple("training",b->b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final ContainerTypeRegistryObject<TrainingMenu> MENU=MENUS.register("holographic_projector",TrainingProjector.class,TrainingMenu::new);
    private static final Machine<TrainingProjector> TYPE=Machine.MachineBuilder.<TrainingProjector>createMachine(()->TrainingContent.TILE,()->"description.overloadcore.holographic_projector")
        .withGui(()->MENU).withEnergyConfig(()->GearCombat.joules(dev.everyonemek.overloadcore.CoreConfig.TRAINING_FE.get()),TrainingContent::capacity).build();
    static{TYPE.remove(mekanism.common.block.attribute.AttributeUpgradeSupport.class,mekanism.common.block.attribute.AttributeParticleFX.class);}
    public static final BlockRegistryObject<TrainingBlock,ItemBlockTooltip<TrainingBlock>> BLOCK=
        BLOCKS.registerDetails("holographic_projector",()->new TrainingBlock(TYPE));
    public static final TileEntityTypeRegistryObject<TrainingProjector> TILE=TILES.mekBuilder(BLOCK,TrainingProjector::new)
        .clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build();
    public static final java.util.function.Supplier<EntityType<TrainingTarget>> TARGET=ENTITIES.register("training_target",()->EntityType.Builder.of(TrainingTarget::new,MobCategory.MISC)
        .sized(.65F,1.8F).fireImmune().noSave().clientTrackingRange(6).updateInterval(10).build("overloadcore:training_target"));
    public static long capacity(){return GearCombat.joules(2000000);}
    static{BLOCK.forItemHolder(item->item.addAttachedContainerCapabilities(mekanism.common.attachments.containers.ContainerType.ENERGY,
        ()->mekanism.common.attachments.containers.energy.EnergyContainersBuilder.builder().addBasic(a->a!=mekanism.api.AutomationType.EXTERNAL,a->true,()->Long.MAX_VALUE,TrainingContent::capacity).build()));}
    public static void register(IEventBus bus){COMPONENTS.register(bus);BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->e.put(TARGET.get(),LivingEntity.createLivingAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,1024).build()));}
    private TrainingContent(){}
}
