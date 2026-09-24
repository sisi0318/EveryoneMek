package dev.everyonemek.gravity.solar;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.*;
public final class SolarStructure {
    private static final Map<Level,Map<BlockPos,SolarController>> OWNERS=new WeakHashMap<>();
    private final Set<BlockPos> claimed=new HashSet<>();
    public final SolarController owner;public final List<SolarPart> parts=new ArrayList<>(),fuelHatches=new ArrayList<>(),ports=new ArrayList<>();
    public SolarPart seed;public boolean formed;public int tier,inputs,outputs;public final int[] wings={-1,-1,-1,-1};
    public String error="structure";public BlockPos errorPos;private boolean dirty=true,checking;
    public SolarStructure(SolarController owner){this.owner=owner;}
    public BlockPos at(int x,int y,int z){return owner.getBlockPos().relative(owner.getDirection().getClockWise(),x-4).above(y-1).relative(owner.getDirection().getOpposite(),z);}
    public boolean contains(BlockPos p){var a=at(0,0,0);var b=at(8,8,8);return p.getX()>=Math.min(a.getX(),b.getX())&&p.getX()<=Math.max(a.getX(),b.getX())&&p.getY()>=a.getY()&&p.getY()<=b.getY()&&p.getZ()>=Math.min(a.getZ(),b.getZ())&&p.getZ()<=Math.max(a.getZ(),b.getZ());}
    public boolean outward(BlockPos p,Direction side){return side!=null&&contains(p)&&!contains(p.relative(side));}
    public Direction direction(Direction d){return switch(d){case NORTH->owner.getDirection();case SOUTH->owner.getDirection().getOpposite();case EAST->owner.getDirection().getClockWise();case WEST->owner.getDirection().getCounterClockWise();default->d;};}
    public static void changed(Level level,BlockPos pos){if(level==null||level.isClientSide)return;var map=OWNERS.get(level);var c=map==null?null:map.get(pos);if(c!=null)c.structure.invalidate();}
    public static void unload(LevelEvent.Unload event){OWNERS.remove(event.getLevel());}
    public static void chunkUnload(ChunkEvent.Unload event){var map=OWNERS.get(event.getLevel());if(map!=null){var set=new HashSet<SolarController>();for(var entry:map.entrySet())if(new net.minecraft.world.level.ChunkPos(entry.getKey()).equals(event.getChunk().getPos()))set.add(entry.getValue());set.forEach(c->c.structure.invalidate());}}
    private void refreshCapabilities(){var l=owner.getLevel();for(var part:List.copyOf(ports))if(l.hasChunkAt(part.getBlockPos())&&!part.isRemoved()){l.invalidateCapabilities(part.getBlockPos());l.updateNeighborsAt(part.getBlockPos(),part.getBlockState().getBlock());}}
    public void invalidate(){boolean was=formed;dirty=true;formed=false;if(was){appearance(false);activity(false);refreshCapabilities();}}
    public void detach(){invalidate();var map=OWNERS.get(owner.getLevel());if(map!=null)for(var pos:claimed)map.remove(pos,owner);claimed.clear();}
    public boolean valid(){if(owner.getLevel()==null||owner.getLevel().isClientSide||owner.isRemoved()||checking)return false;return dirty?validate():formed;}
    private boolean fail(String error,BlockPos pos){this.error=error;errorPos=pos;return false;}
    public long constraintLimit(){return formed?SolarConfig.POWER[tier].get():0;}
    public long collectorLimit(){if(!formed)return 0;long sum=0;for(int wing:wings)sum+=SolarConfig.POWER[wing].get()/4;return sum;}
    public long power(){return Math.min(constraintLimit(),collectorLimit());}
    public boolean validate(){
        if(checking||owner.getLevel()==null||owner.isRemoved())return false;checking=true;dirty=false;formed=false;
        var l=owner.getLevel();var map=OWNERS.computeIfAbsent(l,k->new HashMap<>());for(var pos:claimed)map.remove(pos,owner);claimed.clear();
        parts.clear();fuelHatches.clear();ports.clear();seed=null;tier=3;inputs=outputs=0;Arrays.fill(wings,-1);
        try{
            for(int x=0;x<9;x++)for(int y=0;y<9;y++)for(int z=0;z<9;z++){
                if(!SolarLayout.footprint(x,z))continue;var pos=at(x,y,z);if(!l.hasChunkAt(pos))return fail("unloaded",pos);
                var other=map.get(pos);if(other!=null&&other!=owner&&!other.isRemoved())return fail("occupied",pos);map.put(pos,owner);claimed.add(pos);
                var slot=SolarLayout.get(x,y,z);if(slot!=null&&slot.kind()==null){if(l.getBlockEntity(pos)!=owner)return fail("component",pos);continue;}
                if(slot==null){if(!l.getBlockState(pos).isAir())return fail("interior",pos);continue;}
                if(!(l.getBlockEntity(pos) instanceof SolarPart p)||p.kind()!=slot.kind())return fail("component",pos);
                parts.add(p);if(p.master==null||!p.master.equals(owner.getBlockPos())){p.master=owner.getBlockPos();p.setChanged();}
                if(p.kind()==SolarBlock.Kind.FOCUS||p.kind()==SolarBlock.Kind.COLLECTOR){if(p.getBlockState().getValue(SolarBlock.FACING)!=direction(slot.face()))return fail("facing",pos);}
                if(p.kind()==SolarBlock.Kind.RING||p.kind()==SolarBlock.Kind.FOCUS)tier=Math.min(tier,p.tier());
                if(p.kind()==SolarBlock.Kind.COLLECTOR){int wing=x==0?0:x==8?1:z==0?2:3;if(wings[wing]>=0&&wings[wing]!=p.tier())return fail("wing_mixed",pos);wings[wing]=p.tier();}
                if(p.kind()==SolarBlock.Kind.FUEL){fuelHatches.add(p);ports.add(p);}
                if(p.kind()==SolarBlock.Kind.ENERGY){ports.add(p);if(p.output())outputs++;else inputs++;}
                if(p.kind()==SolarBlock.Kind.SEED)seed=p;
            }
            if(inputs==0||outputs==0)return fail("energy_ports",owner.getBlockPos());
            formed=true;error="ready";errorPos=null;return true;
        }finally{checking=false;appearance(formed);if(formed)refreshCapabilities();}
    }
    private void appearance(boolean assembled){var l=owner.getLevel();if(l==null||l.isClientSide)return;
        var positions=new HashSet<>(claimed);for(var slot:SolarLayout.SLOTS)positions.add(at(slot.x(),slot.y(),slot.z()));positions.add(owner.getBlockPos());
        for(var pos:positions){if(!l.hasChunkAt(pos))continue;var be=l.getBlockEntity(pos);if(be!=owner&&!(be instanceof SolarPart p&&owner.getBlockPos().equals(p.master)))continue;
            var state=l.getBlockState(pos);if(!state.hasProperty(SolarBlock.FORMED))continue;var next=state.setValue(SolarBlock.FORMED,assembled);
            if(!assembled&&next.hasProperty(SolarBlock.ACTIVE))next=next.setValue(SolarBlock.ACTIVE,false);
            if(assembled&&be instanceof SolarPart p&&(p.kind()==SolarBlock.Kind.ENERGY||p.kind()==SolarBlock.Kind.FUEL))for(var d:Direction.values())if(outward(pos,d)){next=next.setValue(SolarBlock.FACING,d);break;}
            if(assembled&&be instanceof SolarPart p&&p.kind()==SolarBlock.Kind.COLLECTOR)for(var slot:SolarLayout.SLOTS)if(slot.kind()==SolarBlock.Kind.COLLECTOR&&at(slot.x(),slot.y(),slot.z()).equals(pos)){
                int column=slot.x()==0?5-slot.z():slot.x()==8?slot.z()-3:slot.z()==0?slot.x()-3:5-slot.x();
                next=next.setValue(SolarBlock.SEGMENT,(slot.y()-3)*3+column);break;
            }
            if(next!=state)l.setBlock(pos,next,2);
        }
    }
    public void activity(boolean active){var l=owner.getLevel();if(l==null||l.isClientSide)return;
        for(var p:parts)if(!p.isRemoved()&&l.hasChunkAt(p.getBlockPos())&&l.getBlockEntity(p.getBlockPos())==p){var s=p.getBlockState();if(s.getValue(SolarBlock.ACTIVE)!=active)l.setBlock(p.getBlockPos(),s.setValue(SolarBlock.ACTIVE,active),2);}
        if(seed!=null&&!seed.isRemoved()&&l.hasChunkAt(seed.getBlockPos()))seed.visual(active?(int)Math.clamp(Math.round(owner.gross*100D/Math.max(1,power())),1,100):0);
    }
}
