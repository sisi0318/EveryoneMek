package dev.everyonemek.overloadcore;

import mekanism.api.*;
import mekanism.api.energy.IEnergyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The intercepted call is production, never storage output or an external insertion. */
public final class GenerationHooks {
    public static long insert(DeviceScope.Device device, IEnergyContainer storage, long offered, Action action, AutomationType automation) {
        return insert(device, offered, action, automation, amount -> storage.insert(amount,action,automation));
    }
    public static long insert(DeviceScope.Device device, long offered, Action action, AutomationType automation, java.util.function.LongUnaryOperator original) {
        if (!CoreConfig.GENERATION.get() || offered <= 0 || automation != AutomationType.INTERNAL || DeviceScope.bearer(device) == null)
            return original.applyAsLong(offered);
        var tag = DeviceScope.data(device.anchor()); int carry = Math.clamp(tag.getInt("generation_carry"), 0, 1);
        long generated = offered / 2 + (offered % 2 + carry) / 2;
        long accepted = generated - original.applyAsLong(generated);
        if (action.simulate()) {
            // Fuel generators ask whether one full fuel operation fits, before actually burning it.
            return accepted == generated ? 0 : Math.max(1, offered - Math.min(offered, accepted * 2));
        }
        tag.putInt("generation_carry", (int) ((offered % 2 + carry) % 2)); DeviceScope.save(device.anchor(), tag);
        if (accepted > 0) DeviceTracker.worked(device.anchor());
        return offered - accepted;
    }
    private GenerationHooks() { }
}
