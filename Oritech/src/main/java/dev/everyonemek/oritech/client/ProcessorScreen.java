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
    private LabelWidget speed,efficiency,status,capacity;
    private ButtonWidget eject,addons;
    private final List<UIComponent> qualityWidgets=new ArrayList<>();
    private int shownTier;
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
        var panel=new SurfaceWidget(-72,0,70,99);panel.withSurface(OritechSurface.PANEL);panel.withZIndex(-10);addComponent(panel);
        addComponent(new LabelWidget(-38,7,34,10,tr("host")).withAlignment(LabelWidget.Alignment.CENTER).withDarkColor());
        addComponent(new ItemSlotWidget(ProcessorMenu.HOST_X,20).withTooltip(tr("host_hint")));
        qualityWidgets.clear();addQualityBadge();
        capacity=new LabelWidget(-65,47,58,10,Component.empty()).withAlignment(LabelWidget.Alignment.CENTER).withDarkColor();addComponent(capacity);
        status=new LabelWidget(-65,65,58,26,tr("status."+menu.processor.status)).withWrap(true).withDarkColor();addComponent(status);
    }
    private void addQualityBadge(){shownTier=menu.processor.tier;int rings=shownTier-1;
        if(rings==6){qualityTexture("ring_6");rings=5;}qualityTexture("center");for(int i=1;i<=rings;i++)qualityTexture("ring_"+i);
    }
    private void qualityTexture(String name){var icon=new TextureWidget(-67,15,25,25,rearth.oritech.Oritech.id("textures/gui/modular/machine_core/"+name+".png"),0,0,64,64,64,64);
        icon.withTooltip(tr("quality",menu.processor.tier,menu.processor.addonSlots()));qualityWidgets.add(icon);addComponent(icon);
    }
    @Override protected void addExtensionContent(List<UIComponent> content){
        content.add(new LabelWidget(0,0,60,10,Component.translatable("title.oritech.details")).withAlignment(LabelWidget.Alignment.CENTER));
        speed=new LabelWidget(0,0,60,10,Component.empty());efficiency=new LabelWidget(0,0,60,10,Component.empty());content.add(speed);content.add(efficiency);
        addons=ButtonWidget.panel(0,0,60,14,tr("addons"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,ProcessorMenu.OPEN_ADDONS)).withTextColor(LabelWidget.DARK_TEXT);
        addons.withTooltip(tr("manage_addons"));content.add(addons);
        eject=ButtonWidget.panel(0,0,60,14,Component.empty(),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,6)).withTextColor(LabelWidget.DARK_TEXT);content.add(eject);
    }
    @Override protected void tickExtra(){var p=menu.processor;var a=p.getBaseAddonData();
        if(speed!=null)speed.setText(tr("speed",String.format(Locale.ROOT,"%.2f",1/a.speed())));
        if(efficiency!=null)efficiency.setText(tr("energy",String.format(Locale.ROOT,"%.2f",a.efficiency())));
        if(status!=null){status.setText(tr("status."+p.status));status.withTooltip(tr("status."+p.status));}
        if(eject!=null)eject.setLabel(tr(p.eject?"eject_on":"eject_off"));
        if(capacity!=null)capacity.setText(tr("capacity",p.addonCount,p.addonSlots()));
        if(addons!=null)addons.setActive(menu.getCarried().isEmpty());
        if(shownTier!=p.tier){for(var widget:qualityWidgets)removeComponent(widget);qualityWidgets.clear();addQualityBadge();}
    }
    @Override public List<Rect2i> getExclusionZones(){var list=new ArrayList<>(super.getExclusionZones());list.add(new Rect2i(leftPos-72,topPos,70,99));return list;}
    @Override public BlockState getTitleState(){return menu.layout.block().defaultBlockState();}
}
