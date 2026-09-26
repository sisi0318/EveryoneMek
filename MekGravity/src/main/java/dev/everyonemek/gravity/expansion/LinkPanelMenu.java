package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.Controller;
import dev.everyonemek.gravity.solar.SolarController;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Menu-scoped discovery. Only loaded chunk BE maps are visited; no block-volume scan or chunk tickets. */
public final class LinkPanelMenu extends AbstractContainerMenu {
    public final BlockPos anchor;public final long session;private final InteractionHand hand;private final OrbitalModule host;private final Player owner;
    private static final String SCAN_RANGE="mekgravity_panel_range";
    public int revision,range,linkRange,total;public boolean canEditLinkRange;public List<LinkPanelNetwork.Device> devices=List.of();public String feedback="panel_ready";
    private Map<BlockPos,BlockEntity> members=Map.of();private long scannedAt=Long.MIN_VALUE,actionTick=Long.MIN_VALUE;private int actions;
    public LinkPanelMenu(int id,Inventory inv,BlockPos anchor,InteractionHand hand,OrbitalModule host,long session){super(ModuleContent.PANEL_MENU.get(),id);this.owner=inv.player;this.anchor=anchor.immutable();this.hand=hand;this.host=host;this.session=session;linkRange=ModuleConfig.RANGE.get();var saved=owner.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);range=saved.contains(SCAN_RANGE)?Math.clamp(saved.getInt(SCAN_RANGE),ModuleConfig.MIN_RANGE,ModuleConfig.MAX_RANGE):linkRange;}
    public static LinkPanelMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf b){var anchor=b.readBlockPos();long session=b.readLong();int hand=b.readByte();return new LinkPanelMenu(id,inv,anchor,hand<0?null:InteractionHand.values()[hand],null,session);}
    public static void open(Player player,InteractionHand hand,OrbitalModule host){if(!(player instanceof ServerPlayer server)||host!=null&&!host.access(player)||host==null&&(hand==null||!player.getItemInHand(hand).is(ModuleContent.LINKER.get())))return;
        var anchor=host==null?player.blockPosition():host.getBlockPos();long session=server.getRandom().nextLong();
        server.openMenu(new SimpleMenuProvider((id,inv,p)->new LinkPanelMenu(id,inv,anchor,hand,host,session),ModuleContent.text("panel_title")),b->{b.writeBlockPos(anchor);b.writeLong(session);b.writeByte(host==null?hand.ordinal():-1);});
    }
    @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){if(p!=owner||p.distanceToSqr(anchor.getCenter())>64)return false;if(p.level().isClientSide)return true;
        return host!=null?host.access(p)&&p.level().hasChunkAt(anchor)&&p.level().getBlockEntity(anchor)==host:hand!=null&&p.getItemInHand(hand).is(ModuleContent.LINKER.get());}
    private boolean eligible(BlockEntity be){return be!=null&&!be.isRemoved()&&(be instanceof Controller||be instanceof SolarController||be instanceof OrbitalModule)&&be.getLevel()==owner.level()&&anchor.distSqr(be.getBlockPos())<=(double)range*range&&IBlockSecurityUtils.INSTANCE.canAccess(owner,owner.level(),be.getBlockPos(),be);}
    public void refresh(){if(!(owner instanceof ServerPlayer player)||!stillValid(player))return;long now=player.level().getGameTime();if(scannedAt!=Long.MIN_VALUE&&now-scannedAt<20)return;scannedAt=now;
        var found=new ArrayList<BlockEntity>();var chunks=player.serverLevel().getChunkSource();
        for(int x=(anchor.getX()-range)>>4;x<=(anchor.getX()+range)>>4;x++)for(int z=(anchor.getZ()-range)>>4;z<=(anchor.getZ()+range)>>4;z++){var chunk=chunks.getChunkNow(x,z);if(chunk!=null)for(var be:chunk.getBlockEntities().values())if(eligible(be))found.add(be);}
        found.sort(Comparator.comparingDouble((BlockEntity b)->b.getBlockPos().distSqr(anchor)).thenComparing(b->b.getBlockPos().asLong()));total=found.size();var next=new LinkedHashMap<BlockPos,BlockEntity>();found.stream().limit(LinkPanelNetwork.MAX_DEVICES).forEach(be->next.put(be.getBlockPos().immutable(),be));
        if(!next.equals(members)){revision++;members=next;}publish();
    }
    private LinkPanelNetwork.Device describe(BlockEntity be){var tile=(TileEntityMekanism)be;int kind;String state;boolean active,enabled,power=false;int channels=0;BlockPos source=null;
        if(be instanceof Controller c){kind=0;state=c.status;enabled=c.enabled;active=c.enabled&&c.ignited&&c.structure.formed&&c.fuelAvailable();}
        else if(be instanceof SolarController c){kind=1;state=c.status;enabled=c.enabled;active=c.isCoreHot();}
        else{var m=(OrbitalModule)be;kind=m.kind().ordinal()+2;state=m.status;enabled=m.enabled;active=m.getActive();channels=m.channels;
            if(m.source!=null){source=m.source.pos();if(m.source.inRange(owner.level(),m.getBlockPos())&&owner.level().hasChunkAt(source))power=FieldConnections.canPower(owner,FieldSource.at(owner.level(),source),m);}
        }
        return new LinkPanelNetwork.Device(be.getBlockPos(),BuiltInRegistries.ITEM.getKey(be.getBlockState().getBlock().asItem()),displayName(tile),kind,state,active,enabled,channels,source,power,be instanceof NodeModule n&&n.frequency!=null?n.frequencyName.isEmpty()?"—":n.frequencyName:"");
    }
    private net.minecraft.network.chat.Component displayName(TileEntityMekanism tile){var custom=tile.getCustomName();if(custom==null)return net.minecraft.network.chat.Component.translatable(tile.getBlockState().getBlock().getDescriptionId());var text=custom.getString();return net.minecraft.network.chat.Component.literal(text.length()>80?text.substring(0,80):text);}
    private static boolean canConfigure(ServerPlayer p){return p.hasPermissions(2)||p.server.isSingleplayerOwner(p.getGameProfile());}
    private void publish(){if(!(owner instanceof ServerPlayer p))return;linkRange=ModuleConfig.RANGE.get();canEditLinkRange=canConfigure(p);devices=members.values().stream().filter(this::eligible).map(this::describe).toList();PacketDistributor.sendToPlayer(p,new LinkPanelNetwork.Snapshot(containerId,session,revision,range,linkRange,canEditLinkRange,total,devices,feedback));}
    private boolean settings(ServerPlayer player,int operation,int value){
        if(value<ModuleConfig.MIN_RANGE||value>ModuleConfig.MAX_RANGE){feedback="panel_range_invalid";publish();return false;}
        if(operation==9&&!canConfigure(player)){feedback="panel_range_admin";publish();return false;}
        if(operation==8){range=value;var saved=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);saved.putInt(SCAN_RANGE,value);player.getPersistentData().put(Player.PERSISTED_NBT_TAG,saved);}
        else{int old=ModuleConfig.RANGE.get();try{ModuleConfig.RANGE.set(value);ModuleConfig.RANGE.save();}catch(RuntimeException e){ModuleConfig.RANGE.set(old);mekanism.common.Mekanism.logger.error("Unable to save field link range",e);feedback="panel_range_error";publish();return false;}}
        revision++;feedback="panel_range_saved";publish();refresh();return true;
    }
    private BlockEntity current(BlockPos pos){if(pos==null||!owner.level().hasChunkAt(pos))return null;var current=owner.level().getBlockEntity(pos);return members.get(pos)==current&&eligible(current)?current:null;}
    public boolean handle(ServerPlayer player,LinkPanelNetwork.Action request){
        if(player!=owner||player.containerMenu!=this||request.menuId()!=containerId||request.session()!=session||!stillValid(player))return false;
        long now=player.level().getGameTime();if(actionTick!=now){actionTick=now;actions=0;}if(++actions>4)return false;
        if(request.operation()==0){refresh();return true;}if(request.revision()!=revision){feedback="panel_stale";publish();return false;}
        if(request.operation()==8||request.operation()==9)return settings(player,request.operation(),request.value());
        var from=current(request.from());var to=current(request.to());boolean ok=false;
        if(request.operation()==1){if(from instanceof Controller||from instanceof SolarController){if(to instanceof OrbitalModule target)ok=FieldConnections.power(player,new FieldSource((TileEntityMekanism)from),target);}
        }else if(from instanceof OrbitalModule module){
            if(request.operation()==2&&(request.to()==null||module.source!=null&&module.source.pos().equals(request.to()))){module.source=null;ok=true;}
            else if(request.operation()==3&&module instanceof NodeModule node){ok=node.selectFrequency(player,null);}
            else if(request.operation()>=4&&request.operation()<=7&&module.kind()==ModuleKind.NODE){module.channels^=1<<(request.operation()-4);ok=true;}
            if(ok){module.markForSave();module.sendUpdatePacket();}
        }
        feedback=ok?"panel_applied":"panel_rejected";publish();return ok;
    }
    public void accept(LinkPanelNetwork.Snapshot packet){if(packet.menuId()!=containerId||packet.session()!=session)return;revision=packet.revision();range=packet.range();linkRange=packet.linkRange();canEditLinkRange=packet.canEditLinkRange();total=packet.total();devices=packet.devices();feedback=packet.feedback();}
    @Override public void broadcastChanges(){super.broadcastChanges();if(owner instanceof ServerPlayer)refresh();}
}
