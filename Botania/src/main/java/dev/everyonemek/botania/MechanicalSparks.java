package dev.everyonemek.botania;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
import vazkii.botania.common.entity.ManaSparkEntity;

public final class MechanicalSparks {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BotanicalMekanism.ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ManaSparkEntity>> ENTITY = ENTITIES.register("mechanical_spark",
          () -> EntityType.Builder.<ManaSparkEntity>of(MechanicalSparkEntity::new, MobCategory.MISC).sized(.2F, .5F).fireImmune()
                .clientTrackingRange(6).updateInterval(10).build("botanicalmekanism:mechanical_spark"));
    public static final DeferredItem<MechanicalSparkItem> SPARK = Content.ITEMS.register("mechanical_spark", () -> new MechanicalSparkItem(false));
    public static final DeferredItem<MechanicalSparkItem> MASTER = Content.ITEMS.register("master_mechanical_spark", () -> new MechanicalSparkItem(true));
    public static final DeferredItem<Item> RANGE = Content.ITEMS.register("spark_range_upgrade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EFFICIENCY = Content.ITEMS.register("spark_efficiency_upgrade", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<MenuType<?>, MenuType<SparkControllerMenu>> MENU = Content.MENUS.register("spark_controller",
          () -> IMenuTypeExtension.create((id, inventory, data) -> new SparkControllerMenu(id, inventory, data.readVarInt(), data.readVarInt())));
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); MechanicalSparkNetworks.register();
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            var behavior = new vazkii.botania.common.block.dispenser.ManaSparkBehavior();
            for (var item : new Item[]{SPARK.get(), MASTER.get()}) net.minecraft.world.level.block.DispenserBlock.registerBehavior(item,
                  (source, stack) -> MechanicalSparkItem.placing(stack, () -> behavior.dispense(source, stack)));
        }));
    }
    private MechanicalSparks() { }
}
