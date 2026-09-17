package dev.everyonemek.factory.client;

import dev.everyonemek.factory.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Model registry wrapping uses only the event's baked registry, never the not-yet-ready model manager. */
public final class FactoryModels {
    private static ModelResourceLocation id(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MekFactory.ID, "block/" + path));
    }

    public static void additional(ModelEvent.RegisterAdditional event) {
        for (var name : List.of("formed_panel", "formed_glass", "formed_port_input", "formed_port_output", "formed_cell", "formed_provider")) event.register(id(name));
        for (var direction : Direction.Plane.HORIZONTAL) for (var active : new boolean[]{false, true})
            event.register(id("formed_controller_" + direction.getSerializedName() + (active ? "_active" : "")));
    }

    private static List<Block> induction(boolean cell) {
        return cell ? List.of(MekanismBlocks.BASIC_INDUCTION_CELL.get(), MekanismBlocks.ADVANCED_INDUCTION_CELL.get(), MekanismBlocks.ELITE_INDUCTION_CELL.get(), MekanismBlocks.ULTIMATE_INDUCTION_CELL.get())
              : List.of(MekanismBlocks.BASIC_INDUCTION_PROVIDER.get(), MekanismBlocks.ADVANCED_INDUCTION_PROVIDER.get(), MekanismBlocks.ELITE_INDUCTION_PROVIDER.get(), MekanismBlocks.ULTIMATE_INDUCTION_PROVIDER.get());
    }

    public static void bake(ModelEvent.ModifyBakingResult event) {
        var blocks = new ArrayList<Block>();
        Content.CONTROLLERS.values().forEach(b -> blocks.add(b.get()));
        Content.FRAMES.values().forEach(b -> blocks.add(b.get()));
        Content.PORTS.values().forEach(b -> blocks.add(b.get()));
        blocks.add(Content.CASING.get()); blocks.add(Content.GLASS.get());
        var cells = induction(true); var providers = induction(false); blocks.addAll(cells); blocks.addAll(providers);
        for (var block : blocks) for (var state : block.getStateDefinition().getPossibleStates()) {
            String name;
            if (block instanceof ControllerBlock) name = "formed_controller_" + Attribute.getFacing(state).getSerializedName() + (active(state) ? "_active" : "");
            else if (block instanceof PartBlock part) name = switch (part.kind) {
                case FRAME, CASING -> "formed_panel";
                case GLASS -> "formed_glass";
                case PORT -> state.getValue(PartBlock.OUTPUT) ? "formed_port_output" : "formed_port_input";
            };
            else name = cells.contains(block) ? "formed_cell" : "formed_provider";
            var location = BlockModelShaper.stateToModelLocation(state);
            var original = event.getModels().get(location);
            if (original != null) event.getModels().put(location, new FactorySkinModel(original,
                  Objects.requireNonNull(event.getModels().get(id(name)), "Missing formed factory model: " + name)));
        }
    }

    private static boolean active(BlockState state) {
        return state.getValues().entrySet().stream().anyMatch(e -> e.getKey().getName().equals("active") && Boolean.TRUE.equals(e.getValue()));
    }

    private FactoryModels() {}
}
