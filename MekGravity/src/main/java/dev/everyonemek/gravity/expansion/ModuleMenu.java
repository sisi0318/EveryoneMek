package dev.everyonemek.gravity.expansion;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.*;
public final class ModuleMenu extends MekanismTileContainer<OrbitalModule>{
    public long nodeSession;long frequencyActionTick=Long.MIN_VALUE;int frequencyActions;
    public static final java.util.List<String> STATES=java.util.List.of("unlinked","range","unloaded","missing","access","source_type","linked","output_full","tier","energy","ingredients","running","paused","cold","transferring","structure","monitoring","receiving","resources_waiting","channels_off","frequency_missing","frequency_power","frequency_alone");
    public ModuleMenu(int id,Inventory inventory,OrbitalModule tile){super(ModuleContent.MENU,id,inventory,tile);if(tile instanceof NodeModule){if(inventory.player instanceof net.minecraft.server.level.ServerPlayer p)nodeSession=p.getRandom().nextLong();track(mekanism.common.inventory.container.sync.SyncableLong.create(()->nodeSession,v->nodeSession=v));}}
    @Override protected int getInventoryXOffset(){return 31;}
    @Override protected int getInventoryYOffset(){return 163;}
    @Override public boolean clickMenuButton(Player player,int action){return !player.level().isClientSide&&stillValid(player)&&tile.command(player,action);}
}
