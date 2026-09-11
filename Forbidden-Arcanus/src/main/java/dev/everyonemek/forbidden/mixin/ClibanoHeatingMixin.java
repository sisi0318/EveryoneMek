package dev.everyonemek.forbidden.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.logic.ClibanoSmeltLogic;
import dev.everyonemek.forbidden.Binding;
import dev.everyonemek.forbidden.ClibanoHeating;
import dev.everyonemek.forbidden.Controller;
import dev.everyonemek.forbidden.NativeInventory;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClibanoMainBlockEntity.class)
public abstract class ClibanoHeatingMixin implements ClibanoHeating {
    @Shadow private int burnTime;
    @Shadow private int burnDuration;
    @Shadow private boolean wasLit;
    @Shadow private ClibanoSmeltLogic logic;
    @Shadow private void updateAppearance(Level level) { throw new AssertionError(); }
    @Unique private boolean forbiddenmekanism$controlled, forbiddenmekanism$heating, forbiddenmekanism$inTick;
    @Unique private int forbiddenmekanism$savedBurn, forbiddenmekanism$savedDuration;

    @Override public void forbiddenmekanism$beginHeatingTick() {
        var main = (ClibanoMainBlockEntity) (Object) this;
        boolean previouslyControlled = forbiddenmekanism$controlled;
        forbiddenmekanism$controlled = Binding.hasClaim(main);
        forbiddenmekanism$heating = false;
        if (!forbiddenmekanism$controlled) {
            if (previouslyControlled) updateAppearance(main.getLevel());
            return;
        }
        forbiddenmekanism$savedBurn = burnTime; forbiddenmekanism$savedDuration = burnDuration;
        forbiddenmekanism$inTick = true;
        var controller = Binding.claimedController(main);
        if (controller != null) NativeInventory.collectOutputs(controller, main);
        if (controller != null && controller.enabled && controller.canFunction() && logic.canSmelt()) {
            long cost = ClibanoHeating.energyPerTick(controller);
            if (controller.energy().extract(cost, Action.SIMULATE, AutomationType.INTERNAL) == cost) {
                controller.energy().extract(cost, Action.EXECUTE, AutomationType.INTERNAL);
                controller.markForSave(); main.setChanged(); forbiddenmekanism$heating = true;
            } else controller.status = Controller.NEED_ENERGY;
        }
        // Only this native tick sees electric heat. Previously purchased combustion time remains intact.
        burnTime = forbiddenmekanism$heating ? 2 : 0;
        burnDuration = burnTime;
        if (!previouslyControlled || wasLit != forbiddenmekanism$heating) updateAppearance(main.getLevel());
    }
    @Override public void forbiddenmekanism$endHeatingTick() {
        if (forbiddenmekanism$inTick) {
            burnTime = forbiddenmekanism$savedBurn; burnDuration = forbiddenmekanism$savedDuration;
            forbiddenmekanism$inTick = false;
        }
    }
    @Override public boolean forbiddenmekanism$controlsHeat() { return forbiddenmekanism$controlled; }
    @Override public boolean forbiddenmekanism$isHeating() { return forbiddenmekanism$heating; }

    @WrapMethod(method = "serverTick")
    private static void forbiddenmekanism$restoreFuel(Level level, BlockPos pos, BlockState state, ClibanoMainBlockEntity main, Operation<Void> original) {
        try { original.call(level, pos, state, main); }
        finally { ((ClibanoHeating) main).forbiddenmekanism$endHeatingTick(); }
    }
    @Inject(method = "serverTick", at = @At(value = "INVOKE",
          target = "Lcom/stal111/forbidden_arcanus/common/block/entity/clibano/logic/ClibanoSmeltLogic;updateRecipes(Ljava/util/List;)V", shift = At.Shift.AFTER))
    private static void forbiddenmekanism$payForHeat(Level level, BlockPos pos, BlockState state, ClibanoMainBlockEntity main, CallbackInfo ci) {
        ((ClibanoHeating) main).forbiddenmekanism$beginHeatingTick();
    }
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
          target = "Lcom/stal111/forbidden_arcanus/common/block/entity/clibano/ClibanoMainBlockEntity;getStack(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack forbiddenmekanism$leaveFuelUntouched(ClibanoMainBlockEntity main, int slot, Operation<ItemStack> original) {
        return slot == 2 && ((ClibanoHeating) main).forbiddenmekanism$controlsHeat() ? ItemStack.EMPTY : original.call(main, slot);
    }
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
          target = "Lcom/stal111/forbidden_arcanus/common/block/entity/clibano/logic/ClibanoSmeltLogic;tick(Z)V"))
    private static void forbiddenmekanism$pauseWithoutPower(ClibanoSmeltLogic logic, boolean lit, Operation<Void> original) {
        if (logic.clibano instanceof ClibanoHeating heat && heat.forbiddenmekanism$controlsHeat() && !heat.forbiddenmekanism$isHeating()) return;
        original.call(logic, lit);
    }
}
