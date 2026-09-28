package dev.everyonemek.overloadcore.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/** Only loaded when the client ArmPose enum is extended; no renderer initialization here. */
public final class GearArmPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> RAIL_HOLD=new EnumProxy<>(HumanoidModel.ArmPose.class,true,
        (IArmPoseTransformer)(m,e,arm)->railArms(m,arm,true));
    public static final EnumProxy<HumanoidModel.ArmPose> RAIL_SINGLE=new EnumProxy<>(HumanoidModel.ArmPose.class,false,
        (IArmPoseTransformer)(m,e,arm)->railArms(m,arm,false));
    public static final EnumProxy<HumanoidModel.ArmPose> BLADE_READY=new EnumProxy<>(HumanoidModel.ArmPose.class,false,
        (IArmPoseTransformer)(m,e,arm)->{
            var hand=arm==HumanoidArm.RIGHT?m.rightArm:m.leftArm;int sign=arm==HumanoidArm.RIGHT?1:-1;
            hand.xRot=-.70F+m.head.xRot*.4F;hand.yRot=-.2F*sign;hand.zRot=-.15F*sign;
        });
    private static void railArms(HumanoidModel<?> m,HumanoidArm arm,boolean both){
        int sign=arm==HumanoidArm.RIGHT?1:-1;var hand=sign==1?m.rightArm:m.leftArm;
        hand.xRot=-(float)Math.PI/2+m.head.xRot;hand.yRot=m.head.yRot-.10F*sign;hand.zRot=0;
        if(both){var support=sign==1?m.leftArm:m.rightArm;support.xRot=-(float)Math.PI/2+.12F+m.head.xRot;support.yRot=m.head.yRot+.55F*sign;support.zRot=0;}
    }
    private GearArmPoses(){}
}
