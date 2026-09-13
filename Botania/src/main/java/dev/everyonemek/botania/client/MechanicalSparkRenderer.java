package dev.everyonemek.botania.client;

import dev.everyonemek.botania.MechanicalSparkEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import vazkii.botania.client.render.entity.ManaSparkRenderer;
import vazkii.botania.common.entity.ManaSparkEntity;

/** Native spark animation and orbiting icons, with Botania's blue-violet master appearance. */
public final class MechanicalSparkRenderer extends ManaSparkRenderer {
    private final TextureAtlasSprite masterSprite;
    public MechanicalSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
        masterSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
              .apply(ResourceLocation.fromNamespaceAndPath("botania", "item/master_corporea_spark"));
    }
    @Override protected TextureAtlasSprite getBaseIcon(ManaSparkEntity entity) {
        return entity instanceof MechanicalSparkEntity spark && spark.isMaster() ? masterSprite : super.getBaseIcon(entity);
    }
}
