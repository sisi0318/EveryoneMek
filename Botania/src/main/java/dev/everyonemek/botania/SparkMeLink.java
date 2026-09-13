package dev.everyonemek.botania;

import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;

/** Optional AE connection lifecycle, kept out of common entity class dependencies. */
public interface SparkMeLink {
    int OFF = 0, NO_AE = 1, NO_ANCHOR = 2, NO_MASTER = 3, CONFLICT = 4, NO_POWER = 5,
          CONNECTING = 6, ONLINE = 7, CONTROLLERS = 8, OUT_OF_RANGE = 9, TOO_MANY = 10, WIRED = 11, INVALID_CONTROLLER = 12;
    record Stats(int capacity, int used, int links, int state, int power) { }
    SparkMeLink NONE = new SparkMeLink() { };
    static Function<MechanicalSparkEntity, SparkMeLink> factory() { return Holder.factory; }
    final class Holder {
        public static Function<MechanicalSparkEntity, SparkMeLink> factory = spark -> NONE;
        public static java.util.function.Predicate<net.minecraft.world.level.block.entity.BlockEntity> anchor = tile -> false;
        private Holder() { }
    }
    default void tick() { }
    default void invalidate() { }
    default void remove() { }
    default void load(CompoundTag tag) { }
    default CompoundTag save() { return new CompoundTag(); }
    default Stats stats() { return new Stats(0, 0, 0, NO_AE, 0); }
}
