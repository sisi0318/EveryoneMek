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
    private FieldConnections(){}
}
