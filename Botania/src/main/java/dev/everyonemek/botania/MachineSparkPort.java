package dev.everyonemek.botania;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.ManaSparkAttachable;
import vazkii.botania.api.mana.spark.ManaSparkHelper;

/** Native spark view of the machine's one Chemical tank. It is not a mana pool. */
public record MachineSparkPort(ManaMachine machine) implements ManaReceiver, ManaSparkAttachable {
    @Override public Level getManaReceiverLevel() { return machine.getLevel(); }
    @Override public BlockPos getManaReceiverPos() { return machine.getBlockPos(); }
    @Override public boolean canAttachSpark(ItemStack stack) { return Flowers.live(machine) && machine.kind().chemical; }
    @Override public boolean canHaveAugment(ItemStack augment) { return augment.is(Content.SPARK_AUGMENT.get()); }
    @Override public void attachSpark(ManaSpark spark) { ManaSparkHelper.registerTransferFromSparksAround(spark, machine.getLevel(), machine.getBlockPos()); }
    @Override public int getCurrentMana() { return Flowers.live(machine) ? (int) machine.mana().getStored() : 0; }
    @Override public int getAvailableSpaceForMana() {
        if (!Flowers.live(machine)) return 0;
        var handler = machine.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), machine.getBlockPos(), Direction.UP);
        int space = (int) machine.mana().getNeeded();
        return handler == null || space == 0 ? 0 : space - (int) handler.insertChemical(new ChemicalStack(ManaContent.MANA, space), Action.SIMULATE).getAmount();
    }
    @Override public boolean isFull() { return getAvailableSpaceForMana() == 0; }
    @Override public boolean areIncomingTransfersDone() { return isFull(); }
    @Override public boolean canReceiveManaFromBursts() { return false; }
    @Override public void receiveMana(int amount) {
        if (!Flowers.live(machine) || machine.getLevel().isClientSide) return;
        if (amount > 0) {
            var handler = machine.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), machine.getBlockPos(), Direction.UP);
            if (handler != null) handler.insertChemical(new ChemicalStack(ManaContent.MANA, amount), Action.EXECUTE);
        } else if (amount < 0) {
            // Preserve accounting even if an externally configured native spark asks to drain.
            machine.mana().extract(-(long) amount, Action.EXECUTE, AutomationType.INTERNAL);
        }
    }
}
