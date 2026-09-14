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
            check(ManaStorageItem.capacity(stack) == oldAmount, "New cell did not receive expanded capacity");
            stack.set(Content.STORED_MANA, oldAmount);
            stack.set(Content.LEGACY_MANA_CAPACITY, oldAmount);
            var loaded = ItemStack.parseOptional(registries, (CompoundTag) stack.saveOptional(registries));
            check(ManaStorageItem.stored(loaded) == oldAmount && ManaStorageItem.capacity(loaded) == oldAmount,
                  "Capacity migration hid stored mana for " + tier);
            var view = new ManaStorageItem.ManaView(loaded);
            check(view.getMaxMana() - view.getMana() == 0, "Overfull cell advertises insert space");
            view.addMana(100); check(ManaStorageItem.stored(loaded) == oldAmount, "Pool insertion erased overfull mana");
            view.addMana(-12345);
            var restored = ItemStack.parseOptional(registries, (CompoundTag) loaded.saveOptional(registries));
            check(ManaStorageItem.stored(restored) == oldAmount - 12345, "Old mana was not withdrawable and persistent");
            long previousSmallCapacity = tier.kilobytes * 500_000L;
            new ManaStorageItem.ManaView(restored).addMana((int) -(oldAmount - 12345 - previousSmallCapacity + 1));
            var drained = ItemStack.parseOptional(registries, (CompoundTag) restored.saveOptional(registries));
            check(ManaStorageItem.stored(drained) == previousSmallCapacity - 1 && ManaStorageItem.capacity(drained) == oldAmount
                  && drained.getOrDefault(Content.LEGACY_MANA_CAPACITY, 0L) == oldAmount, "Reload lost mana, expanded capacity or previous item data");
        }
        var wisp = ManaStorageItem.recovery(Long.MAX_VALUE);
        new ManaStorageItem.ManaView(wisp).addMana(1);
        check(ManaStorageItem.stored(wisp) == Long.MAX_VALUE, "Read-only recovery wisp overflowed");
        h.succeed();
    }
}
