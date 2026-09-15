package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.RecipeHooks;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.recipe.lookup.monitor.RecipeCacheLookupMonitor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(value = RecipeCacheLookupMonitor.class, remap = false)
public abstract class RecipeMonitorMixin {
    @Shadow @Final private IRecipeLookupHandler<?> handler;
    @Shadow @Final protected int cacheIndex;
    @WrapOperation(method = "updateAndProcess()Z", at = @At(value = "INVOKE", target = "Lmekanism/api/recipes/cache/CachedRecipe;process()V"))
    private void overload$process(CachedRecipe<?> recipe, Operation<Void> original) {
        var context = handler instanceof BlockEntity tile ? RecipeHooks.begin(tile, recipe, cacheIndex) : null;
        try { original.call(recipe); } finally { RecipeHooks.end(context); }
    }
}
