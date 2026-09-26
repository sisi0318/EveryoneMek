package dev.everyonemek.gravity.expansion;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.*;
public final class ModuleMenu extends MekanismTileContainer<OrbitalModule>{
    public static final java.util.List<String> STATES=java.util.List.of("unlinked","range","unloaded","missing","access","source_type","linked","output_full","tier","energy","ingredients","running","paused","cold","peer_missing","peer_paused","transferring","structure","monitoring","receiving");
    public ModuleMenu(int id,Inventory inventory,OrbitalModule tile){super(ModuleContent.MENU,id,inventory,tile);}
    @Override protected int getInventoryXOffset(){return 31;}
    @Override protected int getInventoryYOffset(){return 163;}
    @Override public boolean clickMenuButton(Player player,int action){return !player.level().isClientSide&&stillValid(player)&&tile.command(player,action);}
}
