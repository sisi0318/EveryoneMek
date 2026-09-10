package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.ForgeDataCache;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HephaestusForgeBlockEntity.class)
public interface ForgeAccess {
    @Accessor("dataCache") ForgeDataCache forbiddenmekanism$data();
}
