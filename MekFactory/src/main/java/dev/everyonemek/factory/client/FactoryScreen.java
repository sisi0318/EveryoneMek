package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import java.util.*;
import java.util.function.Supplier;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.bar.GuiHorizontalRateBar;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class FactoryScreen extends GuiMekanismTile<Controller,FactoryMenu> {
    private PortConfigurationTab portTab;
    public FactoryScreen(FactoryMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=230;imageHeight=274;inventoryLabelX=34;inventoryLabelY=180;dynamicSlots=true;}
    private void button(int x,int y,int w,Supplier<Component> label,int id){addRenderableWidget(new MekanismButton(this,x,y,w,14,label.get(),(b,mx,my)->{minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);return true;}){@Override public void tick(){super.tick();setMessage(label.get());}});}
    private String amount(long joules){double fe=mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertTo(joules);return mekanism.common.util.text.TextUtils.format((long)fe);}
    private double progress(){return menu.progressRatio();}
    private Component product(){if(!menu.product.isEmpty())return menu.product.getHoverName();if(!menu.productFluid.isEmpty())return Component.translatable(menu.productFluid.getDescriptionId());if(!menu.productChemical.isEmpty())return menu.productChemical.getTextComponent();return Content.text("no_recipe");}
    @Override protected void addGuiElements(){super.addGuiElements();
        portTab=addRenderableWidget(new PortConfigurationTab(this,tile,menu.containerId,()->portTab));
        addRenderableWidget(new GuiVerticalPowerBar(this,tile.energy(),214,53,106));
        addRenderableWidget(new GuiInnerScreen(this,18,53,194,74,()->List.of(
              Content.text(menu.formed?"ready":"structure"),Content.text("current_recipe",product()),
              Content.text("batch",menu.jobUnits,menu.jobCount==0?0:menu.jobIndex+1,menu.jobCount),
              Content.text("parallel",tile.running,menu.availableParallel),
              Content.text("power",amount(tile.powerUsed)),Content.text("power_limit",amount(tile.structure.transfer)))));
        addRenderableWidget(new GuiHorizontalRateBar(this,new IBarInfoHandler(){
            public Component getTooltip(){return Content.text("recipe_progress",String.format(Locale.ROOT,"%.1f%%",progress()*100));}
            public double getLevel(){return progress();}
        },18,131));
        button(174,130,18,()->Component.literal("<"),70);button(194,130,18,()->Component.literal(">"),71);
        addRenderableWidget(new GuiInnerScreen(this,18,146,194,14,()->List.of(Content.text(tile.status))));
        addRenderableWidget(new MekanismButton(this,18,164,76,14,Content.text("settings"),(b,x,y)->{addWindow(new FactoryWindow(this,tile,menu.containerId));return true;}));
        addRenderableWidget(new MekanismButton(this,98,164,64,14,Content.text("legacy_stock"),(b,x,y)->{addWindow(new LegacyStockWindow(this,menu));return true;}){
            @Override public void tick(){super.tick();visible=menu.legacyInput||menu.legacyOutput;}
        });
        button(166,164,46,()->Content.text(tile.enabled?"pause":"resume"),1);
    }
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);
        g.drawString(font,Content.text("template"),18,18,titleTextColor(),false);
        g.drawString(font,font.plainSubstrByWidth((tile.template.getStack().isEmpty()?Content.text("machine"):tile.template.getStack().getHoverName()).getString(),166),42,35,titleTextColor(),false);
        g.drawString(font,Component.literal(String.format(Locale.ROOT,"%.1f%%",progress()*100)),103,131,titleTextColor(),false);
    }
}
