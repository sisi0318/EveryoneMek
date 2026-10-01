package dev.everyonemek.oritech;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Installed through the same real-item inventory as native addons. */
public final class FluidCapacityAddon extends Item {
    public static final int LIMIT=8;
    public FluidCapacityAddon(){super(new Properties().rarity(Rarity.UNCOMMON));}
    public static long capacity(int tank,int count){return (tank<2?8000L:4000L)<<Math.clamp(count,0,LIMIT);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable("tooltip.oritechmekanism.fluid_capacity").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.oritechmekanism.fluid_capacity_limit",LIMIT).withStyle(ChatFormatting.DARK_GRAY));
    }
}
