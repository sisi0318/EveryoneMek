package dev.everyonemek.gravity.expansion;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ModuleConfig {
    public static final int MIN_RANGE=16,MAX_RANGE=512;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RANGE,BATCH,TRANSFER,NODE_REVISION;
    public static final ModConfigSpec.LongValue ITEM_COST,ENERGY_PACKET_COST,FLUID_COST,CHEMICAL_COST;
    static{var b=new ModConfigSpec.Builder();RANGE=b.defineInRange("linkRange",128,MIN_RANGE,MAX_RANGE);BATCH=b.defineInRange("processingBatch",64,1,512);TRANSFER=b.defineInRange("itemsPerNodePerTick",36864,1,36864);ITEM_COST=b.defineInRange("joulesPerTransferredItem",1_000_000L,1,1_000_000_000L);
        ENERGY_PACKET_COST=b.defineInRange("joulesPerEnergyTransfer",1_000_000L,1,1_000_000_000L);FLUID_COST=b.defineInRange("joulesPerFluidMillibucket",1000L,1,1_000_000_000L);CHEMICAL_COST=b.defineInRange("joulesPerChemicalUnit",1000L,1,1_000_000_000L);
        NODE_REVISION=b.defineInRange("nodeTransferRevision",0,0,1);SPEC=b.build();}
    public static void loaded(net.neoforged.fml.event.config.ModConfigEvent.Loading event){if(event.getConfig().getSpec()!=SPEC||NODE_REVISION.get()!=0)return;if(TRANSFER.get()==4096)TRANSFER.set(36864);NODE_REVISION.set(1);event.getConfig().getLoadedConfig().save();}
    private ModuleConfig(){}
}
