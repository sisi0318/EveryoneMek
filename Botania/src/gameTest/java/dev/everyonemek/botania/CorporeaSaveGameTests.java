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
        var registry = h.getLevel().registryAccess(); var saved = flower.saveWithFullMetadata(registry);
        var restored = (CorporeaFlower) BlockEntity.loadStatic(flower.getBlockPos(), flower.getBlockState(), saved, registry);
        check(restored != null && restored.mode == 2 && owner.getUUID().equals(Flowers.owner(restored)) && !Flowers.enabled(restored), "World save lost flower settings or owner");
        var drop = breakAndPick(h, flower.getBlockPos(), Content.CORPOREA.get());
        check(!drop.get(Content.STATE.get()).copyTag().contains("me_connection"), "Flower item copied an old ME node identity");
        placeItem(owner, flower.getBlockPos(), drop);
        var placed = (CorporeaFlower) h.getLevel().getBlockEntity(flower.getBlockPos());
        check(placed != null && placed.mode == 2 && !Flowers.enabled(placed), "Dropped flower did not restore settings");
        if (!net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            try { Class.forName("appeng.api.networking.GridHelper", false, CorporeaSaveGameTests.class.getClassLoader()); throw new GameTestAssertException("AE2 classes leaked into optional-off runtime"); }
            catch (ClassNotFoundException expected) { }
            var state = new CompoundTag(); placed.backend().describe(state); check(state.getString("status").equals("missing_ae2"), "Missing AE2 did not use the safe disabled backend");
        }
        stop(placed); h.succeed();
    }
}
