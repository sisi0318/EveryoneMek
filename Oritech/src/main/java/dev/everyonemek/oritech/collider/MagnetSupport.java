package dev.everyonemek.oritech.collider;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModList;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;

/** The optional Things classes are resolved only after its loaded-mod check. */
public final class MagnetSupport {
    public record Settings(boolean enabled,float baseCost,float divisor,float efficiency,long capacity,long insert){
        public static final Settings NONE=new Settings(false,0,1,1,0,0);
        public Track.Magnet physics(){return new Track.Magnet(enabled,baseCost,divisor,efficiency,capacity);}
        public CompoundTag save(){var tag=new CompoundTag();tag.putBoolean("enabled",enabled);tag.putFloat("base",baseCost);tag.putFloat("divisor",divisor);tag.putFloat("efficiency",efficiency);tag.putLong("capacity",capacity);tag.putLong("insert",insert);return tag;}
        public static Settings load(CompoundTag tag){return tag.isEmpty()?NONE:new Settings(tag.getBoolean("enabled"),tag.getFloat("base"),tag.getFloat("divisor"),tag.getFloat("efficiency"),tag.getLong("capacity"),tag.getLong("insert"));}
    }
    public static boolean loaded(){return ModList.get().isLoaded("oritechthings");}
    public static boolean isField(ItemStack stack){return loaded()&&Bridge.isField(stack);}
    public static boolean isUpgrade(ItemStack stack){
        if(!(stack.getItem() instanceof BlockItem item))return false;
        return item.getBlock()==BlockContent.MACHINE_EFFICIENCY_ADDON||item.getBlock()==BlockContent.MACHINE_CAPACITOR_ADDON||loaded()&&Bridge.isUpgrade(item);
    }
    public static long energy(ItemStack stack){return isField(stack)?Math.max(0,stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(),0L)):0;}
    public static void energy(ItemStack stack,long amount){if(isField(stack))stack.set(EnergyApi.ITEM.getEnergyComponent(),Math.max(0,amount));}
    public static Settings settings(ItemStack field,List<ItemStack> addons){return isField(field)?Bridge.settings(addons):Settings.NONE;}
    public static List<Object> key(ItemStack field,List<ItemStack> addons){var key=new ArrayList<Object>();key.add(isField(field));
        if(loaded())key.add(Bridge.configKey());
        key.add(rearth.oritech.init.OritechConfig.additiveAddons.get());
        for(var stack:addons)if(isUpgrade(stack)&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof MachineAddonBlock block)key.add(block.getAddonSettings());else key.add(Boolean.FALSE);
        return key;
    }
    private static final class Bridge {
        static boolean isField(ItemStack stack){return stack.is(com.lumengrid.oritechthings.block.ModBlocks.ACCELERATOR_MAGNETIC_FIELD.get().asItem());}
        static boolean isUpgrade(BlockItem item){if(!(item.getBlock() instanceof com.lumengrid.oritechthings.block.custom.TierAddonBlock block))return false;
            var type=block.defaultBlockState().getValue(com.lumengrid.oritechthings.block.custom.TierAddonBlock.ADDON_TYPE);
            return type==com.lumengrid.oritechthings.util.Constants.AddonType.EFFICIENCY||type==com.lumengrid.oritechthings.util.Constants.AddonType.CAPACITOR;
        }
        static com.lumengrid.oritechthings.main.ConfigLoader.MagneticField config(){return com.lumengrid.oritechthings.main.ConfigLoader.getInstance().magneticFieldSettings;}
        static Object configKey(){return config();}
        static Settings settings(List<ItemStack> installed){
            // This unattached instance supplies the published native aggregation contract only.
            var nativeField=new com.lumengrid.oritechthings.entity.custom.AcceleratorMagneticFieldBlockEntity(BlockPos.ZERO,com.lumengrid.oritechthings.block.ModBlocks.ACCELERATOR_MAGNETIC_FIELD.get().defaultBlockState());
            var addons=new ArrayList<MachineAddonController.AddonBlock>();
            for(var stack:installed)if(isUpgradeStack(stack)&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof MachineAddonBlock block)
                addons.add(new MachineAddonController.AddonBlock(block,block.defaultBlockState(),BlockPos.ZERO,null));
            nativeField.gatherAddonStats(addons);var stats=nativeField.getBaseAddonData();var cfg=config();
            long capacity=add(nativeField.getDefaultCapacity(),stats.energyBonusCapacity()),insert=add(nativeField.getDefaultInsertRate(),stats.energyBonusTransfer());
            return new Settings(cfg.enabled(),cfg.baseCost(),cfg.speedCostDivisor(),Math.max(.1f,stats.efficiency()),Math.max(1,capacity),Math.max(0,insert));
        }
        private static boolean isUpgradeStack(ItemStack stack){return MagnetSupport.isUpgrade(stack);}
        private static long add(long a,long b){if(b>0&&a>Long.MAX_VALUE-b)return Long.MAX_VALUE;if(b<0&&a<Long.MIN_VALUE-b)return Long.MIN_VALUE;return a+b;}
    }
    private MagnetSupport(){}
}
