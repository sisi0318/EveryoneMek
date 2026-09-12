package dev.everyonemek.botania;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public final class PlantItem extends BlockItem {
    public PlantItem(Block block, Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getBlock().getDescriptionId() + ".description"));
        var data = stack.get(Content.STATE.get());
        if (data != null) {
            int energy = data.copyTag().getInt("fe");
            if (energy > 0) tooltip.add(Component.translatable("gui.botanicalmekanism.energy", energy, Balance.ENERGY_CAPACITY));
        }
    }
}
