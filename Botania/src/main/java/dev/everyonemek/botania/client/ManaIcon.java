package dev.everyonemek.botania.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** The same animated liquid surface that Botania renders inside mana pools. */
public final class ManaIcon {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("botania", "block/mana_water");
    public static void draw(GuiGraphics gui, int x, int y) {
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE);
        gui.blit(x, y, 0, 16, 16, sprite);
    }
    private ManaIcon() { }
}
