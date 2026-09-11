package dev.everyonemek.forbidden;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Reads dependency bytecode without loading client classes into a server. */
final class UpgradeIntegrationContractTest {
    private static ClassNode read(String name) throws IOException {
        try (var input = UpgradeIntegrationContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            var node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    private static void field(ClassNode owner, String name, String descriptor) {
        assertTrue(owner.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor)), owner.name + "." + name);
    }
    @Test void sharedSlotAndComponentMixinTargetsStillExist() throws IOException {
        var slot = read("mekanism/common/inventory/slot/BasicInventorySlot");
        field(slot, "validator", "Ljava/util/function/Predicate;");
        field(slot, "canInsert", "Ljava/util/function/BiPredicate;");
        var component = read("mekanism/common/tile/component/TileComponentUpgrade");
        field(component, "tile", "Lmekanism/common/tile/base/TileEntityMekanism;");
        field(component, "upgradeSlot", "Lmekanism/common/inventory/slot/UpgradeInventorySlot;");
        field(component, "upgradeTicks", "I");
        assertTrue(component.methods.stream().anyMatch(m -> m.name.equals("tickServer") && m.desc.equals("()V")));
    }
    @Test void nativeWindowDetailsHookRunsAfterWindowAndTitleRendering() throws IOException {
        var window = read("mekanism/client/gui/element/window/GuiUpgradeWindow");
        var method = window.methods.stream().filter(m -> m.name.equals("renderForeground") && m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;II)V")).findFirst().orElseThrow();
        int hooks = 0; boolean windowRendered = false, titleRendered = false;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("mekanism/client/gui/element/window/GuiWindow") && call.name.equals("renderForeground")) windowRendered = true;
                if (call.name.equals("drawTitleText")) titleRendered = true;
                if (call.owner.equals("mekanism/client/gui/element/scroll/GuiUpgradeScrollList") && call.name.equals("hasSelection") && call.desc.equals("()Z")) {
                    hooks++; assertTrue(windowRendered && titleRendered, "Resource details must keep Mek's frame, children and title");
                }
            }
        }
        assertEquals(1, hooks);
        var list = read("mekanism/client/gui/element/scroll/GuiUpgradeScrollList");
        assertTrue(list.methods.stream().anyMatch(m -> m.name.equals("setSelected") && m.desc.equals("(Lmekanism/api/Upgrade;)V") && (m.access & Opcodes.ACC_PROTECTED) != 0));
    }
}
