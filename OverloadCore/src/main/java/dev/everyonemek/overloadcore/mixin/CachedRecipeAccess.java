package dev.everyonemek.overloadcore.mixin;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value = CachedRecipe.class, remap = false)
public interface CachedRecipeAccess {
    @Accessor("operatingTicks") int overload$ticks();
    @Accessor("errors") java.util.Set<CachedRecipe.OperationTracker.RecipeError> overload$errors();
}
