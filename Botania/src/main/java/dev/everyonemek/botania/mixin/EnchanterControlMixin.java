package dev.everyonemek.botania.mixin;

import java.util.List;
import dev.everyonemek.botania.NativeControllers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.block_entity.ManaEnchanterBlockEntity;

@Mixin(value = ManaEnchanterBlockEntity.class, remap = false)
public abstract class EnchanterControlMixin {
    @Redirect(method = {"onUsedByWand", "gatherEnchants"}, at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<ItemEntity> bookView(Level level, Class<ItemEntity> type, AABB area) {
        return NativeControllers.books((ManaEnchanterBlockEntity) (Object) this, level.getEntitiesOfClass(type, area));
    }
    @Inject(method = "commonTick", at = @At("HEAD"), cancellable = true)
    private static void preserveControlledWork(Level level, BlockPos pos, BlockState state, ManaEnchanterBlockEntity tile, CallbackInfo callback) {
        if (!level.isClientSide) {
            var controllers = NativeControllers.attached(tile);
            if (!controllers.isEmpty() && (controllers.size() != 1 || !controllers.getFirst().canFunction()
                  || !NativeControllers.validEnchanter(tile))) callback.cancel();
        }
    }
}
