import dev.everyonemek.gravity.CoreMotion;
import dev.everyonemek.gravity.solar.SolarField;
import java.nio.file.*;
import java.util.*;

/** Run with JDK 21 and build/classes/java/main on the classpath. No Minecraft client. */
public class ExportSolarField {
    public static void main(String[] args)throws Exception{
        var frames=new ArrayList<String>();int maximum=0;
        for(float strength:new float[]{0,.01F,.3F,.65F,1})for(int frame=0;frame<240;frame++){
            double phase=frame*2.5;var rows=new ArrayList<String>();int[] quads={0};
            SolarField.emit(phase,strength,(a,b,width,rgb,alpha,halo)->{
                quads[0]+=halo?2:1;
                for(var p:new SolarField.Point[]{a,b})if(!Double.isFinite(p.x()+p.y()+p.z())||Math.max(Math.max(Math.abs(p.x()),Math.abs(p.y())),Math.abs(p.z()))+width*3>4)throw new AssertionError("Outside seed render bounds");
                if(width<=0||alpha<0||alpha>255)throw new AssertionError("Invalid ribbon");
                if(strength==1&&(frameIndex(phase)==24||frameIndex(phase)==60||frameIndex(phase)==96))rows.add(String.format(Locale.ROOT,"[%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d,%d,%s]",a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),width,rgb,alpha,halo));
            });
            if(strength==0&&quads[0]!=0||quads[0]>SolarField.MAX_QUADS)throw new AssertionError("Effect exceeded mesh budget");
            maximum=Math.max(maximum,quads[0]);if(!rows.isEmpty())frames.add("{\"phase\":"+phase+",\"ribbons\":["+String.join(",",rows)+"]}");
        }
        for(int frame=0;frame<240;frame++)SolarField.emit(frame*2.5,.2F,1,(a,b,width,rgb,alpha,halo)->{
            for(var p:new SolarField.Point[]{a,b})if(!Double.isFinite(p.x()+p.y()+p.z())||Math.max(Math.max(Math.abs(p.x()),Math.abs(p.y())),Math.abs(p.z()))+width*3>4)throw new AssertionError("Hot standby exceeded render bounds");
        });
        int[] full={0},reduced={0};SolarField.emit(100,1,1,false,(a,b,w,rgb,alpha,halo)->full[0]++);SolarField.emit(100,1,1,true,(a,b,w,rgb,alpha,halo)->reduced[0]++);
        if(full[0]!=430||reduced[0]!=76)throw new AssertionError("Quality preset geometry budgets changed: "+full[0]+"/"+reduced[0]);
        System.out.println("Quality presets: "+full[0]+" full / "+reduced[0]+" reduced field segments.");
        // The existing world-time smoothing is shared by the solar renderer.
        var motion=new CoreMotion();motion.update(0,1);for(int t=1;t<=120;t++)motion.update(t,1);
        if(motion.strength()<.99)throw new AssertionError("Field did not start");
        double paused=motion.phase();motion.update(120,0);if(motion.phase()!=paused)throw new AssertionError("Paused field moved");
        for(int t=121;t<=320;t++)motion.update(t,0);if(motion.strength()!=0)throw new AssertionError("Field failed to stop");
        Path target=Path.of(args[0]);Files.createDirectories(target.getParent());Files.writeString(target,"["+String.join(",",frames)+"]\n");
        System.out.println("Validated 1200 field samples + 240 hot standby samples; maximum "+maximum+" quads; startup/pause/stop; exported 3 runtime frames.");
    }
    private static int frameIndex(double phase){return (int)Math.round(phase/2.5);}
}
