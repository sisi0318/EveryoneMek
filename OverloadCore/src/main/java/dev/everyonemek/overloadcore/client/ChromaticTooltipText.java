package dev.everyonemek.overloadcore.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * 字体效果参考作者：huige233。
 * 参考 DreamJournalClientTooltipComponent.styleGlitchRGB 的红蓝色散叠影与抖动效果。
 * 按 Overload Core 的持续可读提示独立实现：保留主字，色散不断显现，间歇加强抖动。
 */
public final class ChromaticTooltipText {
    public static final int SIDE_PADDING = 3;
    public static final int TOP_PADDING = 1;
    public static final int LINE_GAP = 2;

    public static void draw(Font font, FormattedCharSequence text, float x, float y, Matrix4f matrix,
          MultiBufferSource.BufferSource buffer, long time, int line) {
        long phase = Math.floorMod(time, 2_400L) + line * 137L;
        boolean surge = phase % 2_400L < 420L;
        long frame = time / 40L;
        float jitterX = (float) Math.sin(phase / 170.0) * .18F;
        float jitterY = (float) Math.cos(phase / 230.0) * .10F;
        if (surge) {
            jitterX += noise(frame, line) * .55F;
            jitterY += noise(frame, line + 37) * .35F;
        }
        float separation = surge ? 1.35F : 1.0F;
        // Set the sequence styles as well as the draw color: the original red/green styles otherwise override these layers.
        font.drawInBatch(tint(text, 0x0000FF), x + jitterX - separation, y + jitterY - .15F, 0xFFFFFFFF, false,
              matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        font.drawInBatch(tint(text, 0xFF0000), x + jitterX + separation, y + jitterY + .15F, 0xFFFFFFFF, false,
              matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        if (surge) {
            font.drawInBatch(tint(text, 0xFFFFFF), x - jitterX, y - jitterY, 0x66FFFFFF, false,
                  matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        font.drawInBatch(text, x + jitterX, y + jitterY, 0xFFFFFFFF, false,
              matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    private static FormattedCharSequence tint(FormattedCharSequence text, int color) {
        return sink -> text.accept((index, style, codePoint) -> sink.accept(index, style.withColor(color), codePoint));
    }

    private static float noise(long frame, int line) {
        long value = (frame ^ (line * 0x9E3779B9L)) * 0x45D9F3BL;
        value ^= value >>> 16;
        return (value & 1023L) / 511.5F - 1F;
    }

    private ChromaticTooltipText() { }
}
