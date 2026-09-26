package dev.everyonemek.gravity.corona;
import java.util.*;
import dev.everyonemek.gravity.solar.*;
import mekanism.api.*;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.inventory.slot.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Independent native Mek inventory, with a paid work snapshot instead of a second solar inventory. */
public final class CoronalMachine extends TileEntityMekanism {
    public List<InputInventorySlot> inputs;public List<OutputInventorySlot> outputs;
    public boolean enabled=true,autoEject=true;public int progress,duration,batch,remaining;public long paidEnergy;
    public String status="unlinked";private ItemStack product=ItemStack.EMPTY,sample=ItemStack.EMPTY;
    private static final int[][] ATTACHMENTS={{-1,4,4},{9,4,4},{4,4,-1},{4,4,9}};
    private long solarTick=Long.MIN_VALUE,visualTick=Long.MIN_VALUE,nextAttempt;private boolean visualRunning;
    public int displayProgress;public boolean displayRunning;public ItemStack displayItem=ItemStack.EMPTY,displayResult=ItemStack.EMPTY;
    public CoronalMachine(BlockPos p,BlockState s){super(CoronalContent.BLOCK,p,s);}
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){return EnergyContainerHelper.forSide(facingSupplier).build();}
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener){
        inputs=new ArrayList<>();outputs=new ArrayList<>();var holder=InventorySlotHelper.forSide(facingSupplier);
        for(int i=0;i<9;i++){var slot=InputInventorySlot.at(listener,12+i%3*18,36+i/3*18);inputs.add(slot);holder.addSlot(slot,RelativeSide.TOP,RelativeSide.BOTTOM,RelativeSide.LEFT,RelativeSide.RIGHT);}
        for(int i=0;i<9;i++){var slot=OutputInventorySlot.at(listener,154+i%3*18,36+i/3*18);outputs.add(slot);holder.addSlot(slot,RelativeSide.FRONT);}return holder.build();
    }
    @Override public boolean persists(ContainerType<?,?,?> type){return type!=ContainerType.ENERGY&&super.persists(type);}
    public SolarController solar(){
        if(level==null||isRemoved())return null;var back=worldPosition.relative(getDirection().getOpposite());if(!level.hasChunkAt(back))return null;
        if(!(level.getBlockEntity(back) instanceof SolarPart part)||part.kind()!=SolarBlock.Kind.COLLECTOR||part.getBlockState().getValue(SolarBlock.SEGMENT)!=4||part.getBlockState().getValue(SolarBlock.FACING)!=getDirection().getOpposite())return null;
        var c=part.controller();return c!=null&&c.structure.valid()?c:null;
    }
    private boolean allowed(SolarController c){return IBlockSecurityUtils.INSTANCE.canAccess(IBlockSecurityUtils.INSTANCE.getOwnerUUID(level,worldPosition,this),level,c.getBlockPos(),c);}
    public static void processAttached(SolarController c){
        if(!c.structure.formed)return;
        for(int[] xyz:ATTACHMENTS){var pos=c.structure.at(xyz[0],xyz[1],xyz[2]);
            if(c.getLevel().hasChunkAt(pos)&&c.getLevel().getBlockEntity(pos) instanceof CoronalMachine chamber&&chamber.solar()==c)chamber.process(c);
        }
    }
    public ItemStack workProduct(){return product.copy();}
    public double fraction(){return duration<=0?0:Math.clamp(progress/(double)duration,0,1);}
    private CoronalRecipe.Inventory snapshot(){return new CoronalRecipe.Inventory(inputs.stream().map(s->s.getStack().copy()).toList());}
    private boolean fits(ItemStack item,int amount){
        int room=0;for(var slot:outputs){var s=slot.getStack();if(s.isEmpty()||ItemStack.isSameItemSameComponents(s,item))room+=Math.max(0,slot.getLimit(item)-s.getCount());}return room>=amount;
    }
    private record Plan(ItemStack result,ItemStack sample,int amount,int batch,int ticks,long energy,int[] consumed){}
    private Plan plan(SolarController c){var view=snapshot();boolean tierBlocked=false,outputBlocked=false;
        // Explicit high-temperature recipes take priority; vanilla cooking is the fallback.
        for(var holder:level.getRecipeManager().getAllRecipesFor(CoronalContent.TYPE.get())){
            var recipe=holder.value();if(recipe.allocate(view,1)==null)continue;
            if(c.structure.tier<recipe.tier()){tierBlocked=true;continue;}
            for(int n=SolarConfig.CORONAL_BATCH.get();n>=1;n--){var used=recipe.allocate(view,n);if(used==null)continue;int amount=recipe.result().getCount()*n;
                if(!fits(recipe.result(),amount)){outputBlocked=true;continue;}
                ItemStack example=ItemStack.EMPTY;for(int i=0;i<used.length;i++)if(used[i]>0){example=view.getItem(i).copyWithCount(1);break;}
                return new Plan(recipe.result().copyWithCount(1),example,amount,n,Math.max(1,(recipe.ticks()+(1<<c.structure.tier)-1)/(1<<c.structure.tier)),Math.multiplyExact(recipe.energy(),n),used);
            }
        }
        for(int i=0;i<view.size();i++){var stack=view.getItem(i);if(stack.isEmpty())continue;var input=new SingleRecipeInput(stack.copyWithCount(1));
            Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> found=level.getRecipeManager().getRecipeFor(RecipeType.BLASTING,input,level);
            if(found.isEmpty())found=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);if(found.isEmpty())continue;
            var result=found.get().value().assemble(input,level.registryAccess());if(result.isEmpty())continue;
            int available=0;for(var s:view.items())if(ItemStack.isSameItemSameComponents(s,stack))available+=s.getCount();
            for(int n=Math.min(available,SolarConfig.CORONAL_BATCH.get());n>=1;n--){if(!fits(result,result.getCount()*n)){outputBlocked=true;continue;}
                int[] used=new int[9];int need=n;for(int slot=0;slot<9&&need>0;slot++)if(ItemStack.isSameItemSameComponents(view.getItem(slot),stack)){used[slot]=Math.min(need,view.getItem(slot).getCount());need-=used[slot];}
                return new Plan(result.copyWithCount(1),stack.copyWithCount(1),result.getCount()*n,n,Math.max(1,(SolarConfig.CORONAL_TICKS.get()+(1<<c.structure.tier)-1)/(1<<c.structure.tier)),Math.multiplyExact(SolarConfig.CORONAL_ENERGY.get(),n),used);
            }
        }
        status=outputBlocked?"output_full":tierBlocked?"tier_low":"no_recipe";return null;
    }
    private void finish(){
        if(product.isEmpty()||remaining<=0)return;
        for(int pass=0;pass<2&&remaining>0;pass++)for(var slot:outputs){if(slot.isEmpty()!=(pass==1))continue;var offered=product.copyWithCount(remaining);var rest=slot.insertItem(offered,Action.EXECUTE,AutomationType.INTERNAL);remaining=rest.getCount();if(remaining==0)break;}
        markForSave();if(remaining==0){product=sample=ItemStack.EMPTY;progress=duration=batch=0;paidEnergy=0;}else status="output_full";
    }
    private void process(SolarController c){
        if(solarTick==level.getGameTime())return;solarTick=level.getGameTime();boolean working=false;
        try{
            if(!allowed(c)){status="access";return;}if(remaining>0&&progress>=duration){finish();if(remaining>0)return;}
            if(!enabled||!canFunction()){status="paused";return;}if(!c.isCoreHot()||!c.canFunction()){status="cold";return;}
            if(remaining==0){if(level.getGameTime()<nextAttempt)return;nextAttempt=level.getGameTime()+5;if(inputs.stream().allMatch(s->s.isEmpty())){status="no_recipe";return;}var plan=plan(c);if(plan==null)return;if(!c.spendForCorona(plan.energy)){status="energy";return;}
                for(int i=0;i<9;i++)if(plan.consumed[i]>0)inputs.get(i).shrinkStack(plan.consumed[i],Action.EXECUTE);
                product=plan.result;sample=plan.sample;remaining=plan.amount;batch=plan.batch;duration=plan.ticks;progress=0;paidEnergy=plan.energy;markForSave();visual(true,true);
            }
            working=true;status="running";progress++;markForSave();visual(true,false);if(progress>=duration){finish();nextAttempt=0;}
        }finally{setActive(working);visual(working,false);}
    }
    private void eject(){if(!autoEject)return;var front=worldPosition.relative(getDirection());if(!level.hasChunkAt(front))return;
        var target=level.getCapability(Capabilities.ItemHandler.BLOCK,front,getDirection().getOpposite());if(target==null)return;
        for(var slot:outputs){if(slot.isEmpty())continue;var offer=slot.getStack().copy();int accepted=offer.getCount()-ItemHandlerHelper.insertItemStacked(target,offer,true).getCount();if(accepted<=0)continue;
            var sent=slot.extractItem(accepted,Action.EXECUTE,AutomationType.INTERNAL);var rest=ItemHandlerHelper.insertItemStacked(target,sent,false);if(!rest.isEmpty())slot.insertItem(rest,Action.EXECUTE,AutomationType.INTERNAL);
        }
    }
    @Override protected boolean onUpdateServer(){boolean changed=super.onUpdateServer();var c=solar();if(c==null){status="unlinked";setActive(false);visual(false,false);}else if(!allowed(c)){status="access";setActive(false);visual(false,false);}eject();return changed;}
    private CompoundTag work(HolderLookup.Provider registry){var tag=new CompoundTag();tag.putBoolean("enabled",enabled);tag.putBoolean("eject",autoEject);tag.putInt("progress",progress);tag.putInt("duration",duration);tag.putInt("batch",batch);tag.putInt("remaining",remaining);tag.putLong("paid",paidEnergy);if(!product.isEmpty())tag.put("product",product.save(registry));if(!sample.isEmpty())tag.put("sample",sample.save(registry));return tag;}
    private void readWork(CompoundTag tag,HolderLookup.Provider registry){enabled=!tag.contains("enabled")||tag.getBoolean("enabled");autoEject=!tag.contains("eject")||tag.getBoolean("eject");product=ItemStack.parseOptional(registry,tag.getCompound("product"));sample=ItemStack.parseOptional(registry,tag.getCompound("sample"));duration=Math.clamp(tag.getInt("duration"),0,72000);progress=Math.clamp(tag.getInt("progress"),0,duration);batch=Math.clamp(tag.getInt("batch"),0,64);remaining=product.isEmpty()?0:Math.clamp(tag.getInt("remaining"),0,4096);paidEnergy=Math.clamp(tag.getLong("paid"),0,64_000_000_000_000L);}
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registry){super.saveAdditional(tag,registry);tag.put("coronal_work",work(registry));}
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider registry){super.loadAdditional(tag,registry);readWork(tag.getCompound("coronal_work"),registry);}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder){super.collectImplicitComponents(builder);builder.set(CoronalContent.WORK.get(),work(level.registryAccess()));}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input){super.applyImplicitComponents(input);var tag=input.get(CoronalContent.WORK.get());if(tag!=null)readWork(tag,level.registryAccess());}
    private void visual(boolean running,boolean force){long now=level.getGameTime();if(force||running!=visualRunning||running&&now-visualTick>=4){visualRunning=running;visualTick=now;sendUpdatePacket();}}
    @Override public CompoundTag getReducedUpdateTag(HolderLookup.Provider registry){var tag=super.getReducedUpdateTag(registry);tag.putBoolean("corona_running",visualRunning);tag.putInt("corona_progress",Math.clamp((int)(fraction()*100),0,100));if(!sample.isEmpty())tag.put("corona_sample",sample.save(registry));if(!product.isEmpty())tag.put("corona_result",product.save(registry));return tag;}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registry){super.handleUpdateTag(tag,registry);displayRunning=tag.getBoolean("corona_running");displayProgress=Math.clamp(tag.getInt("corona_progress"),0,100);displayItem=ItemStack.parseOptional(registry,tag.getCompound("corona_sample"));displayResult=ItemStack.parseOptional(registry,tag.getCompound("corona_result"));}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);menu.track(SyncableInt.create(()->progress,v->progress=v));menu.track(SyncableInt.create(()->duration,v->duration=v));menu.track(SyncableInt.create(()->batch,v->batch=v));menu.track(SyncableLong.create(()->paidEnergy,v->paidEnergy=v));menu.track(SyncableBoolean.create(()->enabled,v->enabled=v));menu.track(SyncableBoolean.create(()->autoEject,v->autoEject=v));menu.track(SyncableInt.create(()->CoronalMenu.STATES.indexOf(status),v->status=CoronalMenu.STATES.get(Math.clamp(v,0,CoronalMenu.STATES.size()-1))));}
}
