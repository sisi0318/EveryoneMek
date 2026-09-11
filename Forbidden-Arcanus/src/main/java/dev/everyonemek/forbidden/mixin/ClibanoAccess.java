package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import net.minecraft.world.inventory.ContainerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClibanoMainBlockEntity.class)
public interface ClibanoAccess {
    @Accessor("frontDirection") net.minecraft.core.Direction forbiddenmekanism$front();
    @Accessor("containerData") ContainerData forbiddenmekanism$data();
    @Accessor("quickCheck") ClibanoMainBlockEntity.CachedRecipeCheck forbiddenmekanism$recipes();
}
