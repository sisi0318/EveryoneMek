package dev.everyonemek.overloadcore.training;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.*;
public final class TrainingMenu extends MekanismTileContainer<TrainingProjector> {
    public TrainingMenu(int id,Inventory inventory,TrainingProjector tile){super(TrainingContent.MENU,id,inventory,tile);}
    @Override protected int getInventoryXOffset(){return 29;}
    @Override protected int getInventoryYOffset(){return 149;}
    @Override public boolean clickMenuButton(Player player,int action){return !player.level().isClientSide&&stillValid(player)&&tile.command(player,action);}
}
