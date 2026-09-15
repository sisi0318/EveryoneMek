package dev.everyonemek.overloadcore;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/** Pack overrides of a supported recipe must still match its audited definition. */
public final class BonusRecipes {
    private static final Map<String, JsonElement> BASELINES = new HashMap<>();
    private static final Map<Level, IdentityHashMap<Recipe<?>, Boolean>> CACHE = new WeakHashMap<>();
    private static boolean loaded;
    private static void load() {
        if (loaded) return; loaded = true;
        try (var in = BonusRecipes.class.getResourceAsStream("/overloadcore-bonus-recipes.json")) {
            if (in != null) JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                  .getAsJsonObject().entrySet().forEach(e -> BASELINES.put(e.getKey(), e.getValue()));
        } catch (Exception error) { throw new IllegalStateException("Cannot load audited recipe definitions", error); }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean allowed(Level level, Recipe<?> recipe, String id) {
        load(); var expected = BASELINES.get(id); if (expected == null) return false;
        var cache = CACHE.computeIfAbsent(level, unused -> new IdentityHashMap<>());
        return cache.computeIfAbsent(recipe, unused -> {
            try {
                var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
                var definition = expected.getAsJsonObject().deepCopy(); definition.remove("type");
                var codec = (com.mojang.serialization.Codec) recipe.getSerializer().codec().codec();
                var baseline = codec.parse(ops, definition).getOrThrow();
                var expectedNormalized = codec.encodeStart(ops, baseline).getOrThrow();
                var actual = codec.encodeStart(ops, recipe).getOrThrow();
                return expectedNormalized.equals(actual);
            } catch (RuntimeException error) { return false; }
        });
    }
    private BonusRecipes() { }
}
