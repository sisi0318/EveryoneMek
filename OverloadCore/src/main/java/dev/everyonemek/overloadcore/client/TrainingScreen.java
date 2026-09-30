package dev.everyonemek.overloadcore.client;

import java.util.List;
import dev.everyonemek.overloadcore.CoreContent;
import dev.everyonemek.overloadcore.training.*;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TrainingScreen extends GuiMekanismTile<TrainingProjector,TrainingMenu>{
    public TrainingScreen(TrainingMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=230;imageHeight=234;inventoryLabelX=28;inventoryLabelY=138;dynamicSlots=true;}
    private static String number(double value){return value>=1e9?String.format(java.util.Locale.ROOT,"%.2fG",value/1e9):value>=1e6?String.format(java.util.Locale.ROOT,"%.2fM",value/1e6):value>=1000?String.format(java.util.Locale.ROOT,"%.1fk",value/1000):String.format(java.util.Locale.ROOT,"%.1f",value);}
    @Override protected void addGuiElements(){super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this,10,22,192,76,()->List.of(CoreContent.text("training.state_"+tile.status),CoreContent.text("training.last",number(tile.lastDamage)),CoreContent.text("training.total",number(tile.totalDamage),tile.hits<1000?Integer.toString(tile.hits):number(tile.hits)),CoreContent.text("training.dps",number(tile.dps)),CoreContent.text("training.spent",EnergyDisplay.of(tile.spent).getTextComponent()))).spacing(1));
        addRenderableWidget(new GuiVerticalPowerBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return EnergyDisplay.of(tile.energy.getEnergy(),tile.energy.getMaxEnergy()).getTextComponent();}public double getLevel(){return tile.energy.getEnergy()/(double)tile.energy.getMaxEnergy();}},209,22,76));
        addRenderableWidget(new MekanismButton(this,10,108,102,20,CoreContent.text("training.start"),(b,x,y)->send(0)){@Override public void tick(){super.tick();setMessage(CoreContent.text(tile.enabled?"training.stop":"training.start"));}});
        addRenderableWidget(new MekanismButton(this,118,108,102,20,CoreContent.text("training.reset"),(b,x,y)->send(1)));
    }
    private boolean send(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){renderTitleText(g);renderInventoryText(g);}
}
