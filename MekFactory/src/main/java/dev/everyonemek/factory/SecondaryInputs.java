package dev.everyonemek.factory;

import java.util.List;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache;

/** Native secondary-item conversions fill the owning input hatch, including any unused remainder. */
public final class SecondaryInputs {
    public static void convert(Controller c, Profiles.Profile profile) {
        if (profile.kind() != Profiles.Kind.METALLURGIC && profile.kind() != Profiles.Kind.PURIFYING) return;
        if (!(profile.recipes().getInputCache() instanceof InputRecipeCache.ItemChemical<?> cache)) return;
        var banks = new java.util.ArrayList<Buffers>();
        if (c.inputs.hasContents()) banks.add(c.inputs);
        for (var port : c.structure.ports) if (!port.getBlockState().getValue(PartBlock.OUTPUT)) banks.add(port.storage());
        for (var bank : banks) for (int i = 0; i < bank.itemSlots(); i++) {
            var stack = bank.item(i); if (stack.isEmpty()) continue;
            // A shared warehouse has no dedicated auxiliary slot: prefer primary ingredients when ambiguous.
            if (cache.containsInputA(c.getLevel(), stack)) continue;
            var recipe = MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findFirstRecipe(c.getLevel(), stack);
            if (recipe == null) continue;
            var input = recipe.getInput().getMatchingInstance(stack); if (input.isEmpty()) continue; var output = recipe.getOutput(input);
            if (output.isEmpty() || output.isRadioactive() || !cache.containsInputB(c.getLevel(), output)) continue;
            int count = stack.getCount() / input.getCount();
            // Batch conversion must fit in this hatch. Simulate every unit before taking any item.
            long space = 0;
            for (int tank = 0; tank < bank.chemicalTanks(); tank++) space = mekanism.api.math.MathUtils.addClamped(space,
                  bank.insertChem(tank, output.copyWithAmount(Long.MAX_VALUE), true));
            count = (int) Math.min(count, space / output.getAmount());
            if (count <= 0) continue;
            var converted = output.copyWithAmount(Math.multiplyExact(output.getAmount(), count));
            if (bank.store(List.of(), List.of(), List.of(converted), false)) bank.take(i, Math.multiplyExact(input.getCount(), count), false);
        }
    }
    private SecondaryInputs() { }
}
