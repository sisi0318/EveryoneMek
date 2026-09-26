import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.gravity.expansion.ModuleField;

import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Actual orbital module shader and bounded original geometry, in a hidden OpenGL context. */
public class VerifyModuleShader {
    private static int program,kind;private static final int W=720,H=720;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static int compile(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    private static void matrix(String name,Matrix4f value){glUniformMatrix4fv(glGetUniformLocation(program,name),false,value.get(new float[16]));}
    private static int upload(ByteBuffer data,int stride){int quads=data.remaining()/stride/4;glBufferData(GL_ARRAY_BUFFER,data,GL_STATIC_DRAW);var idx=BufferUtils.createIntBuffer(quads*6);for(int q=0;q<quads;q++){int i=q*4;idx.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);}idx.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,idx,GL_STATIC_DRAW);return quads*6;}
    private static ByteBuffer field(double phase,float strength){var bytes=BufferUtils.createByteBuffer(128*4*24);
        int sin=(int)((java.lang.Math.sin(phase*.08)+1)*127.5),cos=(int)((java.lang.Math.cos(phase*.08)+1)*127.5);
        ModuleField.emit(kind,false,(x,y,z,angle,across)->{bytes.putFloat(x).putFloat(y).putFloat(z-1.5F).putFloat(angle).putFloat(across);bytes.put((byte)java.lang.Math.round((kind+.56)*51)).put((byte)sin).put((byte)cos).put((byte)(strength*220));});return bytes.flip();
    }
    private static byte[] frame(double phase,float strength,boolean block){
        int count=upload(field(phase,strength),24);glDepthMask(true);glViewport(0,0,W,H);glClearColor(.025F,.03F,.045F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(block){glEnable(GL_SCISSOR_TEST);glScissor(0,0,W/2,H);glClearDepth(.1);glClear(GL_DEPTH_BUFFER_BIT);glClearDepth(1);glDisable(GL_SCISSOR_TEST);}
        glDepthMask(false);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);var result=BufferUtils.createByteBuffer(W*H*4);glReadPixels(0,0,W,H,GL_RGBA,GL_UNSIGNED_BYTE,result);byte[] bytes=new byte[result.remaining()];result.get(bytes);return bytes;
    }
    private static void save(byte[] bytes,Path path)throws Exception{var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;image.setRGB(x,H-y-1,(bytes[i]&255)<<16|(bytes[i+1]&255)<<8|bytes[i+2]&255);}ImageIO.write(image,"png",path.toFile());}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),out=root.resolve("build/module-shader-check");Files.createDirectories(out);check(glfwInit(),"GLFW init");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);long window=glfwCreateWindow(W,H,"Field shader verification (hidden)",0,0);check(window!=0,"Hidden context");
        try{glfwMakeContextCurrent(window);GL.createCapabilities();System.out.println("OpenGL: "+glGetString(GL_VERSION));String fog;
            try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");}
            for(kind=0;kind<5;kind++){String name="orbital_module";Path dir=root.resolve("src/main/resources/assets/mekgravity/shaders/core");
                int vertex=compile(GL_VERTEX_SHADER,Files.readString(dir.resolve(name+".vsh")).replace("#moj_import <fog.glsl>",fog)),fragment=compile(GL_FRAGMENT_SHADER,Files.readString(dir.resolve(name+".fsh")).replace("#moj_import <fog.glsl>",fog));
                program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);String[] attributes={"Position","UV0","Color"};for(int i=0;i<attributes.length;i++)glBindAttribLocation(program,i,attributes[i]);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
                matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(32),1,.1F,100));glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),80);glUniform1f(glGetUniformLocation(program,"FogEnd"),100);glUniform1i(glGetUniformLocation(program,"FogShape"),0);
                int vao=glGenVertexArrays();glBindVertexArray(vao);int vbo=glGenBuffers(),ebo=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);int stride=24;
                glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,stride,12L);glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,stride,20L);for(int i=0;i<attributes.length;i++)glEnableVertexAttribArray(i);
                glEnable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE);
                var active=frame(0,1,false);var moving=frame(40,1,false);var idle=frame(0,0,false);var blocked=frame(0,1,true);save(active,out.resolve(name+"-"+kind+".png"));save(moving,out.resolve(name+"-"+kind+"-moving.png"));save(idle,out.resolve(name+"-"+kind+"-idle.png"));
                int visible=0,changed=0,energized=0;for(int i=0;i<active.length;i+=4){if((active[i]&255)>30)visible++;if(java.lang.Math.abs((active[i]&255)-(moving[i]&255))>15)changed++;if((active[i]&255)>(idle[i]&255)+10)energized++;}
                check(visible>1500&&changed>300&&energized>300,"Missing geometry/flow/load effect: "+name+" "+visible+"/"+changed+"/"+energized);
                for(int y=0;y<H;y++)for(int x=0;x<W/2;x++)check((blocked[(y*W+x)*4]&255)<15,"Depth leak: "+name);
                {var depth=BufferUtils.createFloatBuffer(W*H);glReadPixels(0,0,W,H,GL_DEPTH_COMPONENT,GL_FLOAT,depth);for(int i=0;i<W*H;i++)if(i%W>=W/2)check(depth.get(i)==1,"Transparent field wrote depth");}
                int quads=(field(0,1).remaining()/24)/4;System.out.println("PASS "+name+" kind="+kind+": actual GLSL, moving/idle pixels, depth occlusion; "+quads+" quads");
                glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
            }check(glGetError()==0,"OpenGL error");
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
