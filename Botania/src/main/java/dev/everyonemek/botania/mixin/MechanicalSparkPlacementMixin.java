package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.MechanicalSparkItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import vazkii.botania.common.entity.ManaSparkEntity;
import vazkii.botania.common.item.ManaSparkItem;

@Mixin(value = ManaSparkItem.class, remap = false)
public abstract class MechanicalSparkPlacementMixin {
    @Redirect(method = "attachSpark", at = @At(value = "NEW", target = "vazkii/botania/common/entity/ManaSparkEntity"))
    private static ManaSparkEntity botanicalmekanism$mechanical(Level level) { return MechanicalSparkItem.create(level); }
}
