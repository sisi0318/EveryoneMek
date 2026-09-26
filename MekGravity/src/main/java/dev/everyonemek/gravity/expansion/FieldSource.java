package dev.everyonemek.gravity.expansion;
import dev.everyonemek.gravity.*;
import dev.everyonemek.gravity.solar.*;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
public record FieldSource(TileEntityMekanism tile){
    public static FieldSource at(Level level,BlockPos pos){if(!level.hasChunkAt(pos))return null;BlockEntity be=level.getBlockEntity(pos);
        if(be instanceof SolarPart p)be=p.controller();else if(be instanceof Part p)be=p.controller();
        return be instanceof SolarController s?new FieldSource(s):be instanceof Controller c?new FieldSource(c):null;}
    public boolean solar(){return tile instanceof SolarController;}
    public boolean formed(){return solar()?((SolarController)tile).structure.valid():((Controller)tile).structure.valid();}
    public boolean permitted(OrbitalModule module){return IBlockSecurityUtils.INSTANCE.canAccess(IBlockSecurityUtils.INSTANCE.getOwnerUUID(module.getLevel(),module.getBlockPos(),module),tile.getLevel(),tile.getBlockPos(),tile);}
    public boolean permitted(net.minecraft.world.entity.player.Player p){return IBlockSecurityUtils.INSTANCE.canAccess(p,tile.getLevel(),tile.getBlockPos(),tile);}
    public boolean hot(){return formed()&&tile.canFunction()&&(solar()?((SolarController)tile).isCoreHot():((Controller)tile).enabled&&((Controller)tile).ignited&&((Controller)tile).fuelAvailable());}
    public int tier(){return solar()?((SolarController)tile).structure.tier:((Controller)tile).structure.grade.ordinal();}
    public long stored(){return solar()?((SolarController)tile).stored:((Controller)tile).stored;}
    public long capacity(){return solar()?((SolarController)tile).capacity():((Controller)tile).capacity();}
    public long reserve(){return solar()?((SolarController)tile).reserve():((Controller)tile).reserve();}
    public long available(){return Math.max(0,stored()-reserve());}
    public long net(){return solar()?((SolarController)tile).gross-((SolarController)tile).selfUse:((Controller)tile).gross-((Controller)tile).selfUse;}
    public long exported(){return solar()?((SolarController)tile).lastOutput:((Controller)tile).lastOutput;}
    public long fuel(){return solar()?((SolarController)tile).fuelRemaining:((Controller)tile).fuelRemaining;}
    public long fuelTotal(){return solar()?((SolarController)tile).fuelTotal:((Controller)tile).fuelTotal;}
    public long spare(){return solar()?((SolarController)tile).fuelStock().portions:((Controller)tile).fuelStock().portions;}
    public CoreTuning tuning(){return solar()?((SolarController)tile).tuning:((Controller)tile).tuning;}
    public boolean spend(long amount){return solar()?((SolarController)tile).spendForCorona(amount):((Controller)tile).spendForModules(amount);}
}
