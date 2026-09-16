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
        return context.entity() instanceof Player && context.identifier().equals(SLOT) && !context.cosmetic()
              && !stack.has(CoreContent.WARD_SEAL);
    }
    @Override public boolean canUnequip(SlotContext context, ItemStack stack) { return !WardCustody.locked(context); }
    @Override public top.theillusivec4.curios.api.type.capability.ICurio.DropRule getDropRule(SlotContext context,
          net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit, ItemStack stack) {
        return top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_KEEP;
    }
    @Override public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level,
          net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        // Sealed copies cannot leave custody. A legitimate menu extraction removes this marker first.
        if (!level.isClientSide && slot >= 0 && stack.has(CoreContent.WARD_SEAL) && entity instanceof Player p
              && slot < p.getInventory().getContainerSize() && p.getInventory().getItem(slot) == stack)
            p.getInventory().setItem(slot, ItemStack.EMPTY);
    }
    @Override public boolean onEntityItemUpdate(ItemStack stack, net.minecraft.world.entity.item.ItemEntity entity) {
        if (!entity.level().isClientSide && stack.has(CoreContent.WARD_SEAL)) { entity.discard(); return true; }
        return false;
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> text, TooltipFlag flag) {
        text.add(CoreContent.text("ward.lore").withStyle(ChatFormatting.GRAY));
        text.add(CoreContent.text("ward.equip").withStyle(ChatFormatting.GRAY));
        text.add(CoreContent.text("ward.details_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
