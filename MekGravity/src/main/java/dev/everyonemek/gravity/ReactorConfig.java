package dev.everyonemek.gravity;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ReactorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue[] POWER=new ModConfigSpec.LongValue[4];
    public static final ModConfigSpec.LongValue CAPACITY,STARTUP,RESERVE,PORT_RATE,COOLANT_PER_GJ,TANK_CAPACITY;
    static {
        var b=new ModConfigSpec.Builder();
        for(int i=0;i<4;i++)POWER[i]=b.defineInRange("powerTier"+i,1_250_000_000L<<i,1,10_000_000_000_000L);
        CAPACITY=b.comment("All energy settings use Mekanism joules.").defineInRange("energyCapacity",2_500_000_000_000L,1,100_000_000_000_000L);
        STARTUP=b.defineInRange("startupEnergy",160_000_000_000L,0,100_000_000_000_000L);
        RESERVE=b.defineInRange("protectedReserve",5_000_000_000L,0,100_000_000_000_000L);
        PORT_RATE=b.defineInRange("energyPerPortPerTick",2_500_000_000L,1,10_000_000_000_000L);
        COOLANT_PER_GJ=b.comment("Sodium units heated per billion joules of gross generation. Heat enthalpy is also debited from fuel.").defineInRange("coolantPerGigaJoule",2_000L,1,1_000_000L);
        TANK_CAPACITY=b.defineInRange("chemicalTankCapacity",16_000_000L,1,1_000_000_000L);
        SPEC=b.build();
    }
    private ReactorConfig(){}
}
