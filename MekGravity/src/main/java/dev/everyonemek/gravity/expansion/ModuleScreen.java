package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.client.ReactorScreen;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.gauge.*;
import mekanism.common.tile.component.config.DataType;
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
public class ModuleScreen extends GuiMekanismTile<OrbitalModule,ModuleMenu>{
    public static ModuleScreen create(ModuleMenu menu,Inventory inventory,Component title){return menu.getTileEntity() instanceof NodeModule?new NodeScreen(menu,inventory,title):new ModuleScreen(menu,inventory,title);}
    public ModuleScreen(ModuleMenu m,Inventory i,Component title){super(m,i,title);imageWidth=230;imageHeight=248;inventoryLabelX=30;inventoryLabelY=152;dynamicSlots=true;}
    protected boolean send(int n){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,n);return true;}
    @Override protected void addGuiElements(){super.addGuiElements();
        if(this instanceof NodeScreen nodeScreen){nodeScreen.addNodeElements();return;}
        if(tile.kind().cargo){
            if(tile.kind()!=ModuleKind.NODE)addRenderableWidget(new GuiDynamicHorizontalRateBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return ModuleContent.text("progress",(int)(tile.fraction()*100));}public double getLevel(){return tile.fraction();}},76,51,64,Color.ColorFunction.scale(Color.rgbi(65,45,110),Color.rgbi(225,180,95))));
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
        var panel=addRenderableWidget(new MekanismButton(this,-26,tile.kind()==ModuleKind.NODE?82:54,26,26,ModuleContent.text("panel_short"),(b,x,y)->send(7)));panel.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("panel_title")));
        final DetailTab[] ref=new DetailTab[1];ref[0]=new DetailTab(()->ref[0]);addRenderableWidget(ref[0]);if(tile.kind()==ModuleKind.NODE){final ResourceTab[] r=new ResourceTab[1];r[0]=new ResourceTab(()->r[0]);addRenderableWidget(r[0]);}
    }
    protected void button(int x,int y,int w,int id,java.util.function.Supplier<Component> label){var button=addRenderableWidget(new MekanismButton(this,x,y,w,16,label.get(),(b,mx,my)->send(id)){@Override public void tick(){super.tick();setMessage(label.get());if(id==13)active=tile.sourceCooldown==0&&tile.sourceFormed&&tile.sourceStored<tile.sourceCapacity;}});if(id>=10&&id<=13)button.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text(id==13?"burst_hint":"tuning_"+(id-10))));}
    private final class DetailTab extends GuiWindowCreatorTab<OrbitalModule,DetailTab>{
        DetailTab(java.util.function.Supplier<DetailTab> self){super(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_TAB,"energy_info.png"),ModuleScreen.this,tile,-26,26,26,18,true,self);setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("details")));}
        @Override protected GuiWindow createWindow(SelectedWindowData data){return new Details();}
        @Override protected SelectedWindowData getNextWindowData(){return new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED);}
        @Override protected void colorTab(GuiGraphics g){mekanism.client.render.MekanismRenderer.color(g,mekanism.client.SpecialColors.TAB_CONFIGURATION);}
    }
    private final class Details extends GuiWindow{
        Details(){super(ModuleScreen.this,8,20,214,213,SelectedWindowData.WindowType.UNSPECIFIED);
            addChild(new GuiInnerScreen(ModuleScreen.this,relativeX+8,relativeY+26,198,146,()->List.of(
                ModuleContent.text("source_pos",!tile.sourceBound||tile.source==null?"—":tile.source.pos().toShortString()),
                ModuleContent.text("stored",ReactorScreen.fe(tile.sourceStored),ReactorScreen.fe(tile.sourceCapacity)),
                ModuleContent.text("net",ReactorScreen.fe(tile.sourceNet)),ModuleContent.text("exported",ReactorScreen.fe(tile.sourceOutput)),
                ModuleContent.text("fuel",ReactorScreen.fe(tile.sourceFuel)),ModuleContent.text("spare",tile.sourceSpare),
                ModuleContent.text("profile",ModuleContent.text("profile_"+tile.sourceProfile)),ModuleContent.text("transferred",tile.transferred),
                ModuleContent.text("range_hint",ModuleConfig.RANGE.get()),ModuleContent.text("profile_hint"))).spacing(0));
            addChild(new MekanismButton(ModuleScreen.this,relativeX+8,relativeY+183,198,16,ModuleContent.text("unlink"),(b,x,y)->send(2)));}

        @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,ModuleContent.text("details"),5);}
    }
    protected final class ResourceTab extends GuiWindowCreatorTab<OrbitalModule,ResourceTab>{
        ResourceTab(java.util.function.Supplier<ResourceTab> self){super(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI,"configuration.png"),ModuleScreen.this,tile,-26,54,26,18,true,self);setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("resources")));}
        @Override protected GuiWindow createWindow(SelectedWindowData data){return new Resources(1);}
        @Override protected SelectedWindowData getNextWindowData(){return new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED);}
        @Override protected void colorTab(GuiGraphics g){mekanism.client.render.MekanismRenderer.color(g,mekanism.client.SpecialColors.TAB_CONFIGURATION);}
    }
    protected final class Resources extends GuiWindow{
        private final int page;
        Resources(int page){super(ModuleScreen.this,8,20,214,202,SelectedWindowData.WindowType.UNSPECIFIED);this.page=page;
            for(int i=0;i<4;i++){int target=i;addChild(new MekanismButton(ModuleScreen.this,relativeX+8+i*50,relativeY+27,48,16,ModuleContent.text("resource_"+i),(b,x,y)->{close();ModuleScreen.this.addWindow(new Resources(target));return true;}));}
            addChild(new MekanismButton(ModuleScreen.this,relativeX+8,relativeY+49,198,16,ModuleContent.text(tile.channel(page)?"channel_on":"channel_off"),(b,x,y)->send(20+page)){@Override public void tick(){super.tick();setMessage(ModuleContent.text(tile.channel(page)?"channel_on":"channel_off"));}});
            if(page==0)addChild(new GuiInnerScreen(ModuleScreen.this,relativeX+10,relativeY+86,194,70,()->List.of(ModuleContent.text("item_input",tile.inputs.stream().mapToInt(s->s.getCount()).sum()),ModuleContent.text("item_output",tile.outputs.stream().mapToInt(s->s.getCount()).sum()),ModuleContent.text("item_rate",tile.node.itemsMoved))).spacing(0));
            if(page==1){
                addChild(new GuiVerticalPowerBar(ModuleScreen.this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return ModuleContent.text("stored",ReactorScreen.fe(tile.node.energyIn.getEnergy()),ReactorScreen.fe(tile.node.energyIn.getMaxEnergy()));}public double getLevel(){return tile.node.energyIn.getEnergy()/(double)tile.node.energyIn.getMaxEnergy();}},relativeX+12,relativeY+85,76));
                addChild(new GuiVerticalPowerBar(ModuleScreen.this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return ModuleContent.text("stored",ReactorScreen.fe(tile.node.energyOut.getEnergy()),ReactorScreen.fe(tile.node.energyOut.getMaxEnergy()));}public double getLevel(){return tile.node.energyOut.getEnergy()/(double)tile.node.energyOut.getMaxEnergy();}},relativeX+184,relativeY+85,76));
                addChild(new GuiInnerScreen(ModuleScreen.this,relativeX+36,relativeY+90,140,64,()->List.of(ModuleContent.text("input_energy",ReactorScreen.fe(tile.node.energyIn.getEnergy())),ModuleContent.text("output_energy",ReactorScreen.fe(tile.node.energyOut.getEnergy())),ModuleContent.text("energy_rate",ReactorScreen.fe(tile.node.energyMoved)))).spacing(0));
            }
            if(page==2||page==3)for(int group=0;group<2;group++)for(int i=0;i<4;i++){boolean output=group==1;int index=i,x=relativeX+(output?120:10)+22*i,y=relativeY+85;var type=GaugeType.STANDARD.with(output?DataType.OUTPUT:DataType.INPUT);
                if(page==2)addChild(new GuiFluidGauge(()->(output?tile.node.fluidOut:tile.node.fluidIn).get(index),()->tile.getFluidTanks(null),type,ModuleScreen.this,x,y));
                else addChild(new GuiChemicalGauge(()->(output?tile.node.chemicalOut:tile.node.chemicalIn).get(index),()->tile.getChemicalTanks(null),type,ModuleScreen.this,x,y));
            }
            addChild(new GuiInnerScreen(ModuleScreen.this,relativeX+10,relativeY+170,194,23,()->List.of(page==2?ModuleContent.text("fluid_rate",tile.node.fluidMoved):page==3?ModuleContent.text("chemical_rate",tile.node.chemicalMoved):ModuleContent.text("node_io_hint"))).spacing(0));
        }
        @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,ModuleContent.text("resources"),5);if(page>0){g.drawString(minecraft.font,ModuleContent.text("node_send"),relativeX+11,relativeY+73,0x404040,false);g.drawString(minecraft.font,ModuleContent.text("node_receive"),relativeX+143,relativeY+73,0x404040,false);}}
    }
    @Override protected void renderSlotContents(GuiGraphics g,ItemStack stack,Slot slot,String count){if(slot.index<(tile.kind().cargo?18:1)&&count==null&&stack.getCount()>=1000)count=stack.getCount()/1000+"k";super.renderSlotContents(g,stack,slot,count);}
    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack){var lines=new ArrayList<>(super.getTooltipFromContainerItem(stack));if(hoveredSlot!=null&&hoveredSlot.index<(tile.kind().cargo?18:tile.kind()==ModuleKind.TUNER?1:0))lines.add(ModuleContent.text("stock",stack.getCount(),hoveredSlot.getMaxStackSize(stack)));return lines;}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){super.drawForegroundText(g,x,y);renderTitleText(g);renderInventoryText(g);if(tile.kind().cargo){g.drawString(font,ModuleContent.text("input"),12,24,titleTextColor(),false);g.drawString(font,ModuleContent.text("output"),154,24,titleTextColor(),false);if(tile.kind()==ModuleKind.NODE)g.drawString(font,Component.literal("→"),105,52,titleTextColor(),false);drawScaledScrollingString(g,tile.kind()==ModuleKind.NODE?ModuleContent.text("instant"):ModuleContent.text("batch",tile.batch),74,69,145,87,TextAlignment.CENTER,titleTextColor(),false,.8F,getTimeOpened());}}
}
