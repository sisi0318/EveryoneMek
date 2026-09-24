package dev.everyonemek.gravity.solar;
import java.util.*;
import dev.everyonemek.gravity.Content;
import dev.everyonemek.gravity.client.ReactorScreen;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
public final class SolarWindow extends GuiWindow {
    private final int page;
    public SolarWindow(IGuiWrapper gui,SolarController c,SolarMenu m,int page){super(gui,18,22,204,page==0?175:page==1?183:page==2?146:120,WindowType.UNSPECIFIED);this.page=page;
        if(page==0)addChild(new GuiInnerScreen(gui,relativeX+10,relativeY+27,184,134,()->List.of(Content.text("gross",ReactorScreen.fe(c.gross)),Content.text("self_use",ReactorScreen.fe(c.selfUse)),Content.text("input_rate",ReactorScreen.fe(c.lastInput)),Content.text("input_limit",ReactorScreen.fe(m.inputLimit)),Content.text("exported",ReactorScreen.fe(c.lastOutput)),Content.text("output_limit",ReactorScreen.fe(m.outputLimit)),Content.text("stored",ReactorScreen.fe(c.stored),ReactorScreen.fe(m.capacity)),Content.text("startup_cost",ReactorScreen.fe(m.startup)),Content.text("reserve",ReactorScreen.fe(m.reserve)))).spacing(0));
        if(page==1){addChild(new GuiInnerScreen(gui,relativeX+10,relativeY+27,184,103,()->{
            var lines=new ArrayList<Component>();lines.add(SolarContent.text("size"));lines.add(SolarContent.text("constraint",ReactorScreen.fe(m.constraint)));lines.add(SolarContent.text("collector",ReactorScreen.fe(m.collector)));
            for(int i=0;i<4;i++)lines.add(SolarContent.text("wing_"+i,m.wings[i]<0?SolarContent.text("unknown"):Content.text("grade."+m.wings[i])));
            lines.add(m.errorPos==null?SolarContent.text("ready"):Content.text("error_at",m.errorPos.toShortString()));return lines;
        }).spacing(0));button(gui,m,10,139,88,"preview",2);button(gui,m,106,139,88,"build",3);}
        if(page==2){
            addChild(new MekanismButton(gui,relativeX+10,relativeY+28,184,16,SolarContent.text(c.automatic?"auto_on":"auto_off"),(b,x,y)->send(m,5)){@Override public void tick(){super.tick();setMessage(SolarContent.text(c.automatic?"auto_on":"auto_off"));}});
            addChild(new MekanismButton(gui,relativeX+10,relativeY+51,184,16,SolarContent.text(c.refill?"refill_on":"refill_off_button"),(b,x,y)->send(m,6)){@Override public void tick(){super.tick();setMessage(SolarContent.text(c.refill?"refill_on":"refill_off_button"));}});
            addChild(new MekanismButton(gui,relativeX+10,relativeY+74,184,16,Content.text(c.autoEject?"eject_on":"eject_off"),(b,x,y)->send(m,4)){@Override public void tick(){super.tick();setMessage(Content.text(c.autoEject?"eject_on":"eject_off"));}});
            addChild(new MekanismButton(gui,relativeX+10,relativeY+104,184,16,SolarContent.text("recover"),(b,x,y)->send(m,7)){@Override public void tick(){super.tick();active=!c.enabled&&c.fuelRemaining>0;}});
        }
        if(page==3){var field=addChild(new GuiTextField(gui,relativeX+18,relativeY+42,136,16));field.setMaxLength(3);field.setInputValidator(ch->ch>='0'&&ch<='9');field.setText(Integer.toString(c.load));Runnable apply=()->{try{int n=Integer.parseInt(field.getText());if(n>=1&&n<=100)send(m,100+n);}catch(NumberFormatException ignored){}};field.setEnterHandler(apply);field.addCheckmarkButton(apply);addChild(new GuiInnerScreen(gui,relativeX+10,relativeY+78,184,24,()->List.of(Content.text("confirmed_load",c.load))));}
    }
    private void button(IGuiWrapper gui,SolarMenu m,int x,int y,int width,String key,int id){addChild(new MekanismButton(gui,relativeX+x,relativeY+y,width,16,Content.text(key),(b,mx,my)->send(m,id)));}
    private static boolean send(SolarMenu m,int id){Minecraft.getInstance().gameMode.handleInventoryButtonClick(m.containerId,id);return true;}
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,SolarContent.text("page_"+page),5);}
}
