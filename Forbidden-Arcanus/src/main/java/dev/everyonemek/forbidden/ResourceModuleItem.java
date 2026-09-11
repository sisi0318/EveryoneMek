package dev.everyonemek.forbidden;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class ResourceModuleItem extends Item {
    private final String id;
    public ResourceModuleItem(String id) { super(new Properties().stacksTo(8)); this.id = id; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("description.forbiddenmekanism." + id).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
