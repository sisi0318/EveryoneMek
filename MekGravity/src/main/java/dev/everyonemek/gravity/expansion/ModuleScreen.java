package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.client.ReactorScreen;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.lib.Color;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
public final class ModuleScreen extends GuiMekanismTile<OrbitalModule,ModuleMenu>{
    public ModuleScreen(ModuleMenu m,Inventory i,Component title){super(m,i,title);imageWidth=230;imageHeight=248;inventoryLabelX=30;inventoryLabelY=152;dynamicSlots=true;}
    private boolean send(int n){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,n);return true;}
    @Override protected void addGuiElements(){super.addGuiElements();
        if(tile.kind().cargo){
            addRenderableWidget(new GuiDynamicHorizontalRateBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return ModuleContent.text("progress",(int)(tile.fraction()*100));}public double getLevel(){return tile.fraction();}},76,51,64,Color.ColorFunction.scale(Color.rgbi(65,45,110),Color.rgbi(225,180,95))));
            addRenderableWidget(new GuiInnerScreen(this,10,96,210,27,()->List.of(ModuleContent.text(tile.status),ModuleContent.text(tile.kind()==ModuleKind.NODE?"transfer_energy":"paid",ReactorScreen.fe(tile.paidEnergy)))).spacing(0));
            button(10,130,102,0,()->ModuleContent.text(tile.enabled?"pause":"enable"));button(118,130,102,1,()->ModuleContent.text(tile.autoEject?"eject_on":"eject_off"));
        }else if(tile.kind()==ModuleKind.TUNER){
            addRenderableWidget(new GuiInnerScreen(this,10,27,210,36,()->List.of(ModuleContent.text(tile.status),ModuleContent.text("profile",ModuleContent.text("profile_"+tile.sourceProfile)),ModuleContent.text("cooldown",(tile.sourceCooldown+19)/20))).spacing(0));
            for(int i=0;i<3;i++){int n=i;button(10+i*71,70,68,10+i,()->ModuleContent.text("profile_"+n));}
            button(38,102,182,13,()->ModuleContent.text(tile.sourceBurst>0?"burst_left":"burst",(tile.sourceBurst+19)/20));button(10,130,210,0,()->ModuleContent.text(tile.enabled?"pause":"enable"));
        }else{
            addRenderableWidget(new GuiInnerScreen(this,10,27,210,70,()->List.of(ModuleContent.text(tile.status),ModuleContent.text("stored",ReactorScreen.fe(tile.sourceStored),ReactorScreen.fe(tile.sourceCapacity)),ModuleContent.text("net",ReactorScreen.fe(tile.sourceNet)),ModuleContent.text("exported",ReactorScreen.fe(tile.sourceOutput)),ModuleContent.text("signal",tile.signal))).spacing(0));
            button(10,104,210,3,()->ModuleContent.text("alarm_"+tile.alarm));button(10,130,102,0,()->ModuleContent.text(tile.enabled?"pause":"enable"));button(118,130,102,4,()->ModuleContent.text("threshold",tile.threshold));
        }
        final DetailTab[] ref=new DetailTab[1];ref[0]=new DetailTab(()->ref[0]);addRenderableWidget(ref[0]);
    }
    private void button(int x,int y,int w,int id,java.util.function.Supplier<Component> label){var button=addRenderableWidget(new MekanismButton(this,x,y,w,16,label.get(),(b,mx,my)->send(id)){@Override public void tick(){super.tick();setMessage(label.get());if(id==13)active=tile.sourceCooldown==0&&tile.sourceFormed&&tile.sourceStored<tile.sourceCapacity;}});if(id>=10&&id<=13)button.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text(id==13?"burst_hint":"tuning_"+(id-10))));}
    private final class DetailTab extends GuiWindowCreatorTab<OrbitalModule,DetailTab>{
        DetailTab(java.util.function.Supplier<DetailTab> self){super(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_TAB,"energy_info.png"),ModuleScreen.this,tile,-26,26,26,18,true,self);setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("details")));}
        @Override protected GuiWindow createWindow(SelectedWindowData data){return new Details();}
        @Override protected SelectedWindowData getNextWindowData(){return new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED);}
        @Override protected void colorTab(GuiGraphics g){mekanism.client.render.MekanismRenderer.color(g,mekanism.client.SpecialColors.TAB_CONFIGURATION);}
    }
    private final class Details extends GuiWindow{
        Details(){super(ModuleScreen.this,8,20,214,213,SelectedWindowData.WindowType.UNSPECIFIED);
            addChild(new GuiInnerScreen(ModuleScreen.this,relativeX+8,relativeY+26,198,146,()->List.of(
                ModuleContent.text("source_pos",tile.source==null?"—":tile.source.pos().toShortString()),
                ModuleContent.text("peer_pos",tile.peer==null?"—":tile.peer.pos().toShortString()),
                ModuleContent.text("stored",ReactorScreen.fe(tile.sourceStored),ReactorScreen.fe(tile.sourceCapacity)),
                ModuleContent.text("net",ReactorScreen.fe(tile.sourceNet)),ModuleContent.text("exported",ReactorScreen.fe(tile.sourceOutput)),
                ModuleContent.text("fuel",ReactorScreen.fe(tile.sourceFuel)),ModuleContent.text("spare",tile.sourceSpare),
                ModuleContent.text("profile",ModuleContent.text("profile_"+tile.sourceProfile)),ModuleContent.text("transferred",tile.transferred),
                ModuleContent.text("range_hint",ModuleConfig.RANGE.get()),ModuleContent.text("profile_hint"))).spacing(0));
            addChild(new MekanismButton(ModuleScreen.this,relativeX+8,relativeY+183,198,16,ModuleContent.text("unlink"),(b,x,y)->send(2)));}
        @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,ModuleContent.text("details"),5);}
    }
    @Override protected void renderSlotContents(GuiGraphics g,ItemStack stack,Slot slot,String count){if(slot.index<(tile.kind().cargo?18:1)&&count==null&&stack.getCount()>=1000)count=stack.getCount()/1000+"k";super.renderSlotContents(g,stack,slot,count);}
    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack){var lines=new ArrayList<>(super.getTooltipFromContainerItem(stack));if(hoveredSlot!=null&&hoveredSlot.index<(tile.kind().cargo?18:tile.kind()==ModuleKind.TUNER?1:0))lines.add(ModuleContent.text("stock",stack.getCount(),hoveredSlot.getMaxStackSize(stack)));return lines;}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);if(tile.kind().cargo){g.drawString(font,ModuleContent.text("input"),12,24,titleTextColor(),false);g.drawString(font,ModuleContent.text("output"),154,24,titleTextColor(),false);drawScaledScrollingString(g,ModuleContent.text("batch",tile.batch),74,69,145,87,TextAlignment.CENTER,titleTextColor(),false,.8F,getTimeOpened());}}
}
