package dev.everyonemek.forbidden;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

final class ClibanoIntegrationContractTest {
    private static ClassNode read(String name) throws IOException {
        try (var input = ClibanoIntegrationContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name); var node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    @Test void nativeFormationAndRemovalHooksStillMatchPublishedRelease() throws IOException {
        String fa = "com/stal111/forbidden_arcanus/common/";
        var transform = read(fa + "item/mundabitur/TransformPatternInteraction");
        assertTrue(transform.methods.stream().anyMatch(m -> m.name.equals("canInteract") && m.desc.equals("(L" + fa + "item/mundabitur/TransformPatternInteraction$TransformPatternContext;)Z")));
        var create = read(fa + "item/mundabitur/CreateClibanoInteraction");
        assertTrue(create.methods.stream().anyMatch(m -> m.name.equals("placeBlock") && m.desc.equals("(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V")));
        var frame = read(fa + "block/clibano/AbstractClibanoFrameBlock");
        var remove = frame.methods.stream().filter(m -> m.name.equals("onRemove")).findFirst().orElseThrow();
        int calls = 0;
        for (var instruction : remove.instructions) if (instruction instanceof MethodInsnNode call
              && call.owner.equals("net/minecraft/world/level/Level") && call.name.equals("removeBlock")
              && call.desc.equals("(Lnet/minecraft/core/BlockPos;Z)Z")) calls++;
        assertEquals(1, calls, "Installation must suppress exactly the native center removal call");
        var main = read(fa + "block/entity/clibano/ClibanoMainBlockEntity");
        assertTrue(main.fields.stream().anyMatch(f -> f.name.equals("frontDirection") && f.desc.equals("Lnet/minecraft/core/Direction;")));
    }
    @Test void sideIconHookUpdatesBothDisplayAndTooltipWithoutLoadingClientClasses() throws IOException {
        var button = read("mekanism/client/gui/element/button/SideDataButton");
        assertTrue(button.fields.stream().anyMatch(f -> f.name.equals("tile") && f.desc.equals("Lmekanism/common/tile/base/TileEntityMekanism;")));
        assertTrue(button.fields.stream().anyMatch(f -> f.name.equals("slotPos") && f.desc.equals("Lmekanism/api/RelativeSide;")));
        assertTrue(button.fields.stream().anyMatch(f -> f.name.equals("otherBlockItem") && f.desc.equals("Lnet/minecraft/world/item/ItemStack;")));
        assertTrue(button.methods.stream().anyMatch(m -> m.name.equals("drawBackground") && m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")));
        assertTrue(button.methods.stream().anyMatch(m -> m.name.equals("updateTooltip") && m.desc.equals("(II)V")));
    }
    @Test void electricHeatWrapsTheNativeRecipeTickAndFuelRead() throws IOException {
        String mainName = "com/stal111/forbidden_arcanus/common/block/entity/clibano/ClibanoMainBlockEntity";
        String logicName = "com/stal111/forbidden_arcanus/common/block/entity/clibano/logic/ClibanoSmeltLogic";
        var main = read(mainName);
        for (String field : new String[] {"burnTime", "burnDuration"})
            assertTrue(main.fields.stream().anyMatch(f -> f.name.equals(field) && f.desc.equals("I")));
        assertTrue(main.fields.stream().anyMatch(f -> f.name.equals("wasLit") && f.desc.equals("Z")));
        assertTrue(main.fields.stream().anyMatch(f -> f.name.equals("logic") && f.desc.equals("L" + logicName + ";")));
        assertTrue(main.methods.stream().anyMatch(m -> m.name.equals("updateAppearance") && m.desc.equals("(Lnet/minecraft/world/level/Level;)V")));
        var tick = main.methods.stream().filter(m -> m.name.equals("serverTick") && m.desc.equals(
              "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;L" + mainName + ";)V")).findFirst().orElseThrow();
        int recipes = 0, processing = 0, fuel = 0;
        for (var instruction : tick.instructions) if (instruction instanceof MethodInsnNode call) {
            if (call.owner.equals(logicName) && call.name.equals("updateRecipes") && call.desc.equals("(Ljava/util/List;)V")) recipes++;
            if (call.owner.equals(logicName) && call.name.equals("tick") && call.desc.equals("(Z)V")) processing++;
            if (call.owner.equals(mainName) && call.name.equals("getStack") && call.desc.equals("(I)Lnet/minecraft/world/item/ItemStack;")
                  && call.getPrevious().getOpcode() == org.objectweb.asm.Opcodes.ICONST_2) fuel++;
        }
        assertEquals(1, recipes); assertEquals(1, processing); assertEquals(1, fuel);
    }
}
