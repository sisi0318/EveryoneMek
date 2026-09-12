package dev.everyonemek.botania.mixin;

import java.util.List;
import dev.everyonemek.botania.SparkExpansion;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.common.entity.ManaSparkEntity;

@Mixin(value = ManaSparkEntity.class, remap = false)
public abstract class SparkRangeMixin {
    @Redirect(method = {"updateTransfers", "notifyOthers", "interact"}, at = @At(value = "INVOKE", target =
          "Lvazkii/botania/api/mana/spark/ManaSparkHelper;getSparksAround(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/DyeColor;)Ljava/util/List;"))
    private List<ManaSpark> extendedSearch(Level level, double x, double y, double z, DyeColor color) {
        return SparkExpansion.nearby((ManaSparkEntity) (Object) this, level, x, y, z, color);
    }
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void installRange(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var result = SparkExpansion.interact((ManaSparkEntity) (Object) this, player, hand); if (result != null) callback.setReturnValue(result);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void checkRange(CallbackInfo callback) { SparkExpansion.prune((ManaSparkEntity) (Object) this); }
    @Inject(method = {"setUpgrade", "setNetwork", "remove"}, at = @At("TAIL"))
    private void refreshPeers(CallbackInfo callback) { SparkExpansion.refresh((ManaSparkEntity) (Object) this); }
}
