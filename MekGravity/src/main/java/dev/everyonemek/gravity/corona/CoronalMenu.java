package dev.everyonemek.gravity.corona;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.*;
public final class CoronalMenu extends MekanismTileContainer<CoronalMachine> {
    public static final java.util.List<String> STATES=java.util.List.of("unlinked","access","paused","cold","energy","tier_low","no_recipe","output_full","running");
    public CoronalMenu(int id,Inventory inv,CoronalMachine tile){super(CoronalContent.MENU,id,inv,tile);}
    @Override protected int getInventoryXOffset(){return 31;}
    @Override protected int getInventoryYOffset(){return 163;}
    @Override public boolean clickMenuButton(Player p,int action){if(p.level().isClientSide||!stillValid(p)||!mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(p,p.level(),tile.getBlockPos(),tile))return false;if(action==0)tile.enabled=!tile.enabled;else if(action==1)tile.autoEject=!tile.autoEject;else return false;tile.markForSave();return true;}
}
