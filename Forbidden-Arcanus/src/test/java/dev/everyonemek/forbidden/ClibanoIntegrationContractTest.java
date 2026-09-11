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
}
