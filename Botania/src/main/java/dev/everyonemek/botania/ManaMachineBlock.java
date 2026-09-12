package dev.everyonemek.botania;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;

public final class ManaMachineBlock extends BlockTile<ManaMachine, Machine<ManaMachine>> {
    public final ManaMachineKind kind;
    public ManaMachineBlock(ManaMachineKind kind, Machine<ManaMachine> type) {
        super(type, properties -> properties.strength(4, 12)); this.kind = kind;
    }
}
