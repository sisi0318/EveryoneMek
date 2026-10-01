package dev.everyonemek.oritech;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import rearth.oritech.api.networking.*;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.block.base.entity.*;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.*;
import rearth.oritech.util.*;
import software.bernie.geckolib.animation.*;

public final class Processor extends UpgradableMachineBlockEntity {
    public static final int HOST=8,ADDON_START=9,MAX_TIER=7,ADDONS_PER_TIER=9,MAX_ADDONS=MAX_TIER*ADDONS_PER_TIER,
        ADDON_END=ADDON_START+MAX_ADDONS,LOAD_SLOT=ADDON_END,UNLOAD_SLOT=LOAD_SLOT+1,SIZE=UNLOAD_SLOT+1;
    public UUID identity=UUID.randomUUID();
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK,SyncType.SPARSE_TICK}) public int tier=1;
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK}) public int addonCount;
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK,SyncType.SPARSE_TICK}) public int profileIndex,modules,status;
    @SyncField({SyncType.INITIAL,SyncType.TICK,SyncType.SPARSE_TICK,SyncType.GUI_OPEN,SyncType.GUI_TICK}) public int duration=1;
    @SyncField({SyncType.GUI_OPEN,SyncType.GUI_TICK}) public long usage,paid,rate;
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK}) public boolean fluidAddon,yieldAddon,eject=true;
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK,SyncType.SPARSE_TICK}) public final SimpleFluidStorage fluidIn=new SimpleFluidStorage(8000L,this::setChanged);
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK,SyncType.SPARSE_TICK}) public final SimpleFluidStorage fluidA=new SimpleFluidStorage(8000L,this::setChanged);
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK,SyncType.SPARSE_TICK}) public final SimpleFluidStorage fluidB=new SimpleFluidStorage(4000L,this::setChanged);
    @SyncField({SyncType.INITIAL,SyncType.GUI_OPEN,SyncType.GUI_TICK,SyncType.SPARSE_TICK}) public final SimpleFluidStorage fluidC=new SimpleFluidStorage(4000L,this::setChanged);
    public boolean removingParts;
    private final List<BlockPos> occupied=new ArrayList<>();
    private List<ItemStack> equipment=List.of();
    CompoundTag signature=new CompoundTag();
    private boolean refresh=true;
    private Direction deployedFacing=Direction.NORTH;
    public long deployedAt;
    private Engine.Job job;
    private AnimationController<Processor> animationController;

    public Processor(BlockPos p,BlockState s){super(Content.TILE.get(),p,s,32);}
    public Profiles profile(){return Profiles.index(profileIndex);}
    public Direction facing(){return getBlockState().getValue(ProcessorBlock.FACING);}
    public int addonSlots(){return Math.clamp(tier,1,MAX_TIER)*ADDONS_PER_TIER;}
    public int countAddons(){int count=0;for(int i=ADDON_START;i<ADDON_END;i++)count+=inventory.getItem(i).getCount();return count;}
    @Override public float getCoreQuality(){return Math.clamp(tier,1,MAX_TIER);}
    public void upgradeTier(int target){
        if(level==null||level.isClientSide||target<=tier||target>MAX_TIER)return;
        tier=target;setChanged();syncWorld();sendUpdate(SyncType.GUI_OPEN);
    }
    public List<SimpleFluidStorage> tanks(){return List.of(fluidIn,fluidA,fluidB,fluidC);}
    @Override public int getInventorySize(){return SIZE;}
    @Override public InventorySlotAssignment getSlotAssignments(){return new InventorySlotAssignment(0,4,4,4);}
    @Override public List<GuiSlot> getGuiSlots(){return profile().slots();}
    @Override public List<Vec3i> getAddonSlots(){return List.of();}
    @Override protected OritechRecipeType getOwnRecipeType(){return profile().recipes==null?RecipeContent.PULVERIZER:profile().recipes;}
    @Override public MenuType<?> getScreenHandlerType(){return Content.MENU.get();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new ProcessorMenu(id,inv,this);}
    @Override public void saveExtraData(net.minecraft.network.FriendlyByteBuf buf){sendUpdate(SyncType.GUI_OPEN);buf.writeBlockPos(worldPosition);buf.writeVarInt(profileIndex);buf.writeVarInt(modules);buf.writeBoolean(fluidAddon);buf.writeVarInt(tier);}
    @Override public Component getDisplayName(){return Component.translatable("block.oritechmekanism.universal_processor");}
    @Override public boolean inputOptionsEnabled(){return false;}
    @Override public boolean canEnergyStorageChangeWhileGUIOpen(){return true;}
    @Override public long getDefaultCapacity(){return 1_000_000;}
    @Override public long getDefaultInsertRate(){return 32768;}
    @Override public float getProgress(){return profile()==Profiles.ATOMIC?(usage<=0?0:(float)((double)paid/usage)):(float)progress/Math.max(1,duration);}
    @Override public float getDisplayedEnergyUsage(){return rate;}
    @Override public int getRecipeDuration(){return duration;}
    @Override public BarConfiguration getEnergyConfiguration(){return profile()==Profiles.EMPTY?super.getEnergyConfiguration():profile().metadata().getEnergyConfiguration();}
    @Override public ArrowConfiguration getIndicatorConfiguration(){return profile()==Profiles.EMPTY?super.getIndicatorConfiguration():profile().metadata().getIndicatorConfiguration();}
    @Override public boolean showProgress(){return profile()!=Profiles.EMPTY;}
    @Override public boolean showEnergy(){return true;}
    @Override public boolean supportRecoloring(){return profile()!=Profiles.EMPTY&&profile().metadata().supportRecoloring();}
    @Override public ColorVariant getCurrentColor(){return profile()==Profiles.EMPTY?super.getCurrentColor():profile().metadata().getCurrentColor();}
    @Override public void updateEnergyContainer(){ // Removing a capacitor never deletes already stored power.
        var data=getBaseAddonData();energyStorage.capacity=Math.max(1,getDefaultCapacity()+data.energyBonusCapacity());
        energyStorage.maxInsert=Math.max(1,getDefaultInsertRate()+data.energyBonusTransfer());
    }
    @Override public void initAddons(BlockPos ignored){refresh=true;}
    public static boolean validAddon(ItemStack stack){
        if(!(stack.getItem() instanceof BlockItem item))return false;
        if(item.getBlock()==BlockContent.REFINERY_MODULE_BLOCK)return true;
        if(item.getBlock() instanceof rearth.oritech.block.blocks.addons.CombiAddonBlock)return stack.has(ComponentContent.ADDON_DATA.get());
        if(!(item.getBlock() instanceof MachineAddonBlock block))return false;
        var a=block.getAddonSettings();
        return !a.extender()&&(a.speedMultiplier()!=1||a.efficiencyMultiplier()!=1||a.addedCapacity()!=0||a.addedInsert()!=0||a.chamberCount()!=0||a.burstTicks()!=0
            ||block==BlockContent.MACHINE_FLUID_ADDON||block==BlockContent.MACHINE_YIELD_ADDON);
    }
    public boolean canInstallAddon(ItemStack stack,int slot){if(slot<ADDON_START||slot>=ADDON_START+addonSlots()||!validAddon(stack))return false;
        if(stack.getItem() instanceof BlockItem module&&module.getBlock()==BlockContent.REFINERY_MODULE_BLOCK){int count=0;for(int i=ADDON_START;i<ADDON_END;i++)if(i!=slot&&inventory.getItem(i).is(stack.getItem()))count+=inventory.getItem(i).getCount();return count<2;}
        if(profile()==Profiles.ATOMIC&&chambers(stack)>0)return false;
        boolean combined=stack.getItem() instanceof BlockItem b&&b.getBlock() instanceof rearth.oritech.block.blocks.addons.CombiAddonBlock;
        for(int i=ADDON_START;i<ADDON_END;i++)if(i!=slot&&!inventory.getItem(i).isEmpty()&&inventory.getItem(i).getItem() instanceof BlockItem b){
            if(b.getBlock()==BlockContent.REFINERY_MODULE_BLOCK)continue;
            if(combined||b.getBlock() instanceof rearth.oritech.block.blocks.addons.CombiAddonBlock)return false;
        }return true;
    }
    private static int chambers(ItemStack stack){var data=stack.get(ComponentContent.ADDON_DATA.get());if(data!=null)return data.data().extraChambers();return stack.getItem() instanceof BlockItem b&&b.getBlock() instanceof MachineAddonBlock a?a.getAddonSettings().chamberCount():0;}
    public boolean canInstallHost(ItemStack stack){if(!canChangeHost()||Profiles.of(stack)==Profiles.EMPTY)return false;if(Profiles.of(stack)==Profiles.ATOMIC)for(int i=ADDON_START;i<ADDON_END;i++)if(chambers(inventory.getItem(i))>0)return false;return true;}
    public boolean canChangeHost(){for(int i=0;i<8;i++)if(!inventory.getItem(i).isEmpty())return false;return tanks().stream().allMatch(t->t.getAmount()==0);}
    public boolean canRemoveAddon(int slot){var stack=inventory.getItem(slot);if(!(stack.getItem() instanceof BlockItem b))return true;
        if(hasFluidFeature(stack)&&!anotherFluidAddon(slot))return fluidIn.getAmount()==0&&fluidA.getAmount()==0;
        if(b.getBlock()==BlockContent.REFINERY_MODULE_BLOCK)return fluidB.getAmount()==0&&fluidC.getAmount()==0;
        return true;
    }
    private static boolean hasFluidFeature(ItemStack stack){var data=stack.get(ComponentContent.ADDON_DATA.get());return stack.getItem() instanceof BlockItem b&&(b.getBlock()==BlockContent.MACHINE_FLUID_ADDON||b.getBlock() instanceof rearth.oritech.block.blocks.addons.CombiAddonBlock&&data!=null&&data.fluid());}
    private boolean anotherFluidAddon(int excluded){for(int i=ADDON_START;i<ADDON_END;i++)if(i!=excluded&&hasFluidFeature(inventory.getItem(i)))return true;return false;}
    public String loadStatus(){var stack=inventory.getItem(LOAD_SLOT);if(stack.isEmpty())return "";
        if(countAddons()>=addonSlots())return "capacity_full";
        if(!validAddon(stack))return "load_unsupported";
        if(profile()==Profiles.ATOMIC&&chambers(stack)>0)return "load_atomic";
        if(stack.getItem() instanceof BlockItem b&&b.getBlock()==BlockContent.REFINERY_MODULE_BLOCK&&modules>=2)return "load_modules";
        for(int i=ADDON_START;i<ADDON_START+addonSlots();i++)if(inventory.getItem(i).isEmpty()&&canInstallAddon(stack,i))return "load_pending";
        return "load_combined";
    }
    public void loadAddons(){
        if(level==null||level.isClientSide)return;var stack=inventory.getItem(LOAD_SLOT);boolean changed=false;
        if(validAddon(stack))for(int i=ADDON_START;i<ADDON_START+addonSlots()&&!stack.isEmpty();i++){
            if(!inventory.getItem(i).isEmpty()||!canInstallAddon(stack,i))continue;
            inventory.setItem(i,stack.copyWithCount(1));stack.shrink(1);changed=true;
        }
        if(changed){setChanged();refreshEquipment();}
    }
    /** Move real installed items into the persistent retrieval slot, preserving every component. */
    public int unloadAddon(ItemStack requested,boolean all){
        if(level==null||level.isClientSide||requested.isEmpty())return 0;int moved=0;
        for(int i=ADDON_START;i<ADDON_START+addonSlots();i++){
            var installed=inventory.getItem(i);if(installed.isEmpty()||!ItemStack.isSameItemSameComponents(installed,requested)||!canRemoveAddon(i))continue;
            var out=inventory.getItem(UNLOAD_SLOT);if(!out.isEmpty()&&!ItemStack.isSameItemSameComponents(out,installed))break;
            if(out.getCount()>=Math.min(64,installed.getMaxStackSize()))break;
            inventory.setItem(UNLOAD_SLOT,installed.copyWithCount(out.getCount()+1));installed.shrink(1);moved++;
            if(!all)break;
        }
        if(moved>0){setChanged();refreshEquipment();}return moved;
    }
    private List<ItemStack> equipmentSnapshot(){var result=new ArrayList<ItemStack>(ADDON_END-HOST);for(int i=HOST;i<ADDON_END;i++)result.add(inventory.getItem(i).copy());return result;}
    private boolean equipmentChanged(){if(equipment.size()!=ADDON_END-HOST)return true;for(int i=HOST;i<ADDON_END;i++)if(!ItemStack.matches(equipment.get(i-HOST),inventory.getItem(i)))return true;return false;}
    public void refreshEquipment(){
        boolean changed=equipmentChanged();if(!refresh&&!changed&&facing()==deployedFacing)return;
        if(changed)cancelWork();
        equipment=equipmentSnapshot();refresh=false;
        addonCount=countAddons();
        var newProfile=Profiles.of(inventory.getItem(HOST));
        int oldModules=modules;var oldProfile=profile();profileIndex=newProfile.ordinal();
        modules=0;fluidAddon=false;yieldAddon=false;
        var addons=new ArrayList<MachineAddonController.AddonBlock>();
        for(int i=ADDON_START;i<ADDON_START+addonSlots();i++)if(inventory.getItem(i).getItem() instanceof BlockItem item){
            var block=item.getBlock();
            if(block==BlockContent.REFINERY_MODULE_BLOCK){modules=Math.min(2,modules+1);continue;}
            if(validAddon(inventory.getItem(i))&&block instanceof MachineAddonBlock addon){
                rearth.oritech.block.entity.addons.CombiAddonEntity combined=null;
                if(block instanceof rearth.oritech.block.blocks.addons.CombiAddonBlock){combined=new rearth.oritech.block.entity.addons.CombiAddonEntity(worldPosition,block.defaultBlockState());combined.storedData=inventory.getItem(i).get(ComponentContent.ADDON_DATA.get());fluidAddon|=combined.hasFluid();yieldAddon|=combined.getYieldCount()>0;}
                addons.add(new MachineAddonController.AddonBlock(addon,addon.defaultBlockState(),worldPosition,combined));
                fluidAddon|=block==BlockContent.MACHINE_FLUID_ADDON;yieldAddon|=block==BlockContent.MACHINE_YIELD_ADDON;
            }
        }
        gatherAddonStats(addons);updateEnergyContainer();
        if(oldProfile!=newProfile||modules!=oldModules||facing()!=deployedFacing){removeParts();deployedFacing=facing();deployedAt=level.getGameTime();}
        if(newProfile!=Profiles.EMPTY)energyPerTick=newProfile.metadata().getEnergyPerTick();
        if(changed){setChanged();syncWorld();sendUpdate(SyncType.GUI_OPEN);}
    }
    public List<BlockPos> positions(){return profile().cores(modules).stream().map(v->worldPosition.offset(Geometry.rotatePosition(v,facing()))).toList();}
    public boolean owns(BlockPos pos){return occupied.contains(pos);}
    public boolean complete(){if(level==null||isRemoved()||profile()==Profiles.EMPTY||Profiles.of(inventory.getItem(HOST))!=profile()||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this)return false;
        if(occupied.size()!=positions().size())return false;
        for(var pos:occupied)if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof Part part)||!identity.equals(part.owner)||!part.anchor.equals(worldPosition))return false;
        return true;
    }
    private boolean deploy(){
        if(complete())return true;
        var cells=positions();
        for(var pos:cells){if(!level.hasChunkAt(pos))return false;
            if(level.getBlockEntity(pos) instanceof Part part&&identity.equals(part.owner)&&part.anchor.equals(worldPosition))continue;
            if(!level.getBlockState(pos).isAir()||!level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(pos),e->!e.isSpectator()).isEmpty())return false;
        }
        removingParts=true;
        try{occupied.clear();occupied.addAll(cells);for(var pos:cells){if(level.getBlockEntity(pos) instanceof Part part&&identity.equals(part.owner))continue;
            level.setBlock(pos,Content.PART.get().defaultBlockState(),3);if(level.getBlockEntity(pos) instanceof Part part)part.link(this);
        }}finally{removingParts=false;}
        if(!complete())return false;
        deployedAt=level.getGameTime();level.setBlock(worldPosition,getBlockState().setValue(ProcessorBlock.DEPLOYED,true),3);syncWorld();
        if(profile()!=Profiles.PULVERIZER)triggerAnim("machine","setup");
        setChanged();return true;
    }
    public void removeParts(){if(level==null||level.isClientSide)return;removingParts=true;try{
        for(var p:List.copyOf(occupied))if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof Part part&&identity.equals(part.owner)&&part.anchor.equals(worldPosition))level.removeBlock(p,false);
        occupied.clear();if(level.getBlockState(worldPosition).is(Content.BLOCK.get()))level.setBlock(worldPosition,getBlockState().setValue(ProcessorBlock.DEPLOYED,false),3);
    }finally{removingParts=false;}}
    @Override public boolean isActive(BlockState state){return profile()!=Profiles.EMPTY&&state.getValue(ProcessorBlock.DEPLOYED);}
    @Override public boolean isActivelyWorking(){return level!=null&&lastWorkedAt>0&&level.getGameTime()-lastWorkedAt<5;}
    @Override public void serverTick(Level world,BlockPos pos,BlockState state,NetworkedBlockEntity entity){
        loadAddons();
        refreshEquipment();
        if(profile()==Profiles.EMPTY){status=0;return;}
        if(!deploy()){status=1;return;}
        if(eject&&world.getGameTime()%5==0)Ports.eject(this);
        var a=getBaseAddonData();
        if(!Float.isFinite(a.speed())||a.speed()<=0||!Float.isFinite(a.efficiency())||a.efficiency()<=0){status=6;return;}
        job=Engine.find(this);
        if(job==null){status=2;cancelWork();addBurstTicks();return;}
        var sig=job.signature(this);if(!sig.equals(signature)){cancelWork();signature=sig;}
        duration=job.duration();rate=job.perTick();usage=profile()==Profiles.ATOMIC?job.budget():job.perTick();
        if(!Engine.canOutput(this,job)){status=3;addBurstTicks();return;}
        if(world.hasNeighborSignal(pos)){status=5;addBurstTicks();return;}
        long charge=profile()==Profiles.ATOMIC?Math.min(job.perTick(),Math.max(0,job.budget()-paid)):job.perTick();
        if(energyStorage.amount<charge){status=4;addBurstTicks();return;}
        energyStorage.amount-=charge;lastWorkedAt=world.getGameTime();status=7;
        if(profile()==Profiles.ATOMIC)paid+=charge;else progress++;
        if((profile()==Profiles.ATOMIC&&paid>=job.budget())||(profile()!=Profiles.ATOMIC&&progress>=duration)){
            int crafts=profile()==Profiles.ATOMIC?1:1+Math.clamp(a.extraChambers(),0,MAX_ADDONS*9);
            for(int i=0;i<crafts;i++){if(!Engine.matches(this,job)||!Engine.canOutput(this,job))break;Engine.finish(this,job);}
            progress=0;paid=0;signature=new CompoundTag();
        }
        consumeBurstTicks();addBurstTicks();setChanged();
    }
    void cancelWork(){if(paid>0){energyStorage.amount=energyStorage.amount>Long.MAX_VALUE-paid?Long.MAX_VALUE:energyStorage.amount+paid;paid=0;}progress=0;signature=new CompoundTag();}
    @Override protected float getAnimationSpeed(){return animationController!=null&&animationController.isPlayingTriggeredAnimation()?1:isActivelyWorking()?(float)Math.clamp((profile()==Profiles.CENTRIFUGE?180.0:60.0)/Math.max(1,duration),.25,4):1;}
    private AnimationController<Processor> newController(){animationController=new AnimationController<>(this,"machine",0,this::animate).triggerableAnim("setup",SETUP)
        .setAnimationSpeedHandler(p->(double)getAnimationSpeed()).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>(this::getAnimationSpeed));return animationController;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(newController());}
    private PlayState animate(AnimationState<Processor> s){if(profile()==Profiles.EMPTY)return PlayState.STOP;if(s.getController().isPlayingTriggeredAnimation())return PlayState.CONTINUE;
        return s.setAndContinue(isActive(getBlockState())?(isActivelyWorking()?WORKING:IDLE):(profile()==Profiles.PULVERIZER?IDLE:PACKAGED));}
    public boolean fluidEnabled(){return profile()==Profiles.REFINERY||profile()==Profiles.COOLER||(profile()==Profiles.CENTRIFUGE&&fluidAddon);}
    public int outputTanks(){return profile()==Profiles.REFINERY?1+modules:profile()==Profiles.CENTRIFUGE&&fluidAddon?1:0;}
    void syncWorld(){if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=super.getUpdateTag(r);t.putInt("profile",profileIndex);t.putInt("modules",modules);t.putBoolean("fluidAddon",fluidAddon);t.putLong("deployedAt",deployedAt);t.putInt("tier",tier);return t;}
    @Override public void handleUpdateTag(CompoundTag t,HolderLookup.Provider r){int previous=profileIndex;profileIndex=Profiles.index(t.getInt("profile")).ordinal();modules=Math.clamp(t.getInt("modules"),0,2);fluidAddon=t.getBoolean("fluidAddon");deployedAt=t.getLong("deployedAt");tier=Math.clamp(t.getInt("tier"),1,MAX_TIER);
        if(previous!=profileIndex&&animationController!=null){var manager=animatableInstanceCache.getManagerForId(0);manager.removeController("machine");manager.addController(newController());}}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registry){
        // This packet contains only render metadata. NeoForge's default handler loads a complete save,
        // which would clear inventory and read the absent addon multipliers as zero.
        if(!packet.getTag().isEmpty())handleUpdateTag(packet.getTag(),registry);
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);writeState(t,r);t.putUUID("identity",identity);t.putLongArray("occupied",occupied.stream().mapToLong(BlockPos::asLong).toArray());t.putInt("deployedFacing",deployedFacing.get2DDataValue());}
    private void writeState(CompoundTag t,HolderLookup.Provider r){t.putInt("profile",profileIndex);t.putInt("modules",modules);t.putLong("paid",paid);t.put("signature",signature.copy());t.putBoolean("eject",eject);t.putInt("tier",tier);t.putInt("burst",remainingBurstTicks);
        for(int i=0;i<4;i++)tanks().get(i).writeNbt(t,"tank"+i);
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){
        // Native input-mode loading does not clamp malformed ordinals.
        t=t.copy();t.putShort("oritech.machine_input_mode",(short)0);super.loadAdditional(t,r);readState(t);
        identity=t.hasUUID("identity")?t.getUUID("identity"):UUID.randomUUID();occupied.clear();for(long p:t.getLongArray("occupied"))if(occupied.size()<64)occupied.add(BlockPos.of(p));
        deployedFacing=Direction.from2DDataValue(Math.clamp(t.getInt("deployedFacing"),0,3));equipment=equipmentSnapshot();refresh=true;
    }
    private void readState(CompoundTag t){profileIndex=Profiles.index(t.getInt("profile")).ordinal();modules=Math.clamp(t.getInt("modules"),0,2);paid=Math.max(0,t.getLong("paid"));signature=t.getCompound("signature").copy();
        eject=!t.contains("eject")||t.getBoolean("eject");tier=Math.clamp(t.getInt("tier"),1,MAX_TIER);
        // Slots 0-17 keep their old meaning. Additional slots are appended, never reindexed.
        for(int i=ADDON_START;i<ADDON_END;i++)if(!inventory.getItem(i).isEmpty())tier=Math.max(tier,(i-ADDON_START)/ADDONS_PER_TIER+1);
        addonCount=countAddons();
        for(int i=0;i<4;i++){if(t.contains("fluidtank"+i))tanks().get(i).readNbt(t,"tank"+i);if(tanks().get(i).getAmount()<0)tanks().get(i).setAmount(0);}
        progress=Math.max(0,progress);energyStorage.amount=Math.max(0,energyStorage.amount);remainingBurstTicks=Math.clamp(t.getInt("burst"),-1_000_000,1_000_000);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);var t=new CompoundTag();super.saveAdditional(t,level.registryAccess());writeState(t,level.registryAccess());b.set(Content.DATA.get(),t);}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input){super.applyImplicitComponents(input);var t=input.get(Content.DATA.get());if(t!=null){loadAdditional(t,level.registryAccess());occupied.clear();identity=UUID.randomUUID();}}
}
