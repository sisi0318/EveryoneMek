package dev.everyonemek.natures;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

public final class SimulationModuleItem extends Item {
    public SimulationModuleItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.naturesmekanism.simulation_module"));
        tooltip.add(Component.translatable("tooltip.naturesmekanism.simulation_module.install"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        if (player == null || !player.isShiftKeyDown()
              || !(level.getBlockEntity(context.getClickedPos()) instanceof AuraMachine machine)
              || machine.kind() != MachineKind.AURA_BOTTLER) return InteractionResult.PASS;
        if (!IBlockSecurityUtils.INSTANCE.canAccess(player, level, context.getClickedPos(), machine)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            ItemStack held = context.getItemInHand();
            ItemStack remainder = machine.simulationModuleSlot().insertItem(held.copyWithCount(1), Action.EXECUTE, AutomationType.MANUAL);
            if (remainder.isEmpty() && !player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
