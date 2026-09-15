package dev.everyonemek.botania.mixin;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
import vazkii.botania.common.block.block_entity.flower.generating.RafflowsiaBlockEntity;
@Mixin(RafflowsiaBlockEntity.class)
public interface CultivatedRafflowsiaAccess {
    @Accessor("lastFlowers") List<ResourceLocation> botanicalmekanism$flowers();
    @Accessor("lastFlowerCount") int botanicalmekanism$repeats();
    @Accessor("lastFlowerCount") void botanicalmekanism$repeats(int count);
    @Invoker("processFlower") int botanicalmekanism$process(Block flower);
    @Invoker("getValueForStreak") int botanicalmekanism$mana(int streak);
}
