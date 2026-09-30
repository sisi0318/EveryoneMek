package dev.everyonemek.overloadcore.training;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Non-persistent, unarmored projection. Its controller owns all statistics and permission checks. */
public final class TrainingTarget extends LivingEntity {
    public BlockPos anchor=BlockPos.ZERO;
    public TrainingTarget(EntityType<? extends TrainingTarget> type,Level level){super(type,level);setNoGravity(true);setHealth(getMaxHealth());}
    public TrainingProjector controller(){return level().hasChunkAt(anchor)&&level().getBlockEntity(anchor) instanceof TrainingProjector tile?tile:null;}
    public boolean canTrain(Player p){var tile=controller();return tile!=null&&tile.accepts(this,p);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide||!(source.getEntity() instanceof Player p)||!canTrain(p)||!Float.isFinite(amount)||amount<=0)return false;
        boolean hit=super.hurt(source,amount);setHealth(getMaxHealth());invulnerableTime=0;setDeltaMovement(0,0,0);return hit;
    }
    public void recordHit(DamageSource source,float amount){if(source.getEntity() instanceof Player p&&canTrain(p))controller().recordHit(amount);}
    public static void power(LivingEntity target,Player p,long joules){if(target instanceof TrainingTarget dummy&&dummy.canTrain(p))dummy.controller().recordPower(joules);}
    @Override public void tick(){super.tick();if(!level().isClientSide){var tile=controller();if(tile==null||!tile.owns(this))discard();else setPos(anchor.getX()+.5,anchor.getY()+1.05,anchor.getZ()+.5);}}
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
