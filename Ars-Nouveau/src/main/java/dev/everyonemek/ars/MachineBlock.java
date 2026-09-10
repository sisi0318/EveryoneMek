package dev.everyonemek.ars;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;

public final class MachineBlock extends BlockTile<SourceMachine, Machine<SourceMachine>> {
    public final MachineKind kind;
    public MachineBlock(MachineKind kind, Machine<SourceMachine> type) {
        super(type, properties -> properties.strength(4, 12));
        this.kind = kind;
    }
}
