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
        super(gui,26,24,230,resources?126:152,WindowType.UNSPECIFIED);this.c=c;this.menu=menu;this.resources=resources;
        if(resources){
            addChild(new GuiInnerScreen(gui,relativeX+8,relativeY+28,214,61,()->{
                var b=view>=2?c.outputs:c.inputs;var lines=new ArrayList<Component>();for(int i=0;i<Buffers.TANKS;i++){
                    if(view%2==0&&!b.fluids[i].isEmpty())lines.add(Component.translatable(b.fluids[i].getDescriptionId()).append(" "+b.fluids[i].getAmount()+" mB"));
                    if(view%2==1&&!b.chemicals[i].isEmpty())lines.add(b.chemicals[i].getTextComponent().copy().append(" "+b.chemicals[i].getAmount()));
                }if(lines.isEmpty())lines.add(Content.text("empty"));return lines;
            }));
            addChild(new MekanismButton(gui,relativeX+8,relativeY+98,214,16,Content.text("resource_view.0"),(b,x,y)->{view=(view+1)%4;b.setMessage(Content.text("resource_view."+view));return true;}));
        }else{
            addChild(new GuiInnerScreen(gui,relativeX+8,relativeY+28,214,18,()->List.of(Content.text("dimensions",c.sizeX,c.sizeY,c.sizeZ))));
            button(8,51,24,()->Component.literal("-"),20);button(198,51,24,()->Component.literal("+"),21);
            addChild(new GuiInnerScreen(gui,relativeX+38,relativeY+51,154,18,()->List.of(Content.text("limit",c.parallelLimit))));
            button(8,74,214,()->Content.text(c.rotaryReverse?"rotary_gas":"rotary_fluid"),22);
            addChild(new GuiInnerScreen(gui,relativeX+8,relativeY+97,214,18,()->List.of(Content.text("power_hint"))));
            button(8,123,102,()->Content.text("preview"),2);button(120,123,102,()->Content.text("build"),3);
        }
    }
    private void button(int x,int y,int w,Supplier<Component> text,int id){addChild(new MekanismButton(gui(),relativeX+x,relativeY+y,w,16,text.get(),(b,mx,my)->{int action=(id==20||id==21)&&net.minecraft.client.gui.screens.Screen.hasShiftDown()?id+3:id;Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu,action);return true;}){@Override public void tick(){super.tick();setMessage(text.get());}});}
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,Content.text(resources?"resources":"settings"),5);}
}
