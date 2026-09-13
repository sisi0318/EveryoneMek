package dev.everyonemek.botania.compat.ae2;

import dev.everyonemek.botania.MechanicalSparkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.*;

/** Placement support only. An ME host never becomes a source or sink of mana. */
record SparkMeAnchor(BlockEntity tile) implements ManaSparkAttachable, ManaReceiver {
    static boolean supports(BlockEntity tile) {
        if (tile == null || tile.isRemoved() || tile.getLevel() == null) return false;
        var level = tile.getLevel(); var pos = tile.getBlockPos();
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) == tile
              && appeng.api.networking.GridHelper.getNodeHost(level, pos) != null;
    }
    static SparkMeAnchor at(BlockEntity tile) { return supports(tile) ? new SparkMeAnchor(tile) : null; }
    @Override public boolean canAttachSpark(ItemStack stack) { return supports(tile) && stack.getItem() instanceof MechanicalSparkItem; }
    @Override public boolean canHaveAugment(ItemStack stack) { return stack.is(vazkii.botania.common.lib.BotaniaTags.Items.MANA_SPARK_AUGMENTS); }
    @Override public void attachSpark(ManaSpark spark) { }
    @Override public int getAvailableSpaceForMana() { return 0; }
    @Override public boolean areIncomingTransfersDone() { return true; }
    @Override public int getCurrentMana() { return 0; }
    @Override public boolean isFull() { return true; }
    @Override public void receiveMana(int amount) { }
    @Override public boolean canReceiveManaFromBursts() { return false; }
    @Override public Level getManaReceiverLevel() { return tile.getLevel(); }
    @Override public BlockPos getManaReceiverPos() { return tile.getBlockPos(); }
}
