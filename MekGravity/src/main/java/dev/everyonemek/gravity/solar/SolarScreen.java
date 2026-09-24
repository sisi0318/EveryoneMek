package dev.everyonemek.gravity.solar;
import java.util.*;
import dev.everyonemek.gravity.client.ReactorScreen;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.lib.Color;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class SolarScreen extends GuiMekanismTile<SolarController,SolarMenu> {
    public SolarScreen(SolarMenu m,Inventory i,Component title){super(m,i,title);imageWidth=240;imageHeight=262;inventoryLabelX=38;inventoryLabelY=164;dynamicSlots=true;}
    private Component duration(){long rate=menu.power*tile.load/100;if(tile.gross<=0||rate<=0)return SolarContent.text("paused");long seconds=tile.fuelRemaining/rate/20;return SolarContent.text("load_duration",seconds/3600,seconds/60%60,seconds%60);}
    @Override protected void addGuiElements(){super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this,8,28,212,24,()->List.of(SolarContent.text(tile.status))).spacing(0));
        addRenderableWidget(new GuiInnerScreen(this,8,58,102,34,()->List.of(SolarContent.text("net_label"),Component.literal(ReactorScreen.fe(tile.gross-tile.selfUse)+"/t"))).spacing(0));
        addRenderableWidget(new GuiInnerScreen(this,118,58,102,34,()->List.of(SolarContent.text("output_label"),Component.literal(ReactorScreen.fe(tile.lastOutput)+"/t"))).spacing(0));
        addRenderableWidget(new GuiVerticalPowerBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return dev.everyonemek.gravity.Content.text("stored",ReactorScreen.fe(tile.stored),ReactorScreen.fe(menu.capacity));}public double getLevel(){return Math.clamp(tile.stored/(double)Math.max(1,menu.capacity),0,1);}},225,28,132));
        addRenderableWidget(new GuiDynamicHorizontalRateBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return dev.everyonemek.gravity.Content.text("fuel_energy",ReactorScreen.fe(tile.fuelRemaining));}public double getLevel(){return Math.clamp(tile.fuelRemaining/(double)Math.max(1,tile.fuelTotal),0,1);}},8,110,212,Color.ColorFunction.scale(Color.rgbi(115,50,15),Color.rgbi(255,201,80))));
        addRenderableWidget(new MekanismButton(this,8,143,102,16,SolarContent.text(tile.enabled?"stop":"ignite"),(b,x,y)->send(1)){@Override public void tick(){super.tick();setMessage(SolarContent.text(tile.enabled?"stop":"ignite"));}});
        addRenderableWidget(new MekanismButton(this,118,143,102,16,SolarContent.text("load",tile.load),(b,x,y)->{addWindow(new SolarWindow(SolarScreen.this,tile,menu,3));return true;}){@Override public void tick(){super.tick();setMessage(SolarContent.text("load",tile.load));}});
        for(int page=0;page<3;page++){int which=page;final Tab[] ref=new Tab[1];ref[0]=new Tab(which,()->ref[0]);addRenderableWidget(ref[0]);}
    }
    private boolean send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);return true;}
    private final class Tab extends GuiWindowCreatorTab<SolarController,Tab>{private final int page;
        Tab(int page,java.util.function.Supplier<Tab> self){super(MekanismUtils.getResource(page==1?MekanismUtils.ResourceType.GUI:MekanismUtils.ResourceType.GUI_TAB,page==1?"configuration.png":"energy_info.png"),SolarScreen.this,tile,-26,26+page*28,26,18,true,self);this.page=page;setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(SolarContent.text("page_"+page)));}
        @Override protected GuiWindow createWindow(SelectedWindowData d){return new SolarWindow(SolarScreen.this,tile,menu,page);}
        @Override protected SelectedWindowData getNextWindowData(){return new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED);}
        @Override protected void colorTab(GuiGraphics g){mekanism.client.render.MekanismRenderer.color(g,mekanism.client.SpecialColors.TAB_CONFIGURATION);}
    }
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);g.drawString(font,SolarContent.text("fuel_reserve"),8,98,titleTextColor(),false);drawScaledScrollingString(g,tile.ignited?duration():SolarContent.text("charging_amount",ReactorScreen.fe(tile.stored),ReactorScreen.fe(menu.startup+menu.reserve)),8,122,220,136,TextAlignment.LEFT,titleTextColor(),false,.8F,getTimeOpened());}
}
