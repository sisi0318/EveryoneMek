package dev.everyonemek.overloadcore.gear;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class GearVisualConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHADERS, ANIMATE, BEAMS;
    public static final ModConfigSpec.IntValue DISTANCE;
    static {var b=new ModConfigSpec.Builder();SHADERS=b.define("weaponShaders",true);ANIMATE=b.define("weaponAnimation",true);BEAMS=b.define("combatBeams",true);DISTANCE=b.defineInRange("effectDistance",96,16,256);SPEC=b.build();}
    private GearVisualConfig(){}
}
