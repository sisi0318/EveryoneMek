package dev.everyonemek.oritech;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Part extends BlockEntity {
    public BlockPos anchor=BlockPos.ZERO;
    public UUID owner;
    public Part(BlockPos p,BlockState s){super(Content.PART_TILE.get(),p,s);}
    public Processor controller(){if(level!=null&&level.hasChunkAt(anchor)&&level.getBlockEntity(anchor) instanceof Processor p&&p.identity.equals(owner)&&p.owns(worldPosition)&&p.complete())return p;return null;}
    public void link(Processor p){anchor=p.getBlockPos();owner=p.identity;setChanged();}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putLong("anchor",anchor.asLong());if(owner!=null)t.putUUID("owner",owner);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);anchor=BlockPos.of(t.getLong("anchor"));owner=t.hasUUID("owner")?t.getUUID("owner"):null;}
}
