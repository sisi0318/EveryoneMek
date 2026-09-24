package dev.everyonemek.gravity.solar;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Solar settings are separate from the existing seven-block gravity reactor. All energy values are J. */
public final class SolarConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue[] POWER=new ModConfigSpec.LongValue[4];
    public static final ModConfigSpec.LongValue CAPACITY,STARTUP,RESERVE,PORT_RATE;
    static{var b=new ModConfigSpec.Builder();
        for(int i=0;i<4;i++)POWER[i]=b.defineInRange("powerTier"+i,640_000_000_000L<<i,100,100_000_000_000_000L);
        CAPACITY=b.comment("Solar energy values use Mekanism joules.").defineInRange("energyCapacity",2_560_000_000_000_000L,1,100_000_000_000_000_000L);
        STARTUP=b.defineInRange("startupEnergy",160_000_000_000_000L,0,100_000_000_000_000_000L);
        RESERVE=b.defineInRange("protectedReserve",5_120_000_000_000L,0,100_000_000_000_000_000L);
        PORT_RATE=b.defineInRange("energyPerPortPerTick",2_560_000_000_000L,1,100_000_000_000_000L);SPEC=b.build();}
    private SolarConfig(){}
}
