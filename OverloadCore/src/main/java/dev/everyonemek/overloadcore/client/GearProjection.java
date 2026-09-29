package dev.everyonemek.overloadcore.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Item and world passes use different FOVs. Preserve the rendered muzzle's screen position. */
public final class GearProjection {
    public static Vector3f worldViewPoint(Vector3f handViewPoint,Matrix4f handProjection,Matrix4f inverseWorldProjection){
        var clip=handProjection.transform(new Vector4f(handViewPoint,1));
        var point=inverseWorldProjection.transform(new Vector4f(clip.x/clip.w,clip.y/clip.w,0,1));
        return new Vector3f(point.x,point.y,point.z).mul(handViewPoint.z/point.z);
    }
    private GearProjection(){}
}
