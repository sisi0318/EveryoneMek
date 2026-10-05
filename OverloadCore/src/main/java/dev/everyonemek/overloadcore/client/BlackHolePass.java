package dev.everyonemek.overloadcore.client;

import java.util.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.everyonemek.overloadcore.OverloadCore;
import dev.everyonemek.overloadcore.gear.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

/** World-only optics. Capture once, lens against immutable color/depth, then render solid cores and discs. */
@EventBusSubscriber(modid=OverloadCore.ID,value=Dist.CLIENT)
public final class BlackHolePass {
    private static ShaderInstance lensShader,discShader;
    private static final IdentityHashMap<BlackHoleEntity,Float> VISIBLE=new IdentityHashMap<>();
    private static final VertexBuffer[] SPHERES=new VertexBuffer[2],DISCS=new VertexBuffer[2];
    private static VertexBuffer screen;
    private static TextureTarget scene;
    private static long frame,lastWorldPass,lastUse;
    private static Object world;
    private static boolean captureFailed;
    private static float fogStart,fogEnd;
    private static float[] fogColor={0,0,0,0};
    private record View(UUID id,Matrix4f body,Matrix4f disc,Vector3f center,Vector3f normal,float radius,float phase,float pixels,boolean detailed){}
    public static boolean queue(BlackHoleEntity hole,float partial){
        if(VISIBLE.containsKey(hole))return true;
        if(discShader==null||!GearVisualConfig.SHADERS.get()||frame-lastWorldPass>2||VISIBLE.size()>=64)return false;
        VISIBLE.put(hole,partial);return true;
    }
    @SubscribeEvent public static void begin(RenderFrameEvent.Pre event){
        frame++;VISIBLE.clear();var level=Minecraft.getInstance().level;
        if(world!=level){release();world=level;captureFailed=false;}
        else if(frame-lastUse>180)release();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){VISIBLE.clear();release();world=null;}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event){
        release();lensShader=null;discShader=null;captureFailed=false;
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),id("black_hole"),DefaultVertexFormat.POSITION_TEX),s->lensShader=s);}
        catch(java.io.IOException error){com.mojang.logging.LogUtils.getLogger().warn("Black hole lens unavailable; retaining 3D geometry",error);}
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),id("black_hole_disc"),DefaultVertexFormat.POSITION_TEX),s->discShader=s);}
        catch(java.io.IOException error){com.mojang.logging.LogUtils.getLogger().warn("Black hole disc shader unavailable; using plain geometry",error);}
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,path);}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES){
            fogStart=RenderSystem.getShaderFogStart();fogEnd=RenderSystem.getShaderFogEnd();fogColor=RenderSystem.getShaderFogColor().clone();return;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        lastWorldPass=frame;
        if(VISIBLE.isEmpty()||discShader==null)return;
        var mc=Minecraft.getInstance();var target=mc.getMainRenderTarget();var camera=event.getCamera().getPosition();
        var projection=event.getProjectionMatrix();var viewMatrix=event.getModelViewMatrix();
        var views=new ArrayList<View>(VISIBLE.size());
        for(var entry:VISIBLE.entrySet()){
            var hole=entry.getKey();float partial=entry.getValue();if(hole.isRemoved()||hole.level()!=mc.level)continue;
            float radius=hole.visualRadius(partial);
            var body=new Matrix4f(viewMatrix).translate((float)(Mth.lerp(partial,hole.xo,hole.getX())-camera.x),
                (float)(Mth.lerp(partial,hole.yo,hole.getY())-camera.y),(float)(Mth.lerp(partial,hole.zo,hole.getZ())-camera.z));
            var center=body.transformPosition(new Vector3f());float pixels=BlackHoleOptics.pixelRadius(center,radius,projection,target.viewHeight);
            if(pixels<.4F)continue;
            var orientation=BlackHoleOptics.orientation(hole.getUUID());var normal=new Vector3f(0,1,0).rotate(orientation);
            viewMatrix.transformDirection(normal).normalize();var disc=new Matrix4f(body).rotate(orientation).scale(radius);body.scale(radius);
            float phase=GearVisualConfig.ANIMATE.get()?(float)((hole.level().getGameTime()%120000)+partial)/20:0;
            if((hole.getUUID().getLeastSignificantBits()&1)!=0)phase=-phase;
            views.add(new View(hole.getUUID(),body,disc,center,normal,radius,phase,pixels,pixels>=40));
        }
        VISIBLE.clear();if(views.isEmpty())return;
        views.sort(Comparator.comparingDouble((View v)->v.center.lengthSquared()).reversed().thenComparing(View::id));lastUse=frame;
        var lensViews=views.stream().filter(v->v.pixels>=6&&v.center.length()>v.radius*1.02F).toList();
        if(lensViews.size()>8)lensViews=lensViews.subList(lensViews.size()-8,lensViews.size());
        try(var state=new State()){
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableCull();
            RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);RenderSystem.setShaderFogColor(fogColor[0],fogColor[1],fogColor[2],fogColor[3]);
            RenderSystem.disableScissor();
            boolean lenses=lensShader!=null&&GearVisualConfig.BLACK_HOLE_LENSING.get()&&!lensViews.isEmpty()
                &&state.viewport[0]==0&&state.viewport[1]==0&&state.viewport[2]==target.viewWidth&&state.viewport[3]==target.viewHeight&&capture(target);
            if(lenses){
                var inverse=new Matrix4f(projection).invert();
                lensShader.getUniform("InverseProjection").set(inverse);lensShader.getUniform("CameraProjection").set(projection);
                lensShader.getUniform("FrameSize").set((float)target.viewWidth,(float)target.viewHeight);
                lensShader.setSampler("SceneColor",scene.getColorTextureId());lensShader.setSampler("SceneDepth",scene.getDepthTextureId());
                RenderSystem.disableDepthTest();RenderSystem.depthMask(false);premultiplied();
                for(var v:lensViews){
                    var rect=BlackHoleOptics.bounds(v.center,v.radius*BlackHoleGeometry.LENS_REACH,projection,target.viewWidth,target.viewHeight);
                    if(rect.empty())continue;state.scissor(rect);
                    lensShader.getUniform("CenterView").set(v.center);lensShader.getUniform("DiscNormal").set(v.normal);
                    lensShader.getUniform("Radius").set(v.radius);lensShader.getUniform("Phase").set(v.phase);
                    float visibility=fogEnd>fogStart?1-Mth.clamp((v.center.length()-fogStart)/(fogEnd-fogStart),0,1):1;
                    lensShader.getUniform("Visibility").set(visibility);
                    var quad=screen();quad.bind();quad.drawWithShader(new Matrix4f(),new Matrix4f(),lensShader);
                }
            }
            state.restoreScissor();
            RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(true);RenderSystem.disableBlend();
            for(var v:views){var mesh=sphere(v.detailed);mesh.bind();mesh.drawWithShader(v.body,projection,GameRenderer.getRendertypeLightningShader());}
            RenderSystem.depthMask(false);premultiplied();
            // Closed skins: render exits before entries. Fixed face culling avoids per-frame
            // CPU index sorting and the bright sector seams from unsorted transparent skins.
            RenderSystem.enableCull();
            for(var v:views){
                discShader.getUniform("Phase").set(v.phase);var mesh=disc(v.detailed);mesh.bind();
                GL11.glCullFace(GL11.GL_FRONT);mesh.drawWithShader(v.disc,projection,discShader);
                GL11.glCullFace(GL11.GL_BACK);mesh.drawWithShader(v.disc,projection,discShader);
            }
            VertexBuffer.unbind();
        }
    }
    private static void premultiplied(){RenderSystem.enableBlend();RenderSystem.blendEquation(GL14.GL_FUNC_ADD);RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);}
    private static boolean capture(RenderTarget target){
        // AFTER_LEVEL is after Fabulous compositing and before hand/depth clear in 1.21.1.
        // A foreign framebuffer may have different depth/size conventions; keep the 3D effect in that case.
        if(captureFailed||GlStateManager.getBoundFramebuffer()!=target.frameBufferId||!target.useDepth||target.viewWidth<=0||target.viewHeight<=0)return false;
        try{
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            if(scene!=null&&scene.isStencilEnabled()!=target.isStencilEnabled()){scene.destroyBuffers();scene=null;}
            if(scene==null){scene=new TextureTarget(target.viewWidth,target.viewHeight,true,Minecraft.ON_OSX);if(target.isStencilEnabled())scene.enableStencil();scene.setFilterMode(GL11.GL_LINEAR);}
            else if(scene.width!=target.viewWidth||scene.height!=target.viewHeight){scene.resize(target.viewWidth,target.viewHeight,Minecraft.ON_OSX);scene.setFilterMode(GL11.GL_LINEAR);}
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,scene.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0,0,target.viewWidth,target.viewHeight,0,0,scene.width,scene.height,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,GL11.GL_NEAREST);
            target.bindWrite(true);return true;
        }catch(RuntimeException error){
            captureFailed=true;target.bindWrite(true);com.mojang.logging.LogUtils.getLogger().warn("Black hole scene capture unavailable; retaining 3D core/disc",error);return false;
        }
    }
    private static VertexBuffer sphere(boolean detailed){int n=detailed?1:0;if(SPHERES[n]==null)SPHERES[n]=upload(BlackHoleGeometry.sphere(detailed),false);return SPHERES[n];}
    private static VertexBuffer disc(boolean detailed){int n=detailed?1:0;if(DISCS[n]==null)DISCS[n]=upload(BlackHoleGeometry.disc(detailed),true);return DISCS[n];}
    private static VertexBuffer upload(BlackHoleGeometry.Mesh mesh,boolean disc){
        var builder=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,disc?DefaultVertexFormat.POSITION_TEX:DefaultVertexFormat.POSITION_COLOR);
        BlackHoleGeometry.emit(mesh,(x,y,z,u,v)->{var out=builder.addVertex(x,y,z);if(disc)out.setUv(u,v);else out.setColor(0,0,0,255);});
        var result=new VertexBuffer(VertexBuffer.Usage.STATIC);result.bind();result.upload(builder.buildOrThrow());VertexBuffer.unbind();return result;
    }
    private static VertexBuffer screen(){
        if(screen==null){var builder=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(-1,-1,0).setUv(0,0);builder.addVertex(1,-1,0).setUv(1,0);builder.addVertex(1,1,0).setUv(1,1);builder.addVertex(-1,1,0).setUv(0,1);
            screen=new VertexBuffer(VertexBuffer.Usage.STATIC);screen.bind();screen.upload(builder.buildOrThrow());VertexBuffer.unbind();}
        return screen;
    }
    private static void release(){
        if(scene==null&&screen==null&&SPHERES[0]==null&&SPHERES[1]==null&&DISCS[0]==null&&DISCS[1]==null)return;
        try(var state=new State()){
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            if(scene!=null){scene.destroyBuffers();scene=null;}if(screen!=null){screen.close();screen=null;}
            for(int i=0;i<2;i++){if(SPHERES[i]!=null)SPHERES[i].close();if(DISCS[i]!=null)DISCS[i].close();SPHERES[i]=DISCS[i]=null;}
        }
    }
    /** Preserve the host's render state, including independent read/draw targets and texture bindings. */
    private static final class State implements AutoCloseable {
        final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),write=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        final int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int equation=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),equationAlpha=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        final int cullFace=GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        final int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GlStateManager.getBoundFramebuffer(),active=GlStateManager._getActiveTexture(),program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),vbo=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        final int[] viewport=new int[4],clip=new int[4],textures=new int[2];
        final float[] clear=new float[4],color=RenderSystem.getShaderColor().clone(),fog=RenderSystem.getShaderFogColor().clone();
        final float start=RenderSystem.getShaderFogStart(),end=RenderSystem.getShaderFogEnd();
        State(){GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,clip);GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
            for(int i=0;i<2;i++){GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);textures[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);}GlStateManager._activeTexture(active);}
        void scissor(BlackHoleOptics.Rect r){int x=r.x(),y=r.y(),right=x+r.width(),top=y+r.height();if(scissor){x=Math.max(x,clip[0]);y=Math.max(y,clip[1]);right=Math.min(right,clip[0]+clip[2]);top=Math.min(top,clip[1]+clip[3]);}RenderSystem.enableScissor(x,y,Math.max(0,right-x),Math.max(0,top-y));}
        void restoreScissor(){if(scissor)RenderSystem.enableScissor(clip[0],clip[1],clip[2],clip[3]);else RenderSystem.disableScissor();}
        @Override public void close(){
            VertexBuffer.unbind();GlStateManager._glBindVertexArray(vao);GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);
            GlStateManager._glUseProgram(program);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);restoreScissor();
            RenderSystem.depthMask(write);RenderSystem.depthFunc(depthFunc);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            GL11.glCullFace(cullFace);if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();GL20.glBlendEquationSeparate(equation,equationAlpha);RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.setShaderFogStart(start);RenderSystem.setShaderFogEnd(end);RenderSystem.setShaderFogColor(fog[0],fog[1],fog[2],fog[3]);
            for(int i=0;i<2;i++){GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);GlStateManager._bindTexture(textures[i]);}GlStateManager._activeTexture(active);
        }
    }
    private BlackHolePass(){}
}
