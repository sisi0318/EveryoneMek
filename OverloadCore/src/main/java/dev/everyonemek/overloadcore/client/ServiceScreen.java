package dev.everyonemek.overloadcore.client;

import java.util.*;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.gear.*;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.scroll.GuiTextScrollList;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServiceScreen extends GuiMekanism<ServiceMenu> {
    private int target;
    private ModuleList list;
    private List<GearUpgrade> shown = List.of();
    private List<String> labels = List.of();
    public ServiceScreen(ServiceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=230;imageHeight=272;inventoryLabelX=30;inventoryLabelY=174;}
    @Override protected void addGuiElements(){super.addGuiElements();
        for(int i=0;i<2;i++){int t=i;addRenderableWidget(new MekanismButton(this,10+108*i,20,102,16,CoreContent.text(i==0?"service.core":"service.ward"),(b,x,y)->{target=t;refresh();return true;}){{active=target!=t;}@Override public void tick(){super.tick();active=target!=t;}});}
        addRenderableWidget(new GuiSlot(SlotType.NORMAL,this,12,45));
        addRenderableWidget(new GuiSlot(SlotType.NORMAL,this,12,91));
        list=addRenderableWidget(new ModuleList());
        for(int i=0;i<3;i++){int action=i;addRenderableWidget(new MekanismButton(this,10+72*i,117,66,16,CoreContent.text("service.action_"+i),(b,x,y)->{var u=selection();if(u!=null)PacketDistributor.sendToServer(new GearMenus.Edit(menu.containerId,menu.session,target,u.ordinal(),action,ServiceMenu.identity(menu.view(target))));return true;}){{active=false;}@Override public void tick(){super.tick();var u=selection();var c=IModuleHelper.INSTANCE.getModuleContainer(menu.view(target));active=u!=null&&ServiceMenu.identity(menu.view(target))!=null&&c!=null&&(action==0?c.installedCount(EquipmentModules.get(u))<u.maximum&&menu.getSlot(0).getItem().is(CoreContent.UPGRADE_ITEMS.get(u)):c.installedCount(EquipmentModules.get(u))>0);if(action==2)setMessage(CoreContent.text(c!=null&&u!=null&&c.hasEnabled(EquipmentModules.get(u))?"service.disable":"service.enable"));}});}
        addRenderableWidget(new GuiInnerScreen(this,10,139,210,28,()->List.of(CoreContent.text("service."+ServiceMenu.STATES[Math.clamp(menu.status,0,ServiceMenu.STATES.length-1)]),CoreContent.text("service.power",mekanism.common.util.text.EnergyDisplay.of(menu.energy),mekanism.common.util.text.EnergyDisplay.of(menu.cost)))).spacing(0));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addRenderableWidget(new GuiSlot(SlotType.NORMAL,this,30+col*18,184+row*18));
        for(int col=0;col<9;col++)addRenderableWidget(new GuiSlot(SlotType.NORMAL,this,30+col*18,242));
        refresh();
    }
    private GearUpgrade selection(){int i=list.getSelection();return i>=0&&i<shown.size()?shown.get(i):null;}
    private void refresh(){if(list==null)return;var c=IModuleHelper.INSTANCE.getModuleContainer(menu.view(target));shown=Arrays.stream(GearUpgrade.values()).filter(u->(u.targets&(target==0?8:4))!=0).toList();
        var next=shown.stream().map(u->Component.translatable("module.overloadcore."+u.id).getString()+"  "+(c==null?0:c.installedCount(EquipmentModules.get(u)))+"/"+u.maximum+(c!=null&&c.hasEnabled(EquipmentModules.get(u))?" ✓":"")).toList();
        if(!labels.equals(next)){int previous=list.getSelection();labels=next;list.setText(next);if(!shown.isEmpty())list.select(Math.clamp(previous,0,shown.size()-1));}
        var selected=selection();if(selected!=null)list.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(Component.translatable("description.overloadcore."+selected.id)));
    }
    private final class ModuleList extends GuiTextScrollList { ModuleList(){super(ServiceScreen.this,42,44,178,68);} void select(int row){setSelected(row);} }
    @Override public void containerTick(){super.containerTick();refresh();}
    public void accept(GearMenus.Snapshot s){if(s.menu()!=menu.containerId||s.session()!=menu.session)return;menu.core=s.core();menu.ward=s.ward();menu.energy=s.energy();menu.cost=s.cost();menu.status=s.status();refresh();}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);drawScaledScrollingString(g,CoreContent.text("service.chip"),8,68,38,85,TextAlignment.CENTER,titleTextColor(),false,.7F,getTimeOpened());
        if(!menu.view(target).isEmpty())g.renderItem(menu.view(target),13,92);
    }
}
