package dev.everyonemek.gravity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.*;
public final class Structure {
    private static final Map<Level,Map<BlockPos,Controller>> OWNERS=new WeakHashMap<>();
    private final Set<BlockPos> claimed=new HashSet<>();
    public final Controller owner;
    public final List<Part> ports=new ArrayList<>(),fuelHatches=new ArrayList<>(),coils=new ArrayList<>();
    public Part core;public boolean formed;private boolean dirty=true,checking;
    public Grade grade=Grade.BASIC;public String error="structure";public BlockPos errorPos;
    public Structure(Controller owner){this.owner=owner;}
    public BlockPos at(int x,int y,int z){return owner.getBlockPos().relative(owner.getDirection().getClockWise(),x-3).above(y-1).relative(owner.getDirection().getOpposite(),z);}
    public boolean contains(BlockPos p){var a=at(0,0,0);var b=at(6,6,6);return p.getX()>=Math.min(a.getX(),b.getX())&&p.getX()<=Math.max(a.getX(),b.getX())&&p.getY()>=a.getY()&&p.getY()<=b.getY()&&p.getZ()>=Math.min(a.getZ(),b.getZ())&&p.getZ()<=Math.max(a.getZ(),b.getZ());}
    public boolean outward(BlockPos p,Direction side){return side!=null&&contains(p)&&!contains(p.relative(side));}
    public static void changed(Level l,BlockPos p){if(l==null||l.isClientSide)return;var map=OWNERS.get(l);var c=map==null?null:map.get(p);if(c!=null)c.structure.invalidate();}
    public static void unload(LevelEvent.Unload e){OWNERS.remove(e.getLevel());}
    public static void chunkUnload(ChunkEvent.Unload e){var map=OWNERS.get(e.getLevel());if(map!=null){var cs=new HashSet<Controller>();for(var entry:map.entrySet())if(new net.minecraft.world.level.ChunkPos(entry.getKey()).equals(e.getChunk().getPos()))cs.add(entry.getValue());cs.forEach(c->c.structure.invalidate());}}
    private void notifyPorts(){var l=owner.getLevel();if(l==null||l.isClientSide)return;for(var p:List.copyOf(ports))if(!p.isRemoved()&&l.hasChunkAt(p.getBlockPos())){l.invalidateCapabilities(p.getBlockPos());l.updateNeighborsAt(p.getBlockPos(),p.getBlockState().getBlock());}}
    public void invalidate(){boolean was=formed;dirty=true;formed=false;if(was){activity(false);notifyPorts();}}
    public void detach(){var map=OWNERS.get(owner.getLevel());if(map!=null)for(var p:claimed)map.remove(p,owner);claimed.clear();invalidate();}
    public boolean valid(){if(owner.getLevel()==null||owner.getLevel().isClientSide||owner.isRemoved()||checking)return false;return dirty?validate():formed;}
    private boolean fail(String error,BlockPos pos){this.error=error;errorPos=pos;return false;}
    public static boolean coilPosition(int x,int y,int z){return (x==1||x==5)&&y==3&&z==3||(y==1||y==5)&&x==3&&z==3||(z==1||z==5)&&x==3&&y==3;}
    public boolean validate(){
        if(checking||owner.getLevel()==null||owner.isRemoved())return false;
        checking=true;dirty=false;formed=false;ports.clear();fuelHatches.clear();coils.clear();core=null;grade=Grade.ULTIMATE;
        var l=owner.getLevel();var map=OWNERS.computeIfAbsent(l,k->new HashMap<>());for(var p:claimed)map.remove(p,owner);claimed.clear();
        int cold=0,hot=0,excitation=0,outputs=0;
        try {
            for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++){
                var pos=at(x,y,z);if(!l.hasChunkAt(pos))return fail("unloaded",pos);
                var other=map.get(pos);if(other!=null&&other!=owner&&!other.isRemoved())return fail("occupied",pos);map.put(pos,owner);claimed.add(pos);
                if(pos.equals(owner.getBlockPos()))continue;
                var state=l.getBlockState(pos);var be=l.getBlockEntity(pos);
                int edges=(x==0||x==6?1:0)+(y==0||y==6?1:0)+(z==0||z==6?1:0);
                if(edges==0&&!coilPosition(x,y,z)&&!(x==3&&y==3&&z==3)){if(!state.isAir())return fail("interior",pos);continue;}
                if(!(be instanceof Part p))return fail(edges>0?"shell":"coil",pos);
                var kind=p.kind();
                if(edges>=2){if(kind!=PartBlock.Kind.FRAME)return fail("frame",pos);}
                else if(edges==1){
                    if(kind==PartBlock.Kind.CORE||kind==PartBlock.Kind.COIL)return fail("shell",pos);
                    if(kind==PartBlock.Kind.FUEL){ports.add(p);fuelHatches.add(p);}
                    if(kind==PartBlock.Kind.COOLANT){ports.add(p);if(p.output())hot++;else cold++;}
                    if(kind==PartBlock.Kind.ENERGY){ports.add(p);if(p.output())outputs++;else excitation++;}
                }else if(x==3&&y==3&&z==3){if(kind!=PartBlock.Kind.CORE)return fail("core",pos);core=p;}
                else {if(kind!=PartBlock.Kind.COIL)return fail("coil",pos);var facing=state.getValue(PartBlock.FACING);if(!pos.relative(facing,2).equals(at(3,3,3)))return fail("coil_facing",pos);coils.add(p);var g=((PartBlock)state.getBlock()).grade;if(g.ordinal()<grade.ordinal())grade=g;}
                if(!owner.getBlockPos().equals(p.master)){p.master=owner.getBlockPos();p.setChanged();}
            }
            if(fuelHatches.isEmpty())return fail("fuel_port",owner.getBlockPos());
            if(cold==0)return fail("cold_port",owner.getBlockPos());
            if(hot==0)return fail("hot_port",owner.getBlockPos());
            if(excitation==0)return fail("excitation_port",owner.getBlockPos());
            if(outputs==0)return fail("output_port",owner.getBlockPos());
            if(fuelHatches.size()>8||ports.size()>32)return fail("port_limit",owner.getBlockPos());
            error="ready";errorPos=null;formed=true;return true;
        }finally{checking=false;if(formed)notifyPorts();}
    }
    public void activity(boolean active){var all=new ArrayList<>(coils);if(core!=null)all.add(core);var l=owner.getLevel();if(l==null||l.isClientSide)return;
        for(var p:all)if(!p.isRemoved()&&l.hasChunkAt(p.getBlockPos())&&l.getBlockEntity(p.getBlockPos())==p&&p.getBlockState().getValue(PartBlock.ACTIVE)!=active)l.setBlock(p.getBlockPos(),p.getBlockState().setValue(PartBlock.ACTIVE,active),2);
    }
}
