package dev.everyonemek.factory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
@Mod(MekFactory.ID)
public final class MekFactory {
    public static final String ID="mekfactory";
    public MekFactory(IEventBus bus, ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, FactoryConfig.SPEC);
        Content.register(bus); bus.addListener(Ports::register); bus.addListener(FactoryConfig::loaded);
        bus.addListener(FactoryAppearance::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactoryStructure::unload);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactoryStructure::chunkUnload);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactoryStructure::interact);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactoryAppearance::chunkSent);
    }
}
