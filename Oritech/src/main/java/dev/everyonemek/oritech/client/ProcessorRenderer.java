package dev.everyonemek.oritech.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import dev.everyonemek.oritech.*;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import rearth.oritech.client.renderers.*;
import rearth.oritech.util.ColorHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Original dependency models, materials and animation tracks are referenced in place. */
public final class ProcessorRenderer implements BlockEntityRenderer<Processor> {
    private final Map<Profiles,MachineRenderer<Processor>> models=new EnumMap<>(Profiles.class);
    private final Map<Processor,rearth.oritech.block.entity.processing.RefineryModuleBlockEntity[]> modules=new WeakHashMap<>();
    private final MachineRenderer<rearth.oritech.block.entity.processing.RefineryModuleBlockEntity> moduleRenderer=new MachineRenderer<>("models/refinery_module_block");
    public ProcessorRenderer(){for(var p:Profiles.values())if(p!=Profiles.EMPTY)models.put(p,new NativeRenderer(p));}
    @Override public void render(Processor p,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(p.profile()==Profiles.EMPTY||!p.getBlockState().getValue(ProcessorBlock.DEPLOYED)){modules.remove(p);return;}
        models.get(p.profile()).render(p,partial,pose,buffers,light,overlay);
        if(p.profile()!=Profiles.REFINERY||p.modules==0){modules.remove(p);return;}
        var visuals=modules.computeIfAbsent(p,key->new rearth.oritech.block.entity.processing.RefineryModuleBlockEntity[2]);
        for(int i=0;i<p.modules;i++){
            var state=rearth.oritech.init.BlockContent.REFINERY_MODULE_BLOCK.defaultBlockState().setValue(ProcessorBlock.FACING,p.facing()).setValue(rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED,true);
            // These client render views never tick, save, or expose inventory/capabilities.
            if(visuals[i]==null){visuals[i]=new rearth.oritech.block.entity.processing.RefineryModuleBlockEntity(p.getBlockPos().above(2+i),state);visuals[i].setLevel(p.getLevel());if(p.getLevel().getGameTime()-p.deployedAt<20)visuals[i].triggerAnim(null,"setup");}
            else visuals[i].setBlockState(state);
            visuals[i].currentColor=p.getCurrentColor();pose.pushPose();pose.translate(0,2+i,0);moduleRenderer.render(visuals[i],partial,pose,buffers,light,overlay);pose.popPose();
        }
        for(int i=p.modules;i<2;i++)visuals[i]=null;
    }
    @Override public boolean shouldRenderOffScreen(Processor p){return true;}
    @Override public AABB getRenderBoundingBox(Processor p){return new AABB(p.getBlockPos()).inflate(4);}
    private static final class NativeRenderer extends MachineRenderer<Processor> {
        NativeRenderer(Profiles p){super("models/"+p.path);}
        @Override public void postRender(PoseStack pose,Processor p,BakedGeoModel model,MultiBufferSource buffers,VertexConsumer buffer,boolean reRender,float partial,int light,int overlay,int colour){
            super.postRender(pose,p,model,buffers,buffer,reRender,partial,light,overlay,colour);
            if(p.profile()!=Profiles.REFINERY||reRender)return;
            var consumer=buffers.getBuffer(RenderType.translucent());
            tank(p,0,-24/16f,3/16f,11/16f,12/16f,25/16f,28/16f,pose,consumer,light,overlay);
            tank(p,1,-22/16f,9/16f,-5/16f,7/16f,15/16f,10/16f,pose,consumer,light,overlay);
            for(int i=0;i<p.modules;i++)tank(p,2+i,-21/16f,2+i,-5/16f,26/16f,14/16f,26/16f,pose,consumer,light,overlay);
        }
        private static void tank(Processor p,int index,float x,float y,float z,float w,float h,float d,PoseStack pose,VertexConsumer consumer,int light,int overlay){
            var tank=p.tanks().get(index);var f=tank.getStack();if(f.isEmpty())return;float fill=Math.clamp((float)f.getAmount()/tank.getCapacity(),0,1);if(fill<=.005)return;
            pose.pushPose();pose.translate(x+.01,y+.01,z+.01);pose.scale(w-.02f,Math.max(.001f,h*fill-.02f),d-.02f);
            var sprite=FluidStackHooks.getStillTexture(f.getFluid());int colour=ColorHelper.makeOpaque(FluidStackHooks.getColor(f.getFluid()));
            for(var side:Direction.values())if(side!=Direction.DOWN)SmallTankRenderer.drawQuad(side,consumer,pose.last().pose(),pose.last(),sprite,colour,light,overlay);
            pose.popPose();
        }
    }
}
