import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Reproduces the filled-background depth failure with Minecraft's actual text shader and digit atlas.
 * Checks the replacement native shadow at two distances, both atlas flush orders, and behind a wall.
 * No Minecraft client is started and no upstream assets are copied into the project or release JAR. */
public final class VerifyTrainingText {
    static final int SIZE=384;
    static int program,atlas,white;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static String source(ZipFile zip,String name)throws Exception{return new String(zip.getInputStream(zip.getEntry("assets/minecraft/"+name)).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
    static int shader(int type,String source){int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);check(glGetShaderi(shader,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(shader));return shader;}
    static int texture(BufferedImage image){
        var pixels=BufferUtils.createByteBuffer(image.getWidth()*image.getHeight()*4);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int c=image.getRGB(x,y);pixels.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>24));}pixels.flip();
        int texture=glGenTextures();glBindTexture(GL_TEXTURE_2D,texture);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,image.getWidth(),image.getHeight(),0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);return texture;
    }
    static void vertex(ByteBuffer data,Matrix4f pose,float x,float y,float z,int color,float u,float v){
        var p=pose.transformPosition(new Vector3f(x,y,z));data.putFloat(p.x).putFloat(p.y).putFloat(p.z);
        data.put((byte)(color>>16)).put((byte)(color>>8)).put((byte)color).put((byte)(color>>24)).putFloat(u).putFloat(v);
    }
    static void quad(Matrix4f pose,float x,float y,float w,float h,float z,int color,float u,float v,float U,float V){
        var data=BufferUtils.createByteBuffer(4*24);
        vertex(data,pose,x,y,z,color,u,v);vertex(data,pose,x,y+h,z,color,u,V);
        vertex(data,pose,x+w,y+h,z,color,U,V);vertex(data,pose,x+w,y,z,color,U,v);
        data.flip();glBufferData(GL_ARRAY_BUFFER,data,GL_STREAM_DRAW);glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0L);
    }
    static void text(Matrix4f pose,boolean shadow){
        glBindTexture(GL_TEXTURE_2D,atlas);int[] chars={'1','.','5'};
        for(int i=0;i<chars.length;i++){
            int c=chars[i];float u=(c%16)/16F,v=(c/16)/16F;
            quad(pose,i*6+(shadow?1:0),shadow?1:0,8,8,0,shadow?0xFF3A3F30:0xFFE9FFC1,u,v,u+1/16F,v+1/16F);
        }
    }
    static byte[] frame(float distance,boolean oldBackground,boolean reverse,boolean wall){
        glClearDepth(wall?.1:1);glClearColor(.035F,.045F,.055F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glClearDepth(1);
        var pose=new Matrix4f().translate(-.216F,.096F,-distance).scale(.024F,-.024F,.024F);
        if(oldBackground){
            // Font.StringRenderOutput.finish uses a white glyph at +.01, ahead of normal glyphs at z=0.
            glBindTexture(GL_TEXTURE_2D,white);quad(pose,-1,-1,22,10,.01F,0x55000000,0,0,1,1);text(pose,false);
        }else{
            // Font.drawInternal uses SHADOW_OFFSET=(0,0,.03) for the foreground glyphs.
            var foreground=new Matrix4f(pose).translate(0,0,.03F);
            if(reverse){text(foreground,false);text(pose,true);}else{text(pose,true);text(foreground,false);}
        }
        var pixels=BufferUtils.createByteBuffer(SIZE*SIZE*4);glReadPixels(0,0,SIZE,SIZE,GL_RGBA,GL_UNSIGNED_BYTE,pixels);byte[] result=new byte[pixels.remaining()];pixels.get(result);return result;
    }
    static int visible(byte[] pixels){int count=0;for(int i=0;i<pixels.length;i+=4)if((pixels[i+1]&255)>200)count++;return count;}
    static void save(byte[] pixels,Path path)throws Exception{
        var image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int i=(y*SIZE+x)*4;image.setRGB(x,SIZE-1-y,(pixels[i]&255)<<16|(pixels[i+1]&255)<<8|(pixels[i+2]&255));}ImageIO.write(image,"png",path.toFile());
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),out=root.resolve("build/training-text-check");Files.createDirectories(out);
        check(glfwInit(),"GLFW");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        long window=glfwCreateWindow(SIZE,SIZE,"Training text depth check (hidden)",0,0);check(window!=0,"Hidden GL context");
        try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){
            glfwMakeContextCurrent(window);GL.createCapabilities();String fog=source(zip,"shaders/include/fog.glsl").replace("#version 150","");
            int vs=shader(GL_VERTEX_SHADER,source(zip,"shaders/core/rendertype_text.vsh").replace("#moj_import <fog.glsl>",fog));
            int fs=shader(GL_FRAGMENT_SHADER,source(zip,"shaders/core/rendertype_text.fsh").replace("#moj_import <fog.glsl>",fog));
            program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);String[] attrs={"Position","Color","UV0","UV2"};for(int i=0;i<attrs.length;i++)glBindAttribLocation(program,i,attrs[i]);
            glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
            glUniformMatrix4fv(glGetUniformLocation(program,"ModelViewMat"),false,new Matrix4f().get(new float[16]));
            glUniformMatrix4fv(glGetUniformLocation(program,"ProjMat"),false,new Matrix4f().perspective((float)java.lang.Math.toRadians(70),1,.05F,100).get(new float[16]));
            glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),64);glUniform1f(glGetUniformLocation(program,"FogEnd"),96);glUniform4f(glGetUniformLocation(program,"FogColor"),0,0,0,1);glUniform1i(glGetUniformLocation(program,"FogShape"),0);
            atlas=texture(ImageIO.read(zip.getInputStream(zip.getEntry("assets/minecraft/textures/font/ascii.png"))));
            var whitePixel=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);whitePixel.setRGB(0,0,0xFFFFFFFF);white=texture(whitePixel);
            glActiveTexture(GL_TEXTURE2);glBindTexture(GL_TEXTURE_2D,white);glUniform1i(glGetUniformLocation(program,"Sampler2"),2);glActiveTexture(GL_TEXTURE0);glUniform1i(glGetUniformLocation(program,"Sampler0"),0);
            int vao=glGenVertexArrays(),vbo=glGenBuffers(),ebo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,BufferUtils.createIntBuffer(6).put(new int[]{0,1,2,0,2,3}).flip(),GL_STATIC_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,24,0L);glVertexAttribPointer(1,4,GL_UNSIGNED_BYTE,true,24,12L);glVertexAttribPointer(2,2,GL_FLOAT,false,24,16L);for(int i=0;i<3;i++)glEnableVertexAttribArray(i);glVertexAttribI2i(3,0,0);
            glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);glDepthMask(true);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glEnable(GL_CULL_FACE);glViewport(0,0,SIZE,SIZE);
            var old=frame(2.5F,true,false,false);save(old,out.resolve("old-background.png"));check(visible(old)==0,"Old foreground background did not reproduce missing glyphs");
            for(float distance:new float[]{2.5F,8})for(boolean reverse:new boolean[]{false,true}){
                var fixed=frame(distance,false,reverse,false);int count=visible(fixed);check(count>3,"Text disappeared after an atlas flush order change");
                check(visible(frame(distance,false,reverse,true))==0,"Text became visible through a wall");
                if(distance==2.5F&&!reverse)save(fixed,out.resolve("fixed-shadow.png"));
                System.out.println("PASS native text at "+distance+" blocks, reverse order="+reverse+": "+count+" glyph pixels; wall occlusion preserved");
            }
            check(glGetError()==GL_NO_ERROR,"GL error");glDeleteTextures(atlas);glDeleteTextures(white);glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs);
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
