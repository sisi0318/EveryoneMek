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
    private ModuleSourceWindow sourceWindow;
    protected void openSources(){if(sourceWindow==null){sourceWindow=new ModuleSourceWindow(this,menu);addWindow(sourceWindow);}}
    void sourceWindowClosed(){sourceWindow=null;}
    public void receiveSources(ModuleSourceNetwork.Snapshot packet){if(sourceWindow!=null)sourceWindow.accept(packet);}
    private boolean sourceControls(){return tile.sourceFormed&&menu.canControlSource&&tile.enabled;}
    @Override protected void addGuiElements(){super.addGuiElements();
        if(this instanceof NodeScreen nodeScreen){nodeScreen.addNodeElements();return;}
        if(tile instanceof ProcessModule configured){var ref=new java.util.concurrent.atomic.AtomicReference<mekanism.client.gui.element.tab.window.GuiSideConfigurationTab<ProcessModule>>();ref.set(new mekanism.client.gui.element.tab.window.GuiSideConfigurationTab<>(this,configured,ref::get));addRenderableWidget(ref.get());}
        addRenderableWidget(new GuiInnerScreen(this,10,18,210,24,()->List.of(ModuleContent.text("source_current",tile.sourceBound?Component.translatable(tile.sourceSolar?"block.mekgravity.solar_controller":"block.mekgravity.reactor"):ModuleContent.text("frequency_none")),ModuleContent.text(tile.status))).spacing(0));
        addRenderableWidget(new MekanismButton(this,10,45,138,16,ModuleContent.text("source_select"),(b,x,y)->{openSources();return true;}));
        button(154,45,66,0,()->ModuleContent.text(tile.enabled?"pause":"enable"));
        if(tile.kind().processor()){
            addRenderableWidget(new mekanism.client.gui.element.progress.GuiProgress(()->tile.fraction(),mekanism.client.gui.element.progress.ProgressType.LARGE_RIGHT,this,87,92));
            button(10,134,210,1,()->ModuleContent.text(tile.autoEject?"eject_on":"eject_off"));
        }else if(tile.kind()==ModuleKind.TUNER){
            for(int i=0;i<3;i++){int mode=i;var button=addRenderableWidget(new MekanismButton(this,10+i*71,80,68,16,ModuleContent.text("profile_"+i),(b,x,y)->send(10+mode)){{active=false;}@Override public void tick(){super.tick();active=sourceControls()&&tile.sourceProfile!=mode;}});button.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("tuning_"+mode)));}
            addRenderableWidget(new GuiInnerScreen(this,10,100,210,16,()->List.of(ModuleContent.text("tuning_"+tile.sourceProfile))).spacing(0));
            addRenderableWidget(new MekanismButton(this,38,120,182,16,ModuleContent.text("burst"),(b,x,y)->send(13)){{active=false;}@Override public void tick(){super.tick();active=sourceControls()&&tile.sourceHot&&tile.sourceCooldown==0&&tile.sourceStored<tile.sourceCapacity&&tile.sourceAvailable>0&&!tile.inputs.getFirst().isEmpty();setMessage(ModuleContent.text(tile.sourceBurst>0?"burst_left":"burst",(tile.sourceBurst+19)/20));}}).setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("burst_hint")));
        }else{
            addRenderableWidget(new GuiInnerScreen(this,10,67,210,27,()->List.of(ModuleContent.text("stored",ReactorScreen.fe(tile.sourceStored),ReactorScreen.fe(tile.sourceCapacity)),ModuleContent.text("signal",tile.signal))).spacing(0));
            for(int i=0;i<4;i++){int mode=i;addRenderableWidget(new MekanismButton(this,10+i%2*107,98+i/2*18,103,16,ModuleContent.text("alarm_short_"+i),(b,x,y)->send(30+mode)){@Override public void tick(){super.tick();active=tile.alarm!=mode;}});}
            var threshold=addRenderableWidget(new mekanism.client.gui.element.text.GuiTextField(this,142,136,60,14){@Override public void tick(){super.tick();active=tile.alarm>=2;setEditable(active);}});threshold.setMaxLength(3);threshold.setInputValidator(c->c>='0'&&c<='9');threshold.setText(Integer.toString(tile.threshold));Runnable save=()->{try{int value=Integer.parseInt(threshold.getText());if(value>=1&&value<=99)send(100+value);}catch(NumberFormatException ignored){}};threshold.setEnterHandler(save);threshold.addCheckmarkButton(save);
        }
    }
    protected void button(int x,int y,int w,int id,java.util.function.Supplier<Component> label){addRenderableWidget(new MekanismButton(this,x,y,w,16,label.get(),(b,mx,my)->send(id)){@Override public void tick(){super.tick();setMessage(label.get());}});}
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
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){renderTitleText(g);renderInventoryText(g);
        if(tile.kind().processor()){g.drawString(font,ModuleContent.text("input"),12,67,titleTextColor(),false);g.drawString(font,ModuleContent.text("output"),154,67,titleTextColor(),false);drawScaledScrollingString(g,ModuleContent.text("batch",tile.batch),74,116,145,130,TextAlignment.CENTER,titleTextColor(),false,.8F,getTimeOpened());}
        else if(tile.kind()==ModuleKind.TUNER){g.drawString(font,ModuleContent.text("profile",ModuleContent.text("profile_"+tile.sourceProfile)),10,67,titleTextColor(),false);g.drawString(font,ModuleContent.text("cooldown",(tile.sourceCooldown+19)/20),10,140,titleTextColor(),false);}
        else{g.drawString(font,ModuleContent.text("threshold",tile.threshold),10,139,titleTextColor(),false);}
    }
}
