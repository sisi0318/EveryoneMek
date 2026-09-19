package dev.everyonemek.factory;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class FactoryConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue[] PARALLEL=new ModConfigSpec.IntValue[4], SIZE=new ModConfigSpec.IntValue[4];
    static {var b=new ModConfigSpec.Builder();for(int i=0;i<4;i++) {
        PARALLEL[i]=b.defineInRange("parallelTier"+i,new int[]{8,32,128,512}[i],1,512);
        SIZE[i]=b.defineInRange("sizeTier"+i,5+2*i,4,11);
    }SPEC=b.build();}
    private FactoryConfig(){}
}
