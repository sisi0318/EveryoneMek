package dev.everyonemek.gravity.expansion;
import net.minecraft.nbt.CompoundTag;
/** A single paid core state shared by every bound console; no offline ticking. */
public final class CoreTuning {
    public int profile,burst,cooldown;private long lastTick=Long.MIN_VALUE;
    public int surcharge(){return burst>0?30:profile==2?15:0;}
    public long power(long base){int percent=burst>0?150:profile==1?80:profile==2?125:100;return base/100*percent+base%100*percent/100;}
    public long cost(long gross){int extra=surcharge();return gross+gross/100*extra+(gross%100*extra+99)/100;}
    public long affordable(long fuel){int percent=100+surcharge();return fuel/percent*100+fuel%percent*100/percent;}
    public int processing(){return profile==1?2:1;}
    public void track(mekanism.common.inventory.container.MekanismContainer menu){
        menu.track(mekanism.common.inventory.container.sync.SyncableInt.create(()->profile,v->profile=v));
        menu.track(mekanism.common.inventory.container.sync.SyncableInt.create(()->burst,v->burst=v));
        menu.track(mekanism.common.inventory.container.sync.SyncableInt.create(()->cooldown,v->cooldown=v));
    }
    public boolean trigger(){if(cooldown>0||burst>0)return false;burst=400;cooldown=1600;return true;}
    public boolean tick(long now,boolean online){if(now==lastTick)return false;lastTick=now;boolean changed=burst>0||cooldown>0;if(cooldown>0)cooldown--;if(burst>0)burst=online?burst-1:0;return changed;}
    public CompoundTag save(){var tag=new CompoundTag();tag.putInt("profile",profile);tag.putInt("burst",burst);tag.putInt("cooldown",cooldown);return tag;}
    public void load(CompoundTag tag){profile=Math.clamp(tag.getInt("profile"),0,2);burst=Math.clamp(tag.getInt("burst"),0,400);cooldown=Math.max(burst,Math.clamp(tag.getInt("cooldown"),0,1600));}
}
