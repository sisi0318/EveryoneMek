package dev.everyonemek.gravity.expansion;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/** Opens the overview and links reactor power. Node routing uses frequencies exclusively. */
public final class FieldLinker extends Item {
    public FieldLinker(Properties properties){super(properties);}
    public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){if(event.getItemStack().is(ModuleContent.LINKER.get())){var result=event.getItemStack().useOn(new UseOnContext(event.getEntity(),event.getHand(),event.getHitVec()));if(result!=InteractionResult.PASS){event.setCancellationResult(result);event.setCanceled(true);}}}
    private static FieldLink power(CompoundTag tag){return tag==null?null:FieldLink.read(tag.getCompound("power"));}
    private static InteractionResult result(Player p,boolean ok,String key,Object...args){p.displayClientMessage(ModuleContent.text(key,args),true);return ok?InteractionResult.CONSUME:InteractionResult.FAIL;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!level.isClientSide){if(player.isShiftKeyDown()){stack.remove(ModuleContent.LINK.get());player.displayClientMessage(ModuleContent.text("selection_cleared"),true);}else LinkPanelMenu.open(player,hand,null);}return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext context){var player=context.getPlayer();if(player==null)return InteractionResult.PASS;var level=context.getLevel();if(level.isClientSide)return InteractionResult.SUCCESS;
        var pos=context.getClickedPos();var item=context.getItemInHand();var core=FieldSource.at(level,pos);
        if(core!=null){if(!core.permitted(player))return result(player,false,"access");var data=new CompoundTag();data.put("power",FieldLink.at(level,core.tile().getBlockPos()).save());item.set(ModuleContent.LINK.get(),data);return result(player,true,"source_selected",core.tile().getBlockPos().toShortString());}
        if(!(level.getBlockEntity(pos) instanceof OrbitalModule module))return InteractionResult.PASS;var source=power(item.get(ModuleContent.LINK.get()));if(source==null)return result(player,false,module instanceof NodeModule?"frequency_help":"select_source");
        boolean ok=module.bind(player,source);return result(player,ok,ok?"linked":"link_failed");
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(ModuleContent.text("linker_hint"));var source=power(stack.get(ModuleContent.LINK.get()));if(source!=null){lines.add(ModuleContent.text("power_selection",source.pos().toShortString()));lines.add(ModuleContent.text("clear_selection_hint"));}}
}
