package dev.everyonemek.forbidden;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class GlowModuleItem extends Item {
    public GlowModuleItem() { super(new Properties().stacksTo(8)); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("description.forbiddenmekanism.glow_module").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
