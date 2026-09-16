package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.WardCustody;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WardMenuTransactionMixin {
    @WrapOperation(method = "handleContainerClick", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void overload$manualTransaction(AbstractContainerMenu menu, int slot, int button, ClickType type,
          Player player, Operation<Void> original) {
        WardCustody.click((ServerPlayer)player, menu, slot, type, () -> original.call(menu, slot, button, type, player));
    }
}
