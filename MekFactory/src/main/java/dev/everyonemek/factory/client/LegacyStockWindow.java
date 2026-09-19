package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
public final class LegacyStockWindow extends GuiWindow {
    public LegacyStockWindow(IGuiWrapper gui,FactoryMenu menu){super(gui,27,35,176,80,WindowType.UNSPECIFIED);
        for(int i=0;i<2;i++){final int action=80+i;addChild(new MekanismButton(gui,relativeX+8,relativeY+27+i*22,160,16,Content.text(i==0?"legacy_input":"legacy_output"),(b,x,y)->{Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}));}
    }
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,Content.text("legacy_stock"),5);}
}
