package dev.everyonemek.botania.compat.ae2;

import appeng.api.stacks.GenericStack;
import dev.everyonemek.botania.compat.jei.ManaIngredient;
import mezz.jei.api.ingredients.IIngredientType;
import tamaized.ae2jeiintegration.api.integrations.jei.*;

public final class ManaIngredientConverter implements IngredientConverter<ManaIngredient> {
    public static void register() { IngredientConverters.register(new ManaIngredientConverter()); }
    @Override public IIngredientType<ManaIngredient> getIngredientType() { return ManaIngredient.TYPE; }
    @Override public ManaIngredient getIngredientFromStack(GenericStack stack) {
        return stack.what() == ManaKey.INSTANCE ? new ManaIngredient(Math.max(1, stack.amount())) : null;
    }
    @Override public GenericStack getStackFromIngredient(ManaIngredient ingredient) { return new GenericStack(ManaKey.INSTANCE, ingredient.amount()); }
}
