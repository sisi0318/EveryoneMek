package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.brew.BrewContainer;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.recipe.*;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.crafting.StateIngredients;
import vazkii.botania.common.lib.BotaniaTags;

/** Recipe planning uses copies; resources and random results are committed once, together. */
public final class ManaWork {
    public record Choice(ResourceLocation id, ItemStack icon) { }
    public record Result(ResourceLocation id, int mana, int weight, List<ItemStack> products, StateIngredient randomOutput) { }
    public record Plan(int[] used, int[] extraUsed, Map<Integer, ItemStack> retained, boolean extraValid,
                       int ticks, List<Result> results, List<ItemStack> consumed, List<ItemStack> extraSignature) {
        public int maxMana() { return results.stream().mapToInt(Result::mana).max().orElse(0); }
        public boolean canOutput(ManaMachine tile) {
            for (var result : results) {
                if (result.randomOutput() == null) {
                    if (tile.mergeOutputs(result.products()) == null) return false;
                } else for (var state : result.randomOutput().getDisplayed()) {
                    var products = new ArrayList<>(result.products()); products.add(new ItemStack(state.getBlock()));
                    if (tile.mergeOutputs(products) == null) return false;
                }
            }
            return true;
        }
        public CompoundTag signature(ManaMachine tile) {
            var tag = new CompoundTag(); tag.putInt("ticks", ticks);
            tag.put("consumed", stacks(tile, consumed)); tag.put("extra_stacks", stacks(tile, extraSignature));
            var resultTags = new ListTag();
            for (var result : results) {
                var entry = new CompoundTag(); entry.putString("id", result.id().toString()); entry.putInt("mana", result.mana()); entry.putInt("weight", result.weight());
                entry.put("products", stacks(tile, result.products()));
                if (result.randomOutput() != null) entry.put("random_outputs", stacks(tile, result.randomOutput().getDisplayed().stream().map(s -> new ItemStack(s.getBlock())).toList()));
                resultTags.add(entry);
            }
            tag.put("results", resultTags);
            tag.put("retained", stacks(tile, new ArrayList<>(retained.values()))); return tag;
        }
        public void commit(ManaMachine tile) {
            long total = results.stream().mapToLong(Result::weight).sum();
            long roll = boundedRandom(tile.getLevel().random, total); Result selected = results.getLast();
            for (var result : results) { roll -= result.weight(); if (roll < 0) { selected = result; break; } }
            var products = new ArrayList<>(selected.products());
            if (selected.randomOutput() != null) products.add(new ItemStack(selected.randomOutput().pick(tile.getLevel().random).getBlock()));
            var merged = Objects.requireNonNull(tile.mergeOutputs(products), "Reserved output changed during atomic craft");
            tile.mana().extract(selected.mana(), Action.EXECUTE, AutomationType.INTERNAL);
            for (int i = 0; i < used.length; i++) tile.inputs.get(i).shrinkStack(used[i], Action.EXECUTE);
            retained.forEach((slot, stack) -> {
                var current = tile.inputs.get(slot).getStack();
                tile.inputs.get(slot).setStackUnchecked(stack.copyWithCount(current.getCount() + stack.getCount()));
            });
            for (int i = 0; i < extraUsed.length; i++) tile.extras.get(i).shrinkStack(extraUsed[i], Action.EXECUTE);
            tile.setOutputs(merged);
        }
    }
    private static ListTag stacks(ManaMachine tile, List<ItemStack> stacks) {
        var list = new ListTag(); stacks.stream().map(stack -> stack.saveOptional(tile.getLevel().registryAccess()))
              .sorted(Comparator.comparing(Tag::toString)).forEach(list::add); return list;
    }
    public static boolean accepts(ManaMachineKind kind, Level level, ItemStack stack, boolean extra) {
        if (extra) return switch (kind) {
            case INFUSER -> catalystState(stack) != null;
            case BREWERY -> stack.getItem() instanceof BrewContainer;
            case ORE -> stack.is(vazkii.botania.common.block.BotaniaBlocks.ORECHID.asItem()) || stack.is(vazkii.botania.common.block.BotaniaBlocks.ORECHID_IGNEM.asItem());
            case ENCHANTER -> stack.is(Items.ENCHANTED_BOOK);
            case RUNIC -> level != null && level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.RUNIC_ALTAR_TYPE).stream().anyMatch(r -> r.value().getReagent().test(stack));
            default -> false;
        };
        if (kind == ManaMachineKind.CHARGER) return ManaItem.LOOKUP.find(stack) != null;
        if (kind == ManaMachineKind.ENCHANTER) return stack.isEnchantable();
        if (kind == ManaMachineKind.PURE || kind.random()) return itemState(stack) != null;
        if (level == null) return true;
        return recipes(kind, level).stream().anyMatch(holder -> allIngredients(holder.value()).stream().anyMatch(i -> i.test(stack)));
    }
    private static List<Ingredient> allIngredients(Recipe<?> recipe) {
        var ingredients = new ArrayList<>(recipe.getIngredients());
        if (recipe instanceof RunicAltarRecipe runic) ingredients.addAll(runic.getCatalysts()); return ingredients;
    }
    public static List<? extends RecipeHolder<?>> recipes(ManaMachineKind kind, Level level) {
        return switch (kind) {
            case INFUSER -> level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.MANA_INFUSION_TYPE);
            case RUNIC -> level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.RUNIC_ALTAR_TYPE);
            case PURE -> level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.PURE_DAISY_TYPE);
            case TERRA -> level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE);
            case BREWERY -> level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.BREW_TYPE);
            case ELVEN -> level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.ELVEN_TRADE_TYPE);
            default -> List.of();
        };
    }
    public static List<Choice> choices(ManaMachine tile) {
        List<Choice> choices = new ArrayList<>();
        for (var holder : recipes(tile.kind(), tile.getLevel())) {
            var recipe = holder.value(); ItemStack icon;
            if (recipe instanceof PureDaisyRecipe daisy) {
                if (!safeStateRecipe(daisy) || !safeOutput(daisy.getOutput())) continue;
                icon = new ItemStack(daisy.getOutput().getDisplayed().getFirst().getBlock());
            } else if (recipe instanceof ManaInfusionRecipe infusion) {
                if (!catalystMatches(tile, infusion)) continue;
                icon = infusion.getResultItem(tile.getLevel().registryAccess());
            } else if (recipe instanceof BotanicalBreweryRecipe brew) {
                var container = tile.extras.getFirst().getStack();
                if (!(container.getItem() instanceof BrewContainer vessel) || vessel.getManaCost(brew.getBrew(), container.copy()) < 0) continue;
                icon = brew.getOutput(container.copyWithCount(1));
            } else icon = recipe.getResultItem(tile.getLevel().registryAccess());
            if (!icon.isEmpty()) choices.add(new Choice(holder.id(), icon.copy()));
        }
        choices.sort(Comparator.comparing(choice -> choice.id().toString())); return choices;
    }
    public static Plan find(ManaMachine tile) {
        var inputs = tile.inputs.stream().map(slot -> slot.getStack().copy()).toList();
        if (inputs.isEmpty() || inputs.stream().allMatch(ItemStack::isEmpty)) return null;
        if (tile.kind().random()) return random(tile, inputs);
        List<RecipeHolder<?>> holders = new ArrayList<>(recipes(tile.kind(), tile.getLevel()));
        holders.sort(Comparator.comparing(holder -> holder.id().toString()));
        if (tile.kind() == ManaMachineKind.INFUSER) holders.sort(Comparator.comparingInt(holder ->
              ((ManaInfusionRecipe) holder.value()).getRecipeCatalyst() == StateIngredients.NONE ? 1 : 0));
        Plan missingExtra = null;
        for (var holder : holders) {
            if (!tile.recipeLock().isEmpty() && !holder.id().toString().equals(tile.recipeLock())) continue;
            Plan plan = switch (tile.kind()) {
                case INFUSER -> infusion(tile, holder, inputs);
                case PURE -> pure(tile, holder, inputs);
                case RUNIC, TERRA, BREWERY -> processing(tile, holder, inputs);
                default -> null;
            };
            if (plan != null) {
                if (plan.extraValid()) return plan;
                if (missingExtra == null) missingExtra = plan;
            }
        }
        return missingExtra;
    }
    private static Plan infusion(ManaMachine tile, RecipeHolder<?> holder, List<ItemStack> inputs) {
        var recipe = (ManaInfusionRecipe) holder.value();
        if (!recipe.matches(inputs.getFirst().copy()) || !catalystMatches(tile, recipe)) return null;
        var output = recipe.getRecipeOutput(tile.getLevel().registryAccess(), inputs.getFirst().copy());
        if (output.isEmpty() || !validMana(recipe.getManaToConsume())) return null;
        List<ItemStack> products = new ArrayList<>(); products.add(output.copy());
        return simple(tile, holder.id(), inputs, recipe.getManaToConsume(), 100, products, null);
    }
    private static boolean catalystMatches(ManaMachine tile, ManaInfusionRecipe recipe) {
        if (recipe.getRecipeCatalyst() == StateIngredients.NONE) return true;
        BlockState state = catalystState(tile.extras.getFirst().getStack()); return state != null && recipe.getRecipeCatalyst().test(state);
    }
    // Only native catalyst blocks have a defined machine context. Third-party stateful catalysts need an adapter.
    private static BlockState catalystState(ItemStack stack) {
        var state = itemState(stack);
        return state != null && (state.is(vazkii.botania.common.block.BotaniaBlocks.ALCHEMY_CATALYST)
              || state.is(vazkii.botania.common.block.BotaniaBlocks.CONJURATION_CATALYST)) ? state : null;
    }
    private static Plan pure(ManaMachine tile, RecipeHolder<?> holder, List<ItemStack> inputs) {
        var recipe = (PureDaisyRecipe) holder.value(); var state = itemState(inputs.getFirst());
        if (state == null || !safeStateRecipe(recipe) || !safeOutput(recipe.getOutput())
              || !recipe.matches(tile.getLevel(), tile.getBlockPos(), state)) return null;
        // One output every native check interval: equal throughput to all eight native positions occupied.
        return simple(tile, holder.id(), inputs, 0, Math.max(1, recipe.getTime()), List.of(), recipe.getOutput());
    }
    @SuppressWarnings("unchecked")
    private static Plan processing(ManaMachine tile, RecipeHolder<?> holder, List<ItemStack> inputs) {
        var recipe = (Recipe<ProcessingRecipeInput>) holder.value();
        var ingredients = allIngredients(recipe);
        if (ingredients.isEmpty() || ingredients.size() > tile.inputs.size()) return null;
        for (var stack : inputs) if (!stack.isEmpty() && ingredients.stream().noneMatch(i -> i.test(stack))) return null;
        int[] used = ApothecaryWork.assign(ingredients, inputs); if (used == null) return null;
        List<ItemStack> consumed = new ArrayList<>(); List<Integer> sources = new ArrayList<>();
        for (int i = 0; i < used.length; i++) for (int n = 0; n < used[i]; n++) { consumed.add(inputs.get(i).copyWithCount(1)); sources.add(i); }
        var all = new ArrayList<>(consumed); int cost, ticks; boolean extraValid = true;
        int[] extraUsed = new int[tile.extras.size()]; var extraSignature = tile.extras.stream().map(slot -> slot.getStack().copyWithCount(1)).toList();
        if (recipe instanceof BotanicalBreweryRecipe brew) {
            var stack = tile.extras.getFirst().getStack().copyWithCount(1);
            if (!(stack.getItem() instanceof BrewContainer container)) return null;
            cost = container.getManaCost(brew.getBrew(), stack.copy()); all.addFirst(stack); extraUsed[0] = 1; ticks = 200;
        } else if (recipe instanceof RunicAltarRecipe runic) {
            cost = runic.getMana(); ticks = 200; extraUsed[0] = 1; extraValid = runic.getReagent().test(tile.extras.getFirst().getStack());
        } else if (recipe instanceof TerrestrialAgglomerationRecipe terra) { cost = terra.getMana(); ticks = 400; }
        else return null;
        if (!validMana(cost) || !recipe.matches(new ApothecaryWork.Input(all), tile.getLevel())) return null;
        var output = recipe.assemble(new ApothecaryWork.Input(all), tile.getLevel().registryAccess()); if (output.isEmpty()) return null;
        List<ItemStack> products = new ArrayList<>(); products.add(output.copy()); Map<Integer, ItemStack> retained = new HashMap<>();
        var remaining = recipe.getRemainingItems(new ApothecaryWork.Input(all));
        for (int i = 0; i < remaining.size(); i++) {
            var stack = remaining.get(i); if (stack.isEmpty()) continue;
            if (recipe instanceof RunicAltarRecipe && i < consumed.size() && ItemStack.isSameItemSameComponents(stack, consumed.get(i))) {
                int slot = sources.get(i); var previous = retained.get(slot);
                retained.put(slot, stack.copyWithCount(stack.getCount() + (previous == null ? 0 : previous.getCount())));
            } else products.add(stack.copy());
        }
        for (var entry : retained.entrySet()) if (inputs.get(entry.getKey()).getCount() - used[entry.getKey()] + entry.getValue().getCount()
              > tile.inputs.get(entry.getKey()).getLimit(entry.getValue())) return null;
        if (recipe instanceof RunicAltarRecipe && extraValid) {
            var remainder = tile.extras.getFirst().getStack().copyWithCount(1).getCraftingRemainingItem();
            if (!remainder.isEmpty()) products.add(remainder);
        }
        return new Plan(used, extraUsed, retained, extraValid, ticks, List.of(new Result(holder.id(), cost, 1, products, null)), consumed, extraSignature);
    }
    private static Plan simple(ManaMachine tile, ResourceLocation id, List<ItemStack> inputs, int mana, int ticks, List<ItemStack> products, StateIngredient random) {
        return new Plan(new int[]{1}, new int[tile.extras.size()], Map.of(), true, ticks,
              List.of(new Result(id, mana, 1, products, random)), List.of(inputs.getFirst().copyWithCount(1)),
              tile.extras.stream().map(slot -> slot.getStack().copyWithCount(1)).toList());
    }
    private static Plan random(ManaMachine tile, List<ItemStack> inputs) {
        var state = itemState(inputs.getFirst()); if (state == null) return null;
        RecipeType<OrechidRecipe> type = BotaniaRecipeTypes.MARIMORPHOSIS_TYPE;
        if (tile.kind() == ManaMachineKind.ORE) {
            var flower = tile.extras.getFirst().getStack();
            if (flower.is(vazkii.botania.common.block.BotaniaBlocks.ORECHID.asItem())) type = BotaniaRecipeTypes.ORECHID_TYPE;
            else if (flower.is(vazkii.botania.common.block.BotaniaBlocks.ORECHID_IGNEM.asItem())) {
                if (!tile.getLevel().dimensionType().hasCeiling()) return null;
                type = BotaniaRecipeTypes.ORECHID_IGNEM_TYPE;
            } else return null;
        }
        List<Result> results = new ArrayList<>(); int ticks = 1;
        for (var holder : tile.getLevel().getRecipeManager().getAllRecipesFor(type)) {
            var recipe = holder.value();
            if (!safeStateRecipe(recipe) || !recipe.getInput().test(state) || !validMana(recipe.getManaCost())) continue;
            var output = recipe.getOutput(tile.getLevel(), tile.getBlockPos()); int weight = recipe.getWeight(tile.getLevel(), tile.getBlockPos());
            if (weight <= 0 || !safeOutput(output)) continue;
            results.add(new Result(holder.id(), recipe.getManaCost(), weight, List.of(), output)); ticks = Math.max(ticks, recipe.getCooldown());
        }
        if (results.isEmpty()) return null;
        results.sort(Comparator.comparing(result -> result.id().toString()));
        return new Plan(new int[]{1}, new int[tile.extras.size()], Map.of(), true, ticks, results,
              List.of(inputs.getFirst().copyWithCount(1)), tile.extras.stream().map(slot -> slot.getStack().copyWithCount(1)).toList());
    }
    private static boolean validMana(int value) { return value >= 0 && value <= ManaMachine.MANA_CAPACITY; }
    private static long boundedRandom(net.minecraft.util.RandomSource random, long bound) {
        long bits, value;
        do { bits = random.nextLong() >>> 1; value = bits % bound; } while (bits - value + bound - 1 < 0);
        return value;
    }
    private static BlockState itemState(ItemStack stack) {
        return !stack.isEmpty() && !stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)
              && !stack.has(net.minecraft.core.component.DataComponents.BLOCK_STATE) && stack.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : null;
    }
    private static boolean safeStateRecipe(BlockStateRecipe recipe) {
        String type = recipe.getClass().getName();
        return Set.of("vazkii.botania.common.crafting.PureDaisyRecipe", "vazkii.botania.common.crafting.OrechidRecipe",
              "vazkii.botania.common.crafting.OrechidIgnemRecipe", "vazkii.botania.common.crafting.MarimorphosisRecipe").contains(type)
              && recipe.getPreUpdateFunction().isEmpty() && recipe.getSuccessFunction().isEmpty();
    }
    private static boolean safeOutput(StateIngredient ingredient) {
        var states = ingredient.getDisplayed();
        return !states.isEmpty() && states.size() <= 4096 && states.stream().allMatch(state -> !state.isAir() && state.getFluidState().isEmpty()
              && state.getBlock().asItem() instanceof BlockItem item && item.getBlock() == state.getBlock() && state == state.getBlock().defaultBlockState());
    }
    public static boolean platform(ManaMachine tile) {
        if (!PlantSupport.areaLoaded(tile.getLevel(), tile.getBlockPos(), 1)) return false;
        var lapis = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lapis"));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            var state = tile.getLevel().getBlockState(tile.getBlockPos().offset(x, -1, z));
            if (!(Math.abs(x) + Math.abs(z) == 1 ? state.is(lapis) : state.is(BotaniaTags.Blocks.TERRA_PLATE_BASE))) return false;
        }
        return true;
    }
    private ManaWork() { }
}
