package dev.everyonemek.gravity.corona;
import java.util.List;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.common.lib.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class CoronalScreen extends GuiMekanismTile<CoronalMachine,CoronalMenu>{
    public CoronalScreen(CoronalMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=230;imageHeight=248;inventoryLabelX=30;inventoryLabelY=152;dynamicSlots=true;}
    @Override protected void addGuiElements(){super.addGuiElements();
        addRenderableWidget(new GuiDynamicHorizontalRateBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return CoronalContent.text("progress",(int)(tile.fraction()*100));}public double getLevel(){return tile.fraction();}},76,51,64,Color.ColorFunction.scale(Color.rgbi(125,55,10),Color.rgbi(255,205,85))));
        addRenderableWidget(new GuiInnerScreen(this,10,96,210,27,()->List.of(CoronalContent.text(tile.status),CoronalContent.text("paid",dev.everyonemek.gravity.client.ReactorScreen.fe(tile.paidEnergy)))).spacing(0));
        for(int i=0;i<2;i++){int action=i;addRenderableWidget(new MekanismButton(this,10+i*108,130,102,16,label(action),(b,x,y)->{minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}){@Override public void tick(){super.tick();setMessage(label(action));}});}
    }
    private Component label(int action){return CoronalContent.text(action==0?(tile.enabled?"stop":"start"):(tile.autoEject?"eject_on":"eject_off"));}
    @Override protected void renderSlotContents(GuiGraphics g,net.minecraft.world.item.ItemStack stack,net.minecraft.world.inventory.Slot slot,String count){
        if(slot.index<18&&count==null&&stack.getCount()>=1000)count=stack.getCount()/1000+"k";
        super.renderSlotContents(g,stack,slot,count);
    }
    @Override protected List<Component> getTooltipFromContainerItem(net.minecraft.world.item.ItemStack stack){var lines=new java.util.ArrayList<>(super.getTooltipFromContainerItem(stack));if(hoveredSlot!=null&&hoveredSlot.index<18)lines.add(CoronalContent.text("slot_stock",stack.getCount(),hoveredSlot.getMaxStackSize(stack)));return lines;}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);g.drawString(font,CoronalContent.text("input"),12,24,titleTextColor(),false);g.drawString(font,CoronalContent.text("output"),154,24,titleTextColor(),false);drawScaledScrollingString(g,CoronalContent.text("batch",tile.batch),74,69,145,87,TextAlignment.CENTER,titleTextColor(),false,.8F,getTimeOpened());}
}
