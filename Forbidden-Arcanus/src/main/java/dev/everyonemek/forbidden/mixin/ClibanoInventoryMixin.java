package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoInputSlot;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.item.crafting.ClibanoRecipe;
import dev.everyonemek.forbidden.Binding;
import dev.everyonemek.forbidden.NativeInventory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClibanoMainBlockEntity.class)
public abstract class ClibanoInventoryMixin {
    @Inject(method = "canSmelt", at = @At("RETURN"), cancellable = true)
    private void forbiddenmekanism$reserveOutput(RecipeHolder<ClibanoRecipe> recipe, ClibanoInputSlot slot, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        var main = (ClibanoMainBlockEntity) (Object) this;
        var controller = Binding.claimedController(main);
        if (controller != null && !NativeInventory.canReceive(controller, main, recipe.value().getResultItem(main.getLevel().registryAccess()))) cir.setReturnValue(false);
    }
    @Inject(method = "finishRecipe", at = @At("RETURN"))
    private void forbiddenmekanism$deliverOutput(RecipeHolder<ClibanoRecipe> recipe, ClibanoInputSlot slot, CallbackInfo ci) {
        var main = (ClibanoMainBlockEntity) (Object) this;
        var controller = Binding.claimedController(main);
        if (controller != null) NativeInventory.collectOutputs(controller, main);
    }
    @Inject(method = "consumeSoul", at = @At("RETURN"))
    private void forbiddenmekanism$rememberSoulDuration(Level level, CallbackInfo ci) {
        var main = (ClibanoMainBlockEntity) (Object) this;
        if (Binding.hasClaim(main)) {
            main.getPersistentData().putInt(NativeInventory.SOUL_DURATION, ((ClibanoAccess) main).forbiddenmekanism$data().get(0));
            main.setChanged();
        }
    }
}
