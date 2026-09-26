package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.corona.CoronalInventorySlot;
import mekanism.api.*;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.inventory.slot.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Each module owns its cargo; links only access a reactor's single real energy budget. */
public final class OrbitalModule extends TileEntityMekanism {
    public List<BasicInventorySlot> inputs,outputs;
    public NodeStorage node;public int channels=15;
    public FieldLink source,peer;public boolean enabled=true,autoEject=true,receiverConfigured,sourceBound,peerBound;
    public int progress,duration,batch,remaining,signal,alarm,threshold=25;
    public long paidEnergy,transferred;public String status="unlinked";
    private ItemStack product=ItemStack.EMPTY,sample=ItemStack.EMPTY;
    private long retryAt,receivedUntil,visualTick=Long.MIN_VALUE;private boolean visualRunning;
    public long sourceStored,sourceCapacity,sourceNet,sourceOutput,sourceFuel,sourceFuelTotal,sourceSpare;
    public int sourceProfile,sourceBurst,sourceCooldown;public boolean sourceSolar,sourceFormed;
    public boolean displayRunning;public int displayProgress;public ItemStack displayItem=ItemStack.EMPTY;
    public OrbitalModule(BlockPos pos,BlockState state){super(ModuleContent.BLOCK.get(ModuleContent.kind(state)),pos,state);}
    public ModuleKind kind(){return ModuleContent.kind(getBlockState());}
    private NodeStorage node(IContentsListener listener){if(node==null)node=new NodeStorage(listener);return node;}
    private static final RelativeSide[] INPUT_SIDES={RelativeSide.LEFT,RelativeSide.RIGHT,RelativeSide.TOP,RelativeSide.BOTTOM,RelativeSide.BACK};
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){var holder=EnergyContainerHelper.forSide(facingSupplier);if(kind()==ModuleKind.NODE){var storage=node(listener);holder.addContainer(storage.energyIn,INPUT_SIDES);holder.addContainer(storage.energyOut,RelativeSide.FRONT);}return holder.build();}
    @Override protected mekanism.common.capabilities.holder.fluid.IFluidTankHolder getInitialFluidTanks(IContentsListener listener){var holder=mekanism.common.capabilities.holder.fluid.FluidTankHelper.forSide(facingSupplier);if(kind()==ModuleKind.NODE){var storage=node(listener);storage.fluidIn.forEach(t->holder.addTank(t,INPUT_SIDES));storage.fluidOut.forEach(t->holder.addTank(t,RelativeSide.FRONT));}return holder.build();}
    @Override public mekanism.common.capabilities.holder.chemical.IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener){var holder=mekanism.common.capabilities.holder.chemical.ChemicalTankHelper.forSide(facingSupplier);if(kind()==ModuleKind.NODE){var storage=node(listener);storage.chemicalIn.forEach(t->holder.addTank(t,INPUT_SIDES));storage.chemicalOut.forEach(t->holder.addTank(t,RelativeSide.FRONT));}return holder.build();}
    public boolean channel(int type){return (channels&(1<<type))!=0;}
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener){inputs=new ArrayList<>();outputs=new ArrayList<>();var holder=InventorySlotHelper.forSide(facingSupplier);
        if(kind().cargo){for(int i=0;i<9;i++){var slot=new CoronalInventorySlot(false,listener,12+i%3*18,36+i/3*18);inputs.add(slot);holder.addSlot(slot,RelativeSide.LEFT,RelativeSide.RIGHT,RelativeSide.TOP,RelativeSide.BOTTOM,RelativeSide.BACK);}
            for(int i=0;i<9;i++){var slot=new CoronalInventorySlot(true,listener,154+i%3*18,36+i/3*18);outputs.add(slot);holder.addSlot(slot,RelativeSide.FRONT);}}
        else if(kind()==ModuleKind.TUNER){var slot=InputInventorySlot.at(s->s.is(ModuleContent.FLARE.get()),listener,13,105);inputs.add(slot);holder.addSlot(slot,RelativeSide.TOP,RelativeSide.BOTTOM,RelativeSide.BACK);}return holder.build();}
    @Override public boolean persists(ContainerType<?,?,?> type){return (kind()==ModuleKind.NODE||type!=ContainerType.ENERGY)&&super.persists(type);}
    // Transport cells remain sealed in their native chemical item component, including radioactive cargo.
    @Override public boolean shouldDumpRadiation(){return kind()!=ModuleKind.NODE&&super.shouldDumpRadiation();}
    @Override public void onContentsChanged(){super.onContentsChanged();retryAt=0;}
    public boolean access(Player p){return !isRemoved()&&p.level()==level&&p.distanceToSqr(worldPosition.getCenter())<=64&&IBlockSecurityUtils.INSTANCE.canAccess(p,level,worldPosition,this);}
    public FieldSource linked(){if(source==null){status="unlinked";return null;}if(!source.inRange(level,worldPosition)){status="range";return null;}if(!level.hasChunkAt(source.pos())){status="unloaded";return null;}
        var core=FieldSource.at(level,source.pos());if(core==null){status="missing";return null;}if(!core.permitted(this)){status="access";return null;}
        if(kind()==ModuleKind.CAPTOR&&!core.solar()||kind()==ModuleKind.FORGE&&core.solar()){status="source_type";return null;}return core;}
    public boolean bind(Player player,FieldLink target,boolean node){if(!access(player)||!target.inRange(level,worldPosition)||!level.hasChunkAt(target.pos()))return false;
        if(node){if(kind()!=ModuleKind.NODE||target.pos().equals(worldPosition)||!(level.getBlockEntity(target.pos()) instanceof OrbitalModule other)||other.kind()!=ModuleKind.NODE||!IBlockSecurityUtils.INSTANCE.canAccess(player,level,target.pos(),other))return false;peer=target;other.receiverConfigured=true;other.markForSave();other.sendUpdatePacket();}
        else{var core=FieldSource.at(level,target.pos());if(core==null||!core.permitted(player)||kind()==ModuleKind.CAPTOR&&!core.solar()||kind()==ModuleKind.FORGE&&core.solar())return false;source=FieldLink.at(level,core.tile().getBlockPos());}
        retryAt=0;markForSave();sendUpdatePacket();return true;}
    public boolean command(Player player,int action){if(!access(player))return false;
        if(action==0)enabled=!enabled;
        else if(action==1&&kind().cargo)autoEject=!autoEject;
        else if(action==2){source=null;peer=null;}
        else if(action==5&&kind()==ModuleKind.NODE)peer=null;
        else if(action==6&&kind()==ModuleKind.NODE)source=null;
        else if(action>=20&&action<=23&&kind()==ModuleKind.NODE)channels^=1<<(action-20);
        else if(action==3&&kind()==ModuleKind.OBSERVATORY)alarm=(alarm+1)%4;
        else if(action==4&&kind()==ModuleKind.OBSERVATORY)threshold=threshold==75?10:threshold==10?25:threshold==25?50:75;
        else if(kind()==ModuleKind.TUNER&&action>=10&&action<=13){var core=linked();if(core==null||!core.permitted(player)||!core.formed()||!enabled||!canFunction())return false;
            if(action==13){if(!core.hot()||core.available()<=0||core.stored()>=core.capacity()||inputs.getFirst().isEmpty()||!inputs.getFirst().getStack().is(ModuleContent.FLARE.get())||!core.tuning().trigger())return false;inputs.getFirst().shrinkStack(1,Action.EXECUTE);}
            else core.tuning().profile=action-10;core.tile().markForSave();telemetry(core);
        }else return false;markForSave();return true;
    }
    private int room(ItemStack item){int n=0;for(var s:outputs)if(s.isEmpty()||ItemStack.isSameItemSameComponents(item,s.getStack()))n+=Math.max(0,s.getLimit(item)-s.getCount());return n;}
    private int insert(ItemStack item,int amount){int left=amount;for(int pass=0;pass<2&&left>0;pass++)for(var slot:outputs){if(slot.isEmpty()!=(pass==1))continue;left=slot.insertItem(item.copyWithCount(left),Action.EXECUTE,AutomationType.INTERNAL).getCount();if(left==0)break;}return left;}
    private boolean start(FieldSource core){var inv=new OrbitalRecipe.Inventory(inputs.stream().map(s->s.getStack().copy()).toList());boolean tier=false,blocked=false,energy=false;
        // Prefer a complete multi-input recipe over its single-input fallback, regardless of reload order.
        var recipes=level.getRecipeManager().getAllRecipesFor(ModuleContent.TYPE.get()).stream().filter(h->h.value().machine().equals(kind().id))
            .sorted(Comparator.<net.minecraft.world.item.crafting.RecipeHolder<OrbitalRecipe>>comparingInt(h->h.value().inputs().size()).reversed().thenComparing(h->h.id().toString())).toList();
        int matchedGroups=-1;
        for(var holder:recipes){var recipe=holder.value();if(recipe.inputs().size()<matchedGroups)break;if(recipe.allocate(inv,1)==null)continue;matchedGroups=recipe.inputs().size();
            if(recipe.tier()>core.tier()){tier=true;continue;}int space=room(recipe.result())/recipe.result().getCount();if(space<=0){blocked=true;continue;}
            long funded=core.available()/recipe.energy();if(funded==0){energy=true;continue;}int n=recipe.maxBatch(inv,(int)Math.min(Math.min(space,ModuleConfig.BATCH.get()),funded));if(n<=0)continue;var used=recipe.allocate(inv,n);long paid=Math.multiplyExact(recipe.energy(),n);
            if(!core.spend(paid)){energy=true;continue;}sample=ItemStack.EMPTY;for(int i=0;i<used.length;i++)if(used[i]>0){if(sample.isEmpty())sample=inputs.get(i).getStack().copyWithCount(1);inputs.get(i).shrinkStack(used[i],Action.EXECUTE);}
            product=recipe.result().copyWithCount(1);remaining=recipe.result().getCount()*n;batch=n;paidEnergy=paid;int speed=(1<<core.tier())*core.tuning().processing();duration=Math.max(1,(recipe.ticks()+speed-1)/speed);progress=0;markForSave();return true;}
        status=blocked?"output_full":tier?"tier":energy?"energy":"ingredients";return false;}
    private void finish(){remaining=insert(product,remaining);if(remaining==0){product=sample=ItemStack.EMPTY;}else status="output_full";markForSave();}
    private boolean process(FieldSource core){if(remaining>0&&progress>=duration){finish();if(remaining>0)return false;}
        if(!enabled||!canFunction()){status="paused";return false;}if(!core.hot()){status="cold";return false;}
        if(remaining==0){if(level.getGameTime()<retryAt)return false;retryAt=level.getGameTime()+5;if(!start(core))return false;}
        status="running";progress++;markForSave();visual(true);if(progress>=duration){finish();retryAt=0;eject();}return true;}
    private boolean transfer(FieldSource core){if(!enabled||!canFunction()){status="paused";return false;}if(!core.hot()){status="cold";return false;}if(peer==null){status="peer_missing";return false;}
        if(!peer.inRange(level,worldPosition)){status="range";return false;}if(!level.hasChunkAt(peer.pos())){status="unloaded";return false;}
        if(!(level.getBlockEntity(peer.pos()) instanceof OrbitalModule target)||target==this||target.kind()!=ModuleKind.NODE){status="peer_missing";return false;}
        if(!IBlockSecurityUtils.INSTANCE.canAccess(IBlockSecurityUtils.INSTANCE.getOwnerUUID(level,worldPosition,this),level,target.worldPosition,target)){status="access";return false;}
        if(!target.enabled||!target.canFunction()){status="peer_paused";return false;}if((channels&target.channels)==0){status="channels_off";return false;}boolean moved=false;status="resources_waiting";paidEnergy=0;node.clock(level.getGameTime());
        // Rotate resource priority so a power-limited item stream cannot starve energy or tanks.
        int first=(int)(level.getGameTime()&3);for(int i=0;i<4;i++){int resource=(first+i)&3;moved|=resource==0?transferItems(target,core):NodeStorage.transfer(this,target,core,resource);}
        if(moved){status="transferring";duration=progress=1;target.receiverConfigured=true;target.receivedUntil=level.getGameTime()+5;target.visual(true);target.markForSave();markForSave();target.eject();}return moved;}
    private boolean transferItems(OrbitalModule target,FieldSource core){int budget=channel(0)&&target.channel(0)?ModuleConfig.TRANSFER.get():0;boolean moved=false;
        for(var input:inputs){if(budget==0)break;if(input.isEmpty())continue;int n=(int)Math.min(Math.min(input.getCount(),budget),Math.min(target.room(input.getStack()),core.available()/ModuleConfig.ITEM_COST.get()));
            if(n==0){status=target.room(input.getStack())==0?"output_full":"energy";continue;}long cost=n*ModuleConfig.ITEM_COST.get();if(!core.spend(cost)){status="energy";break;}
            // Only our own locked server-thread inventories are involved; planning and commit cannot interleave.
            var stack=input.getStack().copyWithCount(n);input.shrinkStack(n,Action.EXECUTE);int left=target.insert(stack,n);if(left!=0)throw new IllegalStateException("Reserved node capacity changed during transfer");
            sample=stack.copyWithCount(1);budget-=n;node.itemsMoved+=n;transferred=transferred>Long.MAX_VALUE-n?Long.MAX_VALUE:transferred+n;batch=(int)node.itemsMoved;paidEnergy+=cost;duration=progress=1;target.sample=sample.copy();target.receiverConfigured=true;target.receivedUntil=level.getGameTime()+5;target.visual(true);target.markForSave();moved=true;
        }return moved;}
    private void telemetry(FieldSource core){sourceStored=core.stored();sourceCapacity=core.capacity();sourceNet=core.net();sourceOutput=core.exported();sourceFuel=core.fuel();sourceFuelTotal=core.fuelTotal();sourceSpare=core.formed()?core.spare():0;sourceSolar=core.solar();sourceFormed=core.formed();sourceProfile=core.tuning().profile;sourceBurst=core.tuning().burst;sourceCooldown=core.tuning().cooldown;}
    private void observe(FieldSource core){int next=0;if(enabled&&canFunction()){
        if(core==null)next=alarm==1?15:0;
        else if(alarm==0)next=(int)Math.clamp(Math.ceil(core.stored()*15D/Math.max(1,core.capacity())),0,15);
        else if(alarm==1)next=core.formed()?0:15;
        else if(alarm==2)next=sourceSpare==0&&(sourceFuelTotal==0||sourceFuel*100D/sourceFuelTotal<=threshold)?15:0;
        else next=core.stored()*100D/Math.max(1,core.capacity())<=threshold?15:0;
    }if(next!=signal){signal=next;level.updateNeighborsAt(worldPosition,getBlockState().getBlock());level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
    private void eject(){if(kind()==ModuleKind.NODE)node.eject(this);if(!autoEject||outputs.isEmpty())return;var pos=worldPosition.relative(getDirection());if(!level.hasChunkAt(pos))return;var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,getDirection().getOpposite());if(handler==null)return;
        int budget=kind()==ModuleKind.NODE?36864:4096;for(var slot:outputs){if(budget<=0)break;if(slot.isEmpty())continue;var offered=slot.getStack().copyWithCount(Math.min(budget,slot.getCount()));int count=offered.getCount()-ItemHandlerHelper.insertItemStacked(handler,offered,true).getCount();if(count==0)continue;
            var extracted=slot.extractItem(count,Action.EXECUTE,AutomationType.INTERNAL);var remainder=ItemHandlerHelper.insertItemStacked(handler,extracted,false);budget-=extracted.getCount()-remainder.getCount();if(!remainder.isEmpty())slot.insertItem(remainder,Action.EXECUTE,AutomationType.INTERNAL);}}
    @Override protected boolean onUpdateServer(){boolean changed=super.onUpdateServer();if(kind()==ModuleKind.NODE)node.clock(level.getGameTime());eject();var core=linked();boolean working=false;
        if(core!=null){if(level.getGameTime()%5==0)telemetry(core);if(kind().processor())working=process(core);else if(kind()==ModuleKind.NODE)working=transfer(core);else{status=!enabled||!canFunction()?"paused":!core.formed()?"structure":core.hot()?"monitoring":"cold";working=enabled&&canFunction()&&core.formed();}}
        else{sourceStored=sourceCapacity=sourceNet=sourceOutput=sourceFuel=sourceFuelTotal=sourceSpare=0;sourceFormed=false;}
        if(kind()==ModuleKind.NODE&&source==null&&peer==null)status=enabled&&canFunction()?"receiving_ready":"paused";
        if(kind()==ModuleKind.NODE&&enabled&&canFunction()&&receivedUntil>level.getGameTime()&&!working){status="receiving";working=true;}
        if(kind()==ModuleKind.OBSERVATORY)observe(core);setActive(working);visual(working);return changed;}
    private CompoundTag data(HolderLookup.Provider r){var t=new CompoundTag();if(source!=null)t.put("source",source.save());if(peer!=null)t.put("peer",peer.save());t.putBoolean("receiver",receiverConfigured);t.putInt("channels",channels);t.putBoolean("enabled",enabled);t.putBoolean("eject",autoEject);t.putInt("alarm",alarm);t.putInt("threshold",threshold);t.putInt("progress",progress);t.putInt("duration",duration);t.putInt("batch",batch);t.putInt("remaining",remaining);t.putLong("paid",paidEnergy);t.putLong("transferred",transferred);if(!product.isEmpty())t.put("product",product.save(r));if(!sample.isEmpty())t.put("sample",sample.save(r));return t;}
    private void read(CompoundTag t,HolderLookup.Provider r){receiverConfigured=t.getBoolean("receiver");channels=t.contains("channels")?Math.clamp(t.getInt("channels"),0,15):15;source=FieldLink.read(t.getCompound("source"));peer=FieldLink.read(t.getCompound("peer"));enabled=!t.contains("enabled")||t.getBoolean("enabled");autoEject=!t.contains("eject")||t.getBoolean("eject");alarm=Math.clamp(t.getInt("alarm"),0,3);threshold=t.contains("threshold")?Math.clamp(t.getInt("threshold"),1,99):25;
        duration=Math.clamp(t.getInt("duration"),0,72000);progress=Math.clamp(t.getInt("progress"),0,duration);batch=Math.clamp(t.getInt("batch"),0,36864);product=ItemStack.parseOptional(r,t.getCompound("product"));sample=ItemStack.parseOptional(r,t.getCompound("sample"));remaining=product.isEmpty()?0:Math.clamp(t.getInt("remaining"),0,32768);paidEnergy=Math.clamp(t.getLong("paid"),0,51_200_000_000_000_000L);transferred=Math.max(0,t.getLong("transferred"));}
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("orbital",data(r));}
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);read(tag.getCompound("orbital"),r);}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder){super.collectImplicitComponents(builder);builder.set(ModuleContent.DATA.get(),data(level.registryAccess()));}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input){super.applyImplicitComponents(input);var tag=input.get(ModuleContent.DATA.get());if(tag!=null)read(tag,level.registryAccess());}
    public double fraction(){return duration==0?0:Math.clamp(progress/(double)duration,0,1);}
    private void visual(boolean running){long now=level.getGameTime();if(running!=visualRunning||running&&now-visualTick>=5){visualRunning=running;visualTick=now;sendUpdatePacket();}}
    @Override public CompoundTag getReducedUpdateTag(HolderLookup.Provider r){var t=super.getReducedUpdateTag(r);t.putBoolean("module_running",visualRunning);t.putInt("module_progress",kind()==ModuleKind.TUNER?sourceProfile*50:kind()==ModuleKind.OBSERVATORY?(int)Math.clamp(sourceStored*100D/Math.max(1,sourceCapacity),0,100):(int)(fraction()*100));if(!sample.isEmpty())t.put("module_sample",sample.save(r));return t;}
    @Override public void handleUpdateTag(CompoundTag t,HolderLookup.Provider r){super.handleUpdateTag(t,r);displayRunning=t.getBoolean("module_running");displayProgress=Math.clamp(t.getInt("module_progress"),0,100);displayItem=ItemStack.parseOptional(r,t.getCompound("module_sample"));}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);
        if(kind()==ModuleKind.NODE){menu.track(SyncableInt.create(()->channels,v->channels=v));menu.track(SyncableLong.create(()->node.itemsMoved,v->node.itemsMoved=v));menu.track(SyncableLong.create(()->node.energyMoved,v->node.energyMoved=v));menu.track(SyncableLong.create(()->node.fluidMoved,v->node.fluidMoved=v));menu.track(SyncableLong.create(()->node.chemicalMoved,v->node.chemicalMoved=v));}
        menu.track(SyncableInt.create(()->progress,v->progress=v));menu.track(SyncableInt.create(()->duration,v->duration=v));menu.track(SyncableInt.create(()->batch,v->batch=v));menu.track(SyncableInt.create(()->signal,v->signal=v));menu.track(SyncableInt.create(()->alarm,v->alarm=v));menu.track(SyncableInt.create(()->threshold,v->threshold=v));
        menu.track(SyncableBoolean.create(()->enabled,v->enabled=v));menu.track(SyncableBoolean.create(()->autoEject,v->autoEject=v));menu.track(SyncableLong.create(()->paidEnergy,v->paidEnergy=v));menu.track(SyncableLong.create(()->transferred,v->transferred=v));
        menu.track(SyncableLong.create(()->sourceStored,v->sourceStored=v));menu.track(SyncableLong.create(()->sourceCapacity,v->sourceCapacity=v));menu.track(SyncableLong.create(()->sourceNet,v->sourceNet=v));menu.track(SyncableLong.create(()->sourceOutput,v->sourceOutput=v));menu.track(SyncableLong.create(()->sourceFuel,v->sourceFuel=v));menu.track(SyncableLong.create(()->sourceFuelTotal,v->sourceFuelTotal=v));menu.track(SyncableLong.create(()->sourceSpare,v->sourceSpare=v));
        menu.track(SyncableInt.create(()->sourceProfile,v->sourceProfile=v));menu.track(SyncableInt.create(()->sourceBurst,v->sourceBurst=v));menu.track(SyncableInt.create(()->sourceCooldown,v->sourceCooldown=v));menu.track(SyncableBoolean.create(()->sourceSolar,v->sourceSolar=v));menu.track(SyncableBoolean.create(()->sourceFormed,v->sourceFormed=v));
        menu.track(SyncableInt.create(()->ModuleMenu.STATES.indexOf(status),v->status=ModuleMenu.STATES.get(Math.clamp(v,0,ModuleMenu.STATES.size()-1))));
        menu.track(SyncableBoolean.create(()->source!=null,v->sourceBound=v));menu.track(SyncableBoolean.create(()->peer!=null,v->peerBound=v));
        menu.track(SyncableBlockPos.create(()->source==null?BlockPos.ZERO:source.pos(),v->source=new FieldLink("",v)));menu.track(SyncableBlockPos.create(()->peer==null?BlockPos.ZERO:peer.pos(),v->peer=new FieldLink("",v)));
    }
}
