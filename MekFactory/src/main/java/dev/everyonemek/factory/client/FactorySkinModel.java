package dev.everyonemek.factory.client;

import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

/** Only world rendering in a server-validated factory changes. Item models and native machinery stay original. */
public final class FactorySkinModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<BakedModel> SKIN = new ModelProperty<>();
    private final BakedModel formedModel;

    public FactorySkinModel(BakedModel original, BakedModel formed) { super(original); formedModel = formed; }
    private BakedModel model(ModelData data) { var skin = data.get(SKIN); return skin == null ? originalModel : skin; }

    @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        return originalModel.getModelData(level, pos, state, data).derive()
              .with(SKIN, FactorySkins.formed(pos) ? formedModel : originalModel).build();
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType type) {
        return model(data).getQuads(state, side, random, data, type);
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return model(data).getRenderTypes(state, random, data);
    }
    @Override public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType type) { return model(data).useAmbientOcclusion(state, data, type); }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return model(data).getParticleIcon(data); }
}
