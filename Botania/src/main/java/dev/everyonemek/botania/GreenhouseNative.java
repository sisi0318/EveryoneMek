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

/** Adapts explicit native rules, not world ticks. Formula evaluation only mutates detached copies. */
public final class GreenhouseNative {
    public record Result(int mana, int ticks, int cooldown, ItemStack flower, int preference) { }
    private static int interval(SpecialFlowerBlockEntity flower) { return Math.max(1, ((CultivatedFlowerInterval) flower).botanicalmekanism$interval()); }
    public static CultivatedFluidFlowerAccess thermalily() {
        return (CultivatedFluidFlowerAccess) new ThermalilyBlockEntity(BlockPos.ZERO, BotaniaBlocks.THERMALILY.defaultBlockState());
    }
    public static boolean accepts(String formula, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (formula) {
            case "endoflame" -> !stack.is(BotaniaTags.Items.IGNORED_BY_ENDOFLAME) && !stack.getItem().hasCraftingRemainingItem()
                  && XplatAbstractions.INSTANCE.getSmeltingBurnTime(stack) > 1;
            case "gourmaryllis" -> stack.getItem().components().has(DataComponents.FOOD) && CultivatedGourmaryllisAccess.botanicalmekanism$value(stack) > 0;
            case "spectrolus" -> ColorHelper.isWool(stack.getItem());
            case "kekimurus" -> stack.getItem() instanceof BlockItem b && b.getBlock() instanceof CakeBlock && !stack.has(DataComponents.BLOCK_STATE);
            case "munchdew" -> stack.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().is(BotaniaTags.Blocks.MUNCHDEW_CONSUMABLE);
            case "entropinnyum" -> stack.is(Items.TNT);
            case "rafflowsia" -> stack.getItem() instanceof BlockItem b && b.getBlock() != BotaniaBlocks.RAFFLOWSIA
                  && b.getBlock().defaultBlockState().is(BotaniaTags.Blocks.SPECIAL_FLOWERS);
            default -> false;
        };
    }
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
    public static Result resolve(String formula, ItemStack flower, ItemStack material, FluidStack fluid, Level level) {
        if (!formula.equals("thermalily") && !accepts(formula, material)) return null;
        var next = flower.copyWithCount(1);
        switch (formula) {
            case "endoflame": {
                int ticks = Math.min(CultivatedEndoflameAccess.botanicalmekanism$fuelCap(), XplatAbstractions.INSTANCE.getSmeltingBurnTime(material)) / 2;
                int interval = interval(new EndoflameBlockEntity(BlockPos.ZERO, BotaniaBlocks.ENDOFLAME.defaultBlockState()));
                // Native tickFlower decrements first and emits a literal 3 mana on eligible ticks.
                // Round down the last partial pulse; never exceed the native full-burn yield.
                int mana = 3 * ((ticks - 1) / interval);
                return mana <= 0 ? null : new Result(mana, ticks, CultivatedEndoflameAccess.botanicalmekanism$cooldown(), next, 0);
            }
            case "thermalily": {
                var nativeFlower = thermalily();
                if (!fluid.is(nativeFlower.botanicalmekanism$fluid())) return null;
                int ticks = nativeFlower.botanicalmekanism$burnTicks();
                return new Result(Math.multiplyExact(ticks, nativeFlower.botanicalmekanism$manaPerTick()), ticks, nativeFlower.botanicalmekanism$cooldown(), next, 0);
            }
            case "spectrolus": {
                var colors = colors(flower, level);
                var expected = flower.getOrDefault(BotaniaDataComponents.NEXT_COLOR, colors.getFirst());
                if (!material.is(ColorHelper.WOOL_MAP.apply(expected).asItem())) return null;
                next.set(BotaniaDataComponents.COLOR_SEQUENCE, List.copyOf(colors));
                next.set(BotaniaDataComponents.NEXT_COLOR, colors.get((colors.indexOf(expected) + 1) % colors.size()));
                return new Result(CultivatedSpectrolusAccess.botanicalmekanism$mana(), 1, 0, next, 0);
            }
            case "gourmaryllis": {
                var nativeFlower = (CultivatedGourmaryllisAccess) new GourmaryllisBlockEntity(BlockPos.ZERO, BotaniaBlocks.GOURMARYLLIS.defaultBlockState());
                nativeFlower.botanicalmekanism$foods().addAll(flower.getOrDefault(BotaniaDataComponents.LAST_FOODS, List.of()));
                nativeFlower.botanicalmekanism$repeats(Math.clamp(flower.getOrDefault(BotaniaDataComponents.LAST_REPEATS, 0), 0, 1_000_000));
                int age = nativeFlower.botanicalmekanism$process(material);
                int streak = Math.clamp(Math.min(flower.getOrDefault(BotaniaDataComponents.STREAK_LENGTH, -1) + 1, age), 0, GourmaryllisBlockEntity.getMaxStreak());
                int value = CultivatedGourmaryllisAccess.botanicalmekanism$value(material);
                int mana = CultivatedGourmaryllisAccess.botanicalmekanism$mana(value, nativeFlower.botanicalmekanism$multiplier(streak));
                next.set(BotaniaDataComponents.STREAK_LENGTH, streak); next.set(BotaniaDataComponents.LAST_REPEATS, nativeFlower.botanicalmekanism$repeats());
                next.set(BotaniaDataComponents.LAST_FOODS, List.copyOf(nativeFlower.botanicalmekanism$foods()));
                return new Result(mana, CultivatedGourmaryllisAccess.botanicalmekanism$ticks(value), 0, next, age);
            }
            case "rafflowsia": {
                var flowerEntity = new RafflowsiaBlockEntity(BlockPos.ZERO, BotaniaBlocks.RAFFLOWSIA.defaultBlockState());
                var nativeFlower = (CultivatedRafflowsiaAccess) flowerEntity;
                nativeFlower.botanicalmekanism$flowers().addAll(flower.getOrDefault(BotaniaDataComponents.LAST_FLOWERS, List.of()));
                nativeFlower.botanicalmekanism$repeats(Math.clamp(flower.getOrDefault(BotaniaDataComponents.LAST_REPEATS, 0), 0, 1_000_000));
                int age = nativeFlower.botanicalmekanism$process(((BlockItem) material.getItem()).getBlock());
                int streak = Math.clamp(Math.min(flower.getOrDefault(BotaniaDataComponents.STREAK_LENGTH, -1) + 1, age), 0, RafflowsiaBlockEntity.getMaxStreak());
                int mana = nativeFlower.botanicalmekanism$mana(streak);
                next.set(BotaniaDataComponents.STREAK_LENGTH, streak); next.set(BotaniaDataComponents.LAST_REPEATS, nativeFlower.botanicalmekanism$repeats());
                next.set(BotaniaDataComponents.LAST_FLOWERS, List.copyOf(nativeFlower.botanicalmekanism$flowers()));
                return mana <= 0 ? null : new Result(mana, interval(flowerEntity), 0, next, age);
            }
            case "kekimurus": {
                int slices = CakeBlock.MAX_BITES + 1;
                return new Result(KekimurusBlockEntity.MANA_PER_SLICE * slices,
                      interval(new KekimurusBlockEntity(BlockPos.ZERO, BotaniaBlocks.KEKIMURUS.defaultBlockState())) * slices, 0, next, 0);
            }
            case "munchdew": {
                GreenhouseWork.setLeafRun(next, true);
                return new Result(CultivatedMunchdewAccess.botanicalmekanism$mana(),
                      interval(new MunchdewBlockEntity(BlockPos.ZERO, BotaniaBlocks.MUNCHDEW.defaultBlockState())), 0, next, 0);
            }
            case "entropinnyum":
                return new Result(new EntropinnyumBlockEntity(BlockPos.ZERO, BotaniaBlocks.ENTROPINNYUM.defaultBlockState()).getMaxMana(), 80, 0, next, 0);
            default: return null;
        }
    }
    private GreenhouseNative() { }
}
