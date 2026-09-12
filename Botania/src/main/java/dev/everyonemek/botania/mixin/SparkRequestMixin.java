package dev.everyonemek.botania.mixin;

import java.util.List;
import dev.everyonemek.botania.SparkExpansion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import vazkii.botania.api.mana.spark.*;

@Mixin(value = ManaSparkHelper.class, remap = false)
public abstract class SparkRequestMixin {
    @Redirect(method = "registerTransferFromSparksAround", at = @At(value = "INVOKE", target =
          "Lvazkii/botania/api/mana/spark/ManaSparkHelper;getSparksAround(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/DyeColor;)Ljava/util/List;"))
    private static List<ManaSpark> extendedRequest(Level searchLevel, double x, double y, double z, DyeColor color, ManaSpark spark, Level level, BlockPos position) {
        return SparkExpansion.nearby(spark, searchLevel, x, y, z, color);
    }
}
