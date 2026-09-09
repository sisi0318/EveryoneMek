package dev.everyonemek.natures.mixin;

import dev.everyonemek.natures.AuraMachine;
import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.OreChamberLogic;
import mekanism.api.RelativeSide;
import mekanism.client.gui.element.button.SideDataButton;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep Mek's buttons, packets and tooltips, replacing only the chamber's neighbour preview. */
@Mixin(value = SideDataButton.class, remap = false)
public abstract class ChamberSideDataButtonMixin {
    @Shadow @Final private TileEntityMekanism tile;
    @Shadow @Final private RelativeSide slotPos;
    @Shadow @Final @Mutable private ItemStack otherBlockItem;
    @Unique private long naturesmekanism$previewTick = Long.MIN_VALUE;
    @Unique private net.minecraft.core.Direction naturesmekanism$previewFacing;

    @Inject(method = "drawBackground", at = @At("HEAD"))
    private void naturesmekanism$updatePortIcon(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!(tile instanceof AuraMachine machine) || machine.chamber() == null || machine.getLevel() == null) return;
        var level = machine.getLevel();
        if (naturesmekanism$previewTick == level.getGameTime() && naturesmekanism$previewFacing == machine.getDirection()) return;
        naturesmekanism$previewTick = level.getGameTime();
        naturesmekanism$previewFacing = machine.getDirection();
        var pos = machine.chamber().sideDisplayPosition(slotPos);
        if (!level.hasChunkAt(pos)) { otherBlockItem = ItemStack.EMPTY; return; }
        var state = level.getBlockState(pos);
        if (state.is(Content.CHAMBER_PORT.get())) {
            otherBlockItem = OreChamberLogic.portDisplayStack(state);
        } else if (state.isAir()) {
            otherBlockItem = ItemStack.EMPTY;
        } else {
            var face = slotPos.getDirection(machine.getDirection()).getOpposite();
            otherBlockItem = state.getCloneItemStack(new BlockHitResult(pos.getCenter().relative(face, .5), face, pos, false),
                  level, pos, Minecraft.getInstance().player);
        }
    }
}
