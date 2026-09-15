package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.RecipeHooks;
import mekanism.api.recipes.outputs.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
@Mixin(value = OutputHelper.class, remap = false)
public abstract class RecipeOutputMixin {
    @ModifyReturnValue(method = {
          "getOutputHandler(Lmekanism/api/inventory/IInventorySlot;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
          "getOutputHandler(Lmekanism/api/chemical/IChemicalTank;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;"
    }, at = @At("RETURN"))
    private static <T> IOutputHandler<T> overload$output(IOutputHandler<T> original) { return RecipeHooks.output(original); }
}
