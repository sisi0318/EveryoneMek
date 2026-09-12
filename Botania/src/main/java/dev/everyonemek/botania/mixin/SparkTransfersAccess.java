package dev.everyonemek.botania.mixin;

import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.common.entity.ManaSparkEntity;

@Mixin(value = ManaSparkEntity.class, remap = false)
public interface SparkTransfersAccess {
    @Accessor("inboundTransfers") ArrayList<ManaSpark> botanicalmekanism$inbound();
}
