package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.block.tile.PotionJarTile;
import com.hollingsworth.arsnouveau.common.items.PotionFlask;
import com.hollingsworth.arsnouveau.common.items.data.MultiPotionContents;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.hollingsworth.arsnouveau.common.util.PotionUtil;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import java.util.HashSet;
import java.util.List;
import mekanism.api.RelativeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

final class PotionProcessing {
    static boolean accepts(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE) || stack.is(Items.POTION) || stack.is(Items.ARROW) || stack.getItem() instanceof PotionFlask;
    }

    static AdvancedRecipes.Work plan(SourceMachine m) { return m.kind() == MachineKind.POTION_MIXER ? mixing(m) : bottling(m); }

    private static PotionJarTile jar(SourceMachine m, RelativeSide side) {
        BlockPos pos = m.getBlockPos().relative(side.getDirection(m.getDirection()));
        return m.getLevel().hasChunkAt(pos) && m.getLevel().getBlockEntity(pos) instanceof PotionJarTile jar ? jar : null;
    }

    private static boolean canAdd(PotionJarTile jar, PotionContents contents, int amount) {
        // Ars accepts an empty unlocked jar before checking capacity; reserve the full amount explicitly.
        return amount > 0 && amount <= jar.getMaxFill() - jar.getAmount() && jar.canAccept(contents, amount);
    }

    private static void describe(CompoundTag tag, String key, PotionJarTile jar, SourceMachine m) {
        var value = new CompoundTag(); value.putLong("position", jar.getBlockPos().asLong());
        value.put("contents", ANCodecs.encode(m.getLevel().registryAccess(), PotionContents.CODEC, jar.getData()));
        value.putBoolean("locked", jar.isLocked); tag.put(key, value);
    }

    private static AdvancedRecipes.Work mixing(SourceMachine m) {
        PotionJarTile first = jar(m, RelativeSide.LEFT), second = jar(m, RelativeSide.RIGHT), output = jar(m, RelativeSide.TOP);
        m.observe(first == null ? -1 : first.getAmount(), second == null ? -1 : second.getAmount(), output == null ? -1 : output.getAmount());
        if (first == null || second == null || output == null) { m.status(SourceMachine.NO_POTION_JAR); return null; }
        int inputCost = Config.MELDER_INPUT_COST.get(), outputAmount = Config.MELDER_OUTPUT.get(), source = Config.MELDER_SOURCE_COST.get();
        if (inputCost <= 0 || outputAmount <= 0 || source < 0) return null;
        if (first.getAmount() < inputCost || second.getAmount() < inputCost) { m.status(SourceMachine.NEED_POTION); return null; }
        PotionContents combined = PotionUtil.merge(first.getData(), second.getData());
        boolean duplicate = false;
        var effects = new HashSet<MobEffect>();
        for (var effect : combined.getAllEffects()) if (!effects.add(effect.getEffect().value())) duplicate = true;
        if (duplicate || PotionUtil.arePotionContentsEqual(combined, first.getData()) || PotionUtil.arePotionContentsEqual(combined, second.getData())) {
            m.status(SourceMachine.INVALID_POTION); return null;
        }
        if (!canAdd(output, combined, outputAmount)) { m.status(SourceMachine.OUTPUT_FULL); return null; }
        var external = new CompoundTag();
        describe(external, "first", first, m); describe(external, "second", second, m); describe(external, "output", output, m);
        external.putInt("input_cost", inputCost); external.putInt("output_amount", outputAmount);
        return AdvancedRecipes.work(m, "arsmekanism:mix_potions", new int[0], List.of(), List::of,
              source, 0, 0, 0, external, () -> {
                  output.add(combined, outputAmount); first.remove(inputCost); second.remove(inputCost);
              });
    }

    private static AdvancedRecipes.Work bottling(SourceMachine m) {
        PotionJarTile jar = jar(m, RelativeSide.FRONT);
        m.observe(jar == null ? -1 : jar.getAmount(), -1, -1);
        if (jar == null) { m.status(SourceMachine.NO_POTION_JAR); return null; }
        ItemStack input = m.inputs.getFirst().getStack();
        if (input.isEmpty()) { m.status(SourceMachine.MISSING_INPUT); return null; }
        ItemStack product;
        int amount;
        PotionContents contents;
        boolean draining = m.mode() == 1;
        if (input.getItem() instanceof PotionFlask) {
            var data = input.get(DataComponentRegistry.MULTI_POTION);
            if (data == null || data.charges() < 0 || data.maxUses() < 1 || data.maxUses() > 100 || data.charges() > data.maxUses()) {
                m.status(SourceMachine.INVALID_POTION); return null;
            }
            contents = draining ? data.contents() : jar.getData();
            int doses = draining ? data.charges() : data.maxUses() - data.charges();
            if (doses <= 0) { m.status(SourceMachine.NO_RECIPE); return null; }
            if (!draining && data.charges() > 0 && !PotionUtil.arePotionContentsEqual(data.contents(), contents)) {
                m.status(SourceMachine.INVALID_POTION); return null;
            }
            amount = doses * 100;
            product = input.copyWithCount(1);
            product.set(DataComponentRegistry.MULTI_POTION,
                  new MultiPotionContents(draining ? 0 : data.maxUses(), draining ? data.contents() : contents, data.maxUses()));
        } else if (draining && input.is(Items.POTION)) {
            contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            amount = 100; product = new ItemStack(Items.GLASS_BOTTLE);
        } else if (!draining && (input.is(Items.GLASS_BOTTLE) || input.is(Items.ARROW))) {
            contents = jar.getData(); amount = input.is(Items.ARROW) ? 10 : 100;
            product = new ItemStack(input.is(Items.ARROW) ? Items.TIPPED_ARROW : Items.POTION);
            product.set(DataComponents.POTION_CONTENTS, contents);
        } else return null;
        if (contents.equals(PotionContents.EMPTY) || !jar.validContentTypeForJar(contents)) {
            m.status(SourceMachine.INVALID_POTION); return null;
        }
        if (draining && !canAdd(jar, contents, amount)) { m.status(SourceMachine.OUTPUT_FULL); return null; }
        if (!draining && jar.getAmount() < amount) { m.status(SourceMachine.NEED_POTION); return null; }
        var external = new CompoundTag(); describe(external, "jar", jar, m); external.putInt("amount", amount);
        var outputs = List.of(product);
        return AdvancedRecipes.work(m, "arsmekanism:" + (draining ? "drain_potion" : "fill_potion"), new int[]{1}, outputs, () -> outputs,
              0, 0, 0, 0, external, () -> { if (draining) jar.add(contents, amount); else jar.remove(amount); });
    }

    private PotionProcessing() { }
}
