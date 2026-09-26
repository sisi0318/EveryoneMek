package dev.everyonemek.gravity.expansion;
import java.util.*;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.scroll.GuiTextScrollList;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Binding, selection details and refresh in one native Mek window. */
final class ModuleSourceWindow extends GuiWindow {
    private final ModuleScreen screen;private final ModuleMenu menu;private final OrbitalModule tile;private final GuiTextScrollList list;
    private List<ModuleSourceNetwork.Source> sources=List.of();private String feedback="source_scanning";
    ModuleSourceWindow(ModuleScreen screen,ModuleMenu menu){super(screen,5,6,220,226,WindowType.UNSPECIFIED);this.screen=screen;this.menu=menu;tile=menu.getTileEntity();
        addChild(new GuiInnerScreen(screen,relativeX+10,relativeY+24,200,22,()->List.of(ModuleContent.text("source_current",tile.sourceBound&&tile.source!=null?tile.source.pos().toShortString():"—"),ModuleContent.text("range_hint",ModuleConfig.RANGE.get()))).spacing(0));
        list=addChild(new GuiTextScrollList(screen,relativeX+10,relativeY+52,200,70));
        addChild(new GuiInnerScreen(screen,relativeX+10,relativeY+127,200,42,()->{var selected=selected();return selected==null?List.of(ModuleContent.text(sources.isEmpty()?"source_empty":"source_choose")):List.of(name(selected),Component.literal(selected.pos().toShortString()),ModuleContent.text(selected.hot()?"source_running":selected.formed()?"source_stopped":"source_unformed"),ModuleContent.text("source_available",dev.everyonemek.gravity.client.ReactorScreen.fe(selected.available())));}).spacing(0));
        addChild(new MekanismButton(screen,relativeX+10,relativeY+175,64,16,ModuleContent.text("source_bind"),(b,x,y)->{var source=selected();if(source!=null)request(1,source.pos());return true;}){{active=false;}@Override public void tick(){super.tick();active=selected()!=null;}});
        addChild(new MekanismButton(screen,relativeX+78,relativeY+175,64,16,ModuleContent.text("unlink_power"),(b,x,y)->{request(2,null);return true;}){{active=false;}@Override public void tick(){super.tick();active=tile.sourceBound;}});
        addChild(new MekanismButton(screen,relativeX+146,relativeY+175,64,16,ModuleContent.text("source_refresh"),(b,x,y)->{request(0,null);return true;}));
        addChild(new GuiInnerScreen(screen,relativeX+10,relativeY+197,200,20,()->List.of(ModuleContent.text(feedback))));request(0,null);
    }
    private static Component name(ModuleSourceNetwork.Source s){return s.name().isEmpty()?Component.translatable(s.solar()?"block.mekgravity.solar_controller":"block.mekgravity.reactor"):Component.literal(s.name());}
    private ModuleSourceNetwork.Source selected(){int i=list.getSelection();return i>=0&&i<sources.size()?sources.get(i):null;}
    private void request(int operation,net.minecraft.core.BlockPos source){PacketDistributor.sendToServer(new ModuleSourceNetwork.Action(menu.containerId,menu.nodeSession,operation,source));}
    void accept(ModuleSourceNetwork.Snapshot packet){if(packet.menu()!=menu.containerId||packet.session()!=menu.nodeSession)return;sources=packet.sources();feedback=packet.feedback();list.setText(sources.stream().map(s->name(s).getString()+" · "+Math.round(Math.sqrt(s.pos().distSqr(tile.getBlockPos())))+"m").toList());}
    @Override public void close(){super.close();screen.sourceWindowClosed();}
    @Override public void renderForeground(GuiGraphics g,int x,int y){super.renderForeground(g,x,y);drawTitleText(g,ModuleContent.text("source_select"),5);}
}
