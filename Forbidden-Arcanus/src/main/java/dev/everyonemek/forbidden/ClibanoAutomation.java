package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.*;
import com.stal111.forbidden_arcanus.common.item.crafting.ClibanoRecipeInput;
import dev.everyonemek.forbidden.mixin.ClibanoAccess;
import java.util.List;
import mekanism.api.Action;
import net.minecraft.world.item.ItemStack;

public final class ClibanoAutomation {
    public static boolean tick(Controller controller, ClibanoMainBlockEntity clibano) {
        boolean changed = false;
        for (int slot = 5; slot <= 6; slot++) {
            ItemStack output = clibano.getStack(slot);
            if (output.isEmpty()) continue;
            if (controller.storeOutput(output)) { clibano.setStack(slot, ItemStack.EMPTY); changed = true; }
        }
        var data = ((ClibanoAccess) clibano).forbiddenmekanism$data();
        var cache = ((ClibanoAccess) clibano).forbiddenmekanism$recipes();
        controller.status = clibano.getStack(3).isEmpty() && clibano.getStack(4).isEmpty() ? Controller.NEED_MATERIALS : Controller.RUNNING;
        if (!clibano.getStack(5).isEmpty() && !clibano.getStack(6).isEmpty()) controller.status = Controller.OUTPUT_FULL;
        int fire = data.get(0) > 0 ? data.get(7) : ClibanoFireType.fromItem(clibano.getStack(1)).ordinal();
        if (clibano.getStack(3).isEmpty() && clibano.getStack(4).isEmpty()) {
            for (var holder : clibano.getLevel().getRecipeManager().getAllRecipesFor(ClibanoMainBlockEntity.RECIPE_TYPE)) {
                var recipe = holder.value();
                if (!recipe.isDoubleRecipe() || !controller.recipeLock.isEmpty() && !controller.recipeLock.equals(holder.id().toString())) continue;
                int[] allocation = Recipes.allocate(recipe.getIngredients(), controller.stock.stream().map(s -> s.getStack().copy()).toList());
                if (allocation == null) continue;
                ItemStack first = controller.stock.get(allocation[0]).getStack().copyWithCount(1);
                ItemStack second = controller.stock.get(allocation[1]).getStack().copyWithCount(1);
                var actual = cache.getAlloyRecipe(new ClibanoRecipeInput(first, second), clibano.getLevel()).orElse(null);
                if (actual == null || !actual.id().equals(holder.id())) { controller.status = Controller.RECIPE_CONFLICT; continue; }
                if (recipe.requiredFireType().ordinal() > fire) { controller.status = Controller.NEED_SOUL; continue; }
                if (!controller.canStore(recipe.result())) { controller.status = Controller.OUTPUT_FULL; continue; }
                controller.stock.get(allocation[0]).shrinkStack(1, Action.EXECUTE);
                controller.stock.get(allocation[1]).shrinkStack(1, Action.EXECUTE);
                clibano.setStack(3, first); clibano.setStack(4, second);
                controller.selectedRecipe = holder.id().toString(); controller.status = Controller.RUNNING;
                return true;
            }
        }
        for (int slot = 3; slot <= 4; slot++) {
            if (!clibano.getStack(slot).isEmpty()) continue;
            for (var supply : controller.stock) {
                if (supply.isEmpty()) continue;
                ItemStack offered = supply.getStack().copyWithCount(1);
                ItemStack other = clibano.getStack(slot == 3 ? 4 : 3);
                var combined = slot == 3 ? new ClibanoRecipeInput(offered, other) : new ClibanoRecipeInput(other, offered);
                // Adding the second independent input must not unexpectedly turn the running first input into an alloy.
                if (cache.getAlloyRecipe(combined, clibano.getLevel()).isPresent()) continue;
                var input = slot == 3 ? new ClibanoRecipeInput(offered, ItemStack.EMPTY) : new ClibanoRecipeInput(ItemStack.EMPTY, offered);
                var actual = cache.getRecipeFor(input, clibano.getLevel()).orElse(null);
                if (actual == null || !controller.recipeLock.isEmpty() && !controller.recipeLock.equals(actual.id().toString())) continue;
                if (actual.value().requiredFireType().ordinal() > fire) { controller.status = Controller.NEED_SOUL; continue; }
                if (!controller.canStore(actual.value().result())) { controller.status = Controller.OUTPUT_FULL; continue; }
                supply.shrinkStack(1, Action.EXECUTE); clibano.setStack(slot, offered);
                controller.selectedRecipe = actual.id().toString(); controller.status = Controller.RUNNING; changed = true;
                break;
            }
        }
        return changed;
    }
    private ClibanoAutomation() { }
}
