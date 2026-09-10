package dev.everyonemek.forbidden.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssencesDefinition;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.ActiveRitualData;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.RitualManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RitualManager.class)
public interface RitualAccess {
    @Invoker("canStartRitual") boolean forbiddenmekanism$canStart(Ritual ritual, EssencesDefinition essences);
    @Accessor("activeRitualData") ActiveRitualData forbiddenmekanism$active();
}
