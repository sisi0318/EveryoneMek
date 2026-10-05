import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.overloadcore.client.BlackHoleGeometry;
import dev.everyonemek.overloadcore.client.BlackHoleOptics;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Actual shipped shaders and shared meshes, color/depth snapshot and perspective camera, without Minecraft. */
public final class VerifyBlackHoleShader {
    static int width=768,height=512,lens,disc,solid,back,ground,gasProbe;
    static boolean groundScene;
    static final Quaternionf groundView=new Quaternionf().rotationX(.28F);
    static Matrix4f projection;static Target main,snapshot;static Mesh quad;
    static Mesh[] spheres=new Mesh[2],discs=new Mesh[2];
    static int captures;
    record Hole(Vector3f center,float radius,Quaternionf orientation,float phase,boolean detailed){}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static int compile(int kind,String code){int s=glCreateShader(kind);glShaderSource(s,code);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    static int program(String vertex,String fragment,String... attrs){int p=glCreateProgram();glAttachShader(p,compile(GL_VERTEX_SHADER,vertex));glAttachShader(p,compile(GL_FRAGMENT_SHADER,fragment));for(int i=0;i<attrs.length;i++)glBindAttribLocation(p,i,attrs[i]);glLinkProgram(p);check(glGetProgrami(p,GL_LINK_STATUS)!=0,glGetProgramInfoLog(p));return p;}
    static void matrix(int p,String name,Matrix4f value){glUniformMatrix4fv(glGetUniformLocation(p,name),false,value.get(new float[16]));}
    static void scalar(int p,String name,float value){glUniform1f(glGetUniformLocation(p,name),value);}
    static void vector(int p,String name,Vector3f v){glUniform3f(glGetUniformLocation(p,name),v.x,v.y,v.z);}
    static void defaults(int p,Matrix4f mv){glUseProgram(p);matrix(p,"ModelViewMat",mv);matrix(p,"ProjMat",projection);glUniform4f(glGetUniformLocation(p,"ColorModulator"),1,1,1,1);scalar(p,"FogStart",64);scalar(p,"FogEnd",128);glUniform4f(glGetUniformLocation(p,"FogColor"),.5F,.6F,.7F,1);glUniform1i(glGetUniformLocation(p,"FogShape"),0);}
    static double depth(float z){var p=projection.transform(new Vector4f(0,0,z,1));return p.z/p.w*.5+.5;}
    static final class Target implements AutoCloseable {
        final int fbo=glGenFramebuffers(),color=glGenTextures(),depth=glGenTextures();
        Target(){glBindFramebuffer(GL_FRAMEBUFFER,fbo);texture(color,GL_RGBA8,GL_RGBA,GL_UNSIGNED_BYTE,GL_LINEAR);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,color,0);texture(depth,GL_DEPTH_COMPONENT32F,GL_DEPTH_COMPONENT,GL_FLOAT,GL_NEAREST);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depth,0);check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE,"Framebuffer incomplete");}
        void texture(int id,int internal,int format,int kind,int filter){glBindTexture(GL_TEXTURE_2D,id);glTexImage2D(GL_TEXTURE_2D,0,internal,width,height,0,format,kind,0L);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,filter);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,filter);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);}
        public void close(){glDeleteTextures(color);glDeleteTextures(depth);glDeleteFramebuffers(fbo);}
    }
    static final class Mesh implements AutoCloseable {
        final int vao=glGenVertexArrays(),vbo=glGenBuffers(),ebo=glGenBuffers(),count;
        Mesh(float[] data,int layout){
            int stride=layout==0?16:20;int vertices=data.length/5;count=vertices/4*6;
            var bytes=BufferUtils.createByteBuffer(vertices*stride);
            for(int i=0;i<data.length;i+=5){bytes.putFloat(data[i]).putFloat(data[i+1]).putFloat(data[i+2]);if(layout!=0)bytes.putFloat(data[i+3]).putFloat(data[i+4]);else bytes.putInt(0xFF000000);}
            bytes.flip();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,bytes,GL_STATIC_DRAW);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            var indices=BufferUtils.createIntBuffer(count);for(int i=0;i<vertices;i+=4)indices.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);indices.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0L);glEnableVertexAttribArray(0);
            if(layout!=0){glVertexAttribPointer(1,2,GL_FLOAT,false,stride,12L);glEnableVertexAttribArray(1);}
            if(layout==0){glVertexAttribPointer(1,4,GL_UNSIGNED_BYTE,true,stride,12L);glEnableVertexAttribArray(1);}
        }
        void draw(){glBindVertexArray(vao);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);}
        public void close(){glDeleteVertexArrays(vao);glDeleteBuffers(vbo);glDeleteBuffers(ebo);}
    }
    static void resize(int w,int h){if(main!=null){main.close();snapshot.close();}width=w;height=h;main=new Target();snapshot=new Target();projection=new Matrix4f().perspective((float)Math.toRadians(50),width/(float)height,.05F,128);glViewport(0,0,width,height);}
    static void background(boolean day,int wall){
        glBindFramebuffer(GL_FRAMEBUFFER,main.fbo);glDisable(GL_SCISSOR_TEST);glDepthMask(true);glEnable(GL_DEPTH_TEST);glDisable(GL_BLEND);glClearDepth(1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(groundScene){
            glUseProgram(ground);matrix(ground,"InverseProjection",new Matrix4f(projection).invert());matrix(ground,"CameraProjection",projection);
            vector(ground,"GroundNormal",new Vector3f(0,1,0).rotate(groundView));vector(ground,"GroundForward",new Vector3f(0,0,1).rotate(groundView));quad.draw();
        }else{glUseProgram(back);scalar(back,"Day",day?1:0);scalar(back,"Depth",(float)depth(-30));quad.draw();}
        if(wall>0){glEnable(GL_SCISSOR_TEST);if(wall==1)glScissor(0,0,width/2,height);else glScissor(width/2-30,height/2-55,60,110);glClearColor(.13F,.23F,.36F,1);glClearDepth(depth(-3));glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);}
    }
    static void snapshot(){glBindFramebuffer(GL_READ_FRAMEBUFFER,main.fbo);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,snapshot.fbo);glBlitFramebuffer(0,0,width,height,0,0,width,height,GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT,GL_NEAREST);glBindFramebuffer(GL_FRAMEBUFFER,main.fbo);captures++;}
    static byte[] pixels(){var b=BufferUtils.createByteBuffer(width*height*4);glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,b);byte[] result=new byte[b.remaining()];b.get(result);return result;}
    static byte[] render(List<Hole> holes,boolean day,int wall,boolean lenses,boolean scissor){
        background(day,wall);captures=0;
        if(lenses){snapshot();glUseProgram(lens);matrix(lens,"InverseProjection",new Matrix4f(projection).invert());matrix(lens,"CameraProjection",projection);glUniform2f(glGetUniformLocation(lens,"FrameSize"),width,height);
            glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,snapshot.color);glUniform1i(glGetUniformLocation(lens,"SceneColor"),0);glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,snapshot.depth);glUniform1i(glGetUniformLocation(lens,"SceneDepth"),1);
            glDisable(GL_DEPTH_TEST);glDepthMask(false);glEnable(GL_BLEND);glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            for(var h:holes){var rect=BlackHoleOptics.bounds(h.center,h.radius*BlackHoleGeometry.LENS_REACH,projection,width,height);if(rect.empty())continue;
                if(scissor){glEnable(GL_SCISSOR_TEST);glScissor(rect.x(),rect.y(),rect.width(),rect.height());}
                vector(lens,"CenterView",h.center);vector(lens,"DiscNormal",new Vector3f(0,1,0).rotate(h.orientation));vector(lens,"DiscAxis",new Vector3f(1,0,0).rotate(h.orientation));scalar(lens,"Radius",h.radius);scalar(lens,"Phase",h.phase);scalar(lens,"Visibility",1);quad.draw();
            }glDisable(GL_SCISSOR_TEST);
        }
        glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);glDepthMask(true);glDisable(GL_BLEND);
        for(var h:holes){defaults(solid,new Matrix4f().translation(h.center).scale(h.radius));spheres[h.detailed?1:0].draw();}
        glDepthMask(false);glEnable(GL_BLEND);glBlendFuncSeparate(GL_ONE,GL_ONE,GL_ZERO,GL_ONE);glDisable(GL_CULL_FACE);
        for(var h:holes){defaults(disc,new Matrix4f().translation(h.center).rotate(h.orientation).scale(h.radius));scalar(disc,"Phase",h.phase);scalar(disc,"SoftDepth",lenses?1:0);scalar(disc,"SoftRange",Math.max(.06F,h.radius*.12F));
            if(lenses){matrix(disc,"DepthProjectionInverse",new Matrix4f(projection).invert());glUniform2f(glGetUniformLocation(disc,"FrameSize"),width,height);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,snapshot.depth);glUniform1i(glGetUniformLocation(disc,"SceneDepth"),0);}
            discs[h.detailed?1:0].draw();}
        glDepthMask(true);return pixels();
    }
    static Hole hole(float angle,float phase,boolean detailed){return new Hole(new Vector3f(0,0,-14),BlackHoleOptics.displayRadius(1.15F,true),new Quaternionf().rotationY(-.2F).rotateX(angle),phase,detailed);}
    static int changed(byte[] a,byte[] b){int count=0;for(int i=0;i<a.length;i+=4){if(Math.max(Math.abs((a[i]&255)-(b[i]&255)),Math.max(Math.abs((a[i+1]&255)-(b[i+1]&255)),Math.abs((a[i+2]&255)-(b[i+2]&255))))>5)count++;}return count;}
    static void save(byte[] data,Path file)throws Exception{var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;image.setRGB(x,height-y-1,(data[i]&255)<<16|(data[i+1]&255)<<8|data[i+2]&255);}ImageIO.write(image,"png",file.toFile());}
    static void checks(Path out)throws Exception{
        var h=hole(.34F,0,true);
        for(boolean day:new boolean[]{false,true})for(float tilt:new float[]{0,.34F,1.57F,-.60F}){
            var a=render(List.of(hole(tilt,0,true)),day,0,true,true);var b=render(List.of(hole(tilt,1.8F,true)),day,0,true,true);
            int black=0,lit=0;for(int i=0;i<a.length;i+=4){int r=a[i]&255,g=a[i+1]&255,blue=a[i+2]&255;if(r<3&&g<3&&blue<3)black++;if(r>150&&g>85&&r>blue+20)lit++;}
            check(black>6000&&lit>250,"Missing core/disc at tilt "+tilt+": "+black+" / "+lit);check(changed(a,b)>100,"Frozen gas material");
            save(a,out.resolve((day?"day-":"night-")+tilt+".png"));System.out.println("PASS "+(day?"day":"night")+" tilt "+tilt+": core "+black+", gas "+lit);
        }
        var base=render(List.of(h),true,0,false,true);var lensed=render(List.of(h),true,0,true,true);
        check(changed(base,lensed)>1000,"No scene bending");save(base,out.resolve("no-lens.png"));save(lensed,out.resolve("lens.png"));
        var blocked=render(List.of(h),true,1,true,true);
        for(int y=0;y<height;y++)for(int x=0;x<width/2;x++){int i=(y*width+x)*4;check(Math.abs((blocked[i]&255)-33)<=1&&Math.abs((blocked[i+1]&255)-59)<=1,"Effect drew over foreground wall");}
        save(blocked,out.resolve("wall.png"));
        var narrow=render(List.of(h),true,2,true,true);int spilled=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){if(x>=width/2-30&&x<width/2+30&&y>=height/2-55&&y<height/2+55)continue;int i=(y*width+x)*4;if(Math.abs((narrow[i]&255)-33)<=1&&Math.abs((narrow[i+1]&255)-59)<=1&&Math.abs((narrow[i+2]&255)-92)<=1)spilled++;}
        check(spilled==0,"Foreground wall was sampled into the background lens: "+spilled);save(narrow,out.resolve("foreground-sampling.png"));
        var multiple=List.of(new Hole(new Vector3f(-4,.8F,-24),3,new Quaternionf().rotationX(.6F),2,true),new Hole(new Vector3f(2.6F,-.5F,-18),3,new Quaternionf().rotationX(-.3F),3,true));
        save(render(multiple,false,0,true,true),out.resolve("multiple.png"));check(captures==1,"More than one scene snapshot per frame");
        for(int fov:new int[]{50,90,110}){
            projection=new Matrix4f().perspective((float)Math.toRadians(fov),width/(float)height,.05F,128).translate(.04F,-.015F,0).rotateZ(.04F);
            for(var center:List.of(new Vector3f(3,1,-6),new Vector3f(0,0,-1.3F),new Vector3f(-5,-1,-10))){
                var item=List.of(new Hole(center,1,new Quaternionf().rotationX(.4F),1,true));
                var clipped=render(item,true,0,true,true);var full=render(item,true,0,true,false);check(changed(clipped,full)==0,"Scissor cut the optical field at FOV "+fov);
                if(fov==90&&center.x==3)save(clipped,out.resolve("off-axis-fov90.png"));
            }
        }
        projection=new Matrix4f().perspective((float)Math.toRadians(70),width/(float)height,.05F,128);
        var inside=render(List.of(new Hole(new Vector3f(0,0,-.1F),1,new Quaternionf(),0,true)),true,0,true,true);int black=0;
        for(int i=0;i<inside.length;i+=4)if((inside[i]&255)<3&&(inside[i+1]&255)<3&&(inside[i+2]&255)<3)black++;
        check(black>width*height*.99,"Core opened a hole in the view when the camera went inside");
        check(BlackHoleOptics.displayRadius(.18F,false)==.18F,"Visual enlargement changed the flying projectile");
        projection=new Matrix4f().perspective((float)Math.toRadians(50),width/(float)height,.05F,128);
        save(render(List.of(new Hole(h.center,1.15F,h.orientation,0,true)),false,0,true,true),out.resolve("previous-size.png"));
        resize(960,540);save(render(List.of(hole(.34F,0,false)),true,0,true,true),out.resolve("resized-low-detail.png"));
        check(captures==1&&glGetError()==GL_NO_ERROR,"Resize/capture GL failure");
        check(BlackHoleGeometry.sphere(true).quads()+BlackHoleGeometry.disc(true).quads()<=1600,"Detailed mesh exceeds budget");
        check(BlackHoleGeometry.sphere(false).quads()+BlackHoleGeometry.disc(false).quads()<=500,"Distant mesh exceeds budget");
        System.out.println("PASS color/depth lens, wall and foreground rejection, one capture/multiple holes, FOV/bob/scissor, inside core, resized low LOD");
    }
    static void terrainChecks(Path out)throws Exception{
        resize(1024,640);projection=new Matrix4f().perspective((float)Math.toRadians(70),width/(float)height,.05F,128);groundScene=true;
        var center=new Vector3f(-1,-1.5F,-9).rotate(groundView);var tilt=new Quaternionf(groundView).rotateY(-.2F).rotateX(.34F);
        for(float phase:new float[]{0,4,1000,5000})save(render(List.of(new Hole(center,3,tilt,phase,true)),true,0,true,true),out.resolve("ground-"+phase+".png"));
        save(render(List.of(new Hole(center,3,tilt,1000,true)),true,0,false,true),out.resolve("ground-no-lens.png"));groundScene=false;
    }
    static void longTimeChecks() {
        glBindFramebuffer(GL_FRAMEBUFFER,main.fbo);glDisable(GL_DEPTH_TEST);glDisable(GL_BLEND);glDisable(GL_SCISSOR_TEST);glUseProgram(gasProbe);
        for(float phase:new float[]{0,4,250,1000,5000,6000,-5000}){
            scalar(gasProbe,"Phase",phase);quad.draw();var row=pixels();int turns=0,previous=0;
            for(int x=1;x<width;x++){
                int i=(height/2*width+x)*4;int delta=(row[i]&255)-(row[i-4]&255);
                if(Math.abs(delta)<3)continue;int sign=Integer.signum(delta);if(previous!=0&&sign!=previous)turns++;previous=sign;
            }
            check(turns<160,"Gas wound into dense radial noise at time "+phase+": "+turns+" turns");
            System.out.println("PASS bounded gas frequency at time "+phase+": "+turns+" radial turns");
        }
        glEnable(GL_DEPTH_TEST);glDepthMask(true);
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var out=root.resolve("build/black-hole-optics-check");Files.createDirectories(out);
        check(glfwInit(),"GLFW");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);long window=glfwCreateWindow(960,540,"Original black hole optics (hidden)",0,0);check(window!=0,"Hidden GL context");
        try{glfwMakeContextCurrent(window);GL.createCapabilities();String fog,bodyV,bodyF;
            try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){
                fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes()).replace("#version 150","");
                bodyV=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/core/rendertype_lightning.vsh")).readAllBytes());bodyF=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/core/rendertype_lightning.fsh")).readAllBytes());
            }
            var dir=root.resolve("src/main/resources/assets/overloadcore/shaders/core");
            String gas=Files.readString(dir.getParent().resolve("include/black_hole_gas.glsl"));
            if(args.length>1)gas=Files.readString(Path.of(args[1]));
            gasProbe=program("#version 150\nin vec3 Position;in vec2 UV0;out vec2 uv;void main(){uv=UV0;gl_Position=vec4(Position,1);}","#version 150\n"+gas+"\nuniform float Phase;in vec2 uv;out vec4 fragColor;void main(){fragColor=vec4(gasLight(.8,uv.x,Phase),1);}","Position","UV0");
            lens=program(Files.readString(dir.resolve("black_hole.vsh")),Files.readString(dir.resolve("black_hole.fsh")).replace("#moj_import <overloadcore:black_hole_gas.glsl>",gas),"Position","UV0");
            disc=program(Files.readString(dir.resolve("black_hole_disc.vsh")).replace("#moj_import <fog.glsl>",fog),Files.readString(dir.resolve("black_hole_disc.fsh")).replace("#moj_import <overloadcore:black_hole_gas.glsl>",gas),"Position","UV0");
            solid=program(bodyV.replace("#moj_import <fog.glsl>",fog),bodyF.replace("#moj_import <fog.glsl>",fog),"Position","Color");
            back=program("#version 150\nin vec3 Position;in vec2 UV0;out vec2 uv;void main(){uv=UV0;gl_Position=vec4(Position,1);}","#version 150\nuniform float Day;uniform float Depth;in vec2 uv;out vec4 fragColor;void main(){vec3 sky=mix(vec3(.022,.034,.065),vec3(.46,.67,.86),Day);vec3 ground=mix(vec3(.045,.039,.034),vec3(.22,.27,.30),Day);vec3 c=mix(ground,sky,smoothstep(.30,.60,uv.y));vec2 grid=abs(fract(uv*vec2(30,20))-.5);float line=1.0-smoothstep(.022,.044,min(grid.x,grid.y));c=mix(c,mix(vec3(.15,.18,.23),vec3(.63,.66,.65),Day),line*.6);fragColor=vec4(c,1);gl_FragDepth=Depth;}","Position","UV0");
            ground=program("#version 150\nin vec3 Position;in vec2 UV0;out vec2 uv;void main(){uv=UV0;gl_Position=vec4(Position,1);}","#version 150\nuniform mat4 InverseProjection;uniform mat4 CameraProjection;uniform vec3 GroundNormal;uniform vec3 GroundForward;in vec2 uv;out vec4 fragColor;void main(){vec4 h=InverseProjection*vec4(uv*2.-1.,1,1);vec3 ray=normalize(h.xyz/h.w);float d=dot(GroundNormal,ray);if(d>=-.0001){fragColor=vec4(.52,.69,.93,1);gl_FragDepth=1.;return;}vec3 p=ray*(-1.65/d);vec2 grid=vec2(p.x,dot(p,GroundForward));float tile=mod(floor(grid.x)+floor(grid.y),2.);vec3 c=mix(vec3(.21,.32,.08),vec3(.26,.39,.10),tile);fragColor=vec4(c,1);vec4 clip=CameraProjection*vec4(p,1);gl_FragDepth=clip.z/clip.w*.5+.5;}","Position","UV0");
            quad=new Mesh(new float[]{-1,-1,0,0,0,1,-1,0,1,0,1,1,0,1,1,-1,1,0,0,1},2);
            for(int i=0;i<2;i++){spheres[i]=new Mesh(BlackHoleGeometry.sphere(i==1).vertices(),0);discs[i]=new Mesh(BlackHoleGeometry.disc(i==1).vertices(),2);}
            glDisable(GL_CULL_FACE);glDepthFunc(GL_LEQUAL);resize(768,512);checks(out);terrainChecks(out);longTimeChecks();
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
