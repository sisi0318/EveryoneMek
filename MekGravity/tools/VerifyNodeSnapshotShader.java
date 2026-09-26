import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Actual node GLSL and NEW_ENTITY layout, with a sampled Minecraft sprite, in a hidden GL context. */
public class VerifyNodeSnapshotShader {
    private static final int W=384,H=384;private static int program;
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static int compile(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    private static void matrix(String name,Matrix4f value){glUniformMatrix4fv(glGetUniformLocation(program,name),false,value.get(new float[16]));}
    private static ByteBuffer quad(int kind,double phase,float alpha){var bytes=BufferUtils.createByteBuffer(4*36);int tint=kind==2?0x3F76E4:kind==3?0xDEA5C6:0xFFFFFF;
        for(int i=0;i<4;i++){float u=i<2?0:1,v=i==0||i==3?1:0;bytes.putFloat((u-.5F)*1.4F).putFloat((.5F-v)*1.4F).putFloat(-2);
            bytes.put((byte)(tint>>16)).put((byte)(tint>>8)).put((byte)tint).put((byte)(alpha*240));bytes.putFloat(u).putFloat(v);
            bytes.putShort((short)(u*256)).putShort((short)(v*256)).putShort((short)0).putShort((short)0);
            bytes.put((byte)(java.lang.Math.cos(phase)*127)).put((byte)(java.lang.Math.sin(phase)*127)).put((byte)java.lang.Math.round(kind/3F*127)).put((byte)0);
        }return bytes.flip();
    }
    private static byte[] render(int kind,double phase,float strength,boolean occlude){glBufferData(GL_ARRAY_BUFFER,quad(kind,phase,strength),GL_STATIC_DRAW);glDepthMask(true);glClearColor(.025F,.03F,.04F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(occlude){glEnable(GL_SCISSOR_TEST);glScissor(0,0,W/2,H);glClearDepth(.1);glClear(GL_DEPTH_BUFFER_BIT);glClearDepth(1);glDisable(GL_SCISSOR_TEST);}glDepthMask(false);glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0L);var b=BufferUtils.createByteBuffer(W*H*4);glReadPixels(0,0,W,H,GL_RGBA,GL_UNSIGNED_BYTE,b);byte[] result=new byte[b.remaining()];b.get(result);return result;
    }
    private static void save(byte[] b,Path path)throws Exception{var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;image.setRGB(x,H-y-1,(b[i]&255)<<16|(b[i+1]&255)<<8|b[i+2]&255);}ImageIO.write(image,"png",path.toFile());}
    public static void main(String[] args)throws Exception{var root=Path.of(args[0]);var out=root.resolve("build/node-snapshot-check");Files.createDirectories(out);check(glfwInit(),"GLFW unavailable");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);long window=glfwCreateWindow(W,H,"Node shader validation (hidden)",0,0);check(window!=0,"No hidden context");
        try{glfwMakeContextCurrent(window);GL.createCapabilities();String fog;BufferedImage sprite;
            try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");sprite=ImageIO.read(zip.getInputStream(zip.getEntry("assets/minecraft/textures/item/iron_ingot.png")));}
            var dir=root.resolve("src/main/resources/assets/mekgravity/shaders/core");int vertex=compile(GL_VERTEX_SHADER,Files.readString(dir.resolve("node_snapshot.vsh")).replace("#moj_import <fog.glsl>",fog));int fragment=compile(GL_FRAGMENT_SHADER,Files.readString(dir.resolve("node_snapshot.fsh")).replace("#moj_import <fog.glsl>",fog));program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);String[] attributes={"Position","Color","UV0","UV1","UV2","Normal"};for(int i=0;i<attributes.length;i++)glBindAttribLocation(program,i,attributes[i]);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);for(var attribute:attributes)check(glGetAttribLocation(program,attribute)>=0,"Missing attribute "+attribute);
            matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().ortho(-.85F,.85F,-.85F,.85F,.1F,10));glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),8);glUniform1f(glGetUniformLocation(program,"FogEnd"),10);glUniform4f(glGetUniformLocation(program,"FogColor"),0,0,0,1);glUniform1i(glGetUniformLocation(program,"FogShape"),0);glUniform1i(glGetUniformLocation(program,"Sampler0"),0);
            int texture=glGenTextures();glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);var pixels=BufferUtils.createByteBuffer(sprite.getWidth()*sprite.getHeight()*4);for(int y=0;y<sprite.getHeight();y++)for(int x=0;x<sprite.getWidth();x++){int argb=sprite.getRGB(x,y);pixels.put((byte)(argb>>16)).put((byte)(argb>>8)).put((byte)argb).put((byte)(argb>>24));}pixels.flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,sprite.getWidth(),sprite.getHeight(),0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
            int vao=glGenVertexArrays(),vbo=glGenBuffers(),ebo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);glBufferData(GL_ELEMENT_ARRAY_BUFFER,new int[]{0,1,2,0,2,3},GL_STATIC_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,36,0L);glVertexAttribPointer(1,4,GL_UNSIGNED_BYTE,true,36,12L);glVertexAttribPointer(2,2,GL_FLOAT,false,36,16L);glVertexAttribIPointer(3,2,GL_SHORT,36,24L);glVertexAttribIPointer(4,2,GL_SHORT,36,28L);glVertexAttribPointer(5,3,GL_BYTE,true,36,32L);for(int i=0;i<6;i++)glEnableVertexAttribArray(i);
            glEnable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glViewport(0,0,W,H);
            for(int kind=0;kind<4;kind++){var active=render(kind,0,1,false);var moving=render(kind,1.8,1,false);var idle=render(kind,0,0,false);var blocked=render(kind,0,1,true);int visible=0,changed=0;
                for(int i=0;i<active.length;i+=4){if((active[i]&255)+(active[i+1]&255)+(active[i+2]&255)>80)visible++;if(java.lang.Math.abs((active[i+1]&255)-(moving[i+1]&255))>8)changed++;check((idle[i]&255)<12,"Idle projection failed to fade");}
                check(visible>1500&&changed>300,"Missing or frozen projection "+kind+": "+visible+" / "+changed);for(int y=0;y<H;y++)for(int x=0;x<W/2;x++)check((blocked[(y*W+x)*4]&255)<12,"Projection ignored solid occlusion");var depth=BufferUtils.createFloatBuffer(W*H);glReadPixels(0,0,W,H,GL_DEPTH_COMPONENT,GL_FLOAT,depth);for(int y=0;y<H;y++)for(int x=W/2;x<W;x++)check(depth.get(y*W+x)==1,"Projection wrote transparent depth");save(active,out.resolve("type-"+kind+".png"));System.out.println("PASS resource "+kind+": 1 quad, sprite/bolt, animation, fade, depth occlusion");
            }
            // A categorical varying must not switch shader branches under perspective interpolation.
            matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(58),1,.03F,100));
            for(int view=0;view<12;view++){
                matrix("ModelViewMat",new Matrix4f().translate(0,0,-2.5F).rotateY(view*.071F).rotateX(.23F+view*.017F).translate(0,0,2));
                var red=BufferUtils.createByteBuffer(4);red.put((byte)255).put((byte)0).put((byte)0).put((byte)255).flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,red);var a=render(1,.6,1,false);
                var green=BufferUtils.createByteBuffer(4);green.put((byte)0).put((byte)255).put((byte)0).put((byte)255).flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,green);var b=render(1,.6,1,false);
                int leaked=0;for(int i=0;i<a.length;i++)if(a[i]!=b[i])leaked++;
                check(leaked==0,"Perspective energy branch sampled the atlas: view="+view+", changed channels="+leaked);
            }
            System.out.println("PASS: 12 perspective views; energy branch is independent of atlas contents.");
            check(glGetError()==GL_NO_ERROR,"GL error");glDeleteTextures(texture);glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
