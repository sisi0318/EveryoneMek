package dev.everyonemek.gravity;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Client preferences only; never influence heat, stored energy or server processing. */
public final class VisualConfig {
    public enum Effects{FULL,REDUCED,OFF}
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Effects> WORLD_EFFECTS;
    public static final ModConfigSpec.BooleanValue ANIMATE_ITEMS,SHADERS;
    public static final ModConfigSpec.IntValue EFFECT_DISTANCE;
    static{var b=new ModConfigSpec.Builder();
        WORLD_EFFECTS=b.translation("mekgravity.config.world_effects").comment("FULL: complete field; REDUCED: fewer arcs; OFF: static cores without field effects.").defineEnum("worldEffects",Effects.FULL);
        ANIMATE_ITEMS=b.translation("mekgravity.config.item_animation").define("animateItems",true);
        SHADERS=b.translation("mekgravity.config.shaders").comment("Disable to use baked fallback materials for shader-pack compatibility.").define("shaderMaterials",true);
        EFFECT_DISTANCE=b.translation("mekgravity.config.effect_distance").defineInRange("effectDistance",48,8,128);SPEC=b.build();}
    public static boolean effects(){return WORLD_EFFECTS.get()!=Effects.OFF;}
    public static boolean reduced(){return WORLD_EFFECTS.get()!=Effects.FULL;}
    public static double phase(double phase){return effects()?phase:0;}
    public static double meshDistance(double distance){return WORLD_EFFECTS.get()==Effects.FULL?distance:Math.max(distance,WORLD_EFFECTS.get()==Effects.OFF?40:16);}
    private VisualConfig(){}
}
