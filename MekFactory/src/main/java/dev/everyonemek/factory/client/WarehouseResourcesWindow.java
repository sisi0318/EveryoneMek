package dev.everyonemek.factory.client;

import dev.everyonemek.factory.*;
import java.util.ArrayList;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class WarehouseResourcesWindow extends GuiWindow {
    private final boolean chemicals;
    public WarehouseResourcesWindow(IGuiWrapper gui,WarehouseMenu menu,boolean chemicals){
        super(gui,-12,20,200,126,WindowType.UNSPECIFIED);this.chemicals=chemicals;
        addChild(new GuiInnerScreen(gui,relativeX+8,relativeY+25,184,88,()->{
            var lines=new ArrayList<Component>();
            for(int i=0;i<Buffers.TANKS;i++){
                Component name;long amount;
                if(chemicals){var stack=menu.stock.chemicals[i];name=stack.isEmpty()?Content.text("empty"):stack.getTextComponent();amount=stack.getAmount();}
                else{var stack=menu.stock.fluids[i];name=stack.isEmpty()?Content.text("empty"):Component.translatable(stack.getDescriptionId());amount=stack.getAmount();}
                lines.add(name);lines.add(Content.text(chemicals?"tank_chemical_amount":"tank_fluid_amount",amount,menu.tankCapacity));
            }return lines;
        }));
    }
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,Content.text(chemicals?"chemicals":"fluids"),5);}
}
