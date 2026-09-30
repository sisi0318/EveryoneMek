import com.google.gson.*;
import dev.everyonemek.overloadcore.client.MekaToolAnchor;
import dev.everyonemek.overloadcore.client.GearProjection;
import java.nio.file.*;
import java.util.zip.ZipFile;
import org.joml.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Dependency bytecode and original OBJ anchors; no Minecraft client is started. */
public class VerifyMekaToolContract {
    static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);}
    static ClassNode clazz(ZipFile jar,String name)throws Exception{var c=new ClassNode();new ClassReader(jar.getInputStream(jar.getEntry(name)).readAllBytes()).accept(c,0);return c;}
    static Vector3f vec(JsonObject json,String key){var a=json.getAsJsonArray(key);return a==null?new Vector3f():new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);
        try(var mc=new ZipFile(root.resolve("build/moddev/artifacts/neoforge-21.1.241.jar").toFile());var mek=new ZipFile(root.resolve("build/reference/Mekanism-1.21.1-10.7.19.85-sources.jar").toFile())){
            var renderer=clazz(mc,"net/minecraft/client/renderer/entity/ItemRenderer.class");int matches=0;
            for(var method:renderer.methods)if(method.name.equals("render")&&method.desc.startsWith("(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;")){
                int pops=0;for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("com/mojang/blaze3d/vertex/PoseStack")&&call.name.equals("popPose"))pops++;
                check(pops==1,"Native item render hook no longer has one final pose pop");matches++;
            }
            check(matches==1,"Native item render method changed");
            for(boolean left:new boolean[]{false,true}){
                String hand=left?"left":"right",group="";float minX=100,maxX=-100,minY=100;
                String obj=new String(mek.getInputStream(mek.getEntry("assets/mekanism/models/entity/mekatool_"+hand+".obj")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                for(String line:obj.split("\\R")){
                    if(line.startsWith("g ")||line.startsWith("o "))group=line.substring(2);
                    if(line.startsWith("v ")&&(group.endsWith("barrel3")||group.endsWith("barrel4"))){var v=line.split("\\s+");float x=Float.parseFloat(v[1]),y=Float.parseFloat(v[2]);minX=java.lang.Math.min(minX,x);maxX=java.lang.Math.max(maxX,x);minY=java.lang.Math.min(minY,y);}
                }
                var anchor=MekaToolAnchor.nozzle(left);var center=anchor.transformPosition(new Vector3f());
                check(java.lang.Math.abs(center.x-(minX+maxX)/2)<.00001&&center.y<minY&&minY-center.y<.02,"Effect detached from native "+hand+" nozzle");
                var forward=anchor.transformDirection(new Vector3f(0,0,1));check(forward.y<-.99F,"Effect points into original tool");
                String file="assets/mekanism/models/item/meka_tool"+(left?"_left":"")+".json";
                var model=JsonParser.parseString(new String(mek.getInputStream(mek.getEntry(file)).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if(!left)model=model.getAsJsonObject("base");var display=model.getAsJsonObject("display").getAsJsonObject("firstperson_"+hand+"hand");
                var t=vec(display,"translation").div(16);var r=vec(display,"rotation").mul((float)(java.lang.Math.PI/180));int sign=left?-1:1;
                var held=new Matrix4f().translate(sign*.56F,-.52F,-.72F).translate(sign*t.x,t.y,t.z).rotateXYZ(r.x,sign*r.y,sign*r.z).translate(-.5F,-.5F,-.5F).mul(anchor);
                var point=held.transformPosition(new Vector3f());check(point.z<-.15F,"Native nozzle clips camera");
                var handProj=new Matrix4f().perspective((float)java.lang.Math.toRadians(70),16/9F,.05F,100);
                for(int fov:new int[]{50,70,90,110}){
                    var worldProj=new Matrix4f().perspective((float)java.lang.Math.toRadians(fov),16/9F,.05F,100);
                    var projected=worldProj.transformProject(GearProjection.worldViewPoint(point,handProj,new Matrix4f(worldProj).invert()));
                    var original=handProj.transformProject(new Vector3f(point));check(java.lang.Math.abs(projected.x-original.x)<.00001&&java.lang.Math.abs(projected.y-original.y)<.00001,"FOV detached native muzzle");
                }
            }
        }
        var config=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/overloadcore.mixins.json"))).getAsJsonObject();
        check(config.getAsJsonArray("client").toString().contains("MekaToolRenderMixin")&&!config.getAsJsonArray("mixins").toString().contains("MekaToolRenderMixin"),"Client hook exposed to server");
        System.out.println("PASS native ItemRenderer hook, original right/left OBJ nozzle alignment and four world FOVs; client-only registration");
    }
}
