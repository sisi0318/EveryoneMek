package dev.everyonemek.oritech.collider;

import java.util.*;
import dev.everyonemek.oritech.Content;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import mekanism.api.RelativeSide;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.*;

public final class Collider extends BlockEntity implements ExtendedMenuProvider {
    public static final int INPUT_A=0,INPUT_B=1,OUTPUT=2,PART_IN=3,PART_OUT=4,WORK_A=5,WORK_B=6,MAGNET_SLOT=7,MAGNET_ADDONS=8,STRAIGHT_PARTS=13,SLOTS=14;
    public static final long CAPACITY=20_000_000,RECEIVE=1_000_000;
    public static final int CLOSED=0,FEED_A=1,FEED_B=2,FEED_BOTH=3,EJECT=4;
    public final SimpleContainer inventory=new SimpleContainer(SLOTS){@Override public void setChanged(){super.setChanged();Collider.this.setChanged();}};
    public record Placed(Track.Node node,ItemStack item){}
    public final Map<Integer,Placed> parts=new TreeMap<>();
    public int emitterA=Track.cell(26,32),emitterB=Track.cell(38,32),directionA=0,directionB=4;
    public final int[] sides={EJECT,FEED_A,FEED_B,FEED_BOTH,FEED_BOTH,EJECT};
    public boolean enabled,eject=true,lockedSwapped;
    public ResourceLocation lockedRecipe;
    public long energy,spent,magnetNeeded,magneticSpent;
    public int revision,status,problem=-1;
    public static final int IDLE=0,NEED_A=1,NEED_B=2,NO_RECIPE=3,OUTPUT_FULL=4,NO_POWER=5,ACCELERATING=6,COLLISION=7,PAUSED=8,RETURN_SPACE=9,RECIPE_CHANGED=10,MAGNET_CHARGING=11,TRACK_BASE=20;
    public Track.Beam beam;
    public ResourceLocation taskRecipe;
    public ItemStack taskResult=ItemStack.EMPTY;
    public long requiredSpeed;
    public int collisionTicks;
    public boolean returning;
    private Track.Plan cachedPlan;
    private Track.Rules cachedRules;
    private Track.Rules taskRules;
    private MagnetSupport.Settings taskMagnet=MagnetSupport.Settings.NONE,magnetSettings=MagnetSupport.Settings.NONE;
    private List<Object> magnetKey=List.of();
    private int plannedRevision=-1;

    public Collider(BlockPos pos,BlockState state){super(Content.COLLIDER_TILE.get(),pos,state);}
    public Direction facing(){return getBlockState().getValue(ColliderBlock.FACING);}
    public boolean busy(){return taskRecipe!=null||!inventory.getItem(WORK_A).isEmpty()||!inventory.getItem(WORK_B).isEmpty();}
    public boolean editable(){return !enabled&&!busy();}
    public int mode(Direction worldSide){return worldSide==null?CLOSED:sides[RelativeSide.fromDirections(facing(),worldSide).ordinal()];}
    public MagnetSupport.Settings magnet(){var addons=new ArrayList<ItemStack>();for(int i=MAGNET_ADDONS;i<MAGNET_ADDONS+5;i++)addons.add(inventory.getItem(i));var key=MagnetSupport.key(inventory.getItem(MAGNET_SLOT),addons);
        if(!key.equals(magnetKey)){magnetKey=key;magnetSettings=MagnetSupport.settings(inventory.getItem(MAGNET_SLOT),addons);}return magnetSettings;}
    public long magneticEnergy(){return MagnetSupport.energy(inventory.getItem(MAGNET_SLOT));}
    private void chargeMagnet(){var settings=magnet();if(settings.capacity()<=0)return;long charge=Math.min(energy,Math.min(settings.insert(),Math.max(0,settings.capacity()-magneticEnergy())));
        if(charge>0){energy-=charge;MagnetSupport.energy(inventory.getItem(MAGNET_SLOT),magneticEnergy()+charge);setChanged();}}
    public Map<Integer,Track.Node> nodes(){var nodes=new TreeMap<Integer,Track.Node>();parts.forEach((pos,part)->nodes.put(pos,part.node()));nodes.put(emitterA,new Track.Node(Track.A,directionA,0));nodes.put(emitterB,new Track.Node(Track.B,directionB,0));return nodes;}
    public static int partKind(ItemStack stack){
        if(stack.is(BlockContent.ACCELERATOR_RING.asItem()))return Track.RING;
        if(stack.is(BlockContent.ACCELERATOR_MOTOR.asItem()))return Track.MOTOR;
        if(stack.is(BlockContent.ACCELERATOR_SENSOR.asItem()))return Track.SENSOR;
        return 0;
    }
    public static Track.Rules rules(){return new Track.Rules(OritechConfig.maxGateDist.get(),OritechConfig.bendFactor.get(),OritechConfig.accelerationRFCost.get());}
    public Track.Plan plan(){var rules=rules();if(cachedPlan==null||plannedRevision!=revision||!rules.equals(cachedRules)){cachedPlan=Track.plan(nodes(),rules);plannedRevision=revision;cachedRules=rules;}return cachedPlan;}
    public void layoutChanged(){revision++;cachedPlan=null;problem=-1;status=IDLE;setChanged();}

    /** Edits move real components; the two recipe-funded emitters are only moved/rotated. */
    public boolean edit(int operation,int pos,int direction,int bend){
        return edit(operation,pos,direction,bend,PART_IN);
    }
    public static boolean partSource(int slot){return slot==PART_IN||slot==STRAIGHT_PARTS;}
    public int partCount(int kind){int count=0;for(int slot:new int[]{PART_IN,STRAIGHT_PARTS})if(partKind(inventory.getItem(slot))==kind)count+=inventory.getItem(slot).getCount();return count;}
    public boolean edit(int operation,int pos,int direction,int bend,int source){
        if(!editable()||!partSource(source)||!Track.inBounds(pos)||direction<0||direction>7||bend<0||bend>2)return false;
        if(operation==2||operation==3){
            if(parts.containsKey(pos)||(operation==2?pos==emitterB:pos==emitterA))return false;
            if(operation==2){emitterA=pos;directionA=direction;}else {emitterB=pos;directionB=direction;}
        }else{
            if(pos==emitterA||pos==emitterB)return false;
            var current=parts.get(pos);
            if(operation==1){
                if(current==null||!canMerge(PART_OUT,current.item()))return false;
                merge(PART_OUT,current.item());parts.remove(pos);
            }else if(operation==0){
                if(current!=null){parts.put(pos,new Placed(new Track.Node(current.node().kind(),direction,current.node().kind()==Track.RING?bend:0),current.item()));}
                else {var input=inventory.getItem(source);int kind=partKind(input);if(kind==0)return false;parts.put(pos,new Placed(new Track.Node(kind,direction,kind==Track.RING?bend:0),input.split(1)));}
            }else return false;
        }
        layoutChanged();return true;
    }
    public SmartTrack.Result smartPaint(int[] cells,int fallback){
        if(!editable())return new SmartTrack.Result(Map.of(),0,SmartTrack.Failure.BAD_PATH,-1);
        var originalNodes=nodes();var result=SmartTrack.mixed(originalNodes,cells,fallback,rules().maxGap());if(!result.valid())return result;
        for(int kind:new int[]{Track.RING,Track.MOTOR})if(result.needed(originalNodes,kind)>partCount(kind))return editFailure(result,SmartTrack.Failure.NOT_ENOUGH_PARTS);
        // Work on copies, including replacement refunds, before committing either inventory or layout.
        var guides=inventory.getItem(PART_IN).copy();var straight=inventory.getItem(STRAIGHT_PARTS).copy();var returned=inventory.getItem(PART_OUT).copy();
        var recycled=new ArrayList<ItemStack>();
        for(var entry:result.nodes().entrySet()){var old=parts.get(entry.getKey());if(old!=null&&old.node().kind()!=entry.getValue().kind())recycled.add(old.item().copy());}
        var pool=new ArrayList<>(recycled);pool.add(guides);pool.add(straight);var replacements=new TreeMap<Integer,Placed>();
        for(var entry:result.nodes().entrySet()){
            int pos=entry.getKey();if(pos==emitterA||pos==emitterB)continue;
            var original=parts.get(pos);ItemStack item;
            if(original!=null&&original.node().kind()==entry.getValue().kind())item=original.item();
            else {item=ItemStack.EMPTY;for(var available:pool)if(!available.isEmpty()&&partKind(available)==entry.getValue().kind()){item=available.split(1);break;}
                if(item.isEmpty())return editFailure(result,SmartTrack.Failure.NOT_ENOUGH_PARTS);}
            replacements.put(pos,new Placed(entry.getValue(),item));
        }
        for(var refund:recycled)if(!refund.isEmpty()){
            if(partKind(refund)==Track.RING)guides=mergeCopy(guides,refund);else straight=mergeCopy(straight,refund);
            if(!refund.isEmpty())returned=mergeCopy(returned,refund);
            if(!refund.isEmpty())return editFailure(result,SmartTrack.Failure.RETURN_FULL);
        }
        inventory.setItem(PART_IN,guides);inventory.setItem(STRAIGHT_PARTS,straight);inventory.setItem(PART_OUT,returned);parts.putAll(replacements);layoutChanged();return result;
    }
    private static SmartTrack.Result editFailure(SmartTrack.Result result,SmartTrack.Failure reason){return new SmartTrack.Result(result.nodes(),result.required(),reason,-1);}
    private static ItemStack mergeCopy(ItemStack destination,ItemStack source){if(source.isEmpty()||!destination.isEmpty()&&!ItemStack.isSameItemSameComponents(destination,source))return destination;
        int moved=Math.min(source.getCount(),Math.min(64,source.getMaxStackSize())-destination.getCount());if(moved<=0)return destination;
        var result=source.copyWithCount(destination.getCount()+moved);source.shrink(moved);return result;}
    public List<RecipeHolder<OritechRecipe>> recipes(){if(level==null)return List.of();return level.getRecipeManager().getAllRecipesFor(RecipeContent.PARTICLE_COLLISION).stream()
        .filter(r->r.value().getInputs().size()==2&&r.value().getResults().size()==1&&!r.value().getResults().getFirst().isEmpty()&&r.value().getTime()>0)
        .sorted(Comparator.comparing(r->r.id().toString())).toList();}
    private Optional<RecipeHolder<OritechRecipe>> recipe(ResourceLocation id){return recipes().stream().filter(r->r.id().equals(id)).findFirst();}
    public boolean accepts(int slot,ItemStack stack){
        if(stack.isEmpty()||slot<INPUT_A||slot>INPUT_B)return false;
        if(lockedRecipe!=null)return recipe(lockedRecipe).map(r->r.value().getInputs().get(lockedSwapped?1-slot:slot).test(stack)).orElse(false);
        var other=inventory.getItem(1-slot);
        return recipes().stream().anyMatch(r->{var in=r.value().getInputs();return other.isEmpty()?in.stream().anyMatch(i->i.test(stack)):in.get(0).test(stack)&&in.get(1).test(other)||in.get(1).test(stack)&&in.get(0).test(other);});
    }
    public boolean sameInputs(ItemStack stack){return recipes().stream().filter(r->lockedRecipe==null||lockedRecipe.equals(r.id())).anyMatch(r->r.value().getInputs().get(0).test(stack)&&r.value().getInputs().get(1).test(stack));}
    public boolean selectRecipe(ResourceLocation id){if(busy())return false;if(id!=null&&recipe(id).isEmpty())return false;lockedRecipe=id;lockedSwapped=false;setChanged();return true;}
    public boolean lockDetected(){if(busy())return false;var match=match();if(match==null)return false;lockedRecipe=match.recipe().id();lockedSwapped=match.swapped();setChanged();return true;}
    private record Match(RecipeHolder<OritechRecipe> recipe,boolean swapped){}
    private Match match(){var a=inventory.getItem(INPUT_A);var b=inventory.getItem(INPUT_B);
        for(var holder:recipes()){
            if(lockedRecipe!=null&&!lockedRecipe.equals(holder.id()))continue;var inputs=holder.value().getInputs();
            if(lockedRecipe!=null){if(inputs.get(lockedSwapped?1:0).test(a)&&inputs.get(lockedSwapped?0:1).test(b))return new Match(holder,lockedSwapped);continue;}
            if(inputs.get(0).test(a)&&inputs.get(1).test(b))return new Match(holder,false);
            if(lockedRecipe==null&&inputs.get(1).test(a)&&inputs.get(0).test(b))return new Match(holder,true);
        }return null;
    }
    public ResourceLocation detectedRecipe(){var match=match();return match==null?null:match.recipe().id();}
    public void cancel(){if(!busy())return;enabled=false;returning=true;setChanged();}
    private boolean canMerge(int slot,ItemStack stack){var current=inventory.getItem(slot);return stack.isEmpty()||(current.isEmpty()||ItemStack.isSameItemSameComponents(current,stack))&&current.getCount()+stack.getCount()<=Math.min(64,stack.getMaxStackSize());}
    private void merge(int slot,ItemStack stack){if(stack.isEmpty())return;var old=inventory.getItem(slot);inventory.setItem(slot,stack.copyWithCount(old.getCount()+stack.getCount()));}
    private void clearTask(){taskRecipe=null;taskResult=ItemStack.EMPTY;requiredSpeed=0;beam=null;collisionTicks=0;returning=false;spent=0;magneticSpent=0;magnetNeeded=0;taskRules=null;taskMagnet=MagnetSupport.Settings.NONE;setChanged();}
    private boolean restoreInputs(){
        for(int i=0;i<2;i++){var work=inventory.getItem(WORK_A+i);if(!work.isEmpty()&&canMerge(i,work)){merge(i,work);inventory.setItem(WORK_A+i,ItemStack.EMPTY);}}
        if(!inventory.getItem(WORK_A).isEmpty()||!inventory.getItem(WORK_B).isEmpty()){status=RETURN_SPACE;return false;}clearTask();status=IDLE;return true;
    }
    public void tick(){if(level==null||level.isClientSide)return;
        chargeMagnet();
        if(eject&&level.getGameTime()%5==0)ColliderPorts.eject(this);
        if(returning){restoreInputs();return;}
        if(!enabled||level.hasNeighborSignal(worldPosition)){if(status<TRACK_BASE&&status!=RECIPE_CHANGED)status=busy()?PAUSED:IDLE;return;}
        if(taskRecipe==null){
            if(inventory.getItem(INPUT_A).isEmpty()){status=NEED_A;return;}
            if(inventory.getItem(INPUT_B).isEmpty()){status=NEED_B;return;}
            var match=match();if(match==null){status=NO_RECIPE;return;}
            var result=match.recipe().value().getResults().getFirst();if(!canMerge(OUTPUT,result)){status=OUTPUT_FULL;return;}
            var plan=plan();if(!plan.valid()){status=TRACK_BASE+plan.fault().ordinal();problem=plan.problem();return;}
            var validation=Track.preflight(plan,rules(),match.recipe().value().getTime(),magnet().physics());if(!validation.valid()){status=TRACK_BASE+validation.fault().ordinal();problem=validation.problem();magnetNeeded=validation.magnetNeeded();enabled=false;setChanged();return;}
            inventory.setItem(WORK_A,inventory.getItem(INPUT_A).split(1));inventory.setItem(WORK_B,inventory.getItem(INPUT_B).split(1));
            taskRecipe=match.recipe().id();taskResult=result.copy();requiredSpeed=match.recipe().value().getTime();taskRules=rules();taskMagnet=magnet();beam=new Track.Beam();spent=0;magneticSpent=0;magnetNeeded=0;setChanged();
        }
        if(!taskStillValid()){status=RECIPE_CHANGED;enabled=false;setChanged();return;}
        if(collisionTicks>0){
            status=COLLISION;collisionTicks=Math.min(5,collisionTicks+1);if(collisionTicks<5){setChanged();return;}
            if(!canMerge(OUTPUT,taskResult)){status=OUTPUT_FULL;return;}
            merge(OUTPUT,taskResult);inventory.setItem(WORK_A,ItemStack.EMPTY);inventory.setItem(WORK_B,ItemStack.EMPTY);clearTask();return;
        }
        var plan=plan();var result=Track.advance(plan,beam,rules(),energy,requiredSpeed,magnet().physics(),magneticEnergy());energy-=result.spent();
        if(result.magneticSpent()>0)MagnetSupport.energy(inventory.getItem(MAGNET_SLOT),magneticEnergy()-result.magneticSpent());magnetNeeded=result.magnetNeeded();
        magneticSpent=safeAdd(magneticSpent,result.magneticSpent());spent=safeAdd(spent,safeAdd(result.spent(),result.magneticSpent()));
        problem=result.problem();
        if(result.fault()!=Track.Fault.NONE){status=TRACK_BASE+result.fault().ordinal();enabled=false;}
        else if(result.collision()){collisionTicks=1;status=COLLISION;}
        else status=result.needsMagnet()?MAGNET_CHARGING:result.needsPower()?NO_POWER:ACCELERATING;
        setChanged();
    }
    private boolean taskStillValid(){var found=recipe(taskRecipe);if(found.isEmpty()||taskRules==null||!taskRules.equals(rules())||!taskMagnet.equals(magnet())||found.get().value().getTime()!=requiredSpeed||!ItemStack.matches(found.get().value().getResults().getFirst(),taskResult))return false;
        var in=found.get().value().getInputs();var a=inventory.getItem(WORK_A);var b=inventory.getItem(WORK_B);
        return a.getCount()==1&&b.getCount()==1&&(in.get(0).test(a)&&in.get(1).test(b)||in.get(1).test(a)&&in.get(0).test(b));
    }
    @Override public Component getDisplayName(){return Component.translatable("block.oritechmekanism.mini_particle_collider");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new ColliderMenu(id,inv,this);}
    @Override public void saveExtraData(FriendlyByteBuf buf){buf.writeBlockPos(worldPosition);}

    public CompoundTag view(boolean layout){var tag=new CompoundTag();tag.putInt("revision",revision);tag.putInt("status",status);tag.putInt("problem",problem);tag.putLong("energy",energy);tag.putLong("spent",spent);
        tag.putInt("maxGap",rules().maxGap());
        tag.put("magnet",magnet().save());tag.putLong("magneticEnergy",magneticEnergy());tag.putLong("magnetNeeded",magnetNeeded);tag.putLong("magneticSpent",magneticSpent);
        tag.putBoolean("enabled",enabled);tag.putBoolean("busy",busy());tag.putBoolean("eject",eject);tag.putLong("required",requiredSpeed);tag.putIntArray("sides",sides);
        if(lockedRecipe!=null)tag.putString("locked",lockedRecipe.toString());tag.putBoolean("swapped",lockedSwapped);var found=taskRecipe!=null?taskRecipe:lockedRecipe!=null?lockedRecipe:detectedRecipe();if(found!=null)tag.putString("recipe",found.toString());
        if(beam!=null&&beam.valid(plan())){tag.putDouble("x",beam.x(plan()));tag.putDouble("y",beam.y(plan()));tag.putLong("speed",beam.speed);}
        if(layout)tag.putIntArray("layout",nodes().entrySet().stream().mapToInt(e->Track.pack(e.getKey(),e.getValue())).toArray());return tag;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);write(tag,lookup);}
    private void write(CompoundTag tag,HolderLookup.Provider lookup){
        tag.putInt("partsVersion",1);
        var items=new ListTag();for(int i=0;i<SLOTS;i++){var stack=inventory.getItem(i);if(stack.isEmpty())continue;var entry=new CompoundTag();entry.putInt("slot",i);entry.put("item",stack.save(lookup));items.add(entry);}tag.put("inventory",items);
        var grid=new ListTag();parts.forEach((pos,part)->{var entry=new CompoundTag();entry.putInt("node",Track.pack(pos,part.node()));entry.put("item",part.item().save(lookup));grid.add(entry);});tag.put("parts",grid);
        tag.putInt("a",emitterA);tag.putInt("b",emitterB);tag.putInt("dirA",directionA);tag.putInt("dirB",directionB);tag.putLong("energy",energy);tag.putBoolean("enabled",enabled);tag.putBoolean("eject",eject);tag.putIntArray("sides",sides);
        if(lockedRecipe!=null)tag.putString("locked",lockedRecipe.toString());tag.putBoolean("swapped",lockedSwapped);
        if(taskRecipe!=null){tag.putString("task",taskRecipe.toString());tag.put("result",taskResult.saveOptional(lookup));tag.putLong("required",requiredSpeed);tag.putLong("spent",spent);tag.putInt("collision",collisionTicks);tag.putBoolean("returning",returning);
            tag.put("taskMagnet",taskMagnet.save());tag.putLong("magneticSpent",magneticSpent);
            if(taskRules!=null){var rules=new CompoundTag();rules.putInt("gap",taskRules.maxGap());rules.putDouble("bend",taskRules.bendFactor());rules.putLong("cost",taskRules.accelerationCost());tag.put("rules",rules);}
            if(beam!=null){var data=new CompoundTag();data.putInt("segment",beam.segment);data.putDouble("offset",beam.offset);data.putLong("speed",beam.speed);data.putDouble("bend",beam.bendDistance);data.putDouble("previous",beam.previousBend);tag.put("beam",data);}}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);
        inventory.clearContent();for(var raw:tag.getList("inventory",Tag.TAG_COMPOUND)){var entry=(CompoundTag)raw;int slot=entry.getInt("slot");if(slot>=0&&slot<SLOTS)inventory.setItem(slot,ItemStack.parseOptional(lookup,entry.getCompound("item")));}
        if(!tag.contains("partsVersion")&&partKind(inventory.getItem(PART_IN))>Track.RING&&inventory.getItem(STRAIGHT_PARTS).isEmpty()){
            inventory.setItem(STRAIGHT_PARTS,inventory.getItem(PART_IN));inventory.setItem(PART_IN,ItemStack.EMPTY);
        }
        parts.clear();for(var raw:tag.getList("parts",Tag.TAG_COMPOUND)){var entry=(CompoundTag)raw;int packed=entry.getInt("node"),pos=packed&4095;var item=ItemStack.parseOptional(lookup,entry.getCompound("item"));int kind=partKind(item);
            if(kind>0&&!item.isEmpty()&&parts.size()<Track.MAX_CELLS)parts.put(pos,new Placed(new Track.Node(kind,(packed>>15)&7,Math.min(2,(packed>>18)&3)),item.copyWithCount(1)));}
        emitterA=tag.contains("a")?Math.clamp(tag.getInt("a"),0,Track.MAX_CELLS-1):Track.cell(26,32);emitterB=tag.contains("b")?Math.clamp(tag.getInt("b"),0,Track.MAX_CELLS-1):Track.cell(38,32);
        if(emitterA==emitterB)emitterB=(emitterA+1)%Track.MAX_CELLS;
        directionA=Math.clamp(tag.getInt("dirA"),0,7);directionB=tag.contains("dirB")?Math.clamp(tag.getInt("dirB"),0,7):4;
        energy=Math.max(0,tag.getLong("energy"));enabled=tag.getBoolean("enabled");eject=!tag.contains("eject")||tag.getBoolean("eject");var savedSides=tag.getIntArray("sides");if(savedSides.length==6)for(int i=0;i<6;i++)sides[i]=Math.clamp(savedSides[i],CLOSED,EJECT);
        lockedRecipe=readId(tag.getString("locked"));lockedSwapped=tag.getBoolean("swapped");taskRecipe=readId(tag.getString("task"));taskResult=ItemStack.parseOptional(lookup,tag.getCompound("result"));requiredSpeed=Math.clamp(tag.getLong("required"),0,Integer.MAX_VALUE);spent=Math.max(0,tag.getLong("spent"));collisionTicks=Math.clamp(tag.getInt("collision"),0,5);returning=tag.getBoolean("returning");beam=null;taskRules=null;
        if(tag.contains("rules")){var rules=tag.getCompound("rules");taskRules=new Track.Rules(rules.getInt("gap"),rules.getDouble("bend"),rules.getLong("cost"));}
        taskMagnet=MagnetSupport.Settings.load(tag.getCompound("taskMagnet"));magneticSpent=Math.max(0,tag.getLong("magneticSpent"));magnetNeeded=0;magnetKey=List.of();
        if(tag.contains("beam")){var data=tag.getCompound("beam");beam=new Track.Beam();beam.segment=data.getInt("segment");beam.offset=data.getDouble("offset");beam.speed=data.getLong("speed");beam.bendDistance=data.getDouble("bend");beam.previousBend=data.getDouble("previous");}
        if(busy()&&(taskRecipe==null||beam==null)){returning=true;enabled=false;}
        revision++;cachedPlan=null;
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder){super.collectImplicitComponents(builder);var tag=new CompoundTag();write(tag,level.registryAccess());builder.set(Content.COLLIDER_DATA.get(),tag);}
    @Override protected void applyImplicitComponents(DataComponentInput input){super.applyImplicitComponents(input);var tag=input.get(Content.COLLIDER_DATA.get());if(tag!=null)loadAdditional(tag,level.registryAccess());}
    private static ResourceLocation readId(String value){return value.isEmpty()?null:ResourceLocation.tryParse(value);}
    private static long safeAdd(long a,long b){return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
}
