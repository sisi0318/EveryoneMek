package dev.everyonemek.oritech;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.recipes.*;
import rearth.oritech.util.*;

/** Native instances supply immutable layout/configuration metadata only; they never enter a world or tick. */
public enum Profiles {
    EMPTY("",null), PULVERIZER("pulverizer_block",RecipeContent.PULVERIZER),
    GRINDER("fragment_forge_block",RecipeContent.GRINDER), ASSEMBLER("assembler_block",RecipeContent.ASSEMBLER),
    FOUNDRY("foundry_block",RecipeContent.FOUNDRY), CENTRIFUGE("centrifuge_block",RecipeContent.CENTRIFUGE),
    FURNACE("powered_furnace_block",null), REFINERY("refinery_block",RecipeContent.REFINERY),
    COOLER("cooler_block",RecipeContent.COOLER), ATOMIC("atomic_forge_block",RecipeContent.ATOMIC_FORGE);
    public final String path;
    public final OritechRecipeType recipes;
    private MachineBlockEntity metadata;
    Profiles(String path,OritechRecipeType recipes){this.path=path;this.recipes=recipes;}
    public Block block(){return this==EMPTY?Content.BLOCK.get():BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("oritech",path));}
    public MachineBlockEntity metadata(){
        if(this==EMPTY)throw new IllegalStateException("No host");
        if(metadata==null)metadata=(MachineBlockEntity)((EntityBlock)block()).newBlockEntity(BlockPos.ZERO,block().defaultBlockState());
        return metadata;
    }
    public static Profiles of(ItemStack stack){if(stack.getItem() instanceof BlockItem item)for(var p:values())if(p!=EMPTY&&p.block()==item.getBlock())return p;return EMPTY;}
    public static Profiles index(int i){return values()[Math.clamp(i,0,values().length-1)];}
    public int inputs(){return this==EMPTY?4:metadata().getSlotAssignments().inputCount();}
    public int outputs(){return this==EMPTY?4:metadata().getSlotAssignments().outputCount();}
    public List<ScreenProvider.GuiSlot> slots(){
        if(this==EMPTY)return List.of();
        var a=metadata().getSlotAssignments();
        return metadata().getGuiSlots().stream().map(s->new ScreenProvider.GuiSlot(s.output()?4+s.index()-a.outputStart():s.index()-a.inputStart(),s.x(),s.y(),s.output())).toList();
    }
    public List<Vec3i> cores(int modules){
        var result=new ArrayList<Vec3i>();
        if(this!=EMPTY&&metadata() instanceof MultiblockMachineController multiblock)result.addAll(multiblock.getCorePositions());
        if(this==REFINERY)for(int m=0;m<modules;m++)for(int x=0;x<3;x++)for(int z=-1;z<=0;z++)result.add(new Vec3i(x,2+m,z));
        return result;
    }
}
