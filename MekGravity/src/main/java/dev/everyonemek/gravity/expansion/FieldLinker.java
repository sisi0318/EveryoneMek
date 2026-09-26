package dev.everyonemek.gravity.expansion;
import java.util.List;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
/** Normal click follows source -> sender -> receiver. Old sneak-select links remain valid. */
public final class FieldLinker extends Item {
    public FieldLinker(Properties properties){super(properties);}
    public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){if(event.getItemStack().is(ModuleContent.LINKER.get())){var result=event.getItemStack().useOn(new UseOnContext(event.getEntity(),event.getHand(),event.getHitVec()));if(result!=InteractionResult.PASS){event.setCancellationResult(result);event.setCanceled(true);}}}
    private static FieldLink power(CompoundTag tag){var current=FieldLink.read(tag.getCompound("power"));return current!=null?current:!tag.getBoolean("node")?FieldLink.read(tag):null;}
    private static boolean allowed(Player player,OrbitalModule module){return IBlockSecurityUtils.INSTANCE.canAccess(player,module.getLevel(),module.getBlockPos(),module);}
    private static InteractionResult result(Player p,boolean ok,String key,Object...args){p.displayClientMessage(ModuleContent.text(key,args),true);return ok?InteractionResult.CONSUME:InteractionResult.FAIL;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!player.isShiftKeyDown()){if(!level.isClientSide)LinkPanelMenu.open(player,hand,null);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}if(!level.isClientSide){stack.remove(ModuleContent.LINK.get());player.displayClientMessage(ModuleContent.text("selection_cleared"),true);}return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext context){var player=context.getPlayer();if(player==null)return InteractionResult.PASS;var level=context.getLevel();if(level.isClientSide)return InteractionResult.SUCCESS;
        var pos=context.getClickedPos();var be=level.getBlockEntity(pos);var item=context.getItemInHand();var previous=item.get(ModuleContent.LINK.get());var data=previous==null?new CompoundTag():previous.copy();
        var core=FieldSource.at(level,pos);
        if(core!=null){if(!core.permitted(player))return result(player,false,"access");var next=new CompoundTag();next.put("power",FieldLink.at(level,core.tile().getBlockPos()).save());item.set(ModuleContent.LINK.get(),next);return result(player,true,"source_selected",core.tile().getBlockPos().toShortString());}
        if(!(be instanceof OrbitalModule module))return InteractionResult.PASS;if(!module.access(player))return result(player,false,"access");
        if(player.isShiftKeyDown()&&module.kind()==ModuleKind.NODE){var next=FieldLink.at(level,pos).save();next.putBoolean("node",true);item.set(ModuleContent.LINK.get(),next);return result(player,true,"node_selected",pos.toShortString());}
        if(data.getBoolean("node")&&FieldLink.read(data)!=null){boolean ok=module.bind(player,FieldLink.read(data),true);return result(player,ok,ok?"linked":"link_failed");}
        var selected=FieldLink.read(data.getCompound("sender"));
        if(module.kind()==ModuleKind.NODE&&selected!=null){
            if(!selected.inRange(level,pos)||!level.hasChunkAt(selected.pos()))return result(player,false,"range");
            if(!(level.getBlockEntity(selected.pos()) instanceof OrbitalModule sender)||sender.kind()!=ModuleKind.NODE||sender==module)return result(player,false,"select_different_node");
            if(!allowed(player,sender))return result(player,false,"access");
            if(!FieldConnections.route(player,sender,module))return result(player,false,"link_failed");data.remove("sender");item.set(ModuleContent.LINK.get(),data);
            return result(player,true,sender.source==null?"paired_need_power":"paired",sender.getBlockPos().toShortString(),pos.toShortString());
        }
        var energy=power(data);
        if(energy!=null){if(!energy.inRange(level,pos))return result(player,false,"range");if(!level.hasChunkAt(energy.pos()))return result(player,false,"unloaded");if(!module.bind(player,energy,false))return result(player,false,"link_failed");if(module.kind()!=ModuleKind.NODE){data.remove("sender");item.set(ModuleContent.LINK.get(),data);return result(player,true,"linked");}}
        if(module.kind()==ModuleKind.NODE){var next=new CompoundTag();if(energy!=null)next.put("power",energy.save());next.put("sender",FieldLink.at(level,pos).save());item.set(ModuleContent.LINK.get(),next);return result(player,true,"sender_selected",pos.toShortString());}
        return result(player,false,"select_source");
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(ModuleContent.text("linker_hint"));var data=stack.get(ModuleContent.LINK.get());if(data==null)return;
        var energy=power(data);var sender=FieldLink.read(data.getCompound("sender"));var old=data.getBoolean("node")?FieldLink.read(data):null;
        if(energy!=null)lines.add(ModuleContent.text("power_selection",energy.pos().toShortString()));if(sender!=null)lines.add(ModuleContent.text("sender_selected",sender.pos().toShortString()));if(old!=null)lines.add(ModuleContent.text("node_selected",old.pos().toShortString()));lines.add(ModuleContent.text("clear_selection_hint"));}
}
