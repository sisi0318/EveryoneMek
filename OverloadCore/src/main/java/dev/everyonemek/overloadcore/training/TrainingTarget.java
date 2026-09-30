package dev.everyonemek.overloadcore.training;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Non-persistent, unarmored projection. Its controller owns all statistics and permission checks. */
public final class TrainingTarget extends LivingEntity {
    private static final EntityDataAccessor<Float> LAST_DAMAGE=SynchedEntityData.defineId(TrainingTarget.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DPS=SynchedEntityData.defineId(TrainingTarget.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HITS=SynchedEntityData.defineId(TrainingTarget.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> HIT_TIME=SynchedEntityData.defineId(TrainingTarget.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> HIT_DAMAGE=SynchedEntityData.defineId(TrainingTarget.class,EntityDataSerializers.FLOAT);
    public BlockPos anchor=BlockPos.ZERO;
    private long lastSoundTime=-100;
    public TrainingTarget(EntityType<? extends TrainingTarget> type,Level level){super(type,level);setNoGravity(true);setHealth(getMaxHealth());}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(LAST_DAMAGE,0F);data.define(DPS,0F);data.define(HITS,0);data.define(HIT_TIME,-1L);data.define(HIT_DAMAGE,0F);}
    public float lastDamage(){return entityData.get(LAST_DAMAGE);}
    public float dps(){return entityData.get(DPS);}
    public int hits(){return entityData.get(HITS);}
    public float feedbackDamage(){return entityData.get(HIT_DAMAGE);}
    public float hitAge(float partial){long stamp=entityData.get(HIT_TIME);return stamp<0?1000:Math.max(0,level().getGameTime()-stamp+partial);}
    public void syncReadings(TrainingProjector tile,boolean reset){
        entityData.set(LAST_DAMAGE,(float)tile.lastDamage);entityData.set(DPS,(float)tile.dps);entityData.set(HITS,tile.hits);
        if(reset){entityData.set(HIT_TIME,-1L);entityData.set(HIT_DAMAGE,0F);}
    }
    public TrainingProjector controller(){return level().hasChunkAt(anchor)&&level().getBlockEntity(anchor) instanceof TrainingProjector tile?tile:null;}
    public boolean canTrain(Player p){var tile=controller();return tile!=null&&tile.accepts(this,p);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide||!(source.getEntity() instanceof Player p)||!canTrain(p)||!Float.isFinite(amount)||amount<=0)return false;
        boolean hit=super.hurt(source,amount);setHealth(getMaxHealth());invulnerableTime=0;setDeltaMovement(0,0,0);return hit;
    }
    public void recordHit(DamageSource source,float amount){if(level().isClientSide||!Float.isFinite(amount)||amount<=0)return;
        if(source.getEntity() instanceof Player p&&canTrain(p)){
            var tile=controller();tile.recordHit(amount);syncReadings(tile,false);
            long now=level().getGameTime();float sum=entityData.get(HIT_TIME)==now?feedbackDamage():0;
            entityData.set(HIT_DAMAGE,Math.min(1_000_000_000F,sum+amount));entityData.set(HIT_TIME,now);
        }
    }
    public static void power(LivingEntity target,Player p,long joules){if(target instanceof TrainingTarget dummy&&dummy.canTrain(p))dummy.controller().recordPower(joules);}
    @Override public void tick(){super.tick();if(!level().isClientSide){var tile=controller();if(tile==null||!tile.owns(this))discard();else{
        setPos(anchor.getX()+.5,anchor.getY()+1.05,anchor.getZ()+.5);setYRot(tile.getDirection().toYRot());if(tickCount%5==0)syncReadings(tile,false);
    }}else{
        long stamp=entityData.get(HIT_TIME);
        if(stamp>=0&&stamp-lastSoundTime>=3&&hitAge(0)<5){lastSoundTime=stamp;
            level().playLocalSound(getX(),getY()+1,getZ(),net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(),net.minecraft.sounds.SoundSource.BLOCKS,.22F,1.6F,false);
        }
    }}
    @Override public void die(DamageSource source){setHealth(getMaxHealth());}
    @Override public void kill(){discard();}
    @Override public boolean isPushable(){return false;}
    @Override public void knockback(double strength,double x,double z){}
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect){return false;}
    @Override public Iterable<ItemStack> getArmorSlots(){return java.util.List.of();}
    @Override public ItemStack getItemBySlot(EquipmentSlot slot){return ItemStack.EMPTY;}
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack){}
    @Override public HumanoidArm getMainArm(){return HumanoidArm.RIGHT;}
}
