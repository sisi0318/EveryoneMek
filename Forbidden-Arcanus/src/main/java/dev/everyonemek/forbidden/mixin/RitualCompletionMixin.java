package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.RitualManager;
import dev.everyonemek.forbidden.ForgeAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records completion only for batches owned by a controller; the native engine still executes the ritual. */
@Mixin(RitualManager.class)
abstract class RitualCompletionMixin {
    @Shadow private ServerLevel level;
    @Shadow private BlockPos pos;
    @Inject(method = "finishRitual", at = @At("RETURN"))
    private void finished(CallbackInfoReturnable<ItemStack> callback) { ForgeAutomation.completed(level, pos, true); }
    @Inject(method = "failRitual", at = @At("RETURN"))
    private void failed(CallbackInfoReturnable<ItemStack> callback) { ForgeAutomation.completed(level, pos, false); }
}
