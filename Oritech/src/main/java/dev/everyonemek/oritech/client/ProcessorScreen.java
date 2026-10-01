package dev.everyonemek.oritech.client;

import java.util.*;
import dev.everyonemek.oritech.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.*;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.api.screen.data.*;
import rearth.oritech.client.ui.OritechMachineScreen;

public final class ProcessorScreen extends OritechMachineScreen<ProcessorMenu> {
    private final ButtonWidget[] sideButtons=new ButtonWidget[6];
    private LabelWidget speed,efficiency,status;
    private ButtonWidget eject;
    public ProcessorScreen(ProcessorMenu menu,Inventory inv,Component title){super(menu,inv,title);}
    private static Component tr(String key,Object... args){return Component.translatable("gui.oritechmekanism."+key,args);}
    @Override protected void addExtraComponents(){
        // Keep native widgets while supplying the processor's already-adjusted progress and live capacity.
        components.removeIf(c->c instanceof ProgressDisplayWidget||c instanceof EnergyDisplayWidget);
        var p=menu.processor;
        var energy=new EnergyDisplayWidget(DisplayDataSource.CreateEnergy(p.energyStorage,p.getEnergyConfiguration(),p)){
            @Override protected long getTargetAmount(){return Math.clamp(p.energyStorage.amount,0,p.energyStorage.capacity);}
            @Override protected long getCapacity(){return p.energyStorage.capacity;}
            @Override protected float getFillRatio(){return (float)Math.clamp(displayedAmount/Math.max(1,p.energyStorage.capacity),0,1);}
        };energy.withSurface(OritechSurface.PANEL_INSET).withPadding(Insets.of(1));addComponent(energy);
        if(menu.layout!=Profiles.EMPTY){var arrow=p.getIndicatorConfiguration();var source=new DisplayDataSource(1000,()->(long)(p.getProgress()*1000),
                ()->p.profile()==Profiles.ATOMIC?tr("charge",p.paid,p.usage):tr("progress",p.progress,p.duration),new rearth.oritech.util.ScreenProvider.BarConfiguration(arrow.x(),arrow.y(),arrow.width(),arrow.height())){};
            addComponent(new ProgressDisplayWidget(source));}
        if(menu.layout==Profiles.REFINERY)for(int i=menu.layoutModules+1;i<3;i++){
            var blocker=new SurfaceWidget(92+27*i,6,21,74,OritechSurface.PANEL_DARK);blocker.withTooltip(Component.translatable("tooltip.oritech.refinery_module_missing")).withZIndex(1);addComponent(blocker);
        }
        var panel=new SurfaceWidget(-72,0,70,145);panel.withSurface(OritechSurface.PANEL);panel.withZIndex(-10);addComponent(panel);
        addComponent(new LabelWidget(-65,7,58,10,tr("host")).withAlignment(LabelWidget.Alignment.CENTER));
        addComponent(new ItemSlotWidget(-45,20).withTooltip(tr("host_hint")));
        addComponent(new LabelWidget(-65,41,58,10,tr("addons")).withAlignment(LabelWidget.Alignment.CENTER));
        for(int i=0;i<9;i++)addComponent(new ItemSlotWidget(-63+i%3*18,54+i/3*18).withTooltip(tr("addon_hint")));
        status=new LabelWidget(-65,112,58,26,tr("status."+menu.processor.status)).withWrap(true);addComponent(status);
    }
    @Override protected void addExtensionContent(List<UIComponent> content){
        content.add(new LabelWidget(0,0,60,10,tr("settings")).withAlignment(LabelWidget.Alignment.CENTER));
        speed=new LabelWidget(0,0,60,10,Component.empty());efficiency=new LabelWidget(0,0,60,10,Component.empty());content.add(speed);content.add(efficiency);
        for(int i=0;i<6;i++){final int action=i;var button=ButtonWidget.panel(0,0,60,14,Component.empty(),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action)).withTextColor(LabelWidget.DARK_TEXT);sideButtons[i]=button;content.add(button);}
        eject=ButtonWidget.panel(0,0,60,14,Component.empty(),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,6)).withTextColor(LabelWidget.DARK_TEXT);content.add(eject);
    }
    @Override protected void tickExtra(){var p=menu.processor;var a=p.getBaseAddonData();
        if(speed!=null)speed.setText(tr("speed",String.format(Locale.ROOT,"%.2f",1/a.speed())));
        if(efficiency!=null)efficiency.setText(tr("energy",String.format(Locale.ROOT,"%.2f",a.efficiency())));
        if(status!=null){status.setText(tr("status."+p.status));status.withTooltip(tr("status."+p.status));}
        for(int i=0;i<6;i++)if(sideButtons[i]!=null)sideButtons[i].setLabel(tr("side."+i).copy().append(": ").append(tr("mode."+p.sides[i])));
        if(eject!=null)eject.setLabel(tr(p.eject?"eject_on":"eject_off"));
    }
    @Override public List<Rect2i> getExclusionZones(){var list=new ArrayList<>(super.getExclusionZones());list.add(new Rect2i(leftPos-72,topPos,70,145));return list;}
    @Override public BlockState getTitleState(){return menu.layout.block().defaultBlockState();}
}
