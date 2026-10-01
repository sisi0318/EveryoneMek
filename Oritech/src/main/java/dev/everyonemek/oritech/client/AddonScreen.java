package dev.everyonemek.oritech.client;

import java.util.*;
import dev.everyonemek.oritech.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.oritech.api.screen.*;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.client.ui.OritechWidgetScreen;

/** The load/list/unload workflow uses native Oritech panels around real server-owned slots. */
public final class AddonScreen extends OritechWidgetScreen<AddonMenu> {
    private record Entry(ItemStack stack,int count){}
    private ScrollWidget list;
    private LabelWidget capacity,selection,status;
    private ItemWidget selectedIcon;
    private ButtonWidget unload,back;
    private ItemStack selected=ItemStack.EMPTY;
    private List<ItemStack> snapshot=List.of();
    public AddonScreen(AddonMenu menu,Inventory inventory,Component title){super(menu,inventory,title,276,245);}
    private static Component tr(String key,Object...args){return Component.translatable("gui.oritechmekanism."+key,args);}
    @Override public boolean shouldCreateTitle(){return false;}
    @Override public net.minecraft.world.level.block.state.BlockState getTitleState(){return menu.processor.getBlockState();}
    @Override protected void buildComponents(){
        addComponent(new SurfaceWidget(0,0,276,245,OritechSurface.PANEL).withZIndex(-10));
        addComponent(new LabelWidget(80,8,116,12,tr("upgrades")).withAlignment(LabelWidget.Alignment.CENTER).withDarkColor());
        back=ButtonWidget.panel(8,7,60,14,tr("back"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).withTextColor(LabelWidget.DARK_TEXT);addComponent(back);
        list=new ScrollWidget(8,27,156,108);list.withSurface(OritechSurface.PANEL_INSET);list.withScrollSpeed(20);addComponent(list);
        addComponent(new SurfaceWidget(168,27,74,108,OritechSurface.PANEL_DARK));
        selectedIcon=new ItemWidget(197,33,ItemStack.EMPTY);addComponent(selectedIcon);
        selection=new LabelWidget(174,55,62,37,tr("not_selected")).withWrap(true);addComponent(selection);
        unload=ButtonWidget.darkPanel(174,111,62,16,tr("unload"),b->{if(!selected.isEmpty())PacketDistributor.sendToServer(new AddonPackets.Unload(menu.containerId,selected.copyWithCount(1),hasShiftDown()));}).withTextColor(0xff5ed0a0);
        unload.withTooltip(tr("unload_hint"));addComponent(unload);
        addComponent(new LabelWidget(246,25,27,9,tr("load")).withDarkColor());addComponent(new ItemSlotWidget(AddonMenu.LOAD_X,AddonMenu.LOAD_Y).withTooltip(tr("load_hint")));
        addComponent(new LabelWidget(246,100,27,9,tr("retrieve")).withDarkColor());addComponent(new ItemSlotWidget(AddonMenu.LOAD_X,AddonMenu.UNLOAD_Y).withTooltip(tr("retrieve_hint")));
        capacity=new LabelWidget(8,139,155,10,Component.empty()).withDarkColor();addComponent(capacity);
        status=new LabelWidget(168,139,100,10,Component.empty()).withDarkColor();addComponent(status);
        addComponent(new LabelWidget(AddonMenu.INVENTORY_X,150,162,10,Component.translatable("container.inventory")).withDarkColor());
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addComponent(new ItemSlotWidget(AddonMenu.INVENTORY_X+x*18,AddonMenu.INVENTORY_Y+y*18));
        for(int x=0;x<9;x++)addComponent(new ItemSlotWidget(AddonMenu.INVENTORY_X+x*18,AddonMenu.INVENTORY_Y+58));
        snapshot=List.of();refreshList();
    }
    private void refreshList(){var current=new ArrayList<ItemStack>();boolean changed=snapshot.size()!=Processor.MAX_ADDONS;
        for(int i=0;i<Processor.MAX_ADDONS;i++){var stack=menu.processor.inventory.getItem(Processor.ADDON_START+i);current.add(stack.copy());if(!changed&&!ItemStack.matches(snapshot.get(i),stack))changed=true;}
        if(!changed)return;snapshot=current;var entries=new ArrayList<Entry>();
        for(var stack:current){if(stack.isEmpty())continue;int match=-1;for(int i=0;i<entries.size();i++)if(ItemStack.isSameItemSameComponents(entries.get(i).stack,stack)){match=i;break;}
            if(match<0)entries.add(new Entry(stack.copyWithCount(1),stack.getCount()));else {var old=entries.get(match);entries.set(match,new Entry(old.stack,old.count+stack.getCount()));}}
        list.getChildren().clear();int y=0;
        if(entries.isEmpty())list.addChild(new LabelWidget(4,4,140,10,tr("no_addons")));
        for(var entry:entries){list.addChild(new Row(y,entry));y+=22;}
        list.setContentDimensions(146,Math.max(16,y));
        if(!selected.isEmpty()&&entries.stream().noneMatch(e->ItemStack.isSameItemSameComponents(e.stack,selected)))selected=ItemStack.EMPTY;
    }
    private final class Row extends UIComponent {
        private final Entry entry;
        Row(int y,Entry entry){super(0,y,146,21);this.entry=entry;withTooltip(entry.stack.getHoverName(),tr("installed_count",entry.count));}
        @Override public boolean handleClick(double x,double y,int button){if(button!=0)return false;selected=entry.stack.copyWithCount(1);return true;}
        @Override protected void renderContent(GuiGraphics g,int mouseX,int mouseY,float delta){boolean active=ItemStack.isSameItemSameComponents(selected,entry.stack);g.fill(x,y,x+width,y+height,active?0xff344c49:0xff42474b);
            g.renderItem(entry.stack,x+2,y+2);String name=Minecraft.getInstance().font.plainSubstrByWidth(entry.stack.getHoverName().getString(),94);
            g.drawString(font,name,x+22,y+6,active?0xff75e5bb:0xffeeeeee,false);String count="×"+entry.count;g.drawString(font,count,x+143-font.width(count),y+6,0xffeeeeee,false);}
    }
    @Override protected void containerTick(){super.containerTick();for(var component:components)component.tick();refreshList();
        var p=menu.processor;capacity.setText(tr("capacity_tier",p.tier,p.addonCount,p.addonSlots()));
        selection.setText(selected.isEmpty()?tr("not_selected"):selected.getHoverName());selectedIcon.setStack(selected);
        String loadStatus=p.loadStatus();Component issue=loadStatus.isEmpty()?Component.empty():tr(loadStatus);
        boolean canUnload=!selected.isEmpty()&&menu.getCarried().isEmpty()&&!minecraft.player.isSpectator();
        if(canUnload){var out=p.inventory.getItem(Processor.UNLOAD_SLOT);
            if(!out.isEmpty()&&(!ItemStack.isSameItemSameComponents(out,selected)||out.getCount()>=selected.getMaxStackSize())){canUnload=false;issue=tr("retrieve_first");}
            else {boolean removable=false;for(int i=Processor.ADDON_START;i<Processor.ADDON_END;i++)if(ItemStack.isSameItemSameComponents(p.inventory.getItem(i),selected)&&p.canRemoveAddon(i)){removable=true;break;}
                if(!removable){canUnload=false;issue=tr(selected.is(Content.FLUID_CAPACITY.get())?"drain_for_capacity":"empty_fluid");}}
        }
        unload.setActive(canUnload);back.setActive(menu.getCarried().isEmpty());status.setText(issue);status.withTooltip(issue);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){super.render(graphics,mouseX,mouseY,partial);
        if(list!=null&&list.isMouseOver(mouseX-leftPos,mouseY-topPos)){var row=list.getTopmostHovered(mouseX-leftPos,mouseY-topPos);
            if(row!=null&&row.hasTooltip())graphics.renderComponentTooltip(font,row.getTooltip(),mouseX,mouseY);}
    }
}
