package dev.everyonemek.factory.client;

import dev.everyonemek.factory.*;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.slot.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WarehouseScreen extends GuiMekanism<WarehouseMenu> {
    public WarehouseScreen(WarehouseMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=220;inventoryLabelX=8;inventoryLabelY=126;dynamicSlots=true;}
    @Override protected void addGuiElements(){super.addGuiElements();
        for(int i=0;i<WarehouseMenu.PAGE_SIZE;i++){final int n=i;
            addRenderableWidget(new GuiSlot(menu.output?SlotType.OUTPUT:SlotType.INPUT,this,7+i%9*18,29+i/9*18){
                @Override public void tick(){super.tick();visible=menu.page*WarehouseMenu.PAGE_SIZE+n<menu.visibleSlots;}
            });
        }
        for(int i=0;i<2;i++){final int action=i+1;addRenderableWidget(new MekanismButton(this,i==0?8:144,90,24,14,Component.literal(i==0?"<":">"),(b,x,y)->{
            if(menu.getCarried().isEmpty()){menu.awaitingPage=true;minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}return true;
        }));}
        addRenderableWidget(new MekanismButton(this,8,108,76,14,Content.text("fluids"),(b,x,y)->{addWindow(new WarehouseResourcesWindow(this,menu,false));return true;}));
        addRenderableWidget(new MekanismButton(this,92,108,76,14,Content.text("chemicals"),(b,x,y)->{addWindow(new WarehouseResourcesWindow(this,menu,true));return true;}));
    }
    @Override public boolean mouseClicked(double x,double y,int button){return menu.awaitingPage||super.mouseClicked(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){return menu.awaitingPage&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE||super.keyPressed(key,scan,modifiers);}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);
        g.drawString(font,Content.text("warehouse_slots",menu.visibleSlots),8,18,titleTextColor(),false);
        g.drawString(font,Content.text("page",menu.page+1,menu.pageCount),64,93,titleTextColor(),false);
    }
}
