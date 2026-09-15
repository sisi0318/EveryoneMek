package dev.everyonemek.botania;

import dev.everyonemek.botania.mixin.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.fluids.FluidStack;
import vazkii.botania.api.block_entity.SpecialFlowerBlockEntity;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.flower.generating.*;
import vazkii.botania.common.component.BotaniaDataComponents;
import vazkii.botania.common.helper.ColorHelper;
import vazkii.botania.common.lib.BotaniaTags;
import vazkii.botania.xplat.XplatAbstractions;

/** Built-in rules share the executor while retaining their own native conditions and state. */
final class BuiltinGreenhouseRules {
    static void register() {
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "endoflame"), new Endoflame());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "gourmaryllis"), new Gourmaryllis());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "spectrolus"), new Spectrolus());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "thermalily"), new Thermalily());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "kekimurus"), new Kekimurus());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "munchdew"), new Munchdew());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "entropinnyum"), new Entropinnyum());
        GreenhouseRules.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "rafflowsia"), new Rafflowsia());
    }
    private static int interval(SpecialFlowerBlockEntity flower) { return Math.max(1, ((CultivatedFlowerInterval) flower).botanicalmekanism$interval()); }
    private static net.minecraft.network.chat.Component note(String key) { return net.minecraft.network.chat.Component.translatable("jei.botanicalmekanism.greenhouse." + key); }
    static CultivatedFluidFlowerAccess thermalily() { return Thermalily.nativeFlower(); }
    public static List<DyeColor> colors(ItemStack flower, Level level) {
        var colors = flower.get(BotaniaDataComponents.COLOR_SEQUENCE);
        if (colors != null && !colors.isEmpty()) return colors;
        return level instanceof net.minecraft.server.level.ServerLevel server ? CultivatedSpectrolusAccess.botanicalmekanism$colors(server)
              : ColorHelper.supportedColors().toList();
    }
    public static ItemStack expectedWool(ItemStack flower, Level level) {
        var colors = colors(flower, level);
        var next = flower.getOrDefault(BotaniaDataComponents.NEXT_COLOR, colors.getFirst());
        return new ItemStack(ColorHelper.WOOL_MAP.apply(next));
    }

    private static final class Endoflame implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return !stack.isEmpty() && !stack.is(BotaniaTags.Items.IGNORED_BY_ENDOFLAME) && !stack.getItem().hasCraftingRemainingItem()
                  && XplatAbstractions.INSTANCE.getSmeltingBurnTime(stack) > 1; }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            int ticks = Math.min(CultivatedEndoflameAccess.botanicalmekanism$fuelCap(), XplatAbstractions.INSTANCE.getSmeltingBurnTime(material)) / 2;
            int interval = interval(new EndoflameBlockEntity(BlockPos.ZERO, BotaniaBlocks.ENDOFLAME.defaultBlockState()));
            // Native tickFlower decrements first and emits a literal 3 mana on eligible ticks.
            // Round down the last partial pulse; never exceed the native full-burn yield.
            int mana = 3 * ((ticks - 1) / interval);
            return mana <= 0 ? null : new GreenhouseNative.Result(mana, ticks, CultivatedEndoflameAccess.botanicalmekanism$cooldown(), next, 0);
        }
    }

    private static final class Gourmaryllis implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return !stack.isEmpty() && stack.getItem().components().has(DataComponents.FOOD) && CultivatedGourmaryllisAccess.botanicalmekanism$value(stack) > 0; }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            var nativeFlower = (CultivatedGourmaryllisAccess) new GourmaryllisBlockEntity(BlockPos.ZERO, BotaniaBlocks.GOURMARYLLIS.defaultBlockState());
            nativeFlower.botanicalmekanism$foods().addAll(flower.getOrDefault(BotaniaDataComponents.LAST_FOODS, List.of()));
            nativeFlower.botanicalmekanism$repeats(Math.clamp(flower.getOrDefault(BotaniaDataComponents.LAST_REPEATS, 0), 0, 1_000_000));
            int age = nativeFlower.botanicalmekanism$process(material);
            int streak = Math.clamp(Math.min(flower.getOrDefault(BotaniaDataComponents.STREAK_LENGTH, -1) + 1, age), 0, GourmaryllisBlockEntity.getMaxStreak());
            int value = CultivatedGourmaryllisAccess.botanicalmekanism$value(material);
            int mana = CultivatedGourmaryllisAccess.botanicalmekanism$mana(value, nativeFlower.botanicalmekanism$multiplier(streak));
            next.set(BotaniaDataComponents.STREAK_LENGTH, streak); next.set(BotaniaDataComponents.LAST_REPEATS, nativeFlower.botanicalmekanism$repeats());
            next.set(BotaniaDataComponents.LAST_FOODS, List.copyOf(nativeFlower.botanicalmekanism$foods()));
            return new GreenhouseNative.Result(mana, CultivatedGourmaryllisAccess.botanicalmekanism$ticks(value), 0, next, age);
        }
        @Override public boolean variableOutput() { return true; }
        @Override public net.minecraft.network.chat.Component recipeNote() { return note("variety"); }
    }

    private static final class Spectrolus implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return !stack.isEmpty() && ColorHelper.isWool(stack.getItem()); }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            var colors = colors(flower, level);
            var expected = flower.getOrDefault(BotaniaDataComponents.NEXT_COLOR, colors.getFirst());
            if (!material.is(ColorHelper.WOOL_MAP.apply(expected).asItem())) return null;
            next.set(BotaniaDataComponents.COLOR_SEQUENCE, List.copyOf(colors));
            next.set(BotaniaDataComponents.NEXT_COLOR, colors.get((colors.indexOf(expected) + 1) % colors.size()));
            return new GreenhouseNative.Result(CultivatedSpectrolusAccess.botanicalmekanism$mana(), 1, 0, next, 0);
        }
        @Override public ItemStack prepare(ItemStack flower, Level level) {
            if (!flower.has(BotaniaDataComponents.COLOR_SEQUENCE)) {
                var colors = colors(flower, level); flower.set(BotaniaDataComponents.COLOR_SEQUENCE, List.copyOf(colors));
                if (!flower.has(BotaniaDataComponents.NEXT_COLOR)) flower.set(BotaniaDataComponents.NEXT_COLOR, colors.getFirst());
            }
            return flower;
        }
        @Override public ItemStack previewFlower(ItemStack flower, ItemStack material, Level level) {
            ColorHelper.supportedColors().filter(c -> material.is(ColorHelper.WOOL_MAP.apply(c).asItem())).findFirst()
                  .ifPresent(color -> flower.set(BotaniaDataComponents.NEXT_COLOR, color));
            return flower;
        }
        @Override public net.minecraft.network.chat.Component statusInfo(ItemStack flower, Level level) {
            return net.minecraft.network.chat.Component.translatable("gui.botanicalmekanism.machine.next_wool", expectedWool(flower, level).getHoverName());
        }
        @Override public net.minecraft.network.chat.Component recipeNote() { return note("spectrolus"); }
    }

    private static final class Thermalily implements GreenhouseFlowerRule {
        @Override public int fluidAmount() { return 1000; }
        @Override public boolean acceptsFluid(FluidStack stack) { return !stack.isEmpty() && stack.is(nativeFlower().botanicalmekanism$fluid()); }
        private static CultivatedFluidFlowerAccess nativeFlower() {
            return (CultivatedFluidFlowerAccess) new ThermalilyBlockEntity(BlockPos.ZERO, BotaniaBlocks.THERMALILY.defaultBlockState());
        }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsFluid(fluid)) return null;
            var next = flower.copyWithCount(1);
            var nativeFlower = nativeFlower();
            if (!fluid.is(nativeFlower.botanicalmekanism$fluid())) return null;
            int ticks = nativeFlower.botanicalmekanism$burnTicks();
            return new GreenhouseNative.Result(Math.multiplyExact(ticks, nativeFlower.botanicalmekanism$manaPerTick()), ticks, nativeFlower.botanicalmekanism$cooldown(), next, 0);
        }
    }

    private static final class Kekimurus implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof BlockItem b && b.getBlock() instanceof CakeBlock && !stack.has(DataComponents.BLOCK_STATE); }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            int slices = CakeBlock.MAX_BITES + 1;
            return new GreenhouseNative.Result(KekimurusBlockEntity.MANA_PER_SLICE * slices,
                  interval(new KekimurusBlockEntity(BlockPos.ZERO, BotaniaBlocks.KEKIMURUS.defaultBlockState())) * slices, 0, next, 0);
        }
    }

    private static final class Munchdew implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().is(BotaniaTags.Blocks.MUNCHDEW_CONSUMABLE); }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            GreenhouseWork.setLeafRun(next, true);
            return new GreenhouseNative.Result(CultivatedMunchdewAccess.botanicalmekanism$mana(),
                  interval(new MunchdewBlockEntity(BlockPos.ZERO, BotaniaBlocks.MUNCHDEW.defaultBlockState())), 0, next, 0);
        }
        @Override public ItemStack onBlocked(ItemStack flower, Level level) {
            if (GreenhouseWork.leafRunning(flower)) {
                GreenhouseWork.setLeafRun(flower, false);
                GreenhouseWork.cooldown(flower, CultivatedMunchdewAccess.botanicalmekanism$cooldown());
            }
            return flower;
        }
        @Override public net.minecraft.network.chat.Component recipeNote() { return note("munchdew"); }
    }

    private static final class Entropinnyum implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return stack.is(Items.TNT); }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            return new GreenhouseNative.Result(new EntropinnyumBlockEntity(BlockPos.ZERO, BotaniaBlocks.ENTROPINNYUM.defaultBlockState()).getMaxMana(), 80, 0, next, 0);
        }
    }

    private static final class Rafflowsia implements GreenhouseFlowerRule {
        @Override public boolean acceptsItem(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof BlockItem b && b.getBlock() != BotaniaBlocks.RAFFLOWSIA
                  && b.getBlock().defaultBlockState().is(BotaniaTags.Blocks.SPECIAL_FLOWERS); }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
            if (!acceptsItem(material)) return null;
            var next = flower.copyWithCount(1);
            var flowerEntity = new RafflowsiaBlockEntity(BlockPos.ZERO, BotaniaBlocks.RAFFLOWSIA.defaultBlockState());
            var nativeFlower = (CultivatedRafflowsiaAccess) flowerEntity;
            nativeFlower.botanicalmekanism$flowers().addAll(flower.getOrDefault(BotaniaDataComponents.LAST_FLOWERS, List.of()));
            nativeFlower.botanicalmekanism$repeats(Math.clamp(flower.getOrDefault(BotaniaDataComponents.LAST_REPEATS, 0), 0, 1_000_000));
            int age = nativeFlower.botanicalmekanism$process(((BlockItem) material.getItem()).getBlock());
            int streak = Math.clamp(Math.min(flower.getOrDefault(BotaniaDataComponents.STREAK_LENGTH, -1) + 1, age), 0, RafflowsiaBlockEntity.getMaxStreak());
            int mana = nativeFlower.botanicalmekanism$mana(streak);
            next.set(BotaniaDataComponents.STREAK_LENGTH, streak); next.set(BotaniaDataComponents.LAST_REPEATS, nativeFlower.botanicalmekanism$repeats());
            next.set(BotaniaDataComponents.LAST_FLOWERS, List.copyOf(nativeFlower.botanicalmekanism$flowers()));
            return mana <= 0 ? null : new GreenhouseNative.Result(mana, interval(flowerEntity), 0, next, age);
        }
        @Override public boolean variableOutput() { return true; }
        @Override public net.minecraft.network.chat.Component recipeNote() { return note("variety"); }
    }
    private BuiltinGreenhouseRules() { }
}
