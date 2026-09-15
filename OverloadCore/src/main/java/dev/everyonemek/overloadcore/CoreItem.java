package dev.everyonemek.overloadcore;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.*;

public final class CoreItem extends Item implements ICurioItem {
    public CoreItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var identity=player.getItemInHand(hand).get(CoreContent.DATA.get());
        if(identity!=null&&identity.hasUUID("owner")&&!identity.getUUID("owner").equals(player.getUUID())) {
            if(!level.isClientSide)player.displayClientMessage(CoreContent.text("foreign_core"),true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (CoreBinding.bound(player)) {
            if (!level.isClientSide) CorePackets.sendStatus((net.minecraft.server.level.ServerPlayer) player, true);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        if (!CoreBinding.hasEmptySlot(player)) {
            if (!level.isClientSide) player.displayClientMessage(CoreContent.text("no_slot"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) player.displayClientMessage(CoreContent.text("hold_bind"), false);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 40; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player) CoreBinding.bind(player, stack);
        return stack;
    }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return context.entity() instanceof Player player && context.identifier().equals(CoreBinding.SLOT) && CoreBinding.matches(player, stack);
    }
    @Override public boolean canUnequip(SlotContext context, ItemStack stack) {
        return context.entity() instanceof Player player && player.isCreative();
    }
    @Override public void onUnequip(SlotContext context, ItemStack newStack, ItemStack oldStack) {
        if(context.entity() instanceof net.minecraft.server.level.ServerPlayer player && player.isCreative()
              && CoreBinding.matches(player,oldStack) && !CoreBinding.matches(player,newStack)) {
            player.getPersistentData().remove(CoreBinding.KEY);CorePackets.sendStatus(player,false);
        }
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public ICurio.DropRule getDropRule(SlotContext context, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> text, TooltipFlag flag) {
        text.add(CoreContent.text("lore")); text.add(CoreContent.text("warning")); text.add(CoreContent.text("effects", CoreConfig.RANGE.get()));
        text.add(CoreContent.text("benefits")); text.add(CoreContent.text("details_hint"));
        var data = stack.get(CoreContent.DATA.get());
        if (data != null && data.hasUUID("owner")) text.add(CoreContent.text("bound"));
    }
}
