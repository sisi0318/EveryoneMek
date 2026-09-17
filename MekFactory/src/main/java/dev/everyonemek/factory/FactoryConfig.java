package dev.everyonemek.factory;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class FactoryConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue[] PARALLEL=new ModConfigSpec.IntValue[4], SIZE=new ModConfigSpec.IntValue[4];
    public static final ModConfigSpec.BooleanValue MACHINES_LIMIT_PARALLEL;
    static {var b=new ModConfigSpec.Builder();for(int i=0;i<4;i++) {
        PARALLEL[i]=b.defineInRange("parallelTier"+i,new int[]{8,32,128,512}[i],1,512);
        SIZE[i]=b.defineInRange("sizeTier"+i,5+2*i,4,11);
    }MACHINES_LIMIT_PARALLEL=b.comment("Processing-array mode: installed machine count additionally limits structural parallelism.").define("machinesLimitParallel",false);SPEC=b.build();}
    private FactoryConfig(){}
}
