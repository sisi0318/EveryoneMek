package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.Flowers;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.block.block_entity.flower.functional.HopperhockBlockEntity;
import vazkii.botania.common.block.block_entity.flower.functional.RannuncarpusBlockEntity;

@Mixin(value = {HopperhockBlockEntity.class, RannuncarpusBlockEntity.class}, remap = false)
public abstract class BionicWandMixin {
    @Inject(method = "onUsedByWand", at = @At("HEAD"), cancellable = true)
    private void checkOwner(Player player, ItemStack wand, Direction side, CallbackInfoReturnable<Boolean> callback) {
        var flower = (BlockEntity) (Object) this;
        if (Flowers.isBionic(flower) && flower.getLevel() != null && !flower.getLevel().isClientSide
              && (player == null || !Flowers.owns(player, flower))) callback.setReturnValue(false);
    }
}
