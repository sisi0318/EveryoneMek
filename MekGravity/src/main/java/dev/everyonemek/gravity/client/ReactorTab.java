package dev.everyonemek.gravity.client;
import java.util.function.Supplier;
import dev.everyonemek.gravity.*;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
public final class ReactorTab extends GuiWindowCreatorTab<Controller,ReactorTab> {
    private final ReactorMenu menu;private final ReactorWindow.Page page;
    public ReactorTab(IGuiWrapper gui,Controller c,ReactorMenu menu,ReactorWindow.Page page,Supplier<ReactorTab> self){
        super(MekanismUtils.getResource(page==ReactorWindow.Page.STRUCTURE?ResourceType.GUI:ResourceType.GUI_TAB,page==ReactorWindow.Page.STRUCTURE?"configuration.png":page==ReactorWindow.Page.COOLING?"heat_info.png":"energy_info.png"),gui,c,-26,22+page.ordinal()*28,26,18,true,self);
        this.menu=menu;this.page=page;setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(Content.text(page.key)));
    }
    @Override protected GuiWindow createWindow(SelectedWindowData d){return new ReactorWindow(gui(),dataSource,menu,page);}
    @Override protected void colorTab(net.minecraft.client.gui.GuiGraphics g){mekanism.client.render.MekanismRenderer.color(g,mekanism.client.SpecialColors.TAB_CONFIGURATION);}
    @Override protected SelectedWindowData getNextWindowData(){return new SelectedWindowData(WindowType.UNSPECIFIED);}
}
