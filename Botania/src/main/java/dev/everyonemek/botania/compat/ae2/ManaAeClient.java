package dev.everyonemek.botania.compat.ae2;

import appeng.api.client.*;
import appeng.api.stacks.AEItemKey;
import dev.everyonemek.botania.Content;
import dev.everyonemek.botania.ManaCellTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.mojang.blaze3d.vertex.PoseStack;

public final class ManaAeClient {
    private static ResourceLocation cellModel(ManaCellTier tier) { return ResourceLocation.fromNamespaceAndPath("botanicalmekanism", "block/drive/" + tier.id()); }

    public static void models(ModelEvent.RegisterAdditional event) {
        for (var tier : ManaCellTier.values()) event.register(ModelResourceLocation.standalone(cellModel(tier)));
    }
    public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(ManaAeClient::cellColor, Content.MANA_CELLS.values().stream()
              .map(net.neoforged.neoforge.registries.DeferredItem::get).toArray(net.minecraft.world.item.Item[]::new));
    }
    public static int cellColor(net.minecraft.world.item.ItemStack stack, int tintIndex) {
        // AE returns RGB here; its own registration adds alpha before Minecraft renders it.
        return net.minecraft.util.FastColor.ARGB32.opaque(appeng.items.storage.BasicStorageCell.getColor(stack, tintIndex));
    }

    public static void register() {
        AEKeyRendering.register(ManaKey.TYPE, ManaKey.class, new AEKeyRenderHandler<ManaKey>() {
            @Override public void drawInGui(Minecraft minecraft, GuiGraphics gui, int x, int y, ManaKey key) { dev.everyonemek.botania.client.ManaIcon.draw(gui, x, y); }
            @Override public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, ManaKey key, float scale, int light, Level level) {
                AEKeyRendering.drawOnBlockFace(pose, buffers, AEItemKey.of(Content.MANA_PACKET.get()), scale, light, level);
            }
            @Override public Component getDisplayName(ManaKey key) { return key.getDisplayName(); }
        });
        Content.MANA_CELLS.forEach((tier, cell) -> StorageCellModels.registerModel(cell.get(), cellModel(tier)));
    }
    private ManaAeClient() { }
}
