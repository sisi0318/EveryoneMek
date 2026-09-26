package dev.everyonemek.gravity.expansion;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.world.entity.player.Player;
/** Shared permissions and range rules. Callers additionally validate their tool/menu interaction context. */
public final class FieldConnections {
    public static boolean access(Player player,OrbitalModule module){return !module.isRemoved()&&player.level()==module.getLevel()&&module.getLevel().hasChunkAt(module.getBlockPos())&&IBlockSecurityUtils.INSTANCE.canAccess(player,module.getLevel(),module.getBlockPos(),module);}
    public static boolean canPower(Player player,FieldSource core,OrbitalModule module){return core!=null&&!core.tile().isRemoved()&&access(player,module)&&core.tile().getLevel()==module.getLevel()&&core.permitted(player)&&core.permitted(module)
        &&FieldLink.at(core.tile().getLevel(),core.tile().getBlockPos()).inRange(module.getLevel(),module.getBlockPos())
        &&!(module.kind()==ModuleKind.CAPTOR&&!core.solar()||module.kind()==ModuleKind.FORGE&&core.solar());}
    public static boolean power(Player player,FieldSource core,OrbitalModule module){if(!canPower(player,core,module))return false;module.source=FieldLink.at(module.getLevel(),core.tile().getBlockPos());module.markForSave();module.sendUpdatePacket();return true;}
    public static boolean canRoute(Player player,OrbitalModule sender,OrbitalModule receiver){return sender!=receiver&&sender.kind()==ModuleKind.NODE&&receiver.kind()==ModuleKind.NODE&&access(player,sender)&&access(player,receiver)
        &&FieldLink.at(receiver.getLevel(),receiver.getBlockPos()).inRange(sender.getLevel(),sender.getBlockPos())
        &&IBlockSecurityUtils.INSTANCE.canAccess(IBlockSecurityUtils.INSTANCE.getOwnerUUID(sender.getLevel(),sender.getBlockPos(),sender),receiver.getLevel(),receiver.getBlockPos(),receiver);}
    public static boolean route(Player player,OrbitalModule sender,OrbitalModule receiver){if(!canRoute(player,sender,receiver))return false;sender.peer=FieldLink.at(receiver.getLevel(),receiver.getBlockPos());receiver.receiverConfigured=true;sender.markForSave();receiver.markForSave();sender.sendUpdatePacket();receiver.sendUpdatePacket();return true;}
    private FieldConnections(){}
}
