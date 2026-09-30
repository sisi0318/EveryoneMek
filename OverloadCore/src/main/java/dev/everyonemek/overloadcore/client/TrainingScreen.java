package dev.everyonemek.overloadcore.client;

import java.util.List;
import dev.everyonemek.overloadcore.CoreContent;
import dev.everyonemek.overloadcore.training.*;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.*;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TrainingScreen extends GuiMekanismTile<TrainingProjector,TrainingMenu>{
    private int resetPending,resetFeedback;
    public TrainingScreen(TrainingMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=230;imageHeight=270;inventoryLabelX=28;inventoryLabelY=174;dynamicSlots=true;}
    private Component state(){return CoreContent.text(tile.status==3&&tile.hits==0?"training.ready":"training.state_"+tile.status).withStyle(tile.status==3?ChatFormatting.GREEN:tile.status==0?ChatFormatting.GRAY:ChatFormatting.GOLD);}
    @Override protected void addGuiElements(){super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this,10,22,192,20,()->List.of(state())));
        addRenderableWidget(new GuiInnerScreen(this,10,47,93,43,()->List.of(CoreContent.text("training.last_label"),Component.literal(tile.hits==0?"—":TrainingReadout.number(tile.lastDamage)).withStyle(ChatFormatting.GREEN,ChatFormatting.BOLD))).textScale(1).spacing(5));
        addRenderableWidget(new GuiInnerScreen(this,109,47,93,43,()->List.of(CoreContent.text("training.dps_label"),Component.literal(TrainingReadout.number(tile.dps)).withStyle(ChatFormatting.AQUA,ChatFormatting.BOLD))).textScale(1).spacing(5).tooltip(()->List.of(CoreContent.text("training.dps_hint"))));
        addRenderableWidget(new GuiInnerScreen(this,10,95,192,43,()->List.of(CoreContent.text("training.cumulative",TrainingReadout.number(tile.totalDamage)),CoreContent.text("training.hits",TrainingReadout.count(tile.hits)),CoreContent.text("training.spent",EnergyDisplay.of(tile.spent).getTextComponent()))).spacing(1).tooltip(()->List.of(CoreContent.text("training.spent_hint"))));
        addRenderableWidget(new GuiVerticalPowerBar(this,new GuiBar.IBarInfoHandler(){public Component getTooltip(){return CoreContent.text("training.power",EnergyDisplay.of(tile.energy.getEnergy(),tile.energy.getMaxEnergy()).getTextComponent());}public double getLevel(){return tile.energy.getEnergy()/(double)tile.energy.getMaxEnergy();}},209,22,116));
        addRenderableWidget(new MekanismButton(this,10,144,102,20,CoreContent.text("training.start"),(b,x,y)->send(0)){@Override public void tick(){super.tick();setMessage(CoreContent.text(tile.enabled?"training.stop":"training.start"));}});
        addRenderableWidget(new MekanismButton(this,118,144,102,20,CoreContent.text("training.reset"),(b,x,y)->{resetPending=1;return send(1);}){
            @Override public void tick(){super.tick();active=resetPending==0&&(tile.hits>0||tile.spent>0);setMessage(CoreContent.text(resetFeedback>0?"training.reset_done":"training.reset"));}
        });
    }
    @Override public void containerTick(){super.containerTick();if(resetFeedback>0)resetFeedback--;
        if(resetPending>0){if(tile.hits==0&&tile.spent==0){resetPending=0;resetFeedback=30;}else if(++resetPending>40)resetPending=0;}
    }
    private boolean send(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){renderTitleText(g);renderInventoryText(g);}
}
