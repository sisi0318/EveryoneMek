package dev.everyonemek.natures;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Checks the published client GUI's bytecode without loading or starting a Minecraft client. */
final class ClientMixinContractTest {
    private static ClassNode read(String path) throws IOException {
        try (var stream = ClientMixinContractTest.class.getResourceAsStream("/" + path + ".class")) {
            assertNotNull(stream, path);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
    private static Stream<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        return Stream.concat(visible == null ? Stream.empty() : visible.stream(), invisible == null ? Stream.empty() : invisible.stream());
    }
    @Test
    void clientMixinMatchesPublishedMekanismGui() throws IOException {
        var target = read("mekanism/client/gui/element/button/SideDataButton");
        var mixin = read("dev/everyonemek/natures/mixin/ChamberSideDataButtonMixin");
        for (var field : mixin.fields) {
            boolean shadow = annotations(field.visibleAnnotations, field.invisibleAnnotations).anyMatch(a -> a.desc.endsWith("/Shadow;"));
            if (shadow) assertTrue(target.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)),
                  "Mek GUI field changed: " + field.name);
        }
        int injectors = 0;
        for (var method : mixin.methods) {
            var injection = annotations(method.visibleAnnotations, method.invisibleAnnotations).filter(a -> a.desc.endsWith("/Inject;")).findFirst();
            if (injection.isEmpty()) continue;
            injectors++;
            var values = injection.get().values;
            int index = values.indexOf("method");
            assertTrue(index >= 0);
            var names = (List<?>) values.get(index + 1);
            String descriptor = method.desc.replace("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;", "");
            for (var name : names) assertTrue(target.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)),
                  "Mek GUI injection target changed: " + name + descriptor);
        }
        assertTrue(injectors > 0);
    }
}
