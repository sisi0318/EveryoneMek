package dev.everyonemek.oritech;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** A permanent capacity upgrade; the original host and installed addons stay in place. */
public final class ProcessorUpgrade extends Item {
    public final int tier;
    public ProcessorUpgrade(int tier){super(new Properties().rarity(tier>=6?Rarity.EPIC:tier>=4?Rarity.RARE:Rarity.UNCOMMON));this.tier=tier;}
    public boolean apply(Processor processor,Player player,ItemStack stack){
        if(player.level()!=processor.getLevel()||player.level().isClientSide||processor.isRemoved()||stack.getItem()!=this||stack.isEmpty()
            ||!player.mayBuild()||!player.level().hasChunkAt(processor.getBlockPos())||player.level().getBlockEntity(processor.getBlockPos())!=processor
            ||player.distanceToSqr(Vec3.atCenterOf(processor.getBlockPos()))>64)return false;
        if(tier<=processor.tier){player.displayClientMessage(Component.translatable("message.oritechmekanism.upgrade_lower"),true);return false;}
        processor.upgradeTier(tier);
        if(!player.getAbilities().instabuild)stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.oritechmekanism.upgraded",tier,processor.addonSlots()),true);
        return true;
    }
    public static ItemInteractionResult interact(ItemStack stack,Player player,Processor processor){
        if(!(stack.getItem() instanceof ProcessorUpgrade upgrade))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.level().isClientSide)upgrade.apply(processor,player,stack);
        return ItemInteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable("tooltip.oritechmekanism.capacity_upgrade",tier*Processor.ADDONS_PER_TIER).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.oritechmekanism.upgrade_menu").withStyle(ChatFormatting.DARK_GRAY));
    }
}
