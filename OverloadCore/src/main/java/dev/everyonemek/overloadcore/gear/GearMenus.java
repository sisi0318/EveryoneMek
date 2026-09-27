package dev.everyonemek.overloadcore.gear;

import java.util.UUID;
import dev.everyonemek.overloadcore.OverloadCore;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GearMenus {
    private static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(OverloadCore.ID);
    public static final ContainerTypeRegistryObject<ServiceMenu> SERVICE = MENUS.registerMenu("worn_service",
          () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(ServiceMenu::fromNetwork));
    public static java.util.function.Consumer<Snapshot> client = p -> {};
    public record Open(int menu) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"service_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC = StreamCodec.of((b,p)->b.writeVarInt(p.menu), b->new Open(b.readVarInt()));
        public Type<Open> type(){return TYPE;}
    }
    public record Edit(int menu,long session,int target,int upgrade,int operation,UUID identity) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"service_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC = StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeLong(p.session);b.writeByte(p.target);b.writeByte(p.upgrade);b.writeByte(p.operation);b.writeBoolean(p.identity!=null);if(p.identity!=null)b.writeUUID(p.identity);}, b->new Edit(b.readVarInt(),b.readLong(),b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte(),b.readBoolean()?b.readUUID():null));
        public Type<Edit> type(){return TYPE;}
    }
    public record Snapshot(int menu,long session,ItemStack core,ItemStack ward,long energy,long cost,int status) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"service_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC = StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeLong(p.session);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,p.core);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,p.ward);b.writeLong(p.energy);b.writeLong(p.cost);b.writeVarInt(p.status);},b->new Snapshot(b.readVarInt(),b.readLong(),ItemStack.OPTIONAL_STREAM_CODEC.decode(b),ItemStack.OPTIONAL_STREAM_CODEC.decode(b),b.readLong(),b.readLong(),b.readVarInt()));
        public Type<Snapshot> type(){return TYPE;}
    }
    public static void register(IEventBus bus){MENUS.register(bus);bus.addListener(GearMenus::network);}
    public static void open(ServerPlayer s,Open p){if(s.containerMenu.containerId==p.menu&&s.containerMenu instanceof mekanism.common.inventory.container.tile.MekanismTileContainer<?> menu&&menu.getTileEntity() instanceof mekanism.common.tile.TileEntityModificationStation station&&menu.stillValid(s))ServiceMenu.open(s,station);}
    private static void network(RegisterPayloadHandlersEvent e){var r=e.registrar("1");
        r.playToServer(Open.TYPE,Open.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer s)open(s,p);}));
        r.playToServer(Edit.TYPE,Edit.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer s&&s.containerMenu instanceof ServiceMenu menu)menu.action(s,p);}));
        r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(p,c)->c.enqueueWork(()->client.accept(p)));
    }
    private GearMenus(){}
}
