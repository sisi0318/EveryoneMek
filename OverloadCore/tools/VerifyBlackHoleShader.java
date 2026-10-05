import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.overloadcore.client.BlackHoleGeometry;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Runs the shipped GLSL and shared mesh in a hidden OpenGL context, never a game client. */
public final class VerifyBlackHoleShader {
    static final int W=768,H=512;
    static int program;static Matrix4f projection;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static int shader(int kind,String code){int s=glCreateShader(kind);glShaderSource(s,code);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    static void matrix(String name,Matrix4f value){glUniformMatrix4fv(glGetUniformLocation(program,name),false,value.get(new float[16]));}
    static double depth(float z){var p=projection.transform(new org.joml.Vector4f(0,0,z,1));return p.z/p.w*.5+.5;}
    static byte[] draw(float phase,boolean day,int wall,float radius){
        glClearColor(day?.52F:.035F,day?.70F:.045F,day?.92F:.055F,1);glClearDepth(1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(wall>0){glEnable(GL_SCISSOR_TEST);glScissor(0,0,wall==1?W/2:W,H);glClearDepth(wall==1?.1:depth(-7.6F));glClear(GL_DEPTH_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);}
        var bytes=BufferUtils.createByteBuffer(112);float r=BlackHoleGeometry.quantizedRadius(radius);
        BlackHoleGeometry.billboard((x,y,z,u,v,R,g,b,a)->{
            bytes.putFloat(x).putFloat(y).putFloat(z-8).putFloat(u).putFloat(v);
            bytes.put((byte)Math.round(R*255)).put((byte)Math.round(g*255)).put((byte)0).put((byte)255).putInt(0);
        },r,phase);bytes.flip();glBufferData(GL_ARRAY_BUFFER,bytes,GL_STREAM_DRAW);
        glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0L);
        var out=BufferUtils.createByteBuffer(W*H*4);glReadPixels(0,0,W,H,GL_RGBA,GL_UNSIGNED_BYTE,out);byte[] pixels=new byte[out.remaining()];out.get(pixels);return pixels;
    }
    static void save(byte[] pixels,Path path)throws Exception{
        var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;image.setRGB(x,H-y-1,(pixels[i]&255)<<16|(pixels[i+1]&255)<<8|pixels[i+2]&255);}
        ImageIO.write(image,"png",path.toFile());
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var out=root.resolve("build/black-hole-shader-check");Files.createDirectories(out);
        check(glfwInit(),"GLFW init");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        long window=glfwCreateWindow(W,H,"Black hole shader verification (hidden)",0,0);check(window!=0,"Hidden GL context");
        try{
            glfwMakeContextCurrent(window);GL.createCapabilities();String fog;
            try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");}
            var dir=root.resolve("src/main/resources/assets/overloadcore/shaders/core");program=glCreateProgram();
            glAttachShader(program,shader(GL_VERTEX_SHADER,Files.readString(dir.resolve("black_hole.vsh")).replace("#moj_import <fog.glsl>",fog)));
            glAttachShader(program,shader(GL_FRAGMENT_SHADER,Files.readString(dir.resolve("black_hole.fsh")).replace("#moj_import <fog.glsl>",fog)));
            String[] attrs={"Position","UV0","Color","Normal"};for(int i=0;i<4;i++)glBindAttribLocation(program,i,attrs[i]);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
            projection=new Matrix4f().perspective((float)Math.toRadians(50),W/(float)H,.05F,128);matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",projection);
            glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),64);glUniform1f(glGetUniformLocation(program,"FogEnd"),128);glUniform4f(glGetUniformLocation(program,"FogColor"),0,0,0,1);glUniform1i(glGetUniformLocation(program,"FogShape"),0);
            glBindVertexArray(glGenVertexArrays());glBindBuffer(GL_ARRAY_BUFFER,glGenBuffers());glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,glGenBuffers());
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,new int[]{0,1,2,0,2,3},GL_STATIC_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,28,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,28,12L);glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,28,20L);glVertexAttribPointer(3,3,GL_BYTE,true,28,24L);for(int i=0;i<4;i++)glEnableVertexAttribArray(i);
            glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);glDepthMask(true);glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glViewport(0,0,W,H);
            byte[] first=null;
            for(boolean day:new boolean[]{false,true})for(float phase:new float[]{0,.2F,.4F,.7F}){
                var pixels=draw(phase,day,0,1.15F);int black=0,gold=0,changed=0;
                for(int i=0;i<pixels.length;i+=4){int r=pixels[i]&255,g=pixels[i+1]&255,b=pixels[i+2]&255;
                    if(r<3&&g<3&&b<3)black++;if(r>160&&g>90&&r>b+25)gold++;
                    if(first!=null&&Math.abs(r-(first[i]&255))>5)changed++;
                }
                check(black>10000&&gold>3000,"Core/disk missing: "+black+" / "+gold);
                if(phase>0)check(changed>200,"Accretion disk has no motion");
                save(pixels,out.resolve((day?"day-":"night-")+phase+".png"));if(!day&&phase==0)first=pixels;
                System.out.println("PASS "+(day?"day":"night")+" phase "+phase+": "+black+" black core pixels, "+gold+" gold disk pixels");
            }
            var wall=draw(.2F,true,1,1.15F);
            for(int y=0;y<H;y++)for(int x=0;x<W/2;x++){int i=(y*W+x)*4;check(Math.abs((wall[i]&255)-133)<=1&&Math.abs((wall[i+1]&255)-179)<=1,"Light/core passed through foreground wall");}
            save(wall,out.resolve("wall.png"));
            var partial=draw(.2F,true,2,1.15F);int center=(H/2*W+W/2)*4;check((partial[center]&255)<3,"Curved core was clipped at billboard depth");
            var z=BufferUtils.createFloatBuffer(1);glReadPixels(W/2,H/2,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,z);check(z.get(0)<depth(-7.6F),"Core did not write surface depth");
            save(partial,out.resolve("intersecting-wall.png"));
            var small=draw(0,true,0,.18F);int black=0;for(int i=0;i<small.length;i+=4)if((small[i]&255)<3&&(small[i+1]&255)<3)black++;
            check(black>200&&black<1500,"Flying singularity has wrong size");
            final int[] count={0};BlackHoleGeometry.fallback((x,y,Z,u,v,r,g,b,a)->{check(Float.isFinite(x+y+Z)&&Math.abs(x)<=3&&Math.abs(y)<=3&&Math.abs(Z)<=3,"Unbounded fallback geometry");count[0]++;},1.15F);
            check(count[0]==336*4,"Fallback mesh changed unexpectedly");
            check(glGetError()==GL_NO_ERROR,"GL error");System.out.println("PASS wall depth, curved intersection depth, flight radius and bounded fallback");
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
