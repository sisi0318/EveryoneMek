package dev.everyonemek.gravity.expansion;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Only registry identities are projected, never a second inventory or private item NBT. */
public final class NodeSnapshot {
    private final ResourceLocation[] ids=new ResourceLocation[4];private final long[] seen=new long[4];private int mask;
    public void record(long tick,int type,Object stack){if(type<0||type>3)return;ResourceLocation id=switch(type){case 0->BuiltInRegistries.ITEM.getKey(((ItemStack)stack).getItem());case 2->BuiltInRegistries.FLUID.getKey(((FluidStack)stack).getFluid());case 3->((ChemicalStack)stack).getTypeRegistryName();default->ResourceLocation.fromNamespaceAndPath("mekanism","energy_tablet");};ids[type]=id;seen[type]=tick;mask|=1<<type;}
    public void write(CompoundTag tag,long tick){int live=0;for(int i=0;i<4;i++)if((mask&(1<<i))!=0&&tick-seen[i]<=40)live|=1<<i;if(live==0){tag.putInt("snapshot_type",-1);return;}int select=(int)((tick/20)%Integer.bitCount(live));for(int i=0;i<4;i++)if((live&(1<<i))!=0&&select--==0){tag.putInt("snapshot_type",i);tag.putString("snapshot_id",ids[i].toString());break;}}
}
