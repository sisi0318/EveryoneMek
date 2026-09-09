package dev.everyonemek.natures;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;

public final class MachineBlock extends BlockTile<AuraMachine, Machine<AuraMachine>> {
    public final MachineKind kind;

    public MachineBlock(MachineKind kind, Machine<AuraMachine> type) {
        super(type, properties -> properties.strength(4, 12));
        this.kind = kind;
    }
}
