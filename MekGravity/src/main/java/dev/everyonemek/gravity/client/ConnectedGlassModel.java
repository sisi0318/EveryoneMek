package dev.everyonemek.gravity.client;

import dev.everyonemek.gravity.*;
import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;
import org.jetbrains.annotations.Nullable;

/** Immutable cached quads selected from local model data, with the item model left intact. */
public final class ConnectedGlassModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> NEIGHBORS=new ModelProperty<>();
    private final Map<Direction,List<List<BakedQuad>>> faces;
    private record WindowFaces(List<List<BakedQuad>> rims,List<BakedQuad> pane,List<List<BakedQuad>> combined){}
    private final Map<Direction,WindowFaces> windows;
    private static final ChunkRenderTypeSet WINDOW_LAYERS=ChunkRenderTypeSet.of(RenderType.solid(),RenderType.translucent());

    private ConnectedGlassModel(BakedModel original,Map<Direction,List<List<BakedQuad>>> faces,Map<Direction,WindowFaces> windows){super(original);this.faces=faces;this.windows=windows;}

    private static ModelResourceLocation id(Direction face,int part){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"block/glass/"+face.getSerializedName()+"_"+part));
    }

    private static ModelResourceLocation windowId(Direction face,String part){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"block/window/"+face.getSerializedName()+"_"+part));}
    public static void additional(ModelEvent.RegisterAdditional event){for(var face:Direction.values()){
        for(int part=0;part<8;part++){event.register(id(face,part));event.register(windowId(face,Integer.toString(part)));}
        event.register(windowId(face,"pane"));
    }}

    public static void bake(ModelEvent.ModifyBakingResult event){
        var cache=new EnumMap<Direction,List<List<BakedQuad>>>(Direction.class);
        var windowCache=new EnumMap<Direction,WindowFaces>(Direction.class);
        var random=RandomSource.create(0);
        for(var face:Direction.values()){
            var parts=new ArrayList<List<BakedQuad>>();
            for(int part=0;part<8;part++)parts.add(List.copyOf(Objects.requireNonNull(event.getModels().get(id(face,part)),"Missing glass border model").getQuads(null,face,random,ModelData.EMPTY,null)));
            var combinations=new ArrayList<List<BakedQuad>>(256);
            for(int mask=0;mask<256;mask++){var quads=new ArrayList<BakedQuad>();for(int part=0;part<8;part++)if((mask&(1<<part))!=0)quads.addAll(parts.get(part));combinations.add(List.copyOf(quads));}
            cache.put(face,List.copyOf(combinations));
            var rimParts=new ArrayList<List<BakedQuad>>();
            for(int part=0;part<8;part++)rimParts.add(List.copyOf(Objects.requireNonNull(event.getModels().get(windowId(face,Integer.toString(part))),"Missing window rim model").getQuads(null,null,random,ModelData.EMPTY,null)));
            var pane=List.copyOf(Objects.requireNonNull(event.getModels().get(windowId(face,"pane")),"Missing window pane model").getQuads(null,null,random,ModelData.EMPTY,null));
            var rims=new ArrayList<List<BakedQuad>>();var combined=new ArrayList<List<BakedQuad>>();
            for(int mask=0;mask<256;mask++){
                var quads=new ArrayList<BakedQuad>();for(int part=0;part<8;part++)if((mask&(1<<part))!=0)quads.addAll(rimParts.get(part));
                rims.add(List.copyOf(quads));quads.addAll(pane);combined.add(List.copyOf(quads));
            }
            windowCache.put(face,new WindowFaces(List.copyOf(rims),pane,List.copyOf(combined)));
        }
        var immutable=Collections.unmodifiableMap(cache);
        var wrappers=new IdentityHashMap<BakedModel,ConnectedGlassModel>();
        for(var state:Content.PARTS.get(PartBlock.Kind.GLASS).get().getStateDefinition().getPossibleStates()){
            var key=BlockModelShaper.stateToModelLocation(state);var original=event.getModels().get(key);
            if(original!=null)event.getModels().put(key,wrappers.computeIfAbsent(original,m->new ConnectedGlassModel(m,immutable,Collections.unmodifiableMap(windowCache))));
        }
    }

    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){
        return originalModel.getModelData(level,pos,state,data).derive().with(NEIGHBORS,GlassConnections.sample(level,pos,state)).build();
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,ModelData data,@Nullable RenderType renderType){
        Integer neighbors=data.get(NEIGHBORS);
        if(state!=null&&state.getValue(PartBlock.FORMED)){
            if(side!=null)return List.of();
            var direction=state.getValue(PartBlock.FACING);var window=windows.get(direction);
            int mask=GlassConnections.planarParts(neighbors==null?0:neighbors,direction);
            if(renderType==RenderType.solid())return window.rims.get(mask);
            if(renderType==RenderType.translucent())return window.pane;
            return renderType==null?window.combined.get(mask):List.of();
        }
        if(state==null||neighbors==null)return originalModel.getQuads(state,side,random,data,renderType);
        return side==null?List.of():faces.get(side).get(GlassConnections.visibleParts(neighbors,side));
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){return state.getValue(PartBlock.FORMED)?WINDOW_LAYERS:originalModel.getRenderTypes(state,random,data);}
}
