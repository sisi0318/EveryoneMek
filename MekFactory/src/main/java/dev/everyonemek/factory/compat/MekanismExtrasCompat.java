package dev.everyonemek.factory.compat;

import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeTier;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionCell;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionProvider;
import dev.everyonemek.factory.Controller;
import dev.everyonemek.factory.Profiles;
import java.util.List;
import mekanism.api.Upgrade;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Contract checked against the published Extras 1.4.1 JAR, not just the porting branch. */
public final class MekanismExtrasCompat {
    public static void registerFactories() {
        for (var block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("mekanism_extras")) continue;
            var tier = Attribute.get(block, ExtraAttributeTier.class);
            var type = Attribute.get(block, AttributeFactoryType.class);
            if (tier != null && tier.tier() instanceof ExtraFactoryTier && type != null)
                Profiles.registerFactoryVariant(block, type.getFactoryType());
        }
    }
    public static int factoryLines(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return 0;
        var tier = Attribute.get(item.getBlock(), ExtraAttributeTier.class);
        return tier != null && tier.tier() instanceof ExtraFactoryTier factory ? factory.processes : 0;
    }
    public static int stackOperations(Controller c) {
        return factoryLines(c.template.getStack()) > 0 ? 1 << Profiles.upgrades(c, ExtraUpgrade.STACK) : 1;
    }
    public static boolean creative(Controller c) {
        return factoryLines(c.template.getStack()) > 0 && Profiles.upgrades(c, ExtraUpgrade.CREATIVE) > 0;
    }
    public static Upgrade[] upgrades() { return new Upgrade[]{Upgrade.SPEED, Upgrade.ENERGY, Upgrade.CHEMICAL, ExtraUpgrade.STACK, ExtraUpgrade.CREATIVE}; }
    public static IEnergyContainer cell(BlockEntity tile) {
        return tile instanceof TileEntityExtraInductionCell cell ? cell.getEnergyContainer() : null;
    }
    public static long providerOutput(BlockEntity tile) {
        return tile instanceof TileEntityExtraInductionProvider provider ? provider.tier.getOutput() : -1;
    }
    public static List<Block> inductionBlocks(boolean cell) {
        return cell ? List.of(ExtraBlocks.ABSOLUTE_INDUCTION_CELL.get(), ExtraBlocks.SUPREME_INDUCTION_CELL.get(), ExtraBlocks.COSMIC_INDUCTION_CELL.get(), ExtraBlocks.INFINITE_INDUCTION_CELL.get())
              : List.of(ExtraBlocks.ABSOLUTE_INDUCTION_PROVIDER.get(), ExtraBlocks.SUPREME_INDUCTION_PROVIDER.get(), ExtraBlocks.COSMIC_INDUCTION_PROVIDER.get(), ExtraBlocks.INFINITE_INDUCTION_PROVIDER.get());
    }
    private MekanismExtrasCompat() { }
}
