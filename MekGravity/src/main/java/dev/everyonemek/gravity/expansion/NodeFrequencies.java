package dev.everyonemek.gravity.expansion;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Named routes only. Cargo always remains in a real node's native persistent containers. */
public final class NodeFrequencies extends SavedData {
    public record Entry(UUID id,UUID owner,String name,boolean shared){
        public boolean permits(UUID user){return shared||owner.equals(user);}
    }
    private final Map<UUID,Entry> entries=new LinkedHashMap<>();
    public static NodeFrequencies get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(NodeFrequencies::new,NodeFrequencies::load,null),"mekgravity_node_frequencies");}
    public Entry get(UUID id){return entries.get(id);}
    public List<Entry> visible(UUID player){return entries.values().stream().filter(e->e.permits(player)).sorted(Comparator.comparing(Entry::name).thenComparing(Entry::id)).limit(128).toList();}
    public Entry create(UUID owner,String name,boolean shared){name=name.strip();if(name.isEmpty()||name.length()>32||name.codePoints().anyMatch(c->Character.isISOControl(c)||c=='§'))return null;
        for(var e:entries.values())if(e.shared==shared&&e.name.equals(name)&&(shared||e.owner.equals(owner)))return e;
        if(entries.size()>=2048||entries.values().stream().filter(e->e.owner.equals(owner)).count()>=64)return null;
        var entry=new Entry(UUID.randomUUID(),owner,name,shared);entries.put(entry.id,entry);setDirty();return entry;
    }
    public boolean remove(UUID id,UUID owner){var e=entries.get(id);if(e==null||!e.owner.equals(owner))return false;entries.remove(id);setDirty();return true;}
    private static NodeFrequencies load(CompoundTag tag,HolderLookup.Provider provider){var data=new NodeFrequencies();var list=tag.getList("frequencies",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(2048,list.size());i++){var t=list.getCompound(i);if(t.hasUUID("id")&&t.hasUUID("owner")){var name=t.getString("name");if(!name.isBlank()&&name.length()<=32){var e=new Entry(t.getUUID("id"),t.getUUID("owner"),name,t.getBoolean("public"));data.entries.put(e.id,e);}}}return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){var list=new ListTag();for(var e:entries.values()){var t=new CompoundTag();t.putUUID("id",e.id);t.putUUID("owner",e.owner);t.putString("name",e.name);t.putBoolean("public",e.shared);list.add(t);}tag.put("frequencies",list);return tag;}
}
