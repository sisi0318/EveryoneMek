package dev.everyonemek.gravity.expansion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
public record FieldLink(String dimension,BlockPos pos){
    public static FieldLink at(Level level,BlockPos pos){return new FieldLink(level.dimension().location().toString(),pos.immutable());}
    public boolean inRange(Level level,BlockPos from){return level.dimension().location().toString().equals(dimension)&&from.distSqr(pos)<=(double)ModuleConfig.RANGE.get()*ModuleConfig.RANGE.get();}
    public CompoundTag save(){var tag=new CompoundTag();tag.putString("dimension",dimension);tag.putLong("pos",pos.asLong());return tag;}
    public static FieldLink read(CompoundTag tag){return tag.contains("dimension")&&tag.contains("pos")?new FieldLink(tag.getString("dimension"),BlockPos.of(tag.getLong("pos"))):null;}
}
