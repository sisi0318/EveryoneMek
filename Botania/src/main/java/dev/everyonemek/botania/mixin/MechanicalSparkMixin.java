package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import vazkii.botania.common.entity.ManaSparkEntity;
import vazkii.botania.common.item.BotaniaItems;

@Mixin(value = ManaSparkEntity.class, remap = false)
public abstract class MechanicalSparkMixin {
    @Shadow protected abstract Item getSparkItem();
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 1000), require = 3)
    private int botanicalmekanism$sharedEfficiency(int nativeRate) { return (Object) this instanceof MechanicalSparkEntity spark ? nativeRate * MechanicalSparkNetworks.network(spark).multiplier() : nativeRate; }
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 12), require = 1)
    private double botanicalmekanism$dispersiveRange(double nativeRange) { return (Object) this instanceof MechanicalSparkEntity spark ? MechanicalSparkNetworks.range(spark) : nativeRange; }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lvazkii/botania/common/entity/ManaSparkEntity;getSparkItem()Lnet/minecraft/world/item/Item;"))
    private Item botanicalmekanism$nativeManaItemSource(ManaSparkEntity spark) { return spark instanceof MechanicalSparkEntity ? BotaniaItems.MANA_SPARK : getSparkItem(); }
    @Redirect(method = "dropAndKill", at = @At(value = "NEW", target = "net/minecraft/world/item/ItemStack"))
    private ItemStack botanicalmekanism$keepModules(ItemLike item) {
        return (Object) this instanceof MechanicalSparkEntity spark ? spark.dropStack() : new ItemStack(item);
    }
}
