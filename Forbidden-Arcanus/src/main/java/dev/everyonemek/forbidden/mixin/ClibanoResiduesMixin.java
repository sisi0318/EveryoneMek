package dev.everyonemek.forbidden.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ResiduesStorage;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.residue.ResidueType;
import dev.everyonemek.forbidden.Binding;
import dev.everyonemek.forbidden.Controller;
import dev.everyonemek.forbidden.NativeInventory;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResiduesStorage.class)
public abstract class ClibanoResiduesMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
          target = "Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;forEach(Ljava/util/function/BiConsumer;)V"))
    private void forbiddenmekanism$deliverEachType(Object2IntOpenHashMap<Holder<ResidueType>> map,
          BiConsumer<Holder<ResidueType>, Integer> combine, Operation<Void> original, ClibanoMainBlockEntity main) {
        var controller = Binding.claimedController(main);
        if (controller == null) { original.call(map, combine); return; }
        BiConsumer<Holder<ResidueType>, Integer> guarded = (type, amount) -> {
            var info = type.value().combineInfo();
            if (amount >= info.requiredAmount() && !NativeInventory.canReceive(controller, main, info.result())) {
                controller.status = Controller.OUTPUT_FULL;
                return;
            }
            combine.accept(type, amount);
            NativeInventory.collectOutputs(controller, main);
        };
        original.call(map, guarded);
    }
}
