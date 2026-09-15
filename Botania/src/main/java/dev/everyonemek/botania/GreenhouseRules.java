package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Register rules on both sides during common setup, before datapack recipes are loaded. */
public final class GreenhouseRules {
    public static final int MAX_ID_LENGTH = 256;
    private static final ResourceLocation FIXED = ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "fixed");
    private static volatile Map<ResourceLocation, GreenhouseFlowerRule> rules = Map.of();
    static { BuiltinGreenhouseRules.register(); }

    public static synchronized void register(ResourceLocation id, GreenhouseFlowerRule rule) {
        Objects.requireNonNull(id); Objects.requireNonNull(rule);
        if (id.equals(FIXED) || id.toString().length() > MAX_ID_LENGTH || rules.containsKey(id))
            throw new IllegalArgumentException("Reserved, duplicate or invalid greenhouse rule: " + id);
        if (rule.fluidAmount() < 0 || rule.fluidAmount() > GreenhouseWork.FLUID_CAPACITY)
            throw new IllegalArgumentException("Invalid greenhouse fluid amount: " + id);
        var next = new LinkedHashMap<>(rules); next.put(id, rule); rules = Collections.unmodifiableMap(next);
    }
    public static Map<ResourceLocation, GreenhouseFlowerRule> entries() { return rules; }
    @Nullable public static ResourceLocation id(String name) {
        if (name == null || name.isEmpty() || name.length() > MAX_ID_LENGTH) return null;
        return ResourceLocation.tryParse(name.contains(":") ? name : BotanicalMekanism.ID + ":" + name);
    }
    public static boolean isFixed(String name) { return FIXED.equals(id(name)); }
    @Nullable public static GreenhouseFlowerRule get(String name) { var id = id(name); return id == null ? null : rules.get(id); }
    private GreenhouseRules() { }
}
