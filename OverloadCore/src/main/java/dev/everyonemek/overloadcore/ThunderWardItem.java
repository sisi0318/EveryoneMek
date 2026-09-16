package dev.everyonemek.overloadcore;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class ThunderWardItem extends Item implements ICurioItem {
    public static final String SLOT = "overload_ward";
    public ThunderWardItem(Properties properties) { super(properties); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return context.entity() instanceof Player && context.identifier().equals(SLOT) && !context.cosmetic();
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> text, TooltipFlag flag) {
        text.add(CoreContent.text("ward.lore").withStyle(ChatFormatting.GRAY));
        text.add(CoreContent.text("ward.equip").withStyle(ChatFormatting.GRAY));
        text.add(CoreContent.text("ward.details_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
