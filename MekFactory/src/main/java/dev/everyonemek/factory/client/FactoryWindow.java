package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import java.util.*;
import java.util.function.Supplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
public final class FactoryWindow extends GuiWindow {
    private final Controller c;private final int menu;private final boolean resources;private int view;
    public FactoryWindow(IGuiWrapper gui,Controller c,int menu,boolean resources){
        super(gui,26,24,230,resources?126:174,WindowType.UNSPECIFIED);this.c=c;this.menu=menu;this.resources=resources;
        if(resources){
            addChild(new GuiInnerScreen(gui,relativeX+8,relativeY+28,214,61,()->{
                var b=view>=2?c.outputs:c.inputs;var lines=new ArrayList<Component>();for(int i=0;i<Buffers.TANKS;i++){
                    if(view%2==0&&!b.fluids[i].isEmpty())lines.add(Component.translatable(b.fluids[i].getDescriptionId()).append(" "+b.fluids[i].getAmount()+" mB"));
                    if(view%2==1&&!b.chemicals[i].isEmpty())lines.add(b.chemicals[i].getTextComponent().copy().append(" "+b.chemicals[i].getAmount()));
                }if(lines.isEmpty())lines.add(Content.text("empty"));return lines;
            }));
            addChild(new MekanismButton(gui,relativeX+8,relativeY+98,214,16,Content.text("resource_view.0"),(b,x,y)->{view=(view+1)%4;b.setMessage(Content.text("resource_view."+view));return true;}));
        }else{
            for(int i=0;i<3;i++){final int axis=i;int y=28+i*22;button(8,y,24,()->Component.literal("-"),10+2*i);button(198,y,24,()->Component.literal("+"),11+2*i);
                addChild(new GuiInnerScreen(gui,relativeX+38,relativeY+y,154,18,()->List.of(Content.text("dimension."+axis,axis==0?c.sizeX:axis==1?c.sizeY:c.sizeZ))));}
            button(8,96,24,()->Component.literal("-"),20);button(198,96,24,()->Component.literal("+"),21);
            addChild(new GuiInnerScreen(gui,relativeX+38,relativeY+96,154,18,()->List.of(Content.text("limit",c.parallelLimit))));
            button(8,119,214,()->Content.text(c.rotaryReverse?"rotary_gas":"rotary_fluid"),22);
            button(8,145,102,()->Content.text("preview"),2);button(120,145,102,()->Content.text("build"),3);
        }
    }
    private void button(int x,int y,int w,Supplier<Component> text,int id){addChild(new MekanismButton(gui(),relativeX+x,relativeY+y,w,16,text.get(),(b,mx,my)->{int action=(id==20||id==21)&&net.minecraft.client.gui.screens.Screen.hasShiftDown()?id+3:id;Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu,action);return true;}){@Override public void tick(){super.tick();setMessage(text.get());}});}
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,Content.text(resources?"resources":"settings"),5);}
}
