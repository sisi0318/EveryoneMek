package dev.everyonemek.factory;
import java.util.*;
import mekanism.common.tile.multiblock.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.LevelEvent;
/** Event-invalidated leases cover the whole volume, including floating induction components. */
public final class FactoryStructure {
    private static final Map<Level,Map<BlockPos,Controller>> OWNERS=new WeakHashMap<>();
    public final Controller owner;
    public final List<TileEntityInductionCell> cells=new ArrayList<>();
    public final List<TileEntityInductionProvider> providers=new ArrayList<>();
    public final List<Part> ports=new ArrayList<>();
    private final Set<BlockPos> claimed=new HashSet<>();
    private boolean dirty=true,checking;
    public boolean formed;
    public int parallel,inputSlots,outputSlots;
    public long inputCapacity,outputCapacity,transfer;
    public String error="structure";public BlockPos errorPos;
    public FactoryStructure(Controller owner){this.owner=owner;}
    public static void changed(Level level,BlockPos pos){if(level==null||level.isClientSide)return;var map=OWNERS.get(level);if(map!=null){var c=map.get(pos);if(c!=null)c.structure.invalidate();}}
    public static void unload(LevelEvent.Unload event){OWNERS.remove(event.getLevel());}
    public static void chunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload e){var map=OWNERS.get(e.getLevel());if(map!=null){var affected=new HashSet<Controller>();for(var entry:map.entrySet())if(new net.minecraft.world.level.ChunkPos(entry.getKey()).equals(e.getChunk().getPos()))affected.add(entry.getValue());affected.forEach(c->c.structure.invalidate());}}
    public void invalidate(){boolean was=formed;dirty=true;formed=false;if(was)notifyPorts();}
    private void notifyPorts(){var level=owner.getLevel();if(level==null||level.isClientSide)return;for(var p:List.copyOf(ports))if(!p.isRemoved()&&level.hasChunkAt(p.getBlockPos())){level.invalidateCapabilities(p.getBlockPos());level.updateNeighborsAt(p.getBlockPos(),p.getBlockState().getBlock());}}
    public void detach(){var map=OWNERS.get(owner.getLevel());if(map!=null)for(var pos:claimed)map.remove(pos,owner);claimed.clear();invalidate();}
    public BlockPos at(int x,int y,int z){return owner.getBlockPos().relative(owner.getDirection().getClockWise(),x-1).above(y-1).relative(owner.getDirection().getOpposite(),z);}
    public boolean contains(BlockPos p){var a=at(0,0,0);var b=at(owner.sizeX-1,owner.sizeY-1,owner.sizeZ-1);return p.getX()>=Math.min(a.getX(),b.getX())&&p.getX()<=Math.max(a.getX(),b.getX())&&p.getY()>=a.getY()&&p.getY()<=b.getY()&&p.getZ()>=Math.min(a.getZ(),b.getZ())&&p.getZ()<=Math.max(a.getZ(),b.getZ());}
    public boolean valid(){if(owner.getLevel()==null||owner.getLevel().isClientSide||owner.isRemoved())return false;if(checking)return false;
        if(formed){for(var cell:cells)if(!live(cell)){invalidate();break;}for(var provider:providers)if(!live(provider)){invalidate();break;}}
        return !dirty?formed:validate();}
    private boolean live(net.minecraft.world.level.block.entity.BlockEntity tile){return !tile.isRemoved()&&owner.getLevel().hasChunkAt(tile.getBlockPos())&&owner.getLevel().getBlockEntity(tile.getBlockPos())==tile;}
    public boolean validate(){
        if(checking||owner.getLevel()==null||owner.getLevel().isClientSide)return false;
        checking=true;formed=false;dirty=false;cells.clear();providers.clear();ports.clear();parallel=inputSlots=outputSlots=0;inputCapacity=outputCapacity=transfer=0;
        var level=owner.getLevel();var map=OWNERS.computeIfAbsent(level,l->new HashMap<>());
        for(var p:claimed)map.remove(p,owner);claimed.clear();
        int tier=owner.grade().ordinal(),inputs=0,outputs=0;
        try {
            for(int x=0;x<owner.sizeX;x++)for(int y=0;y<owner.sizeY;y++)for(int z=0;z<owner.sizeZ;z++){
                var pos=at(x,y,z);
                if(!level.hasChunkAt(pos))return fail("unloaded",pos);
                var other=map.get(pos);if(other!=null&&other!=owner&&!other.isRemoved())return fail("occupied",pos);
                // Watch incomplete structures too, so filling the missing location can trigger a new scan.
                map.put(pos,owner);claimed.add(pos);
                var state=level.getBlockState(pos);var block=state.getBlock();int edges=(x==0||x==owner.sizeX-1?1:0)+(y==0||y==owner.sizeY-1?1:0)+(z==0||z==owner.sizeZ-1?1:0);
                if(edges>=2){if(!(block instanceof PartBlock p)||p.kind!=PartBlock.Kind.FRAME)return fail("frame",pos);tier=Math.min(tier,p.grade.ordinal());}
                else if(edges==1){
                    if(pos.equals(owner.getBlockPos()))continue;
                    if(!(block instanceof PartBlock p)||p.kind==PartBlock.Kind.FRAME)return fail("shell",pos);
                    if(level.getBlockEntity(pos) instanceof Part part){part.master=owner.getBlockPos();part.setChanged();if(p.kind==PartBlock.Kind.PORT){
                        ports.add(part);if(state.getValue(PartBlock.OUTPUT)){outputs++;outputSlots+=p.grade.slots;outputCapacity+=p.grade.capacity;}
                        else {inputs++;inputSlots+=p.grade.slots;inputCapacity+=p.grade.capacity;}
                    }}else return fail("shell",pos);
                }else if(!state.isAir()){
                    var tile=level.getBlockEntity(pos);
                    if(tile instanceof mekanism.common.tile.prefab.TileEntityInternalMultiblock internal&&internal.getMultiblock()!=null&&internal.getMultiblock().isFormed())return fail("occupied",pos);
                    if(tile instanceof TileEntityInductionCell cell)cells.add(cell);
                    else if(tile instanceof TileEntityInductionProvider provider){providers.add(provider);transfer=mekanism.api.math.MathUtils.addClamped(transfer,provider.tier.getOutput());}
                    else return fail("interior",pos);
                }
                if(edges>=2&&level.getBlockEntity(pos) instanceof Part part){part.master=owner.getBlockPos();part.setChanged();}
            }
            var grade=Grade.values()[tier];
            if(Math.max(owner.sizeX,Math.max(owner.sizeY,owner.sizeZ))>grade.size())return fail("tier",owner.getBlockPos());
            if(inputs==0||outputs==0)return fail("ports",owner.getBlockPos());
            if(inputs>8||outputs>8)return fail("port_limit",owner.getBlockPos());
            if(cells.isEmpty()||providers.isEmpty())return fail("induction",owner.getBlockPos());
            parallel=Math.min(grade.parallel(),(owner.sizeX-2)*(owner.sizeY-2)*(owner.sizeZ-2));
            error="ready";errorPos=null;formed=true;return true;
        }finally{checking=false;owner.markForSave();if(formed)notifyPorts();}
    }
    private boolean fail(String code,BlockPos pos){error=code;errorPos=pos;return false;}
    public Direction outward(BlockPos p){for(var side:Direction.values())if(!contains(p.relative(side)))return side;return null;}
}
