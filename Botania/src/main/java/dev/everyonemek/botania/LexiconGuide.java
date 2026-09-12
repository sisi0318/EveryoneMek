package dev.everyonemek.botania;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.patchouli.api.PatchouliAPI;

/** Opens the matching entry in the user's existing Lexica Botania. */
public final class LexiconGuide {
    public static boolean open(Player player, ItemStack held, Block block) {
        if (!held.is(BotaniaItems.LEXICA_BOTANIA)) return false;
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (!id.getNamespace().equals(BotanicalMekanism.ID)) return false;
        if (player instanceof ServerPlayer server) PatchouliAPI.get().openBookEntry(server,
              BuiltInRegistries.ITEM.getKey(BotaniaItems.LEXICA_BOTANIA), ResourceLocation.fromNamespaceAndPath("botania", "botanicalmekanism/" + id.getPath()), 0);
        return true;
    }
    private LexiconGuide() { }
}
