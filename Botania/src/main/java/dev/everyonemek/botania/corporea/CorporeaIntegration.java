package dev.everyonemek.botania.corporea;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import dev.everyonemek.botania.Flowers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.botania.api.corporea.*;
import vazkii.botania.common.integration.corporea.CorporeaNodeDetectors;

public final class CorporeaIntegration {
    public static Function<CorporeaFlower, BridgeBackend> factory = tile -> new BridgeBackend() {
        private net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        @Override public void load(net.minecraft.nbt.CompoundTag tag) { saved = tag.copy(); }
        @Override public net.minecraft.nbt.CompoundTag save() { return saved.copy(); }
    };
    public static BiPredicate<Level, BlockPos> cableSupport = (level, pos) -> false;
    public static void register() {
        CorporeaNodeDetectors.register((level, spark) -> level.hasChunkAt(spark.getAttachPos())
              && level.getBlockEntity(spark.getAttachPos()) instanceof CorporeaFlower flower ? new Node(flower, spark) : null);
    }
    public record Node(CorporeaFlower flower, CorporeaSpark spark) implements CorporeaNode {
        @Override public Level getWorld() { return flower.getLevel(); }
        @Override public BlockPos getPos() { return flower.getBlockPos(); }
        @Override public CorporeaSpark getSpark() { return spark; }
        @Override public List<ItemStack> countItems(CorporeaRequest request) { return Flowers.live(flower) ? flower.backend().request(request, false) : List.of(); }
        @Override public List<ItemStack> extractItems(CorporeaRequest request) { return Flowers.live(flower) ? flower.backend().request(request, true) : List.of(); }
    }
    private CorporeaIntegration() { }
}
