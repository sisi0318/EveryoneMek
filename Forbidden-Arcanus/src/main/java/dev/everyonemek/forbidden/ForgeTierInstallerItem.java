package dev.everyonemek.forbidden;

import java.util.List;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class ForgeTierInstallerItem extends Item {
    private final int tier;
    public ForgeTierInstallerItem(int tier) { super(new Properties()); this.tier = tier; }
    public ItemInteractionResult install(Controller machine, Player player, ItemStack stack) {
        if (machine.forge == null || stack.isEmpty() || stack.getItem() != this) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.mayBuild() || player.distanceToSqr(machine.getBlockPos().getCenter()) > 64
              || !IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), machine.getBlockPos(), machine)) return ItemInteractionResult.FAIL;
        if (player.level().isClientSide) return ItemInteractionResult.SUCCESS;
        if (!machine.forge.upgradeTo(tier)) {
            player.displayClientMessage(Component.translatable("gui.forbiddenmekanism.installer_requires", tier - 1), true);
            return ItemInteractionResult.FAIL;
        }
        if (!player.isCreative()) stack.shrink(1);
        player.displayClientMessage(Component.translatable("gui.forbiddenmekanism.installer_done", tier), true);
        return ItemInteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("description.forbiddenmekanism.forge_tier_installer", tier - 1, tier)
              .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
