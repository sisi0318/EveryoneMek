package dev.everyonemek.forbidden.mixin;

import dev.everyonemek.forbidden.ClibanoPortBlock;
import dev.everyonemek.forbidden.Controller;
import mekanism.api.RelativeSide;
import mekanism.client.gui.element.button.SideDataButton;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SideDataButton.class)
public abstract class ClibanoSideIconMixin {
    @Shadow @Final private TileEntityMekanism tile;
    @Shadow @Final private RelativeSide slotPos;
    @Shadow @Final @Mutable private ItemStack otherBlockItem;

    @Inject(method = {"drawBackground", "updateTooltip"}, at = @At("HEAD"))
    private void forbiddenmekanism$structureFace(CallbackInfo ci) {
        if (!(tile instanceof Controller controller) || !controller.binding.embedded || controller.binding.target == null) return;
        var center = controller.binding.target;
        var side = slotPos.getDirection(controller.getDirection());
        var level = controller.getLevel();
        otherBlockItem = ItemStack.EMPTY;
        // Examine the structure face, including the controller's outward face, not its adjacent inner wall.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (side.getAxis().choose(pos.getX() - center.getX(), pos.getY() - center.getY(), pos.getZ() - center.getZ())
                  != side.getAxisDirection().getStep() || !level.hasChunkAt(pos)) continue;
            var block = level.getBlockState(pos).getBlock();
            if (block instanceof ClibanoPortBlock || pos.equals(controller.getBlockPos())) {
                otherBlockItem = new ItemStack(block); return;
            }
        }
    }
}
