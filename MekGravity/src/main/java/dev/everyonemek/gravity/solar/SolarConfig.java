package dev.everyonemek.gravity.solar;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;
/** Solar settings are separate from the existing seven-block gravity reactor. All energy values are J. */
public final class SolarConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue[] POWER=new ModConfigSpec.LongValue[4];
    public static final ModConfigSpec.LongValue CAPACITY,STARTUP,RESERVE,PORT_RATE;
    public static final ModConfigSpec.BooleanValue HEAT_ENABLED;
    public static final ModConfigSpec.DoubleValue HEAT_DAMAGE,CORE_CONTACT_DAMAGE;
    public static final ModConfigSpec.IntValue CORONAL_BATCH,CORONAL_TICKS,CORONAL_ACCELERATION,CORONAL_REVISION;
    public static final ModConfigSpec.LongValue CORONAL_ENERGY;
    static{var b=new ModConfigSpec.Builder();
        for(int i=0;i<4;i++)POWER[i]=b.defineInRange("powerTier"+i,640_000_000_000L<<i,100,100_000_000_000_000L);
        CAPACITY=b.comment("Solar energy values use Mekanism joules.").defineInRange("energyCapacity",2_560_000_000_000_000L,1,100_000_000_000_000_000L);
        STARTUP=b.defineInRange("startupEnergy",160_000_000_000_000L,0,100_000_000_000_000_000L);
        RESERVE=b.defineInRange("protectedReserve",5_120_000_000_000L,0,100_000_000_000_000_000L);
        PORT_RATE=b.defineInRange("energyPerPortPerTick",2_560_000_000_000L,1,100_000_000_000_000L);
        HEAT_ENABLED=b.comment("Hot stellar seeds warn nearby players and burn living entities inside the reaction chamber.").define("stellarHeatEnabled",true);
        HEAT_DAMAGE=b.comment("Stellar heat damage multiplier; zero keeps warnings but disables burning.").defineInRange("stellarHeatDamageMultiplier",1D,0D,100D);
        CORE_CONTACT_DAMAGE=b.comment("Base damage inside the innermost 0.45-block core; multiplied by stellarHeatDamageMultiplier.").defineInRange("stellarCoreContactDamage",1_000_000D,0D,1_000_000_000D);
        CORONAL_BATCH=b.defineInRange("coronalBatchSize",512,1,512);
        CORONAL_TICKS=b.defineInRange("coronalSmeltingTicks",40,1,72000);
        CORONAL_ACCELERATION=b.comment("Additional speed multiplier for both vanilla cooking and coronal datapack recipes.").defineInRange("coronalAcceleration",5,1,100);
        CORONAL_ENERGY=b.comment("Joules per vanilla smelting/blasting item; a batch pays once before processing.").defineInRange("coronalSmeltingEnergy",1_000_000_000L,1,1_000_000_000_000L);
        CORONAL_REVISION=b.comment("One-time migration of alpha.20 coronal defaults; custom values are retained.").defineInRange("coronalPerformanceRevision",0,0,1);
        SPEC=b.build();}
    public static int upgradedBatch(int revision,int value){return revision==0&&value==32?512:value;}
    public static long upgradedEnergy(int revision,long value){return revision==0&&value==5_000_000_000L?1_000_000_000L:value;}
    public static void loaded(ModConfigEvent.Loading event){if(event.getConfig().getSpec()!=SPEC||CORONAL_REVISION.get()>=1)return;
        CORONAL_BATCH.set(upgradedBatch(0,CORONAL_BATCH.get()));CORONAL_ENERGY.set(upgradedEnergy(0,CORONAL_ENERGY.get()));CORONAL_REVISION.set(1);event.getConfig().getLoadedConfig().save();}
    private SolarConfig(){}
}
