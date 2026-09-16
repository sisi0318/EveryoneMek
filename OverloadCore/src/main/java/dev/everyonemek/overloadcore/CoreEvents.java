package dev.everyonemek.overloadcore;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = OverloadCore.ID)
public final class CoreEvents {
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (CoreBinding.bound(event.getOriginal())) CoreBinding.save(event.getEntity(), CoreBinding.data(event.getOriginal()).copy());
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CoreBinding.active(player)) return;
        if (player.tickCount % 20 == 0) CoreBinding.restore(player);
        CoreBinding.charge(player);
        if (player.tickCount % 10 == 0) { Workplace.tick(player); CorePackets.sendStatus(player, false); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { CoreBinding.restore(player); CorePackets.sendStatus(player, false); }
    }
    @SubscribeEvent public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CorePackets.sendStatus(player, false);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) DeviceScope.placed(player.level().getBlockEntity(event.getPos()), player.getUUID());
    }
    @SubscribeEvent public static void drops(BlockDropsEvent event) {
        var tile = event.getBlockEntity(); if (tile == null || !DeviceScope.supported(tile)) return;
        var data = DeviceScope.data(tile); if (data.isEmpty()) return;
        for (var drop : event.getDrops()) if (drop.getItem().is(tile.getBlockState().getBlock().asItem())) drop.getItem().set(CoreContent.MACHINE.get(), data.copy());
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level) { DeviceTracker.unload(level); DeviceScope.unload(level); }
    }
    @SubscribeEvent public static void unloadChunk(ChunkEvent.Unload event) {
        if(event.getLevel() instanceof net.minecraft.world.level.Level level)DeviceTracker.unloadChunk(level,event.getChunk().getPos().toLong());
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed event) {
        var player = event.getEntity(); if (!CoreBinding.active(player) || event.getNewSpeed() <= 1) return;
        var item = player.getMainHandItem();
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals("mekanism:meka_tool")) return;
        var energy = item.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        if (energy != null && energy.getEnergyStored() > 0) event.setNewSpeed(event.getNewSpeed() * 1.25F);
    }
    private static net.minecraft.world.level.block.entity.BlockEntity target(ServerPlayer player) {
        var hit = player.pick(8, 0, false); return hit instanceof BlockHitResult block ? player.level().getBlockEntity(block.getBlockPos()) : null;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        var command = Commands.literal("overloadcore")
              .then(Commands.literal("info").executes(c -> { CorePackets.sendStatus(c.getSource().getPlayerOrException(), true); return 1; }))
              .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> { CoreBinding.clear(EntityArgument.getPlayer(c, "player")); return 1; })))
              .then(Commands.literal("claim").requires(s -> s.hasPermission(2)).executes(c -> {
                  var player = c.getSource().getPlayerOrException(); var tile = target(player);
                  if (tile == null || !DeviceScope.supported(tile) && DeviceScope.powerDevice(tile) == null) return 0;
                  DeviceScope.claim(tile, player.getUUID()); return 1;
              }));
        for (boolean allow : new boolean[]{true, false}) command.then(Commands.literal(allow ? "share" : "unshare")
              .then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                  var owner = c.getSource().getPlayerOrException(); var tile = target(owner);
                  boolean accepted = tile != null && DeviceScope.share(owner, tile, EntityArgument.getPlayer(c, "player").getUUID(), allow);
                  owner.displayClientMessage(CoreContent.text(accepted ? "share_done" : "not_owner"), false); return accepted ? 1 : 0;
              })));
        event.getDispatcher().register(command);
    }
}
