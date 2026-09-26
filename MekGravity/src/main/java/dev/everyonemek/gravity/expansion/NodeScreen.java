package dev.everyonemek.gravity.expansion;
import java.util.*;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.scroll.GuiTextScrollList;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Frequency-first flow with Mek's actual side configuration and tank widgets. */
public final class NodeScreen extends ModuleScreen {
    private List<NodeFrequencies.Entry> frequencies=List.of();private boolean frequenciesLoaded;private String feedback="frequency_help";private FrequencyWindow frequencyWindow;
    public NodeScreen(ModuleMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=248;inventoryLabelY=152;}
    private NodeModule node(){return (NodeModule)tile;}
    void addNodeElements(){
        var ref=new java.util.concurrent.atomic.AtomicReference<GuiSideConfigurationTab<NodeModule>>();ref.set(new GuiSideConfigurationTab<>(this,node(),ref::get));addRenderableWidget(ref.get());
        final ResourceTab[] resources=new ResourceTab[1];resources[0]=new ResourceTab(()->resources[0]);addRenderableWidget(resources[0]);
        addRenderableWidget(new GuiInnerScreen(this,10,18,210,24,()->List.of(ModuleContent.text("frequency_current",node().frequencyName.isEmpty()?ModuleContent.text("frequency_none"):node().frequencyName),ModuleContent.text(tile.status))).spacing(0));
        addRenderableWidget(new MekanismButton(this,10,45,138,16,ModuleContent.text("frequency_select"),(b,x,y)->{request(0,null,"",false);if(frequencyWindow==null){frequencyWindow=new FrequencyWindow();addWindow(frequencyWindow);}return true;}));
        addRenderableWidget(new MekanismButton(this,154,45,66,16,ModuleContent.text("frequency_power_button"),(b,x,y)->send(7)));
        button(10,134,102,0,()->ModuleContent.text(tile.enabled?"pause":"enable"));button(118,134,102,1,()->ModuleContent.text(tile.autoEject?"eject_on":"eject_off"));
    }
    private void request(int operation,UUID id,String name,boolean shared){PacketDistributor.sendToServer(new NodeFrequencyNetwork.Action(menu.containerId,menu.nodeSession,tile.getBlockPos(),operation,id,name,shared));}
    public void receive(NodeFrequencyNetwork.Snapshot packet){if(packet.menu()!=menu.containerId||packet.session()!=menu.nodeSession||!packet.pos().equals(tile.getBlockPos()))return;frequencies=packet.entries();frequenciesLoaded=true;node().frequency=packet.current();if(packet.current()==null)node().frequencyName="";else frequencies.stream().filter(e->e.id().equals(packet.current())).findFirst().ifPresent(e->node().frequencyName=e.name());feedback=packet.feedback();}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){renderTitleText(g);renderInventoryText(g);g.drawString(font,ModuleContent.text("node_send"),12,67,titleTextColor(),false);g.drawString(font,ModuleContent.text("node_receive"),154,67,titleTextColor(),false);drawScaledScrollingString(g,ModuleContent.text("frequency_flow"),74,90,145,108,TextAlignment.CENTER,titleTextColor(),false,.8F,getTimeOpened());}
    private final class FrequencyWindow extends GuiWindow {
        private final GuiTextScrollList list;private List<NodeFrequencies.Entry> shown=List.of();private boolean shared,listedShared,autoScope=true;private List<NodeFrequencies.Entry> sourceEntries;
        FrequencyWindow(){super(NodeScreen.this,5,6,220,236,WindowType.UNSPECIFIED);
            addChild(new GuiInnerScreen(NodeScreen.this,relativeX+10,relativeY+24,200,20,()->List.of(ModuleContent.text("frequency_current",node().frequencyName.isEmpty()?ModuleContent.text("frequency_none"):node().frequencyName),ModuleContent.text("frequency_peers",node().peers))).spacing(0));
            addChild(new MekanismButton(NodeScreen.this,relativeX+10,relativeY+49,72,16,ModuleContent.text("frequency_private"),(b,x,y)->{shared=!shared;autoScope=false;return true;}){@Override public void tick(){super.tick();setMessage(ModuleContent.text(shared?"frequency_public":"frequency_private"));}});
            var name=addChild(new GuiTextField(NodeScreen.this,relativeX+10,relativeY+69,180,16));name.setMaxLength(32);name.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("frequency_name")));Runnable create=()->{if(!name.getText().isBlank())request(1,null,name.getText(),shared);};name.setEnterHandler(create);name.addCheckmarkButton(create);
            addChild(new MekanismButton(NodeScreen.this,relativeX+88,relativeY+49,122,16,ModuleContent.text("panel_refresh"),(b,x,y)->{request(0,null,"",false);return true;}));
            list=addChild(new GuiTextScrollList(NodeScreen.this,relativeX+10,relativeY+92,200,87));
            for(int i=0;i<3;i++){int action=i;addChild(new MekanismButton(NodeScreen.this,relativeX+10+i*68,relativeY+185,64,16,ModuleContent.text(new String[]{"frequency_join","frequency_leave","frequency_delete"}[i]),(b,x,y)->{
                var selected=selection();if(action==1)request(3,null,"",false);else if(selected!=null)request(action==0?2:4,selected.id(),"",false);return true;
            }){{active=false;}@Override public void tick(){super.tick();var entry=selection();active=action==1?node().frequency!=null:entry!=null&&(action==0||entry.owner().equals(minecraft.player.getUUID()));}});}
            addChild(new GuiInnerScreen(NodeScreen.this,relativeX+10,relativeY+207,200,27,()->List.of(ModuleContent.text(feedback),ModuleContent.text("frequency_create_hint"))).spacing(0));
        }
        private NodeFrequencies.Entry selection(){int i=list.getSelection();return i>=0&&i<shown.size()?shown.get(i):null;}
        @Override public void tick(){super.tick();if(autoScope&&frequenciesLoaded){frequencies.stream().filter(e->e.id().equals(node().frequency)).findFirst().ifPresent(e->shared=e.shared());autoScope=false;}if(sourceEntries!=frequencies||listedShared!=shared){sourceEntries=frequencies;listedShared=shared;shown=frequencies.stream().filter(e->e.shared()==shared).toList();list.setText(shown.stream().map(NodeFrequencies.Entry::name).toList());}}
        @Override public void close(){super.close();frequencyWindow=null;}
        @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,ModuleContent.text("frequency_select"),5);}
    }
}
