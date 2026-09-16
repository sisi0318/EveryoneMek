package dev.everyonemek.overloadcore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid = OverloadCore.ID)
public final class WardEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ThunderWard.rescue(player)) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WardCustody.ensure(player);
            ThunderWard.beforeTick(player);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ThunderWard.forget(player);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WardCustody.ensure(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void itemJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
              && WardCustody.rejectDrop(item)) event.setCanceled(true);
    }
}
