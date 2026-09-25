package dev.everyonemek.gravity.client;
import java.util.*;
import dev.everyonemek.gravity.*;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.common.lib.Color;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class ReactorScreen extends GuiMekanismTile<Controller,ReactorMenu> {
    public ReactorScreen(ReactorMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=230;imageHeight=244;inventoryLabelX=34;inventoryLabelY=146;dynamicSlots=true;}
    public static String fe(long joules){double n=EnergyUnit.FORGE_ENERGY.convertTo(joules);String[] suffix={"","k","M","G","T","P","E"};int i=0;while(n>=1000&&i<suffix.length-1){n/=1000;i++;}return String.format(Locale.ROOT,i==0?"%.0f %sFE":"%.2f %sFE",n,suffix[i]);}
    private List<Component> information(){
        if(tile.ignited)return List.of(Content.text(tile.status),Content.text("net",fe(tile.gross-tile.selfUse)),Content.text("exported",fe(tile.lastOutput)),Content.text(tile.gross>0?"field_ready":"field_inactive"));
        if(menu.capacity<=0)return List.of(Content.text(tile.status),Content.text("syncing"));
        long required=menu.startup+menu.reserve;
        return List.of(Content.text(tile.status),Content.text("charge_stored",fe(tile.stored)),Content.text("charge_required",fe(required)),Content.text(tile.stored<required?"excitation_hint":"field_waiting"));
    }
    @Override protected void addGuiElements(){super.addGuiElements();
        int barY=106,buttonY=122;
        addRenderableWidget(new GuiVerticalPowerBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return Content.text("stored",fe(tile.stored),fe(menu.capacity));}public double getLevel(){return Math.clamp(tile.stored/(double)Math.max(1,menu.capacity),0,1);}},208,28,buttonY+16-28));
        addRenderableWidget(new GuiInnerScreen(this,8,28,194,60,this::information).spacing(0));
        addRenderableWidget(new GuiDynamicHorizontalRateBar(this,new GuiBar.IBarInfoHandler(){
            public Component getTooltip(){return Content.text("fuel_energy",fe(tile.fuelRemaining));}
            public double getLevel(){return Math.clamp(tile.fuelRemaining/(double)Math.max(1,tile.fuelTotal),0,1);}
        },8,barY,194,Color.ColorFunction.scale(Color.rgbi(63,48,78),Color.rgbi(152,117,190))));
        addRenderableWidget(new MekanismButton(this,8,buttonY,94,16,Content.text("load",tile.load),(b,x,y)->{addWindow(new ReactorWindow(this,tile,menu,ReactorWindow.Page.LOAD));return true;}){@Override public void tick(){super.tick();setMessage(Content.text("load",tile.load));}});
        addRenderableWidget(new MekanismButton(this,108,buttonY,94,16,Content.text(tile.enabled?"stop":"start"),(b,x,y)->{minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true;}){@Override public void tick(){super.tick();setMessage(Content.text(tile.enabled?"stop":"start"));}});
        for(var page:List.of(ReactorWindow.Page.ENERGY,ReactorWindow.Page.STRUCTURE)){
            final ReactorTab[] holder=new ReactorTab[1];holder[0]=new ReactorTab(this,tile,menu,page,()->holder[0]);addRenderableWidget(holder[0]);
        }
    }
    @Override protected void drawForegroundText(GuiGraphics g,int mx,int my){super.drawForegroundText(g,mx,my);renderTitleText(g);renderInventoryText(g);g.drawString(font,Content.text("fuel_remaining"),8,94,titleTextColor(),false);drawScaledScrollingString(g,menu.reserveFuelCount<0?Content.text("stock_unknown"):Content.text("reserve_fuel",menu.reserveFuelCount),101,91,202,103,TextAlignment.RIGHT,titleTextColor(),false,.8F,getTimeOpened());}
}
