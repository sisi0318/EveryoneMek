package dev.everyonemek.gravity.expansion;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ModuleConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RANGE,BATCH,TRANSFER;
    public static final ModConfigSpec.LongValue ITEM_COST;
    static{var b=new ModConfigSpec.Builder();RANGE=b.defineInRange("linkRange",128,16,512);BATCH=b.defineInRange("processingBatch",64,1,512);TRANSFER=b.defineInRange("itemsPerNodePerTick",4096,1,32768);ITEM_COST=b.defineInRange("joulesPerTransferredItem",1_000_000L,1,1_000_000_000L);SPEC=b.build();}
    private ModuleConfig(){}
}
