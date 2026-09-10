package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.crafting.recipes.CrushRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.datagen.BlockTagProvider;
import com.hollingsworth.arsnouveau.common.datagen.ItemTagProvider;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class AdvancedRecipes {
    public record Work(String id, int ticks, int[] consume, List<ItemStack> maximumOutputs,
                       Supplier<List<ItemStack>> outputs, int sourceCost, int sourceGain, int experienceCost,
                       int heatChange, CompoundTag signature, Runnable commit) { }

    static Work work(SourceMachine machine, String id, int[] consume, List<ItemStack> maximum,
                     Supplier<List<ItemStack>> outputs, int sourceCost, int sourceGain, int experienceCost,
                     int heatChange, CompoundTag external, Runnable commit) {
        int ticks = MachineConfig.PROCESS_TICKS.get(machine.kind()).get();
        var signature = new CompoundTag();
        signature.putString("recipe", id);
        signature.putInt("mode", machine.mode());
        signature.putInt("ticks", ticks);
        signature.putIntArray("consume", consume);
        signature.putInt("source_cost", sourceCost);
        signature.putInt("source_gain", sourceGain);
        signature.putInt("experience", experienceCost);
        signature.putInt("heat", heatChange);
        signature.put("external", external);
        var inputs = new ListTag();
        var products = new ListTag();
        for (var slot : machine.inputs) inputs.add(slot.isEmpty() ? new CompoundTag()
              : slot.getStack().copyWithCount(1).save(machine.getLevel().registryAccess()));
        for (ItemStack stack : maximum) products.add(stack.save(machine.getLevel().registryAccess()));
        signature.put("inputs", inputs);
        signature.put("maximum_outputs", products);
        return new Work(id, ticks, consume, maximum, outputs, sourceCost, sourceGain, experienceCost, heatChange, signature, commit);
    }

    static Work fixed(SourceMachine m, String id, int[] consume, List<ItemStack> products, int sourceCost,
                      int sourceGain, int experienceCost, int heatChange) {
        return work(m, id, consume, products, () -> products, sourceCost, sourceGain, experienceCost, heatChange, new CompoundTag(), () -> { });
    }

    public static Work plan(SourceMachine m) {
        m.status(SourceMachine.NO_RECIPE);
        return switch (m.kind()) {
            case SOURCE_EXTRACTOR -> extraction(m);
            case MAGIC_CRUSHER -> crushing(m);
            case GLYPH_SCRIBE -> scribing(m);
            case POTION_MIXER, POTION_BOTTLER -> PotionProcessing.plan(m);
            default -> null;
        };
    }

    public static boolean accepts(Level level, MachineKind kind, int slot, ItemStack stack) {
        return switch (kind) {
            case SOURCE_EXTRACTOR -> slot == 0 ? foodSource(stack) > 0 || fuelSource(stack) > 0
                  : slot == 1 ? stack.is(Items.STONE) || stack.is(Items.MAGMA_BLOCK) : stack.is(Items.BUCKET);
            case MAGIC_CRUSHER -> level == null || crushes(level).stream().anyMatch(h -> h.value().input().test(stack));
            case GLYPH_SCRIBE -> slot == 0 ? stack.getItem() instanceof SpellBook : level == null
                  || glyphs(level).stream().anyMatch(h -> h.value().inputs.stream().anyMatch(i -> i.test(stack)));
            case POTION_BOTTLER -> PotionProcessing.accepts(stack);
            case RITUAL_CONTROLLER -> slot != 0 || WorldControllers.acceptsTablet(stack);
            case DRYGMY_STATION, WHIRLISPRIG_STATION -> true;
            default -> false;
        };
    }

    public static List<RecipeHolder<CrushRecipe>> crushes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeRegistry.CRUSH_TYPE.get()).stream()
              .sorted(Comparator.comparing(h -> h.id().toString())).toList();
    }

    public static List<RecipeHolder<GlyphRecipe>> glyphs(Level level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeRegistry.GLYPH_TYPE.get()).stream()
              .filter(h -> h.value().getClass() == GlyphRecipe.class && h.value().output.getItem() instanceof Glyph
                    && h.value().getSpellPart().isEnabled() && Config.isGlyphEnabled(h.value().getSpellPart().getRegistryName()))
              .sorted(Comparator.comparing(h -> h.id().toString())).toList();
    }

    public static boolean isSupportedRecipe(SourceMachine m, ResourceLocation id) {
        if (m.kind() == MachineKind.MAGIC_CRUSHER) return crushes(m.getLevel()).stream().anyMatch(h -> h.id().equals(id));
        return m.kind() == MachineKind.GLYPH_SCRIBE && glyphs(m.getLevel()).stream().anyMatch(h -> h.id().equals(id));
    }

    private static boolean allowed(SourceMachine m, ResourceLocation id) { return m.recipeLock().isEmpty() || m.recipeLock().equals(id.toString()); }
    private static ItemStack input(SourceMachine m, int index) { return m.inputs.get(index).getStack(); }

    static int foodSource(ItemStack stack) {
        var food = stack.getItem().getFoodProperties(stack, null);
        if (food == null) return 0;
        int source = 11 * food.nutrition() + (int) (30D * food.saturation() * 2D);
        if (stack.is(ItemTagProvider.MAGIC_FOOD) || stack.getItem() instanceof BlockItem item
              && item.getBlock().defaultBlockState().is(BlockTagProvider.MAGIC_PLANTS)) {
            source += 10;
            source += (int) (source * 1.5F);
        }
        return Math.max(0, source);
    }

    static int fuelSource(ItemStack stack) {
        int source = Math.max(0, stack.getBurnTime(RecipeType.SMELTING)) / 12;
        if (stack.is(BlockRegistry.BLAZING_LOG.asItem())) source += 100;
        else if (stack.is(ItemTagProvider.ARCHWOOD_LOG_TAG)) source += 50;
        if (stack.is(ItemsRegistry.FIRE_ESSENCE.get())) source = 2_000;
        return source;
    }

    static int fuelHeat(ItemStack stack) {
        int heat = stack.getBurnTime(RecipeType.SMELTING) > 0 ? 1 : 0;
        if (stack.is(BlockRegistry.BLAZING_LOG.asItem())) heat += 5;
        else if (stack.is(ItemTagProvider.ARCHWOOD_LOG_TAG)) heat += 3;
        return heat;
    }

    private static Work extraction(SourceMachine m) {
        int[] consume = new int[m.inputs.size()];
        if (m.mode() == 2) {
            if (input(m, 1).is(Items.STONE)) {
                consume[1] = 1;
                return fixed(m, "arsmekanism:heat_stone", consume, List.of(new ItemStack(Items.MAGMA_BLOCK)), 0, 0, 0, -150);
            }
            if (input(m, 1).is(Items.MAGMA_BLOCK) && input(m, 2).is(Items.BUCKET)) {
                consume[1] = 1; consume[2] = 1;
                return fixed(m, "arsmekanism:heat_magma", consume, List.of(new ItemStack(Items.LAVA_BUCKET)), 0, 0, 0, -200);
            }
            m.status(SourceMachine.MISSING_MATERIALS); return null;
        }
        ItemStack stack = input(m, 0);
        if (stack.isEmpty()) { m.status(SourceMachine.MISSING_INPUT); return null; }
        int source = m.mode() == 0 ? foodSource(stack) : fuelSource(stack);
        if (source <= 0) return null;
        consume[0] = 1;
        var products = new ArrayList<ItemStack>();
        addRemainders(products, stack, 1);
        if (products.isEmpty() && m.mode() == 0) {
            var food = stack.getItem().getFoodProperties(stack, null);
            if (food != null) food.usingConvertsTo().ifPresent(container -> products.add(container.copy()));
        }
        return fixed(m, "arsmekanism:" + (m.mode() == 0 ? "extract_food" : "extract_fuel"), consume,
              products, 0, source, 0, m.mode() == 1 ? fuelHeat(stack) : 0);
    }

    private static Work crushing(SourceMachine m) {
        ItemStack input = input(m, 0);
        if (input.isEmpty()) { m.status(SourceMachine.MISSING_INPUT); return null; }
        for (var holder : crushes(m.getLevel())) {
            CrushRecipe recipe = holder.value();
            if (!allowed(m, holder.id()) || !recipe.input().test(input)) continue;
            var maximum = new ArrayList<ItemStack>();
            var rolls = new ListTag();
            for (var output : recipe.outputs()) {
                long count = (long) output.stack().getCount() * Math.max(1, output.maxRange());
                if (count > 384 || count < 0 || !Float.isFinite(output.chance())) return null;
                if (output.chance() >= 0 && count > 0) maximum.add(output.stack().copyWithCount((int) count));
                var roll = new CompoundTag();
                roll.putFloat("chance", output.chance()); roll.putInt("range", output.maxRange());
                roll.put("stack", output.stack().save(m.getLevel().registryAccess())); rolls.add(roll);
            }
            var external = new CompoundTag(); external.put("rolls", rolls);
            int[] consume = {1};
            return work(m, holder.id().toString(), consume, maximum, () -> recipe.getRolledOutputs(m.getLevel().getRandom()),
                  0, 0, 0, 0, external, () -> { });
        }
        return null;
    }

    private static Work scribing(SourceMachine m) {
        if (m.recipeLock().isEmpty()) { m.status(SourceMachine.NEED_SELECTION); return null; }
        if (!isSupportedRecipe(m, ResourceLocation.parse(m.recipeLock()))) { m.status(SourceMachine.BAD_LOCK); return null; }
        if (!(input(m, 0).getItem() instanceof SpellBook book)) { m.status(SourceMachine.NEED_BOOK); return null; }
        for (var holder : glyphs(m.getLevel())) {
            GlyphRecipe recipe = holder.value();
            if (!allowed(m, holder.id())) continue;
            if (recipe.getSpellPart().getConfigTier().value > book.getTier().value) {
                m.status(SourceMachine.BOOK_TIER_LOW);
                continue;
            }
            int[] used = matchMaterials(m, recipe.inputs);
            if (used == null) { m.status(SourceMachine.MISSING_MATERIALS); continue; }
            if (recipe.exp < 0 || recipe.output.isEmpty()) continue;
            int[] consume = new int[m.inputs.size()];
            var products = new ArrayList<ItemStack>(); products.add(recipe.output.copy());
            for (int i = 0; i < used.length; i++) {
                consume[i + 1] = used[i]; addRemainders(products, input(m, i + 1), used[i]);
            }
            return fixed(m, holder.id().toString(), consume, products, 0, 0, recipe.exp, 0);
        }
        return null;
    }

    private static int[] matchMaterials(SourceMachine m, List<Ingredient> ingredients) {
        if (ingredients.size() > 64) return null;
        int count = m.inputs.size() - 1;
        boolean[][] matches = new boolean[ingredients.size()][count];
        int[] capacity = new int[count], required = new int[ingredients.size()]; Arrays.fill(required, 1);
        for (int i = 0; i < count; i++) {
            ItemStack stack = input(m, i + 1); capacity[i] = stack.getCount();
            for (int j = 0; j < ingredients.size(); j++) matches[j][i] = ingredients.get(j).test(stack);
        }
        return IngredientAssignment.matchQuantities(matches, capacity, required);
    }

    private static void addRemainders(List<ItemStack> products, ItemStack input, int consumed) {
        if (consumed == 0) return;
        ItemStack remainder = input.copyWithCount(1).getCraftingRemainingItem();
        if (!remainder.isEmpty()) products.add(remainder.copyWithCount(remainder.getCount() * consumed));
    }

    private AdvancedRecipes() { }
}
