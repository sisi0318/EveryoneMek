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
        container.registerConfig(ModConfig.Type.CLIENT,VisualConfig.SPEC,"mekgravity-client.toml");
        Content.register(bus);bus.addListener(Ports::register);bus.addListener(ReactorConfig::loaded);
        container.registerConfig(ModConfig.Type.SERVER,dev.everyonemek.gravity.solar.SolarConfig.SPEC,"mekgravity-solar-server.toml");
        dev.everyonemek.gravity.solar.SolarContent.register(bus);bus.addListener(dev.everyonemek.gravity.solar.SolarPorts::register);
        dev.everyonemek.gravity.corona.CoronalContent.register(bus);
        NeoForge.EVENT_BUS.addListener(dev.everyonemek.gravity.solar.SolarStructure::unload);NeoForge.EVENT_BUS.addListener(dev.everyonemek.gravity.solar.SolarStructure::chunkUnload);NeoForge.EVENT_BUS.addListener(dev.everyonemek.gravity.solar.SolarStructure::chunkLoad);
        NeoForge.EVENT_BUS.addListener(Structure::unload);NeoForge.EVENT_BUS.addListener(Structure::chunkUnload);NeoForge.EVENT_BUS.addListener(Structure::chunkLoad);
    }
}
