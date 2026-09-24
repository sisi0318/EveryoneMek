import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.gravity.client.GravityRingMesh;
import dev.everyonemek.gravity.solar.SolarField;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Real ring/field GLSL and original geometry, in a standalone hidden OpenGL context. */
public class VerifyFieldShaders {
    private static int program;private static final int W=720,H=720;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static int compile(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    private static void matrix(String name,Matrix4f value){glUniformMatrix4fv(glGetUniformLocation(program,name),false,value.get(new float[16]));}
    private static int upload(ByteBuffer data,int stride){int quads=data.remaining()/stride/4;glBufferData(GL_ARRAY_BUFFER,data,GL_STATIC_DRAW);var idx=BufferUtils.createIntBuffer(quads*6);for(int q=0;q<quads;q++){int i=q*4;idx.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);}idx.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,idx,GL_STATIC_DRAW);return quads*6;}
    private static ByteBuffer rings(double phase,float load){var bytes=BufferUtils.createByteBuffer(192*4*28);
        for(int ring=0;ring<2;ring++){var data=GravityRingMesh.vertices(ring);check(data.length==96*4*9,"Ring geometry budget changed");
            var rotation=new Matrix3f().rotationX(.4F).rotateY((float)((phase*(ring==0?1.2:-1.6))%360*java.lang.Math.PI/180));double turn=(ring==0?phase:-phase)/85+ring*.5;float time=(float)(turn-java.lang.Math.floor(turn));
            for(int i=0;i<data.length;i+=9){var p=rotation.transform(new Vector3f(data[i],data[i+1],data[i+2]));var n=rotation.transform(new Vector3f(data[i+3],data[i+4],data[i+5]));
                check(p.length()<.69,"Ring exceeded its original radius");bytes.putFloat(p.x).putFloat(p.y).putFloat(p.z-3).putFloat(data[i+6]).putFloat(time);
                bytes.put((byte)java.lang.Math.round(load*255)).put((byte)(data[i+8]>0?255:0)).put((byte)java.lang.Math.round(data[i+7]*255)).put((byte)255);
                bytes.put((byte)(n.x*127)).put((byte)(n.y*127)).put((byte)(n.z*127)).put((byte)0);
            }
        }return bytes.flip();
    }
    private static ByteBuffer field(double phase,float strength){var bytes=BufferUtils.createByteBuffer(500*4*24);var camera=new Vector3f(0,0,5);float time=(float)((phase*.03)%(java.lang.Math.PI*2));
        SolarField.emit(phase,strength,1,(A,B,width,rgb,alpha,halo)->{
            var a=new Vector3f((float)A.x(),(float)A.y(),(float)A.z());var b=new Vector3f((float)B.x(),(float)B.y(),(float)B.z());var view=new Vector3f(camera).sub(new Vector3f(a).add(b).mul(.5F));var d=new Vector3f(b).sub(a);var n=view.cross(d);
            if(n.lengthSquared()<1E-12F)n.set(0,1,0).cross(d);if(n.lengthSquared()<1E-12F)n.set(1,0,0).cross(d);float span=halo?3:1;var off=n.normalize().mul((float)width*span);
            Vector3f[] end={a,b,b,a};for(int i=0;i<4;i++){var p=new Vector3f(end[i]).add(new Vector3f(off).mul(i<2?-1:1));bytes.putFloat(p.x).putFloat(p.y).putFloat(p.z-5).putFloat(i<2?-span:span).putFloat(end[i].x*2+end[i].y*3+end[i].z*4-time);bytes.put((byte)(rgb>>16)).put((byte)(rgb>>8)).put((byte)rgb).put((byte)alpha);}
        });return bytes.flip();
    }
    private static byte[] frame(boolean field,double phase,float strength,boolean block){
        int count=upload(field?field(phase,strength):rings(phase,strength),field?24:28);glDepthMask(true);glViewport(0,0,W,H);glClearColor(.025F,.03F,.045F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(block){glEnable(GL_SCISSOR_TEST);glScissor(0,0,W/2,H);glClearDepth(.1);glClear(GL_DEPTH_BUFFER_BIT);glClearDepth(1);glDisable(GL_SCISSOR_TEST);}
        glDepthMask(!field);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);var result=BufferUtils.createByteBuffer(W*H*4);glReadPixels(0,0,W,H,GL_RGBA,GL_UNSIGNED_BYTE,result);byte[] bytes=new byte[result.remaining()];result.get(bytes);return bytes;
    }
    private static void save(byte[] bytes,Path path)throws Exception{var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;image.setRGB(x,H-y-1,(bytes[i]&255)<<16|(bytes[i+1]&255)<<8|bytes[i+2]&255);}ImageIO.write(image,"png",path.toFile());}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),out=root.resolve("build/field-shader-check");Files.createDirectories(out);check(glfwInit(),"GLFW init");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);long window=glfwCreateWindow(W,H,"Field shader verification (hidden)",0,0);check(window!=0,"Hidden context");
        try{glfwMakeContextCurrent(window);GL.createCapabilities();System.out.println("OpenGL: "+glGetString(GL_VERSION));String fog;
            try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");}
            for(boolean field:new boolean[]{false,true}){String name=field?"stellar_field":"gravity_ring";Path dir=root.resolve("src/main/resources/assets/mekgravity/shaders/core");
                int vertex=compile(GL_VERTEX_SHADER,Files.readString(dir.resolve(name+".vsh")).replace("#moj_import <fog.glsl>",fog)),fragment=compile(GL_FRAGMENT_SHADER,Files.readString(dir.resolve(name+".fsh")).replace("#moj_import <fog.glsl>",fog));
                program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);String[] attributes=field?new String[]{"Position","UV0","Color"}:new String[]{"Position","UV0","Color","Normal"};for(int i=0;i<attributes.length;i++)glBindAttribLocation(program,i,attributes[i]);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
                matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(field?70:32),1,.1F,100));glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),80);glUniform1f(glGetUniformLocation(program,"FogEnd"),100);glUniform1i(glGetUniformLocation(program,"FogShape"),0);if(!field)glUniform4f(glGetUniformLocation(program,"FogColor"),0,0,0,1);
                int vao=glGenVertexArrays();glBindVertexArray(vao);int vbo=glGenBuffers(),ebo=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);int stride=field?24:28;
                glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,stride,12L);glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,stride,20L);if(!field)glVertexAttribPointer(3,3,GL_BYTE,true,stride,24L);for(int i=0;i<attributes.length;i++)glEnableVertexAttribArray(i);
                glEnable(GL_DEPTH_TEST);if(field){glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE);}else{glEnable(GL_CULL_FACE);glDisable(GL_BLEND);}
                var active=frame(field,0,1,false);var moving=frame(field,40,1,false);var idle=frame(field,0,0,false);var blocked=frame(field,0,1,true);save(active,out.resolve(name+".png"));
                int visible=0,changed=0,energized=0;for(int i=0;i<active.length;i+=4){if((active[i+2]&255)>30)visible++;if(java.lang.Math.abs((active[i+2]&255)-(moving[i+2]&255))>15)changed++;if((active[i+2]&255)>(idle[i+2]&255)+10)energized++;}
                check(visible>1500&&changed>300&&energized>300,"Missing geometry/flow/load effect: "+name+" "+visible+"/"+changed+"/"+energized);
                for(int y=0;y<H;y++)for(int x=0;x<W/2;x++)check((blocked[(y*W+x)*4+2]&255)<15,"Depth leak: "+name);
                if(field){var depth=BufferUtils.createFloatBuffer(W*H);glReadPixels(0,0,W,H,GL_DEPTH_COMPONENT,GL_FLOAT,depth);for(int i=0;i<W*H;i++)if(i%W>=W/2)check(depth.get(i)==1,"Transparent field wrote depth");}
                int quads=(field?field(0,1).remaining()/24:rings(0,1).remaining()/28)/4;System.out.println("PASS "+name+": actual GLSL, moving/idle pixels, depth occlusion; "+quads+" quads");
                glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
            }check(glGetError()==0,"OpenGL error");
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
