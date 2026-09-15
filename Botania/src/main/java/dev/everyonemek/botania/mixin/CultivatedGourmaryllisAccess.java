package dev.everyonemek.botania.mixin;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
import vazkii.botania.common.block.block_entity.flower.generating.GourmaryllisBlockEntity;
@Mixin(GourmaryllisBlockEntity.class)
public interface CultivatedGourmaryllisAccess {
    @Accessor("lastFoods") List<ItemStack> botanicalmekanism$foods();
    @Accessor("lastFoodCount") int botanicalmekanism$repeats();
    @Accessor("lastFoodCount") void botanicalmekanism$repeats(int count);
    @Invoker("processFood") int botanicalmekanism$process(ItemStack food);
    @Invoker("getMultiplierForStreak") double botanicalmekanism$multiplier(int streak);
    @Invoker("getFoodValue") static int botanicalmekanism$value(ItemStack food) { throw new AssertionError(); }
    @Invoker("getCooldown") static int botanicalmekanism$ticks(int nutrition) { throw new AssertionError(); }
    @Invoker("getDigestingMana") static int botanicalmekanism$mana(int nutrition, double factor) { throw new AssertionError(); }
}
