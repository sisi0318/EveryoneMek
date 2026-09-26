import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.gravity.solar.SolarSphereMesh;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Standalone hidden OpenGL harness. Compiles the real .vsh/.fsh; never launches Minecraft. */
public class VerifySolarShader {
    private static int program,vao,vbo;
    private static final int WIDTH=640,HEIGHT=640;
    private static int compile(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);
        if(glGetShaderi(s,GL_COMPILE_STATUS)==GL_FALSE)throw new AssertionError(glGetShaderInfoLog(s));return s;}
    private static void matrix(String name,Matrix4f m){glUniformMatrix4fv(glGetUniformLocation(program,name),false,m.get(new float[16]));}
    private static ByteBuffer mesh(double distance,double phase,float heat,float offset){
        float[] normals=SolarSphereMesh.vertices(distance);var data=BufferUtils.createByteBuffer(normals.length/3*28);
        float sx=(float)java.lang.Math.cos(phase*.006),sy=(float)java.lang.Math.sin(phase*.006);
        var rotate=new Matrix3f().rotationY(.5F).rotateZ(.2F);
        for(int i=0;i<normals.length;i+=3){float x=normals[i],y=normals[i+1],z=normals[i+2];var n=rotate.transform(new Vector3f(x,y,z));
            data.putFloat(n.x*1.25F+offset).putFloat(n.y*1.25F).putFloat(n.z*1.25F-5);
            data.putFloat(sx).putFloat(sy);
            data.put((byte)java.lang.Math.round((x+1)*127.5F)).put((byte)java.lang.Math.round((y+1)*127.5F)).put((byte)java.lang.Math.round((z+1)*127.5F)).put((byte)java.lang.Math.round(heat*255));
            data.put((byte)(n.x*127)).put((byte)(n.y*127)).put((byte)(n.z*127)).put((byte)0);
        }return data.flip();
    }
    private static void attributes(){int stride=28;
        glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,stride,12L);
        glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,stride,20L);glVertexAttribPointer(3,3,GL_BYTE,true,stride,24L);
        for(int i=0;i<4;i++)glEnableVertexAttribArray(i);
    }
    private static int upload(ByteBuffer data){int count=data.remaining()/28,quads=count/4;glBufferData(GL_ARRAY_BUFFER,data,GL_STATIC_DRAW);
        var indices=BufferUtils.createIntBuffer(quads*6);for(int q=0;q<quads;q++){int i=q*4;indices.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);}indices.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);return quads*6;
    }
    private static byte[] render(double phase,float heat,boolean occluder){
        int count=upload(mesh(0,phase,heat,0));glViewport(0,0,WIDTH,HEIGHT);glClearColor(.035F,.045F,.06F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(occluder){glEnable(GL_SCISSOR_TEST);glScissor(WIDTH/2-12,0,24,HEIGHT);glClearDepth(.1);glClearColor(.15F,.15F,.15F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glClearDepth(1);glDisable(GL_SCISSOR_TEST);}
        glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);var bytes=BufferUtils.createByteBuffer(WIDTH*HEIGHT*4);glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,bytes);byte[] result=new byte[bytes.remaining()];bytes.get(result);return result;
    }
    private static void save(byte[] data,Path file)throws Exception{
        var image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){int i=(y*WIDTH+x)*4;image.setRGB(x,HEIGHT-y-1,0xFF000000|(data[i]&255)<<16|(data[i+1]&255)<<8|data[i+2]&255);}
        ImageIO.write(image,"png",file.toFile());
    }
    private static void geometry(){for(double distance:new double[]{0,16,40}){
        var data=SolarSphereMesh.vertices(distance);int expected=distance==0?864:distance==16?384:96;if(data.length/12!=expected)throw new AssertionError("LOD budget");
        var edges=new HashMap<String,Integer>();
        for(int i=0;i<data.length;i+=12){var a=new Vector3f(data[i],data[i+1],data[i+2]);var b=new Vector3f(data[i+3],data[i+4],data[i+5]);var c=new Vector3f(data[i+6],data[i+7],data[i+8]);if(b.sub(a).cross(c.sub(a)).dot(a)<=0)throw new AssertionError("Winding");
            for(int v=0;v<4;v++){int j=i+v*3,k=i+(v+1)%4*3;var point=new Vector3f(data[j],data[j+1],data[j+2]);if(java.lang.Math.abs(point.length()-1)>1e-6)throw new AssertionError("Radius");String A=key(data,j),B=key(data,k);String edge=A.compareTo(B)<0?A+"/"+B:B+"/"+A;edges.merge(edge,1,Integer::sum);}}
        if(edges.values().stream().anyMatch(n->n!=2))throw new AssertionError("Mesh seam");
    }}
    private static String key(float[] data,int i){return java.lang.Math.round(data[i]*1E6)+","+java.lang.Math.round(data[i+1]*1E6)+","+java.lang.Math.round(data[i+2]*1E6);}
    public static void main(String[] args)throws Exception{
        boolean gravity=args.length>1&&args[1].equals("gravity"),flare=args.length>1&&args[1].equals("flare");geometry();Path root=Path.of(args[0]),out=root.resolve(flare?"build/flare-shader-check":gravity?"build/gravity-shader-check":"build/shader-check");Files.createDirectories(out);
        if(!glfwInit())throw new AssertionError("Cannot initialize standalone OpenGL");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        long window=glfwCreateWindow(WIDTH,HEIGHT,"MekGravity shader validation (hidden)",0,0);if(window==0)throw new AssertionError("Cannot create hidden GL context");
        try{
            glfwMakeContextCurrent(window);GL.createCapabilities();System.out.println("OpenGL "+glGetString(GL_VERSION)+" / "+glGetString(GL_RENDERER));
            String fog;try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");}
            Path shaders=root.resolve("src/main/resources/assets/mekgravity/shaders/core");
            int vertex=compile(GL_VERTEX_SHADER,Files.readString(shaders.resolve(flare?"flare_cell.vsh":"stellar_surface.vsh")).replace("#moj_import <fog.glsl>",fog));
            String noise=Files.readString(shaders.resolve("../include/core_convection.glsl"));
            int fragment=compile(GL_FRAGMENT_SHADER,Files.readString(shaders.resolve(flare?"flare_cell.fsh":gravity?"gravity_surface.fsh":"stellar_surface.fsh")).replace("#moj_import <fog.glsl>",fog).replace("#moj_import <mekgravity:core_convection.glsl>",noise));
            program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);
            String[] attr={"Position","UV0","Color","Normal"};for(int i=0;i<attr.length;i++)glBindAttribLocation(program,i,attr[i]);glLinkProgram(program);if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new AssertionError(glGetProgramInfoLog(program));glUseProgram(program);
            for(String uniform:new String[]{"ModelViewMat","ProjMat","ColorModulator","FogStart","FogEnd","FogColor","FogShape"})if(glGetUniformLocation(program,uniform)<0)throw new AssertionError("Missing uniform "+uniform);
            matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(40),1,.1F,100));
            glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),90);glUniform1f(glGetUniformLocation(program,"FogEnd"),100);glUniform4f(glGetUniformLocation(program,"FogColor"),.1F,.1F,.1F,1);glUniform1i(glGetUniformLocation(program,"FogShape"),0);
            vao=glGenVertexArrays();glBindVertexArray(vao);vbo=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,vbo);int ebo=glGenBuffers();glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);attributes();glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);glDisable(GL_BLEND);
            var active=render(0,1,false);var moving=render(140,1,false);var cold=render(0,0,false);var blocked=render(0,1,true);
            save(active,out.resolve("active.png"));save(moving,out.resolve("flow.png"));save(cold,out.resolve("cold.png"));
            int changed=0,lit=0;long hotEnergy=0,coldEnergy=0;
            for(int i=0;i<active.length;i+=4){if((active[i+(gravity?2:0)]&255)>(gravity?50:100)){lit++;hotEnergy+=(active[i]&255)+(active[i+1]&255)+(active[i+2]&255);coldEnergy+=(cold[i]&255)+(cold[i+1]&255)+(cold[i+2]&255);if(java.lang.Math.abs((active[i+(gravity?2:1)]&255)-(moving[i+(gravity?2:1)]&255))>5)changed++;}}
            if(lit<50000||changed<lit/10||hotEnergy<coldEnergy*1.5)throw new AssertionError("Blank, frozen or incorrect thermal shader");
            int center=(HEIGHT/2*WIDTH+WIDTH/2)*4;if((blocked[center]&255)>50)throw new AssertionError("Shader bypassed solid depth occlusion");
            // Both stars in ONE vertex/index buffer, with different heat and phases. No uniform changes.
            var left=mesh(0,0,1,-1.5F);var right=mesh(0,140,0,1.5F);var pair=BufferUtils.createByteBuffer(left.remaining()+right.remaining());pair.put(left).put(right).flip();int pairCount=upload(pair);
            matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(68),1,.1F,100));glClearColor(.035F,.045F,.06F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glDrawElements(GL_TRIANGLES,pairCount,GL_UNSIGNED_INT,0L);
            var pairPixels=BufferUtils.createByteBuffer(WIDTH*HEIGHT*4);glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pairPixels);long leftHeat=0,rightHeat=0;
            for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){int at=(y*WIDTH+x)*4;if(x<WIDTH/2)leftHeat+=pairPixels.get(at+1)&255;else rightHeat+=pairPixels.get(at+1)&255;}
            if(leftHeat<=rightHeat*(gravity?1.3:2))throw new AssertionError("Batched stars shared heat/phase state");
            matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(40),1,.1F,100));
            // Resource-reload equivalent: compile/link a second program from the same files.
            int reloaded=glCreateProgram();glAttachShader(reloaded,vertex);glAttachShader(reloaded,fragment);for(int i=0;i<attr.length;i++)glBindAttribLocation(reloaded,i,attr[i]);glLinkProgram(reloaded);if(glGetProgrami(reloaded,GL_LINK_STATUS)==0)throw new AssertionError("Shader relink failed");glDeleteProgram(reloaded);
            // Re-upload once per LOD; GPU timer excludes CPU mesh construction and screenshot readback.
            for(double distance:new double[]{0,16,40}){int count=upload(mesh(distance,0,1,0));matrix("ModelViewMat",new Matrix4f().translate(0,0,(float)-distance));glViewport(0,0,WIDTH,HEIGHT);for(int i=0;i<5;i++){glClear(GL_DEPTH_BUFFER_BIT);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);}glFinish();
                int timer=glGenQueries();glBeginQuery(GL_TIME_ELAPSED,timer);for(int i=0;i<100;i++){glClear(GL_DEPTH_BUFFER_BIT);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);}glEndQuery(GL_TIME_ELAPSED);long ns=glGetQueryObjecti64(timer,GL_QUERY_RESULT);glDeleteQueries(timer);System.out.printf(Locale.ROOT,"LOD %.0f: %d quads, %.3f ms/draw in 640px offscreen benchmark%n",distance,count/6,ns/100_000_000D);}
            if(glGetError()!=GL_NO_ERROR)throw new AssertionError("OpenGL error");
            if(!flare){
                // Bake a 16px particle tile directly with the currently selected surface shader.
                // A front-surface patch fills the viewport, so no background pixels enter the atlas.
                var tile=BufferUtils.createByteBuffer(4*28);float nx=.4F,ny=.4F,nz=(float)java.lang.Math.sqrt(1-2*.16);
                for(float[] xy:new float[][]{{-1,-1},{1,-1},{1,1},{-1,1}}){
                    tile.putFloat(xy[0]).putFloat(xy[1]).putFloat(-5).putFloat(1).putFloat(0);
                    tile.put((byte)java.lang.Math.round((xy[0]*nx+1)*127.5F)).put((byte)java.lang.Math.round((xy[1]*ny+1)*127.5F)).put((byte)java.lang.Math.round((nz+1)*127.5F)).put((byte)255);
                    tile.put((byte)0).put((byte)0).put((byte)127).put((byte)0);
                }tile.flip();int count=upload(tile);matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().ortho(-1,1,-1,1,.1F,10));glViewport(0,0,16,16);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0L);
                var bytes=BufferUtils.createByteBuffer(16*16*4);glReadPixels(0,0,16,16,GL_RGBA,GL_UNSIGNED_BYTE,bytes);var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<16;y++)for(int x=0;x<16;x++){int i=(y*16+x)*4;image.setRGB(x,15-y,0xFF000000|(bytes.get(i)&255)<<16|(bytes.get(i+1)&255)<<8|bytes.get(i+2)&255);}
                ImageIO.write(image,"png",root.resolve("src/main/resources/assets/mekgravity/textures/block/"+(gravity?"gravity":"stellar")+"_surface_particle.png").toFile());
            }
            System.out.println("PASS: real GLSL150 compile/link/relink, animated/thermal pixels, depth occlusion, batched independent stars, three closed LOD meshes.");
            glDeleteBuffers(ebo);glDeleteBuffers(vbo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
