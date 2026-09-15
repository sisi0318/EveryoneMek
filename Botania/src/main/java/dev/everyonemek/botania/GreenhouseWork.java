package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.*;
import vazkii.botania.common.component.BotaniaDataComponents;

public final class GreenhouseWork {
    public static final int FLUID_CAPACITY = 16_000;
    private static final String DATA = "botanicalmekanism_greenhouse";
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BotanicalMekanism.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BotanicalMekanism.ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<GreenhouseRecipe>> TYPE = TYPES.register("mana_greenhouse", () -> new RecipeType<>() {
        @Override public String toString() { return "botanicalmekanism:mana_greenhouse"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GreenhouseRecipe>> SERIALIZER = SERIALIZERS.register("mana_greenhouse", GreenhouseRecipe.Serializer::new);
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
    public static List<RecipeHolder<GreenhouseRecipe>> recipes(Level level) {
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(TYPE.get());
    }
    public static boolean accepts(Level level, ItemStack stack, boolean flower) {
        if (stack.isEmpty()) return false;
        if (level == null) return true;
        return recipes(level).stream().anyMatch(r -> flower ? r.value().flower().test(stack) : r.value().fixed()
              ? r.value().materials().stream().anyMatch(i -> i.test(stack)) : r.value().rule().acceptsItem(stack));
    }
    public static boolean acceptsFluid(ManaMachine tile, FluidStack stack) {
        var level = tile.getLevel();
        if (stack.isEmpty()) return false;
        if (level == null) return true;
        var flower = tile.extras == null || tile.extras.isEmpty() ? ItemStack.EMPTY : tile.extras.getFirst().getStack();
        return recipes(level).stream().filter(r -> flower.isEmpty() || r.value().flower().test(flower)).anyMatch(r -> r.value().fixed()
              ? !r.value().fluid().isEmpty() && FluidStack.isSameFluidSameComponents(r.value().fluid(), stack)
              : r.value().rule().fluidAmount() > 0 && r.value().rule().acceptsFluid(stack));
    }
    private static CompoundTag data(ItemStack flower) { return flower.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(DATA); }
    private static void data(ItemStack flower, CompoundTag data) {
        CustomData.update(DataComponents.CUSTOM_DATA, flower, tag -> { if (data.isEmpty()) tag.remove(DATA); else tag.put(DATA, data); });
    }
    public static void setLeafRun(ItemStack flower, boolean running) { var data = data(flower); if (running) data.putBoolean("leaf_run", true); else data.remove("leaf_run"); data(flower, data); }
    public static int cooldown(ItemStack flower) { return Math.clamp(flower.getOrDefault(BotaniaDataComponents.COOLDOWN, 0), 0, 2_000_000); }
    public static void cooldown(ItemStack flower, int ticks) {
        // One native component remains authoritative after replanting and breaking a flower.
        if (ticks > 0) flower.set(BotaniaDataComponents.COOLDOWN, ticks); else flower.remove(BotaniaDataComponents.COOLDOWN);
    }
    public static boolean cool(ManaMachine tile) {
        var flower = tile.extras.getFirst().getStack(); int remaining = cooldown(flower);
        if (remaining == 0) return false;
        var next = flower.copy(); cooldown(next, remaining - 1); tile.extras.getFirst().setStackUnchecked(next);
        tile.status(ManaMachine.COOLING); return true;
    }
    public static boolean leafRunning(ItemStack flower) { return data(flower).getBoolean("leaf_run"); }
    public static void prepareFlower(ManaMachine tile) { updateFlower(tile, false); }
    public static void onBlocked(ManaMachine tile) { updateFlower(tile, true); }
    /** Kept for callers of the alpha.29 API. */
    public static void stopLeafRun(ManaMachine tile) { onBlocked(tile); }
    private static void updateFlower(ManaMachine tile, boolean blocked) {
        var slot = tile.extras.getFirst(); if (slot.isEmpty()) return;
        for (var holder : recipes(tile.getLevel())) {
            var recipe = holder.value(); var flower = slot.getStack();
            if (recipe.fixed() || !recipe.flower().test(flower)) continue;
            var next = blocked ? recipe.rule().onBlocked(flower.copy(), tile.getLevel()) : recipe.rule().prepare(flower.copy(), tile.getLevel());
            if (sameFlower(flower, next) && !ItemStack.matches(flower, next)) slot.setStackUnchecked(next.copy());
        }
    }
    public static net.minecraft.network.chat.Component flowerInfo(ManaMachine tile) {
        var flower = tile.extras.getFirst().getStack();
        for (var holder : recipes(tile.getLevel())) {
            var recipe = holder.value(); if (recipe.fixed() || !recipe.flower().test(flower)) continue;
            var info = recipe.rule().statusInfo(flower.copy(), tile.getLevel());
            if (!info.getString().isEmpty()) return info;
        }
        return net.minecraft.network.chat.Component.empty();
    }
    private static boolean sameFlower(ItemStack before, ItemStack after) {
        return after != null && !after.isEmpty() && after.getCount() == before.getCount() && before.is(after.getItem());
    }
    public static boolean validResult(GreenhouseNative.Result result, ItemStack flower) {
        return result != null && result.mana() > 0 && result.mana() <= ManaMachine.MANA_CAPACITY
              && result.ticks() >= 1 && result.ticks() <= 2_000_000 && result.cooldown() >= 0 && result.cooldown() <= 2_000_000
              && sameFlower(flower, result.flower());
    }
    public record Plan(ResourceLocation id, int[] consume, FluidStack fluid, int mana, int ticks, int cooldown,
          ItemStack flower, int preference) {
        public CompoundTag signature(ManaMachine tile) {
            var tag = new CompoundTag(); tag.putString("recipe", id.toString()); tag.putInt("mana", mana); tag.putInt("ticks", ticks); tag.putInt("cooldown", cooldown);
            tag.put("flower", tile.extras.getFirst().getStack().save(tile.getLevel().registryAccess()));
            tag.put("next_flower", flower.save(tile.getLevel().registryAccess()));
            var used = new ListTag();
            for (int i = 0; i < consume.length; i++) if (consume[i] > 0) {
                var entry = new CompoundTag(); entry.putInt("slot", i); entry.put("stack", tile.inputs.get(i).getStack().copyWithCount(consume[i]).save(tile.getLevel().registryAccess())); used.add(entry);
            }
            tag.put("inputs", used); if (!fluid.isEmpty()) tag.put("fluid", fluid.save(tile.getLevel().registryAccess())); return tag;
        }
        public List<ItemStack> remainders(ManaMachine tile) {
            var products = new ArrayList<ItemStack>();
            for (int i = 0; i < consume.length; i++) if (consume[i] > 0) {
                var remainder = tile.inputs.get(i).getStack().getCraftingRemainingItem();
                if (!remainder.isEmpty()) for (int count = 0; count < consume[i]; count++) products.add(remainder.copy());
            }
            return products;
        }
        public void commit(ManaMachine tile, List<ItemStack> products) {
            for (int i = 0; i < consume.length; i++) if (consume[i] > 0) tile.inputs.get(i).shrinkStack(consume[i], Action.EXECUTE);
            if (!fluid.isEmpty()) tile.greenhouseFluid().extract(fluid.getAmount(), Action.EXECUTE, AutomationType.INTERNAL);
            var next = flower.copy(); GreenhouseWork.cooldown(next, cooldown); tile.extras.getFirst().setStackUnchecked(next);
            tile.setOutputs(products); tile.mana().insert(new ChemicalStack(ManaContent.MANA, mana), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }
    static final class Cache {
        Plan plan;
        Recipe<?> recipe;
        List<ItemStack> inputs;
        ItemStack flower;
        FluidStack fluid;
        long retry;
        boolean matches(ManaMachine tile) {
            if (flower == null || !ItemStack.matches(flower, tile.extras.getFirst().getStack())
                  || !FluidStack.matches(fluid, tile.greenhouseFluid().getFluid())) return false;
            for (int i = 0; i < inputs.size(); i++) if (!ItemStack.matches(inputs.get(i), tile.inputs.get(i).getStack())) return false;
            return plan == null ? tile.getLevel().getGameTime() < retry
                  : tile.getLevel().getRecipeManager().byKey(plan.id()).map(r -> r.value() == recipe).orElse(false);
        }
    }
    public static Plan find(ManaMachine tile) {
        if (tile.greenhouseCache == null) tile.greenhouseCache = new Cache();
        var cache = tile.greenhouseCache;
        if (cache.matches(tile)) return cache.plan;
        cache.plan = resolve(tile);
        cache.recipe = cache.plan == null ? null : tile.getLevel().getRecipeManager().byKey(cache.plan.id()).orElseThrow().value();
        cache.inputs = tile.inputs.stream().map(slot -> slot.getStack().copy()).toList();
        cache.flower = tile.extras.getFirst().getStack().copy(); cache.fluid = tile.greenhouseFluid().getFluid().copy();
        cache.retry = tile.getLevel().getGameTime() + 20;
        return cache.plan;
    }
    private static Plan resolve(ManaMachine tile) {
        var flower = tile.extras.getFirst().getStack(); if (flower.isEmpty() || flower.getCount() != 1) return null;
        var inputs = tile.inputs.stream().map(slot -> slot.getStack()).toList();
        var candidates = new ArrayList<>(recipes(tile.getLevel())); candidates.sort(Comparator.comparing(r -> r.id().toString()));
        for (var holder : candidates) {
            var recipe = holder.value(); if (!recipe.flower().test(flower)) continue;
            if (recipe.fixed()) {
                int[] consumed = assign(recipe.materials(), inputs); if (consumed == null || !hasFluid(tile, recipe.fluid())) continue;
                return new Plan(holder.id(), consumed, recipe.fluid(), recipe.mana(), recipe.ticks(), recipe.cooldown(), flower.copyWithCount(1), 0);
            }
            var rule = recipe.rule();
            if (rule.fluidAmount() > 0) {
                var tank = tile.greenhouseFluid().getFluid();
                if (tank.getAmount() < rule.fluidAmount() || !rule.acceptsFluid(tank)) continue;
                var result = rule.resolve(flower.copy(), ItemStack.EMPTY, tank.copy(), tile.getLevel());
                if (validResult(result, flower)) return plan(holder.id(), new int[inputs.size()], tank.copyWithAmount(rule.fluidAmount()), result);
                continue;
            }
            Plan best = null;
            for (int i = 0; i < inputs.size(); i++) {
                if (!rule.acceptsItem(inputs.get(i))) continue;
                var result = rule.resolve(flower.copy(), inputs.get(i).copy(), FluidStack.EMPTY, tile.getLevel());
                if (validResult(result, flower) && (best == null || result.preference() > best.preference)) {
                    var consumed = new int[inputs.size()]; consumed[i] = 1; best = plan(holder.id(), consumed, FluidStack.EMPTY, result);
                }
            }
            if (best != null) return best;
        }
        return null;
    }
    private static Plan plan(ResourceLocation id, int[] consume, FluidStack fluid, GreenhouseNative.Result result) {
        return new Plan(id, consume, fluid, result.mana(), result.ticks(), result.cooldown(), result.flower(), result.preference());
    }
    private static boolean hasFluid(ManaMachine tile, FluidStack required) {
        return required.isEmpty() || tile.greenhouseFluid().getFluidAmount() >= required.getAmount()
              && FluidStack.isSameFluidSameComponents(required, tile.greenhouseFluid().getFluid());
    }
    private static int[] assign(List<Ingredient> required, List<ItemStack> inputs) {
        if (required.isEmpty()) return new int[inputs.size()];
        return ApothecaryWork.assign(required, inputs);
    }
    private GreenhouseWork() { }
}
