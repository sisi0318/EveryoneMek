package dev.everyonemek.botania.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

/** Native AE cell plus mana label; an absent optional AE must not leave missing parents. */
public final class ManaCellModelLoader implements IGeometryLoader<CompositeModel> {
    @Override public CompositeModel read(JsonObject json, JsonDeserializationContext context) {
        var model = json.deepCopy();
        if (!ModList.get().isLoaded("ae2")) {
            var base = new JsonObject(); base.addProperty("parent", "minecraft:item/generated");
            var textures = new JsonObject(); textures.addProperty("layer0", "botania:item/mana_diamond"); base.add("textures", textures);
            model.getAsJsonObject("children").add("base", base);
        }
        return CompositeModel.Loader.INSTANCE.read(model, context);
    }
}
