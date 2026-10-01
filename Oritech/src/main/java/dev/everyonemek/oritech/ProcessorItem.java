package dev.everyonemek.oritech;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class ProcessorItem extends BlockItem {
    public ProcessorItem(){super(Content.BLOCK.get(),new Properties().stacksTo(1));}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        var data=stack.get(Content.DATA.get());int tier=data==null?1:Math.clamp(data.getInt("tier"),1,Processor.MAX_TIER);
        tooltip.add(Component.translatable("tooltip.oritechmekanism.processor_tier",tier,tier*Processor.ADDONS_PER_TIER).withStyle(ChatFormatting.GRAY));
    }
}
