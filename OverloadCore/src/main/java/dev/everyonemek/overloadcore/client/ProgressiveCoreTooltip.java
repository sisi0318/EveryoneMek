package dev.everyonemek.overloadcore.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;

/**
 * Native tooltip content: stable completed lines and a brief bright writing edge on the newest line.
 * 字体渲染效果参考作者：huige233。
 * 参考来源：com.huige233.autism_and_insomnia.client.DreamJournalClientTooltipComponent
 * 中的打字机与扫描提亮效果；本类按 Overload Core 的逐行提示需求独立实现。
 */
public final class ProgressiveCoreTooltip implements TooltipComponent, ClientTooltipComponent {
    private record Row(FormattedCharSequence text, int line, int offset, int glyphs) { }
    private final List<Component> lines;
    private final long elapsed;
    private final int maxWidth;
    private final List<Row> rows = new ArrayList<>();
    private final int[] glyphCounts;
    private Font layoutFont;
    private int width, height;

    public ProgressiveCoreTooltip(List<Component> lines, long elapsed, int maxWidth) {
        this.lines = List.copyOf(lines);
        this.elapsed = elapsed;
        this.maxWidth = Math.max(8, maxWidth);
        glyphCounts = new int[lines.size()];
    }

    private void layout(Font font) {
        if (layoutFont == font) return;
        layoutFont = font;
        rows.clear();
        width = 0;
        height = 0;
        int visible = TooltipReveal.visibleLines(elapsed, lines.size());
        for (int line = 0; line < lines.size(); line++) {
            int offset = 0;
            for (var text : font.split(lines.get(line), maxWidth - 4)) {
                int count = glyphs(text);
                rows.add(new Row(text, line, offset, count));
                offset += count;
                // Measure all lines up front so the tooltip does not widen while typing.
                width = Math.max(width, font.width(text) + 4);
                if (line < visible) height += font.lineHeight + 1;
            }
            glyphCounts[line] = offset;
        }
    }

    @Override public int getWidth(Font font) {
        layout(font);
        return width;
    }

    @Override public int getHeight() {
        if (layoutFont == null) layout(Minecraft.getInstance().font);
        return height;
    }

    @Override public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        layout(font);
        int visibleLines = TooltipReveal.visibleLines(elapsed, lines.size());
        int rowY = y;
        for (var row : rows) {
            if (row.line >= visibleLines) break;
            int shown = TooltipReveal.visibleGlyphs(elapsed, row.line, glyphCounts[row.line]);
            int localShown = Math.clamp(shown - row.offset, 0, row.glyphs);
            boolean writing = TooltipReveal.writing(elapsed, row.line);
            var text = prefix(row.text, localShown, writing);
            font.drawInBatch(text, x, rowY, 0xFFFFFFFF, true, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            if (writing && shown > row.offset && shown <= row.offset + row.glyphs) {
                font.drawInBatch("_", x + font.width(text), rowY, 0xFFDDDDDD, false, matrix, buffer,
                      Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            rowY += font.lineHeight + 1;
        }
    }

    private static int glyphs(FormattedCharSequence text) {
        int[] count = {0};
        text.accept((index, style, codePoint) -> { count[0]++; return true; });
        return count[0];
    }

    private static FormattedCharSequence prefix(FormattedCharSequence text, int shown, boolean writing) {
        return sink -> {
            int[] position = {0};
            return text.accept((index, style, codePoint) -> {
                int at = position[0]++;
                if (at >= shown) return false;
                if (writing && at >= shown - 2) {
                    int color = style.getColor() == null ? 0xAAAAAA : style.getColor().getValue();
                    int r = ((color >> 16) & 255), g = ((color >> 8) & 255), b = (color & 255);
                    style = style.withColor(((r + (255 - r) / 2) << 16) | ((g + (255 - g) / 2) << 8) | (b + (255 - b) / 2));
                }
                return sink.accept(index, style, codePoint);
            });
        };
    }
}
