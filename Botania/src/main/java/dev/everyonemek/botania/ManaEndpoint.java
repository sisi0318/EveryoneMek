package dev.everyonemek.botania;

import java.util.UUID;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.*;
import mekanism.api.security.ISecurityUtils;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

/** A view onto one real pool or one addon machine tank, never a resource store. */
public record ManaEndpoint(BlockEntity tile, Direction face, UUID actor) {
    public static ManaEndpoint at(Level level, BlockPos pos, Direction face, UUID actor) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        var pool = ManaTransfer.pool(level, pos); if (pool != null) return new ManaEndpoint(pool, face, actor);
        if (!(level.getBlockEntity(pos) instanceof ManaMachine machine) || !machine.kind().chemical || machine.isRemoved()
              || actor == null || !ISecurityUtils.INSTANCE.canAccessObject(actor, machine, false)) return null;
        var endpoint = new ManaEndpoint(machine, face, actor); return endpoint.handler() == null ? null : endpoint;
    }
    public static ManaEndpoint at(NetworkPlant node) { return at(node.getLevel(), node.targetPos(), node.direction().getOpposite(), Flowers.owner(node)); }
    private IChemicalHandler handler() {
        return Flowers.live(tile) ? tile.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), tile.getBlockPos(), face) : null;
    }
    public boolean same(ManaEndpoint other) { return other != null && tile == other.tile && face == other.face && java.util.Objects.equals(actor, other.actor); }
    public BlockPos getBlockPos() { return tile.getBlockPos(); }
    public int getCurrentMana() { return tile instanceof ManaPoolBlockEntity pool ? pool.getCurrentMana() : (int) ((ManaMachine) tile).mana().getStored(); }
    public int getMaxMana() { return tile instanceof ManaPoolBlockEntity pool ? pool.getMaxMana() : ManaMachine.MANA_CAPACITY; }
    public int extractable() {
        if (tile instanceof ManaPoolBlockEntity pool) return ManaTransfer.canTake(pool) ? pool.getCurrentMana() : 0;
        var handler = handler(); return handler == null ? 0 : (int) handler.extractChemical(new ChemicalStack(ManaContent.MANA, getCurrentMana()), Action.SIMULATE).getAmount();
    }
    public int space() {
        int space = Math.max(0, getMaxMana() - getCurrentMana());
        if (tile instanceof ManaPoolBlockEntity pool) return ManaTransfer.canGive(pool) ? space : 0;
        var handler = handler(); return handler == null || space == 0 ? 0 : space - (int) handler.insertChemical(new ChemicalStack(ManaContent.MANA, space), Action.SIMULATE).getAmount();
    }
    public int take(int amount) {
        if (amount <= 0) return 0;
        if (tile instanceof ManaPoolBlockEntity pool) {
            int before = pool.getCurrentMana(); pool.receiveMana(-Math.min(amount, extractable())); pool.setChanged(); return before - pool.getCurrentMana();
        }
        var handler = handler(); return handler == null ? 0 : (int) handler.extractChemical(new ChemicalStack(ManaContent.MANA, amount), Action.EXECUTE).getAmount();
    }
    public int give(int amount) {
        if (amount <= 0) return 0;
        if (tile instanceof ManaPoolBlockEntity pool) {
            int before = pool.getCurrentMana(); pool.receiveMana(Math.min(amount, space())); pool.setChanged(); return pool.getCurrentMana() - before;
        }
        var handler = handler(); return handler == null ? 0 : amount - (int) handler.insertChemical(new ChemicalStack(ManaContent.MANA, amount), Action.EXECUTE).getAmount();
    }
    public void refund(int amount) {
        if (amount <= 0) return;
        if (tile instanceof ManaPoolBlockEntity pool) pool.receiveMana(amount);
        else ((ManaMachine) tile).mana().insert(new ChemicalStack(ManaContent.MANA, amount), Action.EXECUTE, AutomationType.INTERNAL);
        tile.setChanged();
    }
    public void setChanged() { tile.setChanged(); }
}
