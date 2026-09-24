package dev.everyonemek.gravity.solar;

import java.util.*;
import dev.everyonemek.gravity.MekGravity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** Server-side stellar heat. Exposure timers are transient and never store entity references. */
public final class SolarHeat {
    public static final double WARNING_RADIUS=4,HEAT_RADIUS=3,CONTACT_RADIUS=1.25,CORE_RADIUS=.45;
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"stellar_heat"));
    private final SolarController owner;
    private final Map<UUID,Exposure> exposures=new HashMap<>();
    private static final class Exposure {int zone;long warned,hit=Long.MIN_VALUE,seen;}
    public SolarHeat(SolarController owner){this.owner=owner;}
    private static double distance(Vec3 center,AABB body){
        double x=center.x-Math.clamp(center.x,body.minX,body.maxX),y=center.y-Math.clamp(center.y,body.minY,body.maxY),z=center.z-Math.clamp(center.z,body.minZ,body.maxZ);
        return Math.sqrt(x*x+y*y+z*z);
    }
    public void tick(){var level=owner.getLevel();
        if(level==null||level.isClientSide||!SolarConfig.HEAT_ENABLED.get()||!owner.isCoreHot()){exposures.clear();return;}
        long now=level.getGameTime();if((now+owner.getBlockPos().asLong())%5!=0)return;
        var seed=owner.structure.seed;if(seed==null||seed.isRemoved()||!level.hasChunkAt(seed.getBlockPos())||level.getBlockEntity(seed.getBlockPos())!=seed){exposures.clear();return;}
        var center=seed.getBlockPos().getCenter();var source=new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE),center);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(WARNING_RADIUS),e->e.isAlive()&&!e.isSpectator())){
            double distance=distance(center,entity.getBoundingBox());if(distance>WARNING_RADIUS)continue;
            int zone=distance<=CORE_RADIUS?5:distance<=CONTACT_RADIUS?4:distance<=2?3:distance<=HEAT_RADIUS?2:1;
            var exposure=exposures.computeIfAbsent(entity.getUUID(),k->new Exposure());exposure.seen=now;
            if(entity instanceof ServerPlayer player&&(zone>exposure.zone||now-exposure.warned>=40)){
                player.displayClientMessage(SolarContent.text(zone==5?"heat_core":zone==4?"heat_contact":zone>=2?"heat_burning":"heat_warning").copy().withStyle(zone>=2?ChatFormatting.RED:ChatFormatting.GOLD),true);
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(),SoundSource.BLOCKS,.4F,zone>=4?1.7F:zone>=2?1.25F:.85F);exposure.warned=now;
            }
            exposure.zone=zone;
            if(zone<2||entity instanceof ServerPlayer player&&player.getAbilities().invulnerable||entity.isInvulnerableTo(source))continue;
            int interval=zone>=4?5:zone==3?10:20;
            if(exposure.hit!=Long.MIN_VALUE&&now-exposure.hit<interval)continue;exposure.hit=now;
            float damage=(float)((zone==5?SolarConfig.CORE_CONTACT_DAMAGE.get():zone==4?8:zone==3?4:2)*SolarConfig.HEAT_DAMAGE.get());
            if(damage>0&&entity.hurt(source,damage))entity.igniteForSeconds(zone>=4?6:zone==3?4:2);
        }
        exposures.values().removeIf(e->e.seen!=now);
    }
}
