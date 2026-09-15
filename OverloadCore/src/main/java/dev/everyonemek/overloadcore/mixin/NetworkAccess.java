package dev.everyonemek.overloadcore.mixin;
import mekanism.common.lib.transmitter.DynamicNetwork;
import mekanism.common.lib.transmitter.acceptor.NetworkAcceptorCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value = DynamicNetwork.class, remap = false)
public interface NetworkAccess { @Accessor("acceptorCache") NetworkAcceptorCache<?> overload$acceptors(); }
