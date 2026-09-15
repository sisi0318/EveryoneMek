package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.Upgrade;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

public final class Workplace {
    public static double temperature(BlockEntity tile) {
        if (!DeviceScope.live(tile)) return 0;
        var heat = tile.getLevel().getCapability(Capabilities.HEAT, tile.getBlockPos(), null);
        double temperature = heat == null ? 0 : heat.getTotalTemperature();
        if(tile instanceof mekanism.common.tile.prefab.TileEntityMultiblock<?> multi&&multi.getMultiblock().isFormed())
            temperature=Math.max(temperature,multi.getMultiblock().getTotalTemperature());
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tile.getType()).getPath();
        if (DeviceTracker.working(tile) && (id.contains("smelting") || id.equals("energized_smelter"))) temperature = Math.max(temperature, 500);
        return Double.isFinite(temperature) ? Math.max(0, temperature) : 0;
    }
    private static Vec3 closest(AABB box, Vec3 point) { return new Vec3(Math.clamp(point.x, box.minX, box.maxX), Math.clamp(point.y, box.minY, box.maxY), Math.clamp(point.z, box.minZ, box.maxZ)); }
    private static boolean exposed(ServerPlayer player, DeviceScope.Device device) {
        var target = closest(device.bounds(), player.getEyePosition());
        var hit = player.level().clip(new ClipContext(player.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || device.bounds().inflate(.02).contains(hit.getLocation());
    }
    public static void tick(ServerPlayer player) {
        var data = CoreBinding.data(player); long tick = player.level().getGameTime();
        int load = MetalLoad.measure(player); boolean heavy = data.getBoolean("heavy"); int limit = CoreConfig.METAL_LIMIT.get();
        data.putInt("load", load); data.putBoolean("heavy", heavy ? load >= Math.max(0, limit - 32) : load >= limit);
        int heat = Math.clamp(data.getInt("heat"), 0, 100), noise = Math.clamp(data.getInt("noise"), 0, 100);
        double noiseRate = 0; int heatRate = 0; DeviceScope.Device shock = null; double nearest = Double.MAX_VALUE;
        var seen = new HashSet<BlockPos>();
        if (CoreConfig.HAZARDS.get()) for (var tile : DeviceTracker.nearby(player.level(), player.position(), 8)) {
            var device = DeviceScope.device(tile);
            if (!DeviceScope.allowed(device, player) || !seen.add(device.anchor().getBlockPos()) || !exposed(player, device)) continue;
            double distance = player.position().distanceTo(closest(device.bounds(), player.position()));
            if (DeviceTracker.working(tile) || DeviceTracker.working(device.anchor())) {
                if (distance < 2 && distance < nearest) { shock = device; nearest = distance; }
                if (distance < 8 && tile instanceof TileEntityMekanism machine) {
                    double volume = machine.supportsUpgrade(Upgrade.MUFFLING) ? 1 - Math.clamp(machine.getComponent().getUpgrades(Upgrade.MUFFLING) / (double) Upgrade.MUFFLING.getMax(), 0, 1) : 1;
                    noiseRate = Math.min(10, noiseRate + volume * 10 * Math.max(.25, 1 - distance / 8));
                }
            }
            double temperature = temperature(tile);
            if (distance <= 5 && temperature >= 900) heatRate = Math.max(heatRate, 5);
            else if (distance <= 3 && temperature >= 450) heatRate = Math.max(heatRate, 2);
        }
        if (shock != null && tick >= data.getLong("next_shock")) {
            if (!data.contains("shock_warning")) {
                data.putLong("shock_warning",tick);
                var point=closest(shock.bounds(),player.getEyePosition());
                player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,5,.1,.1,.1,.01);
            } else if (tick-data.getLong("shock_warning")>=10) {
                var center = shock.bounds().getCenter();
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.REDSTONE_TORCH_BURNOUT, net.minecraft.sounds.SoundSource.BLOCKS, .3F, 1.3F);
                player.hurt(player.damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "electric"))), 1);
                player.knockback(.35, center.x - player.getX(), center.z - player.getZ());
                data.putLong("next_shock", tick + 60); data.remove("shock_warning");
            }
        } else data.remove("shock_warning");
        // Called every ten ticks; rates below are per half-second.
        int heatStep = player.isInWater() ? -12 : heatRate > 0 ? heatRate : -5;
        int heatRemainder = data.getInt("heat_remainder") + heatStep;
        heat = Math.clamp(heat + heatRemainder / 2, 0, 100); data.putInt("heat_remainder", heatRemainder % 2);
        noise = Math.clamp(noise + (noiseRate > 0 ? (int) Math.ceil(noiseRate / 2) : -10), 0, 100);
        if (!CoreConfig.HAZARDS.get()) { heat=0;noise=0; }
        if (noise >= 100) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
        boolean overheated = data.getBoolean("overheated") ? heat >= 80 : heat >= 100;
        if (overheated && tick >= data.getLong("next_heat_damage")) {
            player.hurt(player.damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "heat"))), 1);
            data.putLong("next_heat_damage", tick + 40);
        }
        data.putInt("heat", heat); data.putInt("noise", noise); data.putBoolean("overheated", overheated); CoreBinding.save(player, data);
        if (data.getBoolean("heavy")) player.setSprinting(false);
    }
    private Workplace() { }
}
