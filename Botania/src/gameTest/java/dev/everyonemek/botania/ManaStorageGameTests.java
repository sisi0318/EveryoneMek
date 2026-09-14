package dev.everyonemek.botania;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import static dev.everyonemek.botania.BotanicalGameTests.check;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ManaStorageGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void oldExpandedCellsKeepTheirContentsWithoutAeInstalled(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        for (var tier : ManaCellTier.values()) {
            long oldAmount = tier.kilobytes * 1024L * 8000;
            var stack = new ItemStack(Content.MANA_CELLS.get(tier).get());
            check(ManaStorageItem.capacity(stack) == tier.kilobytes * 500_000L, "New cell did not use the standard capacity");
            stack.set(Content.STORED_MANA, oldAmount);
            var loaded = ItemStack.parseOptional(registries, (CompoundTag) stack.saveOptional(registries));
            check(ManaStorageItem.stored(loaded) == oldAmount && ManaStorageItem.capacity(loaded) == oldAmount,
                  "Capacity migration hid stored mana for " + tier);
            var view = new ManaStorageItem.ManaView(loaded);
            check(view.getMaxMana() - view.getMana() == 0, "Overfull cell advertises insert space");
            view.addMana(100); check(ManaStorageItem.stored(loaded) == oldAmount, "Pool insertion erased overfull mana");
            view.addMana(-12345);
            var restored = ItemStack.parseOptional(registries, (CompoundTag) loaded.saveOptional(registries));
            check(ManaStorageItem.stored(restored) == oldAmount - 12345, "Old mana was not withdrawable and persistent");
            new ManaStorageItem.ManaView(restored).addMana((int) -(oldAmount - 12345 - tier.capacity + 1));
            var drained = ItemStack.parseOptional(registries, (CompoundTag) restored.saveOptional(registries));
            check(ManaStorageItem.stored(drained) == tier.capacity - 1 && ManaStorageItem.capacity(drained) == oldAmount,
                  "Draining below the standard erased legacy capacity");
        }
        var wisp = ManaStorageItem.recovery(Long.MAX_VALUE);
        new ManaStorageItem.ManaView(wisp).addMana(1);
        check(ManaStorageItem.stored(wisp) == Long.MAX_VALUE, "Read-only recovery wisp overflowed");
        h.succeed();
    }
}
