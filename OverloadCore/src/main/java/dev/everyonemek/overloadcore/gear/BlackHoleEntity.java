package dev.everyonemek.overloadcore.gear;

import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.training.TrainingTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A finite, unsaved projectile/field. Never changes blocks, item entities or chunk loading. */
public final class BlackHoleEntity extends Projectile {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,OverloadCore.ID);
    public static final java.util.function.Supplier<EntityType<BlackHoleEntity>> TYPE=ENTITIES.register("black_hole",()->EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new,MobCategory.MISC)
        .sized(.35F,.35F).fireImmune().noSave().clientTrackingRange(8).updateInterval(1).build("overloadcore:black_hole"));
    private static final EntityDataAccessor<Long> OPENED=SynchedEntityData.defineId(BlackHoleEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> EXPIRES=SynchedEntityData.defineId(BlackHoleEntity.class,EntityDataSerializers.LONG);
    private static final ResourceKey<net.minecraft.world.damagesource.DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"gravity"));
    private final java.util.Set<java.util.UUID> meteredTargets=new java.util.HashSet<>();
    private long paidEnergy;
    private double travelled;
    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type,Level level){super(type,level);setNoGravity(true);}
    public static void register(IEventBus bus){ENTITIES.register(bus);}
    public void setPaidEnergy(long energy){paidEnergy=Math.max(0,energy);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){data.define(OPENED,-1L);data.define(EXPIRES,-1L);}
    public boolean isOpen(){return entityData.get(OPENED)>=0;}
    public float visualRadius(float partial){
        if(!isOpen())return .18F;
        float age=(float)(level().getGameTime()-entityData.get(OPENED))+partial;
        float left=(float)(entityData.get(EXPIRES)-level().getGameTime())-partial;
        return 1.15F*Math.clamp(Math.min((age+1)/8F,left/12F),.02F,1F);
    }
    @Override public AABB getBoundingBoxForCulling(){return getBoundingBox().inflate(3.6);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<128*128;}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){if(!isOpen())setPos(position().add(getDeltaMovement()));return;}
        if(!(getOwner() instanceof ServerPlayer owner)||!owner.isAlive()||owner.isSpectator()||owner.level()!=level()||tickCount>250){discard();return;}
        if(isOpen()){
            if(level().getGameTime()>=entityData.get(EXPIRES)){discard();return;}
            if((level().getGameTime()-entityData.get(OPENED))%2==0)attract(owner);
            return;
        }
        var delta=getDeltaMovement();
        // Check every section of travel before collision queries: a projectile cannot load the next chunk.
        if(!Double.isFinite(delta.lengthSqr())||delta.lengthSqr()>4||!loadedSegment(position(),position().add(delta))){discard();return;}
        var hit=ProjectileUtil.getHitResultOnMoveVector(this,e->e instanceof LivingEntity living&&eligible(owner,living));
        if(hit.getType()!=HitResult.Type.MISS){
            if(net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this,hit)){discard();return;}
            // Stay on the visible side of a wall, so the center can see nearby targets on that side.
            setPos(hit.getLocation().subtract(delta.normalize().scale(.15)));open();
        }else{
            setPos(position().add(delta));travelled+=delta.length();if(travelled>=48)open();
        }
    }
    private boolean eligible(ServerPlayer owner,LivingEntity target){return GearCombat.target(owner,target)&&!(target instanceof Player p&&p.getAbilities().instabuild);}
    private boolean loadedSegment(Vec3 start,Vec3 end){
        int steps=Math.max(1,(int)Math.ceil(start.distanceTo(end)*4));
        for(int i=0;i<=steps;i++)if(!level().hasChunkAt(BlockPos.containing(start.lerp(end,i/(double)steps))))return false;
        return true;
    }
    private void open(){
        setDeltaMovement(Vec3.ZERO);hasImpulse=true;entityData.set(OPENED,level().getGameTime());entityData.set(EXPIRES,level().getGameTime()+CoreConfig.BLACK_HOLE_LIFETIME.get());
        level().playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,.75F,.55F);
    }
    private void attract(ServerPlayer owner){
        double radius=CoreConfig.BLACK_HOLE_RADIUS.get();
        var targets=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius),e->eligible(owner,e)&&e.getBoundingBox().getCenter().distanceToSqr(position())<=radius*radius)
            .stream().sorted(java.util.Comparator.comparingDouble(e->e.distanceToSqr(this))).limit(32).toList();
        var source=damageSources().source(DAMAGE,this,owner);
        boolean pulse=(level().getGameTime()-entityData.get(OPENED))%10==0;
        for(var target:targets){
            var point=target.getBoundingBox().getCenter();
            if(!loadedSegment(position(),point)||level().clip(new ClipContext(position(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS||target.isInvulnerableTo(source))continue;
            if(!(target instanceof TrainingTarget)){
                var toward=position().subtract(point);double distance=toward.length();
                if(distance>.25){var velocity=target.getDeltaMovement().scale(.72).add(toward.normalize().scale(.08+.16*(1-distance/radius)));
                    if(velocity.lengthSqr()>.35*.35)velocity=velocity.normalize().scale(.35);
                    target.setDeltaMovement(velocity);target.hurtMarked=true;
                }
            }
            if(pulse){
                if(target instanceof TrainingTarget&&meteredTargets.size()<32&&meteredTargets.add(target.getUUID()))TrainingTarget.power(target,owner,paidEnergy);
                target.hurt(source,CoreConfig.BLACK_HOLE_DAMAGE.get().floatValue());
            }
        }
    }
}
