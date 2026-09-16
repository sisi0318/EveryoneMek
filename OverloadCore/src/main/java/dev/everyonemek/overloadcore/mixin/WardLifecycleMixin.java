package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import dev.everyonemek.overloadcore.WardCustody;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(PlayerList.class)
public abstract class WardLifecycleMixin {
    @WrapMethod(method = "remove")
    private void overload$logout(ServerPlayer player, Operation<Void> original) {
        WardCustody.ensure(player);
        ThunderWard.beginLifecycle(player);
        try { original.call(player); }
        finally { ThunderWard.endLifecycle(player); WardCustody.forget(player); dev.everyonemek.overloadcore.WardRuntime.forget(player); }
    }
    @WrapMethod(method = "respawn")
    private ServerPlayer overload$respawn(ServerPlayer player, boolean keepEverything, Entity.RemovalReason reason, Operation<ServerPlayer> original) {
        ThunderWard.beginLifecycle(player);
        try {
            var replacement = original.call(player, keepEverything, reason);
            WardCustody.forget(player);
            dev.everyonemek.overloadcore.WardRuntime.forget(player);
            ThunderWard.newLife(replacement);
            WardCustody.ensure(replacement);
            dev.everyonemek.overloadcore.WardRuntime.sync(replacement);
            return replacement;
        }
        finally { ThunderWard.endLifecycle(player); }
    }
}
