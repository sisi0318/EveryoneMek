package dev.everyonemek.factory;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.RelativeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/** Face controls operate on actual ports; a corner port has one mode shared by all exposed faces. */
public final class PortConfiguration {
    private static List<Part> parts(Controller c,boolean conversion) {
        if (c.structure.formed) return List.copyOf(conversion?c.structure.converters:c.structure.ports);
        var found = new ArrayList<Part>();
        var a = c.structure.at(0, 0, 0);
        var b = c.structure.at(c.sizeX - 1, c.sizeY - 1, c.sizeZ - 1);
        for (var pos : BlockPos.betweenClosed(a, b)) {
            if (!c.getLevel().hasChunkAt(pos) || c.structure.outward(pos) == null) continue;
            if (c.getLevel().getBlockEntity(pos) instanceof Part p
                  && (conversion?p.isConverter():p.isPort())
                  && (p.controller() == null || p.controller() == c)) found.add(p);
        }
        return found;
    }

    public static void refresh(Controller c) {
        java.util.Arrays.fill(c.portInputs, 0);
        java.util.Arrays.fill(c.portOutputs, 0);
        java.util.Arrays.fill(c.portConverters,0);
        for (var port : parts(c,false)) for (var side : RelativeSide.values()) {
            if (c.structure.isOutward(port.getBlockPos(), side.getDirection(c.getDirection()))) {
                (port.getBlockState().getValue(PartBlock.OUTPUT) ? c.portOutputs : c.portInputs)[side.ordinal()]++;
            }
        }
        for(var part:parts(c,true))for(var side:RelativeSide.values())if(c.structure.isOutward(part.getBlockPos(),side.getDirection(c.getDirection())))c.portConverters[side.ordinal()]++;
    }

    public static boolean cycleFace(Controller c, Player player, RelativeSide side) {
        if (!c.access(player)) return false;
        Direction direction = side.getDirection(c.getDirection());
        var targets = parts(c,false).stream().filter(p -> c.structure.isOutward(p.getBlockPos(), direction)).toList();
        if (targets.isEmpty() || targets.stream().anyMatch(p -> !player.mayInteract(c.getLevel(), p.getBlockPos()))) return false;
        // All input -> output. Mixed or all output -> input, independent of port iteration order.
        boolean output = targets.stream().noneMatch(p -> p.getBlockState().getValue(PartBlock.OUTPUT));
        for (var port : targets) port.applyOutput(output);
        c.structure.validate();
        refresh(c);
        return true;
    }

    private PortConfiguration() {}
}
