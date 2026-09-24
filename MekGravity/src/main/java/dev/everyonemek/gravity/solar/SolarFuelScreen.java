package dev.everyonemek.gravity.solar;
import dev.everyonemek.gravity.*;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.slot.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class SolarFuelScreen extends GuiMekanism<SolarFuelMenu> {
    public SolarFuelScreen(SolarFuelMenu m,Inventory i,Component title){super(m,i,title);imageWidth=176;imageHeight=208;inventoryLabelX=8;inventoryLabelY=110;dynamicSlots=true;}
    @Override protected void addGuiElements(){super.addGuiElements();for(int i=0;i<18;i++)addRenderableWidget(new GuiSlot(SlotType.INPUT,this,7+i%9*18,32+i/9*18));}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);g.drawString(font,SolarContent.text("fuel_title"),8,22,titleTextColor(),false);drawScaledScrollingString(g,SolarContent.text("fuel_hint"),8,78,168,99,TextAlignment.LEFT,titleTextColor(),false,.8F,getTimeOpened());}
}
