package dev.everyonemek.gravity;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;
public final class ReactorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue[] POWER=new ModConfigSpec.LongValue[4];
    public static final ModConfigSpec.LongValue CAPACITY,STARTUP,RESERVE,PORT_RATE,COOLANT_PER_GJ,TANK_CAPACITY;
    public static final ModConfigSpec.IntValue PERFORMANCE_REVISION;
    private static final long OLD_PORT_RATE=2_500_000_000L,DEFAULT_PORT_RATE=40_000_000_000L;
    static {
        var b=new ModConfigSpec.Builder();
        for(int i=0;i<4;i++)POWER[i]=b.defineInRange("powerTier"+i,5_000_000_000L<<i,1,10_000_000_000_000L);
        CAPACITY=b.comment("All energy settings use Mekanism joules.").defineInRange("energyCapacity",2_500_000_000_000L,1,100_000_000_000_000L);
        STARTUP=b.defineInRange("startupEnergy",160_000_000_000L,0,100_000_000_000_000L);
        RESERVE=b.defineInRange("protectedReserve",5_000_000_000L,0,100_000_000_000_000L);
        PORT_RATE=b.defineInRange("energyPerPortPerTick",DEFAULT_PORT_RATE,1,10_000_000_000_000L);
        COOLANT_PER_GJ=b.comment("Legacy setting retained for old configurations; the reactor no longer consumes coolant.").defineInRange("coolantPerGigaJoule",2_000L,1,1_000_000L);
        TANK_CAPACITY=b.defineInRange("chemicalTankCapacity",16_000_000L,1,1_000_000_000L);
        PERFORMANCE_REVISION=b.comment("One-time migration of original power and transfer defaults.").defineInRange("performanceRevision",0,0,1);
        SPEC=b.build();
    }
    static long upgradedPower(int revision,int tier,long current){return revision==0&&current==(1_250_000_000L<<tier)?5_000_000_000L<<tier:current;}
    static long upgradedPortRate(int revision,long current){return revision==0&&current==OLD_PORT_RATE?DEFAULT_PORT_RATE:current;}
    public static void loaded(ModConfigEvent.Loading e){
        if(e.getConfig().getSpec()!=SPEC||PERFORMANCE_REVISION.get()>=1)return;
        for(int i=0;i<4;i++)POWER[i].set(upgradedPower(0,i,POWER[i].get()));
        PORT_RATE.set(upgradedPortRate(0,PORT_RATE.get()));PERFORMANCE_REVISION.set(1);e.getConfig().getLoadedConfig().save();
    }
    private ReactorConfig(){}
}
