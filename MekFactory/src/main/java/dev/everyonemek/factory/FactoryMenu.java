package dev.everyonemek.factory;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.inventory.container.sync.chemical.SyncableChemicalStack;
import mekanism.api.chemical.ChemicalStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;

/** Controller dashboard: machine/upgrades and actual work, with no shared material slots. */
public final class FactoryMenu extends MekanismTileContainer<Controller> {
    private BlockPos clicked;
    public int jobIndex,jobCount,jobUnits,jobProgress,jobTicks=1,availableParallel,frameParallel;
    public ItemStack product=ItemStack.EMPTY;
    public FluidStack productFluid=FluidStack.EMPTY;
    public ChemicalStack productChemical=ChemicalStack.EMPTY;
    public boolean formed,legacyInput,legacyOutput;
    public FactoryMenu(int id,Inventory inv,Controller c,BlockPos clicked){
        super(Content.MENU,id,inv,c);this.clicked=clicked;
        track(SyncableInt.create(()->c.processing.jobs.size(),v->jobCount=v));
        track(SyncableInt.create(()->{current();return jobIndex;},v->jobIndex=v));
        track(SyncableInt.create(()->current()==null?0:current().units,v->jobUnits=v));
        track(SyncableInt.create(()->current()==null?0:current().progress,v->jobProgress=v));
        track(SyncableInt.create(()->current()==null?1:current().ticks,v->jobTicks=v));
        track(SyncableItemStack.create(()->current()==null||current().items.isEmpty()?ItemStack.EMPTY:current().items.getFirst(),v->product=v));
        track(SyncableFluidStack.create(()->current()==null||current().fluids.isEmpty()?FluidStack.EMPTY:current().fluids.getFirst(),v->productFluid=v));
        track(SyncableChemicalStack.create(()->current()==null||current().chemicals.isEmpty()?ChemicalStack.EMPTY:current().chemicals.getFirst(),v->productChemical=v));
        track(SyncableBoolean.create(()->c.structure.formed,v->formed=v));
        track(SyncableBoolean.create(c.inputs::hasContents,v->legacyInput=v));
        track(SyncableBoolean.create(c.outputs::hasContents,v->legacyOutput=v));
        track(SyncableInt.create(()->Profiles.availableParallel(c),v->availableParallel=v));
        track(SyncableInt.create(()->c.structure.formed?c.structure.parallel:0,v->frameParallel=v));
    }
    public double progressRatio(){if(!getLevel().isClientSide){var job=current();return job==null?0:Math.clamp(job.progress/(double)Math.max(1,job.ticks),0,1);}return jobCount==0?0:Math.clamp(jobProgress/(double)Math.max(1,jobTicks),0,1);}
    private Processing.Job current(){var jobs=tile.processing.jobs;if(jobs.isEmpty()){jobIndex=0;return null;}jobIndex=Math.clamp(jobIndex,0,jobs.size()-1);return jobs.get(jobIndex);}
    public static FactoryMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf buf){var pos=buf.readBlockPos();var clicked=buf.readBlockPos();return new FactoryMenu(id,inv,(Controller)inv.player.level().getBlockEntity(pos),clicked);}
    public static boolean access(Player p,Controller c,BlockPos clicked){
        if(c.isRemoved()||p.level()!=c.getLevel()||!p.level().hasChunkAt(clicked)||p.distanceToSqr(clicked.getCenter())>64||!c.access(p))return false;
        return clicked.equals(c.getBlockPos())?p.level().getBlockEntity(clicked)==c:
              p.level().getBlockEntity(clicked) instanceof Part part&&part.controller()==c||FactoryStructure.inductionController(p.level(),clicked)==c;
    }
    public static void open(Player player,Controller c,BlockPos clicked){if(player instanceof ServerPlayer server&&access(player,c,clicked))
        server.openMenu(new SimpleMenuProvider((id,inv,p)->new FactoryMenu(id,inv,c,clicked),c.getDisplayName()),buf->{buf.writeBlockPos(c.getBlockPos());buf.writeBlockPos(clicked);});}
    @Override protected int getInventoryXOffset(){return 34;}
    @Override protected int getInventoryYOffset(){return 178;}
    @Override public boolean stillValid(Player p){return clicked==null||super.stillValid(p)&&(p.level().isClientSide||access(p,tile,clicked));}
    public boolean canConfigure(Player p){return !p.level().isClientSide&&stillValid(p)&&tile.access(p);}
    @Override public boolean clickMenuButton(Player p,int id){if(!canConfigure(p))return false;
        if(id==1){tile.enabled=!tile.enabled;tile.markForSave();return true;}
        if(id==2){Construction.preview(tile,(ServerPlayer)p);return true;}
        if(id==3)return Construction.build(tile,(ServerPlayer)p);
        if(id>=10&&id<16)return tile.resize((id-10)/2,id%2==0?-1:1);
        if(id==20||id==21||id==23||id==24){tile.parallelLimit=Math.clamp(tile.parallelLimit+(id==20||id==23?-1:1)*(id>=23?16:1),1,FactoryConfig.MAX_PARALLEL);tile.markForSave();return true;}
        if(id==22&&Profiles.rotary(tile.template.getStack())&&tile.processing.jobs.isEmpty()){tile.rotaryReverse=!tile.rotaryReverse;tile.markForSave();return true;}
        if(id>=40&&id<46)return PortConfiguration.cycleFace(tile,p,mekanism.api.RelativeSide.values()[id-40]);
        if(id==46){tile.autoEject=!tile.autoEject;tile.markForSave();return true;}
        if(id==70||id==71){jobIndex=Math.clamp(jobIndex+(id==70?-1:1),0,Math.max(0,tile.processing.jobs.size()-1));return true;}
        if(id==80||id==81){WarehouseMenu.openLegacy(p,tile,clicked,id==81);return true;}
        return false;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){return canConfigure(p)?super.quickMoveStack(p,index):ItemStack.EMPTY;}
}
