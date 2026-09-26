package dev.everyonemek.gravity.expansion;
import java.util.*;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Loaded membership index; no world-volume scans or chunk loading. */
public final class NodeNetwork {
    private static final class Index {final Set<NodeModule> nodes=Collections.newSetFromMap(new WeakHashMap<>());long tick=Long.MIN_VALUE;Map<UUID,List<java.lang.ref.WeakReference<NodeModule>>> groups=Map.of();}
    private static final Map<Level,Index> LOADED=new WeakHashMap<>();
    public static void add(NodeModule node){if(node.getLevel() instanceof ServerLevel level){var index=LOADED.computeIfAbsent(level,l->new Index());index.nodes.add(node);index.tick=Long.MIN_VALUE;}}
    public static void remove(NodeModule node){if(!(node.getLevel() instanceof ServerLevel))return;var index=LOADED.get(node.getLevel());if(index!=null){index.nodes.remove(node);index.tick=Long.MIN_VALUE;}}
    public static void changed(NodeModule node){if(!(node.getLevel() instanceof ServerLevel))return;var index=LOADED.get(node.getLevel());if(index!=null)index.tick=Long.MIN_VALUE;}
    public static UUID owner(NodeModule node){return IBlockSecurityUtils.INSTANCE.getOwnerUUID(node.getLevel(),node.getBlockPos(),node);}
    public static List<NodeModule> members(NodeModule node){if(!(node.getLevel() instanceof ServerLevel level)||node.frequency==null)return List.of();var entry=NodeFrequencies.get(level).get(node.frequency);if(entry==null||!entry.permits(owner(node)))return List.of();
        var index=LOADED.get(level);if(index==null)return List.of();if(index.tick!=level.getGameTime()){var groups=new HashMap<UUID,List<java.lang.ref.WeakReference<NodeModule>>>();index.nodes.stream().filter(n->n.frequency!=null&&!n.isRemoved()).sorted(Comparator.comparingLong(n->n.getBlockPos().asLong())).forEach(n->groups.computeIfAbsent(n.frequency,k->new ArrayList<>()).add(new java.lang.ref.WeakReference<>(n)));index.groups=groups;index.tick=level.getGameTime();}
        return index.groups.getOrDefault(node.frequency,List.of()).stream().map(java.lang.ref.WeakReference::get).filter(n->n!=null&&!n.isRemoved()&&node.frequency.equals(n.frequency)&&level.hasChunkAt(n.getBlockPos())&&level.getBlockEntity(n.getBlockPos())==n&&entry.permits(owner(n))).toList();
    }
    public static boolean reachable(NodeModule from,NodeModule to){return from!=to&&!from.isRemoved()&&!to.isRemoved()&&from.getLevel()==to.getLevel()&&from.frequency!=null&&from.frequency.equals(to.frequency)&&to.getLevel().hasChunkAt(to.getBlockPos())&&to.getLevel().getBlockEntity(to.getBlockPos())==to&&FieldLink.at(from.getLevel(),to.getBlockPos()).inRange(from.getLevel(),from.getBlockPos())&&IBlockSecurityUtils.INSTANCE.canAccess(owner(from),to.getLevel(),to.getBlockPos(),to);}
    public static FieldSource power(NodeModule node){FieldSource best=null;for(var member:members(node)){if(member.source==null||!member.source.inRange(node.getLevel(),member.getBlockPos())||!member.source.inRange(node.getLevel(),node.getBlockPos()))continue;var core=FieldSource.at(node.getLevel(),member.source.pos());if(core==null||!core.permitted(node)||!core.permitted(member))continue;if(best==null||core.hot()&&(!best.hot()||core.available()>best.available()))best=core;}return best;}
    private NodeNetwork(){}
}
