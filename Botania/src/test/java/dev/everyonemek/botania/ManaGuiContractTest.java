package dev.everyonemek.botania;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    @Test void manaRecipeHooksMatchTypedBotaniaMethods() throws IOException {
        var methods = java.util.Map.of("ManaPoolRecipeCategory", "ManaInfusionRecipe", "RunicAltarRecipeCategory", "RunicAltarRecipe",
              "TerrestrialAgglomerationRecipeCategory", "TerrestrialAgglomerationRecipe", "BreweryRecipeCategory", "BotanicalBreweryRecipe");
        for (var entry : methods.entrySet()) {
            var category = read("vazkii/botania/client/integration/jei/" + entry.getKey());
            String descriptor = "(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lvazkii/botania/api/recipe/" + entry.getValue() + ";Lmezz/jei/api/recipe/IFocusGroup;)V";
            assertTrue(category.methods.stream().anyMatch(m -> m.name.equals("setRecipe") && m.desc.equals(descriptor)), entry.getKey());
        }
    }
    @Test void cellColorRegistrationRetainsTheNativeOpaqueColorContract() throws IOException {
        assumeTrue(getClass().getClassLoader().getResource("appeng/init/client/InitItemColors.class") != null);
        var upstream = read("appeng/init/client/InitItemColors");
        // AE's RGB callbacks need the alpha adapter applied by its registration layer.
        assertTrue(upstream.methods.stream().anyMatch(method -> {
            for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                  && call.owner.equals("net/minecraft/util/FastColor$ARGB32") && call.name.equals("opaque")) return true;
            return false;
        }));
        var client = read("dev/everyonemek/botania/compat/ae2/ManaAeClient");
        var callback = client.methods.stream().filter(m -> m.name.equals("cellColor")).findFirst().orElseThrow();
        boolean nativeRgb = false, opaque = false;
        for (var instruction : callback.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("appeng/items/storage/BasicStorageCell") && call.name.equals("getColor")) nativeRgb = true;
                if (call.owner.equals("net/minecraft/util/FastColor$ARGB32") && call.name.equals("opaque") && call.desc.equals("(I)I")) {
                    assertTrue(nativeRgb); opaque = true;
                }
            } else if (instruction.getOpcode() == org.objectweb.asm.Opcodes.IRETURN) assertTrue(opaque, "RGB returned without alpha makes the cell invisible");
        }
        var registration = client.methods.stream().filter(m -> m.name.equals("colors")).findFirst().orElseThrow();
        boolean registered = false;
        for (var instruction : registration.instructions) if (instruction instanceof InvokeDynamicInsnNode lambda)
            for (var argument : lambda.bsmArgs) if (argument instanceof org.objectweb.asm.Handle handle
                  && handle.getOwner().equals(client.name) && handle.getName().equals("cellColor")) registered = true;
        assertTrue(registered, "The ItemColor registration bypasses the alpha adapter");
    }
}
