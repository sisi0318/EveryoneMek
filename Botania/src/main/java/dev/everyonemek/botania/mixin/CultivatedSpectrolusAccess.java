package dev.everyonemek.botania.mixin;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
import vazkii.botania.common.block.block_entity.flower.generating.SpectrolusBlockEntity;
@Mixin(SpectrolusBlockEntity.class)
public interface CultivatedSpectrolusAccess {
    @Accessor("WOOL_GEN") static int botanicalmekanism$mana() { throw new AssertionError(); }
    @Invoker("getDefaultColorList") static List<DyeColor> botanicalmekanism$colors(ServerLevel level) { throw new AssertionError(); }
}
