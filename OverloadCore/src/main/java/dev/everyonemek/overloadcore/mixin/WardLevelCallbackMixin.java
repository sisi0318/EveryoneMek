package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = "net.minecraft.world.level.entity.PersistentEntitySectionManager$Callback")
public abstract class WardLevelCallbackMixin {
    @Shadow @Final private EntityAccess entity;
    @Shadow @Final private PersistentEntitySectionManager<?> this$0;
    @Inject(method = "onRemove", at = @At("HEAD"), cancellable = true)
    private void overload$callback(Entity.RemovalReason reason, CallbackInfo ci) {
        // Compare the owning manager, not the callback object: other mods may wrap and delegate this callback.
        if (entity instanceof ServerPlayer player && ((WardServerLevelAccess)player.serverLevel()).overload$manager() == this$0
              && ThunderWard.removal(player, reason)) ci.cancel();
    }
}
