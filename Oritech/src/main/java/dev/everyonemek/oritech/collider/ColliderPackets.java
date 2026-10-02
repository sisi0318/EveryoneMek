package dev.everyonemek.oritech.collider;

import dev.everyonemek.oritech.Content;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ColliderPackets {
    public record Control(int menu,int action,int revision,int cell,int direction,int bend,String recipe) implements CustomPacketPayload {
        public static final Type<Control> TYPE=new Type<>(Content.id("collider_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Control> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.action);b.writeVarInt(p.revision);b.writeVarInt(p.cell);b.writeVarInt(p.direction);b.writeVarInt(p.bend);b.writeUtf(p.recipe,256);},
            b->new Control(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(256)));
        @Override public Type<Control> type(){return TYPE;}
    }
    public record Snapshot(int menu,CompoundTag data) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(Content.id("collider_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeNbt(p.data);},b->new Snapshot(b.readVarInt(),b.readNbt()));
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public record Paint(int menu,int revision,int[] cells,int direction,int bend,boolean remove) implements CustomPacketPayload {
        public static final Type<Paint> TYPE=new Type<>(Content.id("collider_paint"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Paint> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.revision);b.writeVarIntArray(p.cells);b.writeVarInt(p.direction);b.writeVarInt(p.bend);b.writeBoolean(p.remove);},
            b->new Paint(b.readVarInt(),b.readVarInt(),b.readVarIntArray(256),b.readVarInt(),b.readVarInt(),b.readBoolean()));
        @Override public Type<Paint> type(){return TYPE;}
    }
    public static int paint(Paint packet,Player player){
        if(!(player.containerMenu instanceof ColliderMenu menu)||menu.containerId!=packet.menu||!menu.canControl(player)||!menu.getCarried().isEmpty()||packet.cells.length>256
            ||menu.collider.revision!=packet.revision||!menu.collider.editable()||packet.direction<0||packet.direction>7||packet.bend<0||packet.bend>2)return 0;
        int changed=0;var visited=new java.util.HashSet<Integer>();for(int pos:packet.cells)if(visited.add(pos)&&menu.collider.edit(packet.remove?1:0,pos,packet.direction,packet.bend))changed++;
        menu.broadcastChanges();return changed;
    }
    public static boolean handle(Control packet,Player player){
        if(!(player.containerMenu instanceof ColliderMenu menu)||menu.containerId!=packet.menu||!menu.canControl(player)||!menu.getCarried().isEmpty())return false;
        var collider=menu.collider;boolean changed=true;
        switch(packet.action){
            case 0->collider.enabled=!collider.enabled;
            case 1->collider.cancel();
            case 2->collider.eject=!collider.eject;
            case 3->{if(packet.cell<0||packet.cell>=6||packet.direction<Collider.CLOSED||packet.direction>Collider.EJECT)return false;collider.sides[packet.cell]=packet.direction;}
            case 4,5,6,7->{if(packet.revision!=collider.revision)return false;changed=collider.edit(packet.action-4,packet.cell,packet.direction,packet.bend);}
            case 8->{var id=ResourceLocation.tryParse(packet.recipe);if(id==null)return false;changed=collider.selectRecipe(id);}
            case 9->changed=collider.selectRecipe(null);
            case 10->changed=collider.lockDetected();
            default->{return false;}
        }
        if(changed){collider.setChanged();menu.broadcastChanges();}return changed;
    }
    public static void register(RegisterPayloadHandlersEvent event){var registrar=event.registrar("1");
        registrar.playToServer(Control.TYPE,Control.CODEC,(packet,context)->context.enqueueWork(()->handle(packet,context.player())));
        registrar.playToServer(Paint.TYPE,Paint.CODEC,(packet,context)->context.enqueueWork(()->paint(packet,context.player())));
        registrar.playToClient(Snapshot.TYPE,Snapshot.CODEC,(packet,context)->context.enqueueWork(()->{if(context.player().containerMenu instanceof ColliderMenu menu&&menu.containerId==packet.menu&&packet.data!=null)menu.readView(packet.data);}));
    }
    private ColliderPackets(){}
}
