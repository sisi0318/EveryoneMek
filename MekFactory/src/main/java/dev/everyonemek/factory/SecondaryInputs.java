package dev.everyonemek.factory;

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
            ChemicalConversions.convert(bank,i,recipe);
        }
    }
    private SecondaryInputs() { }
}
