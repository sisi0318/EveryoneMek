package dev.everyonemek.overloadcore.client;

import org.joml.Matrix4f;

/** Camera-space grip transforms, also used by the offline held-model check. */
public final class GearPose {
    public static Matrix4f first(boolean rail, int side, float charge, float equip, float swing) {
        float stroke=(float)Math.sin(Math.sqrt(swing)*Math.PI);
        var pose=new Matrix4f();
        if (rail) {
            pose.translate(side*(.34F-.12F*charge),-.36F+.04F*charge-.6F*equip,-.95F-.06F*charge)
                .rotateXYZ(.08F+.07F*charge,side*(.13F-.08F*charge),side*-.06F).scale(.60F);
        } else {
            pose.translate(side*(.46F-.04F*charge-.22F*stroke),-.67F+.04F*charge-.6F*equip,-1.05F)
                .rotateXYZ(-.22F-.14F*charge-.45F*stroke,side*.15F*stroke,side*(-.22F-.16F*charge-1.0F*stroke)).scale(.90F);
        }
        // ItemRenderer subsequently translates by (-.5, -.5, -.5). Center on the actual grip.
        return pose.translate(0,.5F-3/16F,rail?.5F-12/16F:0);
    }
    private GearPose() {}
}
