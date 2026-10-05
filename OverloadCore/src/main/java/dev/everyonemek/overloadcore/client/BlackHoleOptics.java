package dev.everyonemek.overloadcore.client;

import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Camera math only, with no Minecraft state. Shared with the render checks. */
public final class BlackHoleOptics {
    public record Rect(int x,int y,int width,int height) {public boolean empty(){return width<=0||height<=0;}}
    public static Quaternionf orientation(UUID id){
        long bits=id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),19);
        float yaw=(bits&65535)/65535F*(float)(2*Math.PI);
        float lean=.24F+((bits>>>16)&255)/255F*.20F;
        return new Quaternionf().rotationY(yaw).rotateX(lean);
    }
    public static Rect bounds(Vector3f center,float reach,Matrix4f projection,int width,int height){
        if(center.z-reach>=-.01F)return new Rect(0,0,0,0);
        if(center.z+reach>=-.05F)return new Rect(0,0,width,height);
        float minX=1,minY=1,maxX=-1,maxY=-1;
        var p=new Vector4f();
        for(int i=0;i<8;i++){
            projection.transform(p.set(center.x+((i&1)==0?-reach:reach),center.y+((i&2)==0?-reach:reach),center.z+((i&4)==0?-reach:reach),1));
            if(p.w<=0)return new Rect(0,0,width,height);
            minX=Math.min(minX,p.x/p.w);maxX=Math.max(maxX,p.x/p.w);minY=Math.min(minY,p.y/p.w);maxY=Math.max(maxY,p.y/p.w);
        }
        int x=Math.clamp((int)Math.floor((minX+1)*width*.5F)-2,0,width),y=Math.clamp((int)Math.floor((minY+1)*height*.5F)-2,0,height);
        int right=Math.clamp((int)Math.ceil((maxX+1)*width*.5F)+2,0,width),top=Math.clamp((int)Math.ceil((maxY+1)*height*.5F)+2,0,height);
        return new Rect(x,y,Math.max(0,right-x),Math.max(0,top-y));
    }
    public static float pixelRadius(Vector3f center,float radius,Matrix4f projection,int height){
        return radius*Math.abs(projection.m11())*height*.5F/Math.max(radius,-center.z);
    }
    private BlackHoleOptics(){}
}
