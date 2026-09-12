package dev.everyonemek.botania;

import mekanism.api.Upgrade;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.fluid.FluidTanksBuilder;
import mekanism.common.attachments.containers.item.ItemSlotsBuilder;
import mekanism.common.block.attribute.AttributeSideConfig;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.*;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ApothecaryContent {
    private static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(BotanicalMekanism.ID);
    private static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister(BotanicalMekanism.ID);
    private static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(BotanicalMekanism.ID);
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BotanicalMekanism.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BotanicalMekanism.ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MechanicalFlowerRecipe>> RECIPE_TYPE = TYPES.register("mechanical_apothecary", () -> new RecipeType<>() {
        @Override public String toString() { return "botanicalmekanism:mechanical_apothecary"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MechanicalFlowerRecipe>> SERIALIZER = SERIALIZERS.register("mechanical_apothecary", MechanicalFlowerRecipe.Serializer::new);
    public static final ContainerTypeRegistryObject<ApothecaryMenu> MENU = MENUS.register("mechanical_apothecary", MechanicalApothecary.class, ApothecaryMenu::new);
    private static final Machine<MechanicalApothecary> TYPE = Machine.MachineBuilder.<MechanicalApothecary>createMachine(() -> ApothecaryContent.TILE,
          () -> "description.botanicalmekanism.mechanical_apothecary")
          .withGui(() -> MENU)
          .withEnergyConfig(() -> EnergyUnit.FORGE_ENERGY.convertFrom(50L), () -> EnergyUnit.FORGE_ENERGY.convertFrom(200000L))
          .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY).with(AttributeSideConfig.create(
                mekanism.common.lib.transmitter.TransmissionType.ITEM, mekanism.common.lib.transmitter.TransmissionType.FLUID,
                mekanism.common.lib.transmitter.TransmissionType.ENERGY)).build();
    public static final BlockRegistryObject<ApothecaryBlock, ItemBlockTooltip<ApothecaryBlock>> BLOCK = BLOCKS.registerDetails("mechanical_apothecary", () -> new ApothecaryBlock(TYPE));
    public static final TileEntityTypeRegistryObject<MechanicalApothecary> TILE;
    static {
        BLOCK.forItemHolder(holder -> {
            holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder().addInput(17).addOutput(6).addEnergy().addFluidFillSlot(0).addOutput().build());
            holder.addAttachmentOnlyContainers(ContainerType.FLUID, () -> FluidTanksBuilder.builder().addBasic(MechanicalApothecary.WATER_CAPACITY, stack -> stack.is(Fluids.WATER)).build());
        });
        TILE = TILES.mekBuilder(BLOCK, MechanicalApothecary::new).clientTicker(TileEntityMekanism::tickClient).serverTicker(TileEntityMekanism::tickServer).build();
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); TILES.register(bus); MENUS.register(bus); TYPES.register(bus); SERIALIZERS.register(bus); }
    private ApothecaryContent() { }
}
