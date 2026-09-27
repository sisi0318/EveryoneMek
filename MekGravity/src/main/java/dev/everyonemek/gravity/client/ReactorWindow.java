package dev.everyonemek.gravity.client;
import java.util.*;
import dev.everyonemek.gravity.*;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
public final class ReactorWindow extends GuiWindow {
    public enum Page {ENERGY("energy_title"),COOLING("cooling_title"),STRUCTURE("structure_title"),LOAD("load_title");public final String key;Page(String k){key=k;}}
    private final Page page;
    public ReactorWindow(IGuiWrapper gui,Controller c,ReactorMenu menu,Page page){super(gui,23,22,184,page==Page.ENERGY?230:page==Page.STRUCTURE?207:138,WindowType.UNSPECIFIED);this.page=page;
        if(page==Page.LOAD){
            addChild(new PercentField(gui,relativeX+18,relativeY+43,116,16,100,()->c.load,()->true,n->send(menu,100+n)));
            addChild(new GuiInnerScreen(gui,relativeX+12,relativeY+76,160,30,()->List.of(Content.text("confirmed_load",c.load),Content.text("load_range"))));
        }else if(page==Page.STRUCTURE){
            addChild(new GuiInnerScreen(gui,relativeX+10,relativeY+28,164,48,()->List.of(Content.text("dimensions"),Content.text("coils",menu.coils,Content.text("grade."+menu.grade)),Content.text("output_ports",menu.outputs),menu.formed?Content.text("ready"):menu.errorPos==null?Content.text("structure"):Content.text("error_at",menu.errorPos.toShortString()))));
            addChild(new MekanismButton(gui,relativeX+10,relativeY+84,78,16,Content.text("preview"),(b,x,y)->send(menu,2)));
            addChild(new MekanismButton(gui,relativeX+96,relativeY+84,78,16,Content.text("build"),(b,x,y)->send(menu,3)));
            addChild(new MekanismButton(gui,relativeX+10,relativeY+109,164,16,Content.text(c.autoEject?"eject_on":"eject_off"),(b,x,y)->send(menu,4)){@Override public void tick(){super.tick();setMessage(Content.text(c.autoEject?"eject_on":"eject_off"));}});
            addChild(new MekanismButton(gui,relativeX+10,relativeY+133,164,16,Content.text("build_grade",Content.text("grade."+c.buildTier)),(b,x,y)->send(menu,20)){@Override public void tick(){super.tick();setMessage(Content.text("build_grade",Content.text("grade."+c.buildTier)));}});
            addChild(new MekanismButton(gui,relativeX+10,relativeY+157,164,16,Content.text("upgrade_assembly"),(b,x,y)->send(menu,21)));
            addChild(new MekanismButton(gui,relativeX+10,relativeY+181,164,16,Content.text("legacy_buffers"),(b,x,y)->{gui.addWindow(new ReactorWindow(gui,c,menu,Page.COOLING));return true;}){
                {refresh();}private void refresh(){visible=active=!c.cold.isEmpty()||!c.hot.isEmpty();}@Override public void tick(){super.tick();refresh();}
            });
        }else addChild(new GuiInnerScreen(gui,relativeX+10,relativeY+28,164,page==Page.ENERGY?124:93,()->page==Page.ENERGY?List.of(
              Content.text("gross",ReactorScreen.fe(c.gross)),Content.text("self_use",ReactorScreen.fe(c.selfUse)),
              Content.text("input_rate",ReactorScreen.fe(c.lastInput)),Content.text("input_limit",ReactorScreen.fe(menu.inputLimit)),
              Content.text("exported",ReactorScreen.fe(c.lastOutput)),Content.text("output_limit",ReactorScreen.fe(menu.outputLimit)),
              Content.text("stored",ReactorScreen.fe(c.stored),ReactorScreen.fe(menu.capacity)),Content.text("startup_cost",ReactorScreen.fe(menu.startup)),Content.text("reserve",ReactorScreen.fe(menu.reserve)),menu.reserveFuelCount<0?Content.text("stock_unknown"):Content.text("reserve_fuel",menu.reserveFuelCount),Content.text("reserve_fuel_energy",(menu.reserveFuelEnergy==Long.MAX_VALUE?"≥":"")+ReactorScreen.fe(menu.reserveFuelEnergy)))
              :List.of(Content.text("cold_amount",c.cold.getAmount(),menu.tankCapacity),Content.text("hot_amount",c.hot.getAmount(),menu.tankCapacity),Content.text("legacy_recovery_hint"))).spacing(0));
        if(page==Page.ENERGY){
            addChild(new GuiInnerScreen(gui,relativeX+10,relativeY+157,164,42,()->List.of(Content.text("port_selected",menu.portCount==0?0:menu.portIndex+1,menu.portCount,Content.text(menu.portIsOutput?"output":"input")),Content.text("port_location",menu.portPos==null?"—":menu.portPos.toShortString()),Content.text("port_rates",ReactorScreen.fe(menu.portInput),ReactorScreen.fe(menu.portOutput)))).spacing(0));
            addChild(new MekanismButton(gui,relativeX+10,relativeY+205,78,16,net.minecraft.network.chat.Component.literal("<"),(b,x,y)->send(menu,30)){{active=false;}@Override public void tick(){super.tick();active=menu.portCount>1;}}).setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(Content.text("port_previous")));
            addChild(new MekanismButton(gui,relativeX+96,relativeY+205,78,16,net.minecraft.network.chat.Component.literal(">"),(b,x,y)->send(menu,31)){{active=false;}@Override public void tick(){super.tick();active=menu.portCount>1;}}).setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(Content.text("port_next")));
        }
    }
    private static boolean send(ReactorMenu m,int action){Minecraft.getInstance().gameMode.handleInventoryButtonClick(m.containerId,action);return true;}
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,Content.text(page.key),5);}
}
