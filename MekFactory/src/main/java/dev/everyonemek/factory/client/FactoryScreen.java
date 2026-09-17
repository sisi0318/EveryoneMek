package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import java.util.*;
import java.util.function.Supplier;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.slot.*;
import mekanism.client.gui.element.progress.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class FactoryScreen extends GuiMekanismTile<Controller,FactoryMenu> {
    private PortConfigurationTab portTab;
    public FactoryScreen(FactoryMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=244;imageHeight=244;inventoryLabelX=41;inventoryLabelY=151;dynamicSlots=true;}
    private void button(int x,int y,int w,Supplier<Component> label,int id){addRenderableWidget(new MekanismButton(this,x,y,w,14,label.get(),(b,mx,my)->{if(id>=30&&id<=33){if(!menu.getCarried().isEmpty())return false;menu.awaitingPage=true;}minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);return true;}){@Override public void tick(){super.tick();setMessage(label.get());}});}
    @Override public boolean mouseClicked(double x,double y,int button){return menu.awaitingPage||super.mouseClicked(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){return menu.awaitingPage&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE||super.keyPressed(key,scan,modifiers);}
    @Override protected void addGuiElements(){super.addGuiElements();
        portTab=addRenderableWidget(new PortConfigurationTab(this,tile,menu.containerId,()->portTab));
        addRenderableWidget(new GuiProgress(()->tile.progress/100.0,ProgressType.SMALL_RIGHT,this,107,63));
        addRenderableWidget(new GuiVerticalPowerBar(this,tile.energy(),230,44,54));
        for(boolean out:new boolean[]{false,true})for(int i=0;i<FactoryMenu.PAGE_SIZE;i++)addRenderableWidget(new GuiSlot(out?SlotType.OUTPUT:SlotType.INPUT,this,(out?171:17)+i%3*18,43+i/3*18));
        addRenderableWidget(new GuiInnerScreen(this,80,83,84,31,()->List.of(Content.text("parallel",tile.running,tile.parallel),Content.text("power",mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertTo(tile.powerUsed)))));
        button(18,102,24,()->Component.literal("<"),30);button(48,102,24,()->Component.literal(">"),31);
        button(172,102,24,()->Component.literal("<"),32);button(202,102,24,()->Component.literal(">"),33);
        addRenderableWidget(new GuiInnerScreen(this,18,119,208,14,()->List.of(Content.text(tile.status))));
        addRenderableWidget(new MekanismButton(this,18,135,82,14,Content.text("settings"),(b,x,y)->{addWindow(new FactoryWindow(this,tile,menu.containerId,false));return true;}));
        addRenderableWidget(new MekanismButton(this,104,135,64,14,Content.text("resources"),(b,x,y)->{addWindow(new FactoryWindow(this,tile,menu.containerId,true));return true;}));
        button(172,135,54,()->Content.text(tile.enabled?"pause":"resume"),1);
    }
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);
        g.drawString(font,Content.text("input",menu.inputPage+1),18,31,titleTextColor(),false);g.drawString(font,Content.text("output_page",menu.outputPage+1),172,31,titleTextColor(),false);
        g.drawString(font,Content.text("template"),104,21,titleTextColor(),false);
    }
}
