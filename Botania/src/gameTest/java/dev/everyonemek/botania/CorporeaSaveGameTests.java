package dev.everyonemek.botania;

import dev.everyonemek.botania.corporea.CorporeaFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;

import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class CorporeaSaveGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bridgeFlowerWorldAndItemStateRemainSafeWithOrWithoutAe2(GameTestHelper h) {
        var owner = player(h, "bridge-save");
        var flower = (CorporeaFlower) plant(h, owner, new BlockPos(15, 2, 15), Content.CORPOREA.get()); flower.mode = 2; stop(flower);
        flower.filter.mode = 1; flower.filter.exact = true; flower.autocraft = true;
        var sample = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD);
        sample.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Filter sample"));
        flower.filter.samples[0] = sample;
        flower.filter.samples[62] = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
        var registry = h.getLevel().registryAccess();
        var legacy = new CompoundTag(); flower.filter.save(legacy, registry);
        var oldSamples = legacy.getList("filter_samples", net.minecraft.nbt.Tag.TAG_COMPOUND);
        while (oldSamples.size() > 9) oldSamples.removeLast();
        var oldFilter = new dev.everyonemek.botania.corporea.BridgeFilter(); oldFilter.load(legacy, registry);
        check(oldFilter.allows(sample) && oldFilter.samples[62].isEmpty(), "Old nine-slot filter did not expand safely");
        var cell = new net.minecraft.world.item.ItemStack(Content.MANA_CELL.get()); ManaStorageItem.store(cell, 765432);
        var savedCell = net.minecraft.world.item.ItemStack.parseOptional(registry, (net.minecraft.nbt.CompoundTag) cell.saveOptional(registry));
        check(ManaStorageItem.stored(savedCell) == 765432, "Mana cell lost its addon-owned data without AE2"); var saved = flower.saveWithFullMetadata(registry);
        var restored = (CorporeaFlower) BlockEntity.loadStatic(flower.getBlockPos(), flower.getBlockState(), saved, registry);
        check(restored != null && restored.mode == 2 && owner.getUUID().equals(Flowers.owner(restored)) && !Flowers.enabled(restored), "World save lost flower settings or owner");
        check(restored.filter.mode == 1 && restored.autocraft && restored.filter.allows(sample)
              && !restored.filter.allows(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)), "World lost component-aware filter");
        var drop = breakAndPick(h, flower.getBlockPos(), Content.CORPOREA.get());
        check(!drop.get(Content.STATE.get()).copyTag().contains("me_connection"), "Flower item copied an old ME node identity");
        placeItem(owner, flower.getBlockPos(), drop);
        var placed = (CorporeaFlower) h.getLevel().getBlockEntity(flower.getBlockPos());
        check(placed != null && placed.mode == 2 && !Flowers.enabled(placed), "Dropped flower did not restore settings");
        check(placed.filter.mode == 1 && placed.autocraft && placed.filter.allows(sample)
              && placed.filter.samples[62].is(net.minecraft.world.item.Items.DIAMOND), "Drop lost filter or crafting toggle");
        if (!net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            try { Class.forName("appeng.api.networking.GridHelper", false, CorporeaSaveGameTests.class.getClassLoader()); throw new GameTestAssertException("AE2 classes leaked into optional-off runtime"); }
            catch (ClassNotFoundException expected) { }
            var state = new CompoundTag(); placed.backend().describe(state); check(state.getString("status").equals("missing_ae2"), "Missing AE2 did not use the safe disabled backend");
        }
        stop(placed); h.succeed();
    }
}
