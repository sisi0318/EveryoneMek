import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import dev.everyonemek.overloadcore.client.GearEffectGeometry;
import dev.everyonemek.overloadcore.client.TacticalGeometry;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

/** Actual Meka-Tool form accents and combat GLSL in a hidden context; no Minecraft client. */
public class VerifyGearShader {
    static final int W=384,H=384;static int program;static boolean daylight;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static int compile(int type,String code){int s=glCreateShader(type);glShaderSource(s,code);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    static void matrix(String name,Matrix4f m){glUniformMatrix4fv(glGetUniformLocation(program,name),false,m.get(new float[16]));}
    static byte[] draw(ByteBuffer data,boolean occlude){return draw(data,occlude,true);}
    static byte[] draw(ByteBuffer data,boolean occlude,boolean clear){int quads=data.remaining()/112;glBufferData(GL_ARRAY_BUFFER,data,GL_STATIC_DRAW);var indices=BufferUtils.createIntBuffer(quads*6);for(int q=0;q<quads;q++){int i=q*4;indices.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);}indices.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);boolean depthWrite=glGetBoolean(GL_DEPTH_WRITEMASK);
        if(clear){glDepthMask(true);if(daylight)glClearColor(.52F,.70F,.92F,1);else glClearColor(.035F,.045F,.055F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if(occlude){glEnable(GL_SCISSOR_TEST);glScissor(0,0,W/2,H);glClearDepth(.1);glClear(GL_DEPTH_BUFFER_BIT);glClearDepth(1);glDisable(GL_SCISSOR_TEST);}}glDepthMask(depthWrite);glDrawElements(GL_TRIANGLES,quads*6,GL_UNSIGNED_INT,0L);var b=BufferUtils.createByteBuffer(W*H*4);glReadPixels(0,0,W,H,GL_RGBA,GL_UNSIGNED_BYTE,b);byte[] result=new byte[b.remaining()];b.get(result);return result;
    }
    static ByteBuffer filter(ByteBuffer data,int pass){var out=BufferUtils.createByteBuffer(data.limit());var quads=new java.util.ArrayList<Integer>();
        for(int i=0;i<data.limit();i+=112)if(GearEffectGeometry.renderPass(data.get(i+20)&255)==pass)quads.add(i);
        if(pass==1)quads.sort(java.util.Comparator.comparingDouble((Integer i)->{
            float x=0,y=0,z=0;for(int v=0;v<4;v++){x+=data.getFloat(i+v*28);y+=data.getFloat(i+v*28+4);z+=data.getFloat(i+v*28+8);}return -(x*x+y*y+z*z);
        }));
        for(int i:quads)for(int j=0;j<112;j++)out.put(data.get(i+j));return out.flip();}
    static byte[] drawEffect(ByteBuffer data,boolean occlude){
        glDepthMask(true);glDisable(GL_BLEND);draw(filter(data,0),occlude);
        glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glDepthMask(false);draw(filter(data,1),false,false);
        glBlendFunc(GL_SRC_ALPHA,GL_ONE);return draw(filter(data,2),false,false);
    }
    static void save(byte[] b,Path file)throws Exception{var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;image.setRGB(x,H-y-1,(b[i]&255)<<16|(b[i+1]&255)<<8|b[i+2]&255);}ImageIO.write(image,"png",file.toFile());}
    static ByteBuffer effect(int kind,float age,float phase){return effect(kind,age,phase,false);}
    static ByteBuffer effect(int kind,float age,float phase,boolean front){
        var bytes=BufferUtils.createByteBuffer(64*1024);var transform=new Matrix4f();
        if(front)transform.rotateY((float)java.lang.Math.PI);
        else{transform.translate(0,0,-4).rotateX(.65F).rotateY(-.9F);
        if(kind<3)transform.translate(0,0,-1.5F).scale(.48F);else if(kind==7)transform.translate(0,-.9F,0);else transform.translate(0,0,-.2F);}
        GearEffectGeometry.Sink sink=(x,y,z,u,v,mat,power,alpha)->{
            var p=transform.transformPosition(new Vector3f(x,y,z));bytes.putFloat(p.x).putFloat(p.y).putFloat(p.z).putFloat(u).putFloat(v);
            bytes.put((byte)mat).put((byte)(power*255)).put((byte)(phase/(2*java.lang.Math.PI)*255)).put((byte)(alpha*255));bytes.putInt(0);
        };
        if(kind<3)GearEffectGeometry.shot(sink,kind,4,age,true,!front);
        else if(kind<5)GearEffectGeometry.toolForm(sink,kind==3,1,1);
        else if(kind==5)TacticalGeometry.guard(sink,phase>0);
        else if(kind==6)TacticalGeometry.mark(sink,phase>0?3:1);
        else TacticalGeometry.target(sink);
        return bytes.flip();
    }
    static void effects(Path out)throws Exception{
        glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE);glDepthMask(false);
        for(int kind=0;kind<5;kind++){
            var data=effect(kind,kind==0?.75F:1.7F,0);var normal=drawEffect(data,false);var moving=drawEffect(effect(kind,kind==0?.9F:1.7F,1.1F),false);
            var blocked=drawEffect(effect(kind,kind==0?.75F:1.7F,0),true);int visible=0,changed=0;
            for(int i=0;i<normal.length;i+=4){if((normal[i]&255)+(normal[i+1]&255)+(normal[i+2]&255)>90)visible++;
                int difference=0;for(int c=0;c<3;c++)difference=java.lang.Math.max(difference,java.lang.Math.abs((normal[i+c]&255)-(moving[i+c]&255)));if(difference>4)changed++;
            }
            save(normal,out.resolve("effect-"+kind+".png"));check(visible>15&&visible<W*H/5,"Blank or oversized effect "+kind+": "+visible);check(changed>10,"Frozen effect "+kind+": "+changed);
            for(int y=0;y<H;y++)for(int x=0;x<W/2;x++)check((blocked[(y*W+x)*4]&255)<15,"Effect ignored wall depth");
            save(normal,out.resolve("effect-"+kind+".png"));System.out.println("PASS effect "+kind+": "+data.limit()/112+" quads, "+visible+" visible pixels, animation and depth");
        }
        for(int kind=5;kind<8;kind++){
            var data=effect(kind,0,0);var normal=drawEffect(data,false);var changedFrame=drawEffect(effect(kind,0,1.1F),false);var blocked=drawEffect(effect(kind,0,0),true);
            int visible=0,changed=0;for(int i=0;i<normal.length;i+=4){if((normal[i]&255)+(normal[i+1]&255)+(normal[i+2]&255)>90)visible++;for(int c=0;c<3;c++)if(java.lang.Math.abs((normal[i+c]&255)-(changedFrame[i+c]&255))>4){changed++;break;}}
            save(normal,out.resolve("tactical-"+kind+".png"));check(visible>40&&changed>10,"Tactical geometry blank or state does not update: "+kind);
            for(int y=0;y<H;y++)for(int x=0;x<W/2;x++)check((blocked[(y*W+x)*4]&255)<15,"Tactical geometry ignored wall depth");
            System.out.println("PASS tactical "+kind+": "+data.limit()/112+" quads, "+visible+" visible pixels; state and depth");
        }
        save(drawEffect(effect(0,2.2F,0),false),out.resolve("rail-impact.png"));
        matrix("ProjMat",new Matrix4f().perspective((float)java.lang.Math.toRadians(70),1,.05F,100));
        for(int kind=0;kind<2;kind++){
            var front=drawEffect(effect(kind,kind==0?.75F:1.7F,0,true),false);int visible=0,minY=H,maxY=0;
            for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;if((front[i]&255)+(front[i+1]&255)+(front[i+2]&255)>90){visible++;minY=java.lang.Math.min(y,minY);maxY=java.lang.Math.max(y,maxY);}}
            save(front,out.resolve("shooter-"+kind+".png"));check(visible>(kind==0?30:500)&&maxY-minY>(kind==0?7:30),"Effect collapsed to a line from shooter's view: "+kind+" / "+visible+" / "+(maxY-minY));
            System.out.println("PASS shooter's perspective "+kind+": "+visible+" pixels, "+(maxY-minY)+" px high");
        }
        daylight=true;
        for(float age:new float[]{0,.8F,1.7F,3.5F,5.6F}){
            var front=drawEffect(effect(1,age,age*.2F,true),false);int changed=0,white=0,green=0;
            for(int i=0;i<front.length;i+=4){int r=front[i]&255,g=front[i+1]&255,b=front[i+2]&255;
                if(java.lang.Math.max(java.lang.Math.abs(r-133),java.lang.Math.max(java.lang.Math.abs(g-179),java.lang.Math.abs(b-235)))>5)changed++;
                if(r>245&&g>245&&b>245)white++;if(g>r+25&&g>b+5)green++;
            }
            save(front,out.resolve("slash-day-"+age+".png"));
            check(changed>40&&changed<W*H/8,"Slash vanished or became a solid panel against the sky");
            check(white<30,"Slash overexposed to a white sheet: "+white);if(age<2)check(green>15,"Slash lost its green cutting edge in daylight");
            System.out.println("PASS daylight slash "+age+": "+changed+" silhouette pixels, "+white+" white pixels");
        }
        daylight=false;
        for(float length:new float[]{.1F,8,48,192})for(float age:new float[]{0,.08F,.5F,1.5F,4,8,14,15}){
            final float[] bounds={Float.MAX_VALUE,-Float.MAX_VALUE};final int[] count={0};
            GearEffectGeometry.shot((x,y,z,u,v,mat,power,a)->{count[0]++;check(mat!=2,"Rail still emits a continuous beam");if(mat==8){bounds[0]=java.lang.Math.min(bounds[0],z);bounds[1]=java.lang.Math.max(bounds[1],z);}},0,length,age,false);
            if(bounds[1]>=bounds[0])check(bounds[1]-bounds[0]<=.481F&&bounds[0]>=0&&bounds[1]<=length,"Slug stretched/overshot");
            if(age>=java.lang.Math.max(GearEffectGeometry.flightTime(length),1.8F))check(count[0]==0,"Miss retained impact or trail");
        }
        glDepthMask(true);glDisable(GL_BLEND);
    }
    public static void main(String[] args)throws Exception{var root=Path.of(args[0]);var out=root.resolve("build/gear-shader-check");Files.createDirectories(out);check(glfwInit(),"GLFW");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);long window=glfwCreateWindow(W,H,"Material shader verification (hidden)",0,0);check(window!=0,"Hidden GL context");
        try{glfwMakeContextCurrent(window);GL.createCapabilities();String fog;try(var zip=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241-client-extra-aka-minecraft-resources.jar").toFile())){fog=new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replace("#version 150","");}
            var dir=root.resolve("src/main/resources/assets/overloadcore/shaders/core");int vs=compile(GL_VERTEX_SHADER,Files.readString(dir.resolve("gear_field.vsh")).replace("#moj_import <fog.glsl>",fog)),fs=compile(GL_FRAGMENT_SHADER,Files.readString(dir.resolve("gear_field.fsh")).replace("#moj_import <fog.glsl>",fog));program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);String[] attributes={"Position","UV0","Color","Normal"};for(int i=0;i<4;i++)glBindAttribLocation(program,i,attributes[i]);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);for(var name:new String[]{"Position","UV0","Color"})check(glGetAttribLocation(program,name)>=0,"Missing "+name);
            matrix("ModelViewMat",new Matrix4f());matrix("ProjMat",new Matrix4f().ortho(-1.05F,1.05F,-1.05F,1.05F,.1F,10));glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);glUniform1f(glGetUniformLocation(program,"FogStart"),8);glUniform1f(glGetUniformLocation(program,"FogEnd"),10);glUniform4f(glGetUniformLocation(program,"FogColor"),0,0,0,1);glUniform1i(glGetUniformLocation(program,"FogShape"),0);
            int vao=glGenVertexArrays(),vbo=glGenBuffers(),ebo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);glVertexAttribPointer(0,3,GL_FLOAT,false,28,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,28,12L);glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,28,20L);glVertexAttribPointer(3,3,GL_BYTE,true,28,24L);for(int i=0;i<4;i++)glEnableVertexAttribArray(i);glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);glViewport(0,0,W,H);
            effects(out);check(glGetError()==GL_NO_ERROR,"GL error");glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs);
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
