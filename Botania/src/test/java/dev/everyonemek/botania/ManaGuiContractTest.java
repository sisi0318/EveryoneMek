package dev.everyonemek.botania;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ManaGuiContractTest {
    private static ClassNode read(String path) throws IOException {
        try (var input = ManaGuiContractTest.class.getClassLoader().getResourceAsStream(path + ".class")) {
            assertNotNull(input); var node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    @Test void manaConfigurationMixinsMatchTheInstalledMekanism() throws IOException {
        var window = read("mekanism/client/gui/element/window/GuiSideConfiguration");
        assertTrue(window.fields.stream().anyMatch(f -> f.name.equals("tile") && f.desc.equals("Lmekanism/common/tile/base/TileEntityMekanism;")));
        assertTrue(window.fields.stream().anyMatch(f -> f.name.equals("currentType") && f.desc.equals("Lmekanism/common/lib/transmitter/TransmissionType;")));
        var foreground = window.methods.stream().filter(m -> m.name.equals("renderForeground")).findFirst().orElseThrow();
        boolean title = false;
        for (var instruction : foreground.instructions) if (instruction instanceof MethodInsnNode call)
            title |= call.owner.equals("mekanism/common/MekanismLang") && call.name.equals("translate") && call.desc.equals("([Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;");
        assertTrue(title);
        var tab = read("mekanism/client/gui/element/tab/GuiConfigTypeTab");
        assertEquals("mekanism/client/gui/element/GuiInsetElement", tab.superName);
        assertTrue(tab.fields.stream().anyMatch(f -> f.name.equals("typeTooltips") && f.desc.equals("Ljava/util/Map;")));
        assertTrue(tab.fields.stream().anyMatch(f -> f.name.equals("config") && f.desc.equals("Lmekanism/client/gui/element/window/GuiSideConfiguration;")));
        assertTrue(read(tab.superName).methods.stream().anyMatch(m -> m.name.equals("drawBackgroundOverlay") && m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;)V")));
    }
}
