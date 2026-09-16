package dev.everyonemek.overloadcore.mixin;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.common.inventory.DynamicStackHandler;
@Mixin(DynamicStackHandler.class)
public interface WardSlotAccess {
    @Accessor("ctxBuilder") Function<Integer, SlotContext> overload$context();
}
