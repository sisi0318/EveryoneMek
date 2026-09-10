package dev.everyonemek.ars;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.common.inventory.container.sync.list.SyncableList;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Mek's collection tracker carries the confirmed lock and matched recipe together. */
public final class RecipeSelectionSync extends SyncableList<String> {
    public RecipeSelectionSync(Supplier<List<String>> getter, Consumer<List<String>> setter) { super(getter, setter); }
    @Override protected List<String> deserializeList(RegistryFriendlyByteBuf buffer) { return buffer.readList(buf -> buf.readUtf(256)); }
    @Override protected void serializeListElement(RegistryFriendlyByteBuf buffer, String value) { buffer.writeUtf(value, 256); }
}
