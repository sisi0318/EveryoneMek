package dev.everyonemek.overloadcore.training;

import java.util.UUID;
import dev.everyonemek.overloadcore.CoreConfig;
import dev.everyonemek.overloadcore.gear.GearCombat;
import mekanism.api.*;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class TrainingProjector extends TileEntityMekanism {
    public BasicEnergyContainer energy;
    public boolean enabled;
    public int hits,status;
    public double lastDamage,totalDamage,dps;
    public long spent;
    private UUID target;
    private final double[] window=new double[100];
    private final long[] windowTicks=new long[100];
    public TrainingProjector(BlockPos pos,BlockState state){super(TrainingContent.BLOCK,pos,state);}
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){
        energy=BasicEnergyContainer.input(TrainingContent.capacity(),listener);var holder=EnergyContainerHelper.forSide(facingSupplier);holder.addContainer(energy,RelativeSide.values());return holder.build();
    }
    public boolean permitted(Player p){return level!=null&&p.level()==level&&IBlockSecurityUtils.INSTANCE.canAccess(p,level,worldPosition,this);}
    public boolean owns(TrainingTarget entity){return level!=null&&getActive()&&target!=null&&target.equals(entity.getUUID())&&entity.distanceToSqr(Vec3.atCenterOf(worldPosition.above()))<9;}
    public boolean accepts(TrainingTarget entity,Player p){return owns(entity)&&permitted(p);}
    @Override protected boolean onUpdateServer(){boolean changed=super.onUpdateServer();
        updateDps();
        if(!enabled||!canFunction()){status=enabled?4:0;stopTarget();setActive(false);return changed;}
        long cost=GearCombat.joules(CoreConfig.TRAINING_FE.get());
        if(energy.extract(cost,Action.SIMULATE,AutomationType.INTERNAL)<cost){status=1;stopTarget();setActive(false);return changed;}
        if(!level.hasChunkAt(worldPosition.above(2))||!level.getBlockState(worldPosition.above()).isAir()||!level.getBlockState(worldPosition.above(2)).isAir()){
            status=2;stopTarget();setActive(false);return changed;
        }
        var server=(ServerLevel)level;var entity=target==null?null:server.getEntity(target);
        if(!(entity instanceof TrainingTarget)){
            var fresh=TrainingContent.TARGET.get().create(server);if(fresh==null){status=5;setActive(false);return changed;}
            fresh.anchor=worldPosition.immutable();fresh.setPos(worldPosition.getX()+.5,worldPosition.getY()+1.05,worldPosition.getZ()+.5);
            fresh.setYRot(getDirection().toYRot());fresh.syncReadings(this,true);
            if(!server.addFreshEntity(fresh)){status=5;setActive(false);return changed;}target=fresh.getUUID();
        }
        energy.extract(cost,Action.EXECUTE,AutomationType.INTERNAL);status=3;setActive(true);return changed;
    }
    private void stopTarget(){if(level instanceof ServerLevel server&&target!=null){var entity=server.getEntity(target);target=null;if(entity instanceof TrainingTarget)entity.discard();}}
    @Override public void setRemoved(){stopTarget();super.setRemoved();}
    public void recordHit(double amount){if(!Double.isFinite(amount)||amount<=0)return;lastDamage=Math.min(1_000_000_000,amount);totalDamage=Math.min(1e15,totalDamage+lastDamage);hits=(int)Math.min(Integer.MAX_VALUE,(long)hits+1);
        long now=level.getGameTime();int bucket=(int)(now%100);if(windowTicks[bucket]!=now){window[bucket]=0;windowTicks[bucket]=now;}window[bucket]+=lastDamage;updateDps();markForSave();}
    private void updateDps(){long now=level.getGameTime();double sum=0;for(int i=0;i<100;i++)if(now-windowTicks[i]<100)sum+=window[i];dps=sum/5;}
    public void recordPower(long joules){if(joules>0){spent=spent>Long.MAX_VALUE-joules?Long.MAX_VALUE:spent+joules;markForSave();}}
    public boolean command(Player p,int action){if(!permitted(p)||p.distanceToSqr(Vec3.atCenterOf(worldPosition))>64)return false;
        if(action==0){enabled=!enabled;if(!enabled){status=0;stopTarget();setActive(false);}}
        else if(action==1){hits=0;lastDamage=totalDamage=dps=0;spent=0;java.util.Arrays.fill(window,0);
            if(level instanceof ServerLevel server&&target!=null&&server.getEntity(target) instanceof TrainingTarget projection)projection.syncReadings(this,true);
        }else return false;
        markForSave();return true;
    }
    private CompoundTag data(){var t=new CompoundTag();t.putBoolean("enabled",enabled);t.putInt("hits",hits);t.putDouble("last",lastDamage);t.putDouble("damage",totalDamage);t.putLong("spent",spent);return t;}
    private void read(CompoundTag t){enabled=t.getBoolean("enabled");hits=Math.max(0,t.getInt("hits"));lastDamage=finite(t.getDouble("last"));totalDamage=finite(t.getDouble("damage"));spent=Math.max(0,t.getLong("spent"));}
    private static double finite(double value){return Double.isFinite(value)?Math.clamp(value,0,1e15):0;}
    @Override public void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("training",data());}
    @Override public void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);read(t.getCompound("training"));}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);b.set(TrainingContent.DATA.get(),data());}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input){super.applyImplicitComponents(input);var t=input.get(TrainingContent.DATA.get());if(t!=null)read(t);}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);menu.track(SyncableBoolean.create(()->enabled,v->enabled=v));menu.track(SyncableInt.create(()->status,v->status=v));menu.track(SyncableInt.create(()->hits,v->hits=v));
        menu.track(SyncableDouble.create(()->lastDamage,v->lastDamage=v));menu.track(SyncableDouble.create(()->totalDamage,v->totalDamage=v));menu.track(SyncableDouble.create(()->dps,v->dps=v));menu.track(SyncableLong.create(()->spent,v->spent=v));}
}
