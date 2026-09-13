package dev.everyonemek.botania.compat.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.ingredients.IIngredientType;

/** A recipe-viewer amount, never an item or a source of mana. */
public record ManaIngredient(long amount) {
    public static final IIngredientType<ManaIngredient> TYPE = () -> ManaIngredient.class;
    public static final Codec<ManaIngredient> CODEC = Codec.LONG.comapFlatMap(amount -> amount > 0
          ? com.mojang.serialization.DataResult.success(new ManaIngredient(amount))
          : com.mojang.serialization.DataResult.error(() -> "Mana must be positive"), ManaIngredient::amount);
    public ManaIngredient { if (amount <= 0) throw new IllegalArgumentException("Mana ingredient amount must be positive"); }
}
