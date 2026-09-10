package dev.everyonemek.forbidden;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class InfiniteHammerModuleItem extends Item {
    public InfiniteHammerModuleItem() { super(new Properties().stacksTo(1)); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("description.forbiddenmekanism.infinite_hammer_module").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
