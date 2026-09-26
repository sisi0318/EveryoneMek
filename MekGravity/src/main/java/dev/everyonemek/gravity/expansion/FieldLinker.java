package dev.everyonemek.gravity.expansion;
import java.util.List;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
public final class FieldLinker extends Item {
    public FieldLinker(Properties properties){super(properties);}
    public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getItemStack().is(ModuleContent.LINKER.get())){var result=event.getItemStack().useOn(new UseOnContext(event.getEntity(),event.getHand(),event.getHitVec()));if(result!=InteractionResult.PASS){event.setCancellationResult(result);event.setCanceled(true);}}
    }
    @Override public InteractionResult useOn(UseOnContext context){var player=context.getPlayer();if(player==null)return InteractionResult.PASS;var level=context.getLevel();if(level.isClientSide)return InteractionResult.SUCCESS;
        var pos=context.getClickedPos();var be=level.getBlockEntity(pos);var item=context.getItemInHand();
        if(player.isShiftKeyDown()){
            var source=FieldSource.at(level,pos);boolean node=be instanceof OrbitalModule m&&m.kind()==ModuleKind.NODE;
            if(source==null&&!node){player.displayClientMessage(ModuleContent.text("select_source"),true);return InteractionResult.FAIL;}
            var target=source==null?be:source.tile();if(!IBlockSecurityUtils.INSTANCE.canAccess(player,level,target.getBlockPos(),target)){player.displayClientMessage(ModuleContent.text("access"),true);return InteractionResult.FAIL;}
            var tag=FieldLink.at(level,target.getBlockPos()).save();tag.putBoolean("node",node);item.set(ModuleContent.LINK.get(),tag);player.displayClientMessage(ModuleContent.text(node?"node_selected":"source_selected",target.getBlockPos().toShortString()),true);return InteractionResult.CONSUME;
        }
        if(!(be instanceof OrbitalModule module))return InteractionResult.PASS;var data=item.get(ModuleContent.LINK.get());var link=data==null?null:FieldLink.read(data);
        if(link==null){player.displayClientMessage(ModuleContent.text("select_source"),true);return InteractionResult.FAIL;}
        boolean success=module.bind(player,link,data.getBoolean("node"));player.displayClientMessage(ModuleContent.text(success?"linked":"link_failed"),true);return success?InteractionResult.CONSUME:InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(ModuleContent.text("linker_hint"));var tag=stack.get(ModuleContent.LINK.get());var link=tag==null?null:FieldLink.read(tag);if(link!=null)lines.add(ModuleContent.text("selected",link.dimension(),link.pos().toShortString()));}
}
