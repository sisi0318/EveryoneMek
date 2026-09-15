package dev.everyonemek.overloadcore.mixin;
import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class OptionalMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String pkg) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return !mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Generator") || net.neoforged.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
              .anyMatch(mod -> mod.getModId().equals("mekanismgenerators"));
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) { }
}
