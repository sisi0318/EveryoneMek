package dev.everyonemek.forbidden;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;

public final class MachineBlock extends BlockTile<Controller, Machine<Controller>> {
    public final MachineKind kind;
    public MachineBlock(MachineKind kind, Machine<Controller> type) {
        super(type, properties -> properties.strength(4, 12));
        this.kind = kind;
    }
}
