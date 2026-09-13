package dev.everyonemek.botania.compat.ae2;

import java.util.ArrayList;
import appeng.api.ids.AEComponents;
import appeng.api.util.KeyTypeSelection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** One-time terminal migration. Later opt-outs remain player settings. */
public final class ManaVisibilityDefaults {
    public static final String MARKER = "botanicalmekanism_mana_visibility_initialized";
    public static void enableMana(KeyTypeSelection selection) {
        var mana = ManaKeys.type();
        if (!selection.enabled().containsKey(mana) || selection.isEnabled(mana)) return;
        var enabled = new ArrayList<>(selection.enabledSet()); enabled.add(mana);
        selection.setEnabledSet(enabled);
    }
    public static void wireless(ItemStack stack, KeyTypeSelection selection) {
        if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(MARKER)) return;
        enableMana(selection);
        stack.set(AEComponents.ENABLED_KEY_TYPES, selection.enabledSet());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MARKER, true));
    }
    private ManaVisibilityDefaults() { }
}
