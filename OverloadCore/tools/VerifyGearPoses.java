import com.google.gson.*;
import dev.everyonemek.overloadcore.client.GearPose;
import dev.everyonemek.overloadcore.client.GearProjection;
import java.nio.file.*;
import java.util.*;
import org.joml.*;

/** Actual first-person helper + generated display transforms + 1.21.1 ItemInHandLayer chain. No game launch. */
public class VerifyGearPoses {
    static float rad(float deg){return deg*(float)(java.lang.Math.PI/180);}
    static Vector3f vec(JsonObject j,String key,float fallback){var a=j.getAsJsonArray(key);return a==null?new Vector3f(fallback):new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    static Matrix4f display(JsonObject d,int side){var t=vec(d,"translation",0).div(16);var r=vec(d,"rotation",0);return new Matrix4f().translate(t.x*side,t.y,t.z).rotateXYZ(rad(r.x),rad(r.y*side),rad(r.z*side)).scale(vec(d,"scale",1));}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Map<String,Object> box(float[] a,float[] b,Matrix4f matrix){return Map.of("from",a,"to",b,"matrix",matrix.get(new float[16]));}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var panels=new ArrayList<Map<String,Object>>();
        for(boolean first:new boolean[]{true,false})for(boolean rail:new boolean[]{true,false})for(int charge=0;charge<=1;charge++)for(int sign:new int[]{1,-1}){
            String name=rail?"rail_lance":"thunder_blade",hand=sign==1?"righthand":"lefthand";
            var json=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/assets/overloadcore/models/item/"+name+".json"))).getAsJsonObject();
            var d=json.getAsJsonObject("display").getAsJsonObject((first?"firstperson_":"thirdperson_")+hand);
            var grip=new Vector3f(.5F,3/16F,rail?12/16F:.5F);Matrix4f matrix,project;
            var boxes=new ArrayList<Map<String,Object>>();
            if(first){
                check(display(d,sign).equals(new Matrix4f(),.00001F),"First-person JSON must not reapply pose");
                matrix=GearPose.first(rail,sign,charge,0,0).mul(display(d,sign)).translate(-.5F,-.5F,-.5F);
                project=new Matrix4f().perspective(rad(70),16/9F,.05F,100);
                // The item pass has its own 70 degree projection, independent of world FOV.
                // Idle/full charge must not vanish or expand into a screen-covering box.
                var fallback=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/assets/overloadcore/models/item/"+name+"_fallback.json"))).getAsJsonObject();
                int visible=0,total=0;float min=100,max=-100;
                for(var element:fallback.getAsJsonArray("elements"))for(String corner:new String[]{"from","to"}){
                    var p=matrix.transformPosition(vec(element.getAsJsonObject(),corner,0).div(16));check(p.z<-.15F,"Weapon clips camera: "+name);
                    var clip=project.transformProject(new Vector3f(p));if(java.lang.Math.abs(clip.x)<1&&java.lang.Math.abs(clip.y)<1)visible++;total++;
                    min=java.lang.Math.min(min,clip.x);max=java.lang.Math.max(max,clip.x);
                }
                check(visible>total*.75,"Weapon outside screen: "+name+" "+visible+"/"+total);check(max-min<.9,"Weapon fills screen: "+name+" / "+charge+" / "+sign+" / "+min+".."+max);
                var mirror=GearPose.first(rail,-sign,charge,0,0).translate(-.5F,-.5F,-.5F).transformPosition(new Vector3f(grip));
                var anchor=matrix.transformPosition(new Vector3f(grip));check(java.lang.Math.abs(anchor.x+mirror.x)<.001&&java.lang.Math.abs(anchor.y-mirror.y)<.001,"Left grip is not mirrored");
                if(!rail){var tip=matrix.transformPosition(new Vector3f(.5F,28/16F,.5F));check(tip.distance(anchor)>1.35F,"Blade still has miniature held scale");}
                else for(int fov:new int[]{50,70,90,110}){
                    var world=new Matrix4f().perspective(rad(fov),16/9F,.05F,200);
                    var muzzle=matrix.transformPosition(new Vector3f(.5F,7/16F,-.765F));
                    var aligned=GearProjection.worldViewPoint(muzzle,project,new Matrix4f(world).invert());
                    var screen=project.transformProject(new Vector3f(muzzle));var actual=world.transformProject(aligned);
                    check(java.lang.Math.abs(screen.x-actual.x)<.00001&&java.lang.Math.abs(screen.y-actual.y)<.00001,"Muzzle detached under world FOV "+fov);
                }
            }else{
                var body=new Matrix4f().translate(0,1.5F,0).scale(-1,-1,1);
                float xr=rail?-(float)java.lang.Math.PI/2:charge==1?-.7F:-.314F,yr=rail?-.10F*sign:charge==1?-.2F*sign:0,zr=!rail&&charge==1?-.15F*sign:0;
                var arm=new Matrix4f(body).translate(-sign*5/16F,2/16F,0).rotateZYX(zr,yr,xr);
                var handMatrix=new Matrix4f(arm).rotateX(rad(-90)).rotateY(rad(180)).translate(sign/16F,.125F,-.625F);
                matrix=new Matrix4f(handMatrix).mul(display(d,sign)).translate(-.5F,-.5F,-.5F);
                check(matrix.transformPosition(new Vector3f(grip)).distance(handMatrix.transformPosition(new Vector3f()))<.0001,"Grip missed third-person hand");
                if(rail){var forward=matrix.transformDirection(new Vector3f(0,0,-1)).normalize();check(forward.z<-.95,"Rail points sideways or backwards");}
                project=new Matrix4f().perspective(rad(45),16/9F,.05F,100).lookAt(3,2.4F,-4,0,1,0,0,1,0);
                boxes.add(box(new float[]{-.25F,0,-.125F},new float[]{.25F,.75F,.125F},body));
                boxes.add(box(new float[]{-.25F,-.5F,-.25F},new float[]{.25F,0,.25F},body));
                boxes.add(box(new float[]{-.125F,-.125F,-.125F},new float[]{.125F,.625F,.125F},arm));
                var support=new Matrix4f(body).translate(sign*5/16F,2/16F,0).rotateY(rail?.55F*sign:0).rotateX(rail?-(float)java.lang.Math.PI/2+.12F:0);
                boxes.add(box(new float[]{-.125F,-.125F,-.125F},new float[]{.125F,.625F,.125F},support));
            }
            var panel=new LinkedHashMap<String,Object>();panel.put("label",name+" / "+hand+" / "+(charge==1?"charged":"idle"));panel.put("first",first);panel.put("name",name+"_fallback");panel.put("matrix",matrix.get(new float[16]));panel.put("projection",project.get(new float[16]));panel.put("boxes",boxes);panels.add(panel);
        }
        var dir=root.resolve("build/weapon-visual-check");Files.createDirectories(dir);Files.writeString(dir.resolve("poses.json"),new Gson().toJson(panels));
        System.out.println("PASS 16 held poses: generated transforms, camera clearance, mirrored grips, forward rail axis; matrices exported");
    }
}
