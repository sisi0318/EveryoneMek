package dev.everyonemek.overloadcore.client;

import dev.everyonemek.overloadcore.CoreConfig;
import dev.everyonemek.overloadcore.CoreContent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Compact, steady readings; the text effect belongs to the pendant tooltip. */
public final class CoreHud {
    public static void render(GuiGraphics gui, Font font, CompoundTag data) {
        int load = Math.max(0, data.getInt("load"));
        int limit = CoreConfig.METAL_LIMIT.get();
        int heat = Math.clamp(data.getInt("heat"), 0, 100);
        var metalLabel = CoreContent.text("hud.metal");
        var heatLabel = CoreContent.text("hud.heat");
        var metalValue = Component.literal(load + " / " + limit);
        var heatValue = Component.literal(heat + "%");
        int innerWidth = Math.max(116, Math.max(font.width(metalLabel) + font.width(metalValue),
              font.width(heatLabel) + font.width(heatValue)) + 12);
        int x = 8, y = Math.max(8, gui.guiHeight() - 104);
        int width = innerWidth + 12;
        int color = data.getBoolean("heavy") ? 0xFFEC717A : load >= limit * 2L / 3 ? 0xFFE8B66A : 0xFFBBA4E0;
        int heatColor = heat >= 80 ? 0xFFEC717A : heat >= 60 ? 0xFFF0A965 : 0xFFC98268;

        gui.fill(x, y, x + width, y + 44, 0xA312101B);
        // Short corner brackets leave the panel visually lighter than a machine window.
        int frame = 0xC16C5C83;
        gui.fill(x, y, x + 7, y + 1, frame);
        gui.fill(x, y, x + 1, y + 7, frame);
        gui.fill(x + width - 7, y, x + width, y + 1, frame);
        gui.fill(x + width - 1, y, x + width, y + 7, frame);
        gui.fill(x, y + 43, x + 7, y + 44, frame);
        gui.fill(x, y + 37, x + 1, y + 44, frame);
        gui.fill(x + width - 7, y + 43, x + width, y + 44, frame);
        gui.fill(x + width - 1, y + 37, x + width, y + 44, frame);

        meter(gui, font, x + 6, y + 5, innerWidth, metalLabel, metalValue, (double) load / limit, color);
        meter(gui, font, x + 6, y + 25, innerWidth, heatLabel, heatValue, heat / 100.0, heatColor);
    }

    private static void meter(GuiGraphics gui, Font font, int x, int y, int width, Component label,
          Component value, double fill, int color) {
        gui.drawString(font, label, x, y, 0xFFD8CBE5, false);
        gui.drawString(font, value, x + width - font.width(value), y, color, false);
        int barY = y + font.lineHeight + 2;
        gui.fill(x, barY, x + width, barY + 3, 0xC02B2638);
        int filled = (int) Math.ceil(width * Math.clamp(fill, 0, 1));
        if (filled > 0) gui.fill(x, barY, x + filled, barY + 3, color);
        for (int division = 1; division < 4; division++) {
            int markX = x + width * division / 4;
            gui.fill(markX, barY, markX + 1, barY + 3, 0xB0181422);
        }
    }

    private CoreHud() { }
}
