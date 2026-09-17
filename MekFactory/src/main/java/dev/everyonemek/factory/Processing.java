package dev.everyonemek.factory;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.*;
/** Paid material reservations persist independently of structure availability; energy advances actual work only. */
public final class Processing {
    public final List<Job> jobs=new ArrayList<>();private int cursor;private long failedRevision=-1;private String failedTemplate="";
    public static final class Job {
        public String recipe;public int units,ticks,progress,operations=1,baseTicks;public long energy,baseEnergy;public boolean fixedEnergy,exponential;
        public List<ItemStack> items;public List<FluidStack> fluids;public List<ChemicalStack> chemicals;
        Job(RecipePlan p,int n){recipe=p.id;units=n;ticks=p.ticks;energy=p.energy;baseTicks=p.baseTicks;baseEnergy=p.baseEnergy;fixedEnergy=p.fixedEnergy;exponential=p.exponential;operations=p.operations;items=List.copyOf(p.outItems);fluids=List.copyOf(p.outFluids);chemicals=List.copyOf(p.outChemicals);}
        Job copy(int n){var p=new RecipePlan(recipe);p.ticks=ticks;p.energy=energy;p.baseTicks=baseTicks;p.baseEnergy=baseEnergy;p.fixedEnergy=fixedEnergy;p.exponential=exponential;p.operations=operations;p.outItems.addAll(items);p.outFluids.addAll(fluids);p.outChemicals.addAll(chemicals);var j=new Job(p,n);j.progress=progress;return j;}
        int lanes(){return (units+operations-1)/operations;}
        void refresh(Controller c){if(progress>=ticks)return;var next=Profiles.settings(c,baseTicks,baseEnergy,exponential,fixedEnergy);ticks=next.ticks();energy=next.energy();operations=next.operations();progress=Math.min(progress,ticks-1);}
        boolean store(Buffers b,int n,boolean simulate){try{return b.store(items.stream().map(s->s.copyWithCount(Math.multiplyExact(s.getCount(),n))).toList(),fluids.stream().map(s->s.copyWithAmount(Math.multiplyExact(s.getAmount(),n))).toList(),chemicals.stream().map(s->s.copyWithAmount(Math.multiplyExact(s.getAmount(),n))).toList(),simulate);}catch(ArithmeticException e){return false;}}
        boolean deliver(Buffers b){int lo=0,hi=units;while(lo<hi){int mid=lo+(hi-lo+1)/2;if(store(b,mid,true))lo=mid;else hi=mid-1;}if(lo>0){store(b,lo,false);units-=lo;}return units==0;}
    }
    public int reserved(){return jobs.stream().mapToInt(j->j.units).sum();}
    public void tick(Controller c){
        c.running=0;c.powerUsed=0;c.progress=0;c.status="idle";
        if(!c.structure.valid()){c.status=c.structure.error;return;}
        if(!c.canFunction()){c.status="redstone";return;}
        if(!c.enabled&&jobs.isEmpty()){c.status="paused";return;}
        var profile=Profiles.get(c.template.getStack());if(profile==null){c.status="machine";return;}
        if(!Profiles.clean(c.template.getStack())){c.status="template_not_empty";return;}
        if(!profile.condition().test(c)){c.status="conditions";return;}
        c.parallel=Math.min(c.parallelLimit,c.structure.parallel);
        if(FactoryConfig.MACHINES_LIMIT_PARALLEL.get())c.parallel=Math.min(c.parallel,c.template.getStack().getCount());
        int limit=c.parallel;
        for(var j:jobs)j.refresh(c);
        for(var it=jobs.iterator();it.hasNext();){var j=it.next();if(j.progress>=j.ticks&&j.deliver(c.outputs)){it.remove();c.markForSave();}}
        int free=Math.max(0,limit-jobs.stream().mapToInt(Job::lanes).sum());String template=c.template.getStack().getItem().toString()+"/"+c.rotaryReverse;
        if(c.enabled&&free>0&&jobs.size()<512&&c.energy().available()>0&&(failedRevision!=c.inputRevision||!failedTemplate.equals(template)||c.getLevel().getGameTime()%20==0)){
            for(int tries=0;tries<16&&free>0;tries++){
                var p=profile.find(c);if(p==null){failedRevision=c.inputRevision;failedTemplate=template;break;}
                int n=p.available(c.inputs,(int)Math.min(65536L,(long)free*p.operations));n=(int)Math.min(n,c.energy().available()/p.energy);
                var candidate=new Job(p,n);int low=0,high=n;while(low<high){int mid=low+(high-low+1)/2;if(candidate.store(c.outputs,mid,true))low=mid;else high=mid-1;}n=low;
                if(n==0){c.status="output_or_energy";break;}
                p.consume(c.inputs,n);var added=new Job(p,n);jobs.add(added);free-=added.lanes();c.markForSave();
            }
        }
        int original=jobs.size(),budget=limit;var split=new ArrayList<Job>();
        for(int step=0;step<original&&budget>0;step++){
            var j=jobs.get((cursor+step)%original);if(j.progress>=j.ticks){c.status="output";continue;}
            int n=(int)Math.min(Math.min(j.units,(long)budget*j.operations),c.energy().available()/j.energy);if(n==0){c.status="energy";continue;}
            if(n<j.units&&jobs.size()+split.size()>=512){c.status="energy";continue;}
            long cost=Math.multiplyExact(j.energy,n);if(c.energy().extract(cost,Action.EXECUTE,AutomationType.INTERNAL)!=cost)throw new IllegalStateException("Induction payment changed on server thread");
            Job active=j;if(n<j.units){active=j.copy(n);j.units-=n;split.add(active);}active.progress++;
            int used=(n+j.operations-1)/j.operations;c.running+=used;budget-=used;c.powerUsed=mekanism.api.math.MathUtils.addClamped(c.powerUsed,cost);c.progress=Math.max(c.progress,active.progress*100/active.ticks);c.markForSave();
        }
        jobs.addAll(split);if(!jobs.isEmpty())cursor=(cursor+1)%jobs.size();
        for(var it=jobs.iterator();it.hasNext();){var j=it.next();if(j.progress>=j.ticks&&j.deliver(c.outputs)){it.remove();c.markForSave();}}
        if(c.running>0)c.status=c.enabled?"working":"draining";else if(jobs.isEmpty()&&c.status.equals("idle"))c.status=c.enabled?"materials":"paused";
    }
    public ListTag save(HolderLookup.Provider r){var list=new ListTag();for(var j:jobs){var tag=new CompoundTag();tag.putString("recipe",j.recipe);tag.putInt("units",j.units);tag.putInt("ticks",j.ticks);tag.putInt("progress",j.progress);tag.putLong("energy",j.energy);tag.putInt("base_ticks",j.baseTicks);tag.putLong("base_energy",j.baseEnergy);tag.putInt("operations",j.operations);tag.putBoolean("fixed",j.fixedEnergy);tag.putBoolean("exponential",j.exponential);var i=new ListTag();j.items.forEach(s->i.add(s.save(r)));tag.put("items",i);var f=new ListTag();j.fluids.forEach(s->f.add(s.saveOptional(r)));tag.put("fluids",f);var g=new ListTag();j.chemicals.forEach(s->g.add(s.saveOptional(r)));tag.put("chemicals",g);list.add(tag);}return list;}
    public void load(ListTag list,HolderLookup.Provider r){jobs.clear();for(var value:list){if(jobs.size()>=512)break;var tag=(CompoundTag)value;var p=new RecipePlan(tag.getString("recipe"));p.ticks=Math.clamp(tag.getInt("ticks"),1,10000000);p.energy=Math.max(1,tag.getLong("energy"));p.baseTicks=tag.contains("base_ticks")?Math.clamp(tag.getInt("base_ticks"),1,10000000):p.ticks;p.baseEnergy=tag.contains("base_energy")?Math.max(1,tag.getLong("base_energy")):p.energy;p.operations=Math.clamp(tag.getInt("operations"),1,65536);p.fixedEnergy=tag.getBoolean("fixed");p.exponential=tag.getBoolean("exponential");
        for(var v:tag.getList("items",Tag.TAG_COMPOUND))p.out(ItemStack.parseOptional(r,(CompoundTag)v));for(var v:tag.getList("fluids",Tag.TAG_COMPOUND))p.out(FluidStack.parseOptional(r,(CompoundTag)v));for(var v:tag.getList("chemicals",Tag.TAG_COMPOUND))p.out(ChemicalStack.parseOptional(r,(CompoundTag)v));
        var j=new Job(p,Math.clamp(tag.getInt("units"),1,65536));j.progress=Math.clamp(tag.getInt("progress"),0,j.ticks);jobs.add(j);
    }}
}
