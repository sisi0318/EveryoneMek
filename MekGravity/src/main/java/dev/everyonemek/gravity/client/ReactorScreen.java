package dev.everyonemek.gravity.client;
import java.util.*;
import dev.everyonemek.gravity.*;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.gauge.*;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.lib.Color;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class ReactorScreen extends GuiMekanismTile<Controller,ReactorMenu> {
    public ReactorScreen(ReactorMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=230;imageHeight=244;inventoryLabelX=34;inventoryLabelY=146;dynamicSlots=true;}
    public static String fe(long joules){double n=EnergyUnit.FORGE_ENERGY.convertTo(joules);String[] suffix={"","k","M","G","T","P","E"};int i=0;while(n>=1000&&i<suffix.length-1){n/=1000;i++;}return String.format(Locale.ROOT,i==0?"%.0f %sFE":"%.2f %sFE",n,suffix[i]);}
    @Override protected void addGuiElements(){super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return Content.text("stored",fe(tile.stored),fe(menu.capacity));}public double getLevel(){return Math.clamp(tile.stored/(double)Math.max(1,menu.capacity),0,1);}},208,28,101));
        for(boolean heated:new boolean[]{false,true}){var tank=new Views.Chemical(tile,menu,heated);addRenderableWidget(new GuiChemicalGauge(()->tank,List::of,GaugeType.MEDIUM,this,heated?29:8,28){@Override public void onClick(double x,double y,int button){}@Override public int getScaledLevel(){return Math.min(height-2,super.getScaledLevel());}}.setLabel(Content.text(heated?"hot_sodium":"sodium")));}
        addRenderableWidget(new GuiInnerScreen(this,51,28,151,56,()->List.of(Content.text(tile.status),Content.text("net",fe(tile.gross-tile.selfUse)),Content.text("exported",fe(tile.lastOutput)),Content.text(tile.ignited?"field_ready":"field_waiting"))).spacing(0));
        addRenderableWidget(new GuiDynamicHorizontalRateBar(this,new GuiBar.IBarInfoHandler(){
            public Component getTooltip(){return Content.text("fuel_energy",fe(tile.fuelRemaining));}
            public double getLevel(){return Math.clamp(tile.fuelRemaining/(double)Math.max(1,tile.fuelTotal),0,1);}
        },8,101,194,Color.ColorFunction.scale(Color.rgbi(63,48,78),Color.rgbi(152,117,190))));
        addRenderableWidget(new MekanismButton(this,8,114,94,16,Content.text("load",tile.load),(b,x,y)->{addWindow(new ReactorWindow(this,tile,menu,ReactorWindow.Page.LOAD));return true;}){@Override public void tick(){super.tick();setMessage(Content.text("load",tile.load));}});
        addRenderableWidget(new MekanismButton(this,108,114,94,16,Content.text(tile.enabled?"stop":"start"),(b,x,y)->{minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true;}){@Override public void tick(){super.tick();setMessage(Content.text(tile.enabled?"stop":"start"));}});
        for(var page:List.of(ReactorWindow.Page.ENERGY,ReactorWindow.Page.COOLING,ReactorWindow.Page.STRUCTURE)){
            final ReactorTab[] holder=new ReactorTab[1];holder[0]=new ReactorTab(this,tile,menu,page,()->holder[0]);addRenderableWidget(holder[0]);
        }
    }
    @Override protected void drawForegroundText(GuiGraphics g,int mx,int my){super.drawForegroundText(g,mx,my);renderTitleText(g);renderInventoryText(g);g.drawString(font,Content.text("fuel_remaining"),8,89,titleTextColor(),false);}
}
