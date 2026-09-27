import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.overloadcore.client.GearGlowMesh;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Actual generated item meshes and GLSL in a hidden context; no Minecraft client. */
public class VerifyGearShader {
    static final int W=384,H=384;static int program;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static int compile(int type,String code){int s=glCreateShader(type);glShaderSource(s,code);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    static void matrix(String name,Matrix4f m){glUniformMatrix4fv(glGetUniformLocation(program,name),false,m.get(new float[16]));}
    static ByteBuffer mesh(int kind,double phase,float charge){float[] vertices=(kind==0?GearGlowMesh.RAIL:GearGlowMesh.BLADE);var bytes=BufferUtils.createByteBuffer(vertices.length/6*28);var rotate=new Matrix3f().rotationX(.25F).rotateY(.6F);
        for(int i=0;i<vertices.length;i+=6){float x=vertices[i],y=vertices[i+1],z=vertices[i+2];var p=rotate.transform(new Vector3f(x-.5F,y-.8F,z-.4F));var n=rotate.transform(new Vector3f(vertices[i+3],vertices[i+4],vertices[i+5]));bytes.putFloat(p.x).putFloat(p.y).putFloat(p.z-2);bytes.putFloat(y+z).putFloat((float)phase);
            bytes.put((byte)kind).put((byte)(charge*255)).put((byte)0).put((byte)255);bytes.put((byte)(n.x*127)).put((byte)(n.y*127)).put((byte)(n.z*127)).put((byte)0);
        }return bytes.flip();
    }
    static byte[] render(int kind,double phase,float charge,boolean occlude){var data=mesh(kind,phase,charge);int quads=data.remaining()/112;glBufferData(GL_ARRAY_BUFFER,data,GL_STATIC_DRAW);var indices=BufferUtils.createIntBuffer(quads*6);for(int q=0;q<quads;q++){int i=q*4;indices.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);}indices.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);glClearColor(.035F,.045F,.055F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(occlude){glEnable(GL_SCISSOR_TEST);glScissor(0,0,W/2,H);glClearDepth(.1);glClear(GL_DEPTH_BUFFER_BIT);glClearDepth(1);glDisable(GL_SCISSOR_TEST);}glDrawElements(GL_TRIANGLES,quads*6,GL_UNSIGNED_INT,0L);var b=BufferUtils.createByteBuffer(W*H*4);glReadPixels(0,0,W,H,GL_RGBA,GL_UNSIGNED_BYTE,b);byte[] result=new byte[b.remaining()];b.get(result);return result;
    }
    static void save(byte[] b,Path file)throws Exception{var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;image.setRGB(x,H-y-1,(b[i]&255)<<16|(b[i+1]&255)<<8|b[i+2]&255);}ImageIO.write(image,"png",file.toFile());}
    public static void main(String[] args)throws Exception{var root=Path.of(args[0]);var out=root.resolve("build/gear-shader-check");Files.createDirectories(out);check(glfwInit(),"GLFW");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);long window=glfwCreateWindow(W,H,"Material shader verification (hidden)",0,0);check(window!=0,"Hidden GL context");
        try{glfwMakeContextCurrent(window);GL.createCapabilities();String fog;try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");}
            var dir=root.resolve("src/main/resources/assets/overloadcore/shaders/core");int vs=compile(GL_VERTEX_SHADER,Files.readString(dir.resolve("gear_field.vsh")).replace("#moj_import <fog.glsl>",fog)),fs=compile(GL_FRAGMENT_SHADER,Files.readString(dir.resolve("gear_field.fsh")).replace("#moj_import <fog.glsl>",fog));program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);String[] attributes={"Position","UV0","Color","Normal"};for(int i=0;i<4;i++)glBindAttribLocation(program,i,attributes[i]);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);for(var name:new String[]{"Position","UV0","Color"})check(glGetAttribLocation(program,name)>=0,"Missing "+name);
            matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().ortho(-1.05F,1.05F,-1.05F,1.05F,.1F,10));glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),8);glUniform1f(glGetUniformLocation(program,"FogEnd"),10);glUniform4f(glGetUniformLocation(program,"FogColor"),0,0,0,1);glUniform1i(glGetUniformLocation(program,"FogShape"),0);
            int vao=glGenVertexArrays(),vbo=glGenBuffers(),ebo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);glVertexAttribPointer(0,3,GL_FLOAT,false,28,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,28,12L);glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,28,20L);glVertexAttribPointer(3,3,GL_BYTE,true,28,24L);for(int i=0;i<4;i++)glEnableVertexAttribArray(i);glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);glViewport(0,0,W,H);
            for(int kind=0;kind<2;kind++){var active=render(kind,0,1,false);var flowing=render(kind,1.1,1,false);var blocked=render(kind,0,1,true);int visible=0,changed=0;for(int i=0;i<active.length;i+=4){if((active[i]&255)+(active[i+1]&255)+(active[i+2]&255)>90)visible++;if(java.lang.Math.abs((active[i]&255)-(flowing[i]&255))>6)changed++;}check(visible>600&&changed>40,"Blank/frozen material "+kind+": "+visible+"/"+changed);for(int y=0;y<H;y++)for(int x=0;x<W/2;x++)check((blocked[(y*W+x)*4]&255)<15,"Material ignored depth");save(active,out.resolve("material-"+kind+".png"));System.out.println("PASS material "+kind+": "+(kind==0?GearGlowMesh.RAIL:GearGlowMesh.BLADE).length/24+" glow quads, animation and occlusion");}
            var full=render(1,0,1,false);var empty=render(1,0,0,false);int dimmed=0;for(int i=0;i<full.length;i+=4)if((full[i]&255)>(empty[i]&255)+20)dimmed++;check(dimmed>300,"Unpowered weapon did not dim");save(empty,out.resolve("blade-empty.png"));check(glGetError()==GL_NO_ERROR,"GL error");glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs);
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
