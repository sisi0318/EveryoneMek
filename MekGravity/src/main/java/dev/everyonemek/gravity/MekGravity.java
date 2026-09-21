package dev.everyonemek.gravity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
@Mod(MekGravity.ID)
public final class MekGravity {
    public static final String ID="mekgravity";
    public MekGravity(IEventBus bus,ModContainer container){
        container.registerConfig(ModConfig.Type.SERVER,ReactorConfig.SPEC);
        Content.register(bus);bus.addListener(Ports::register);
        NeoForge.EVENT_BUS.addListener(Structure::unload);NeoForge.EVENT_BUS.addListener(Structure::chunkUnload);
    }
}
