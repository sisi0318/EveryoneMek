package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.Controller;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.bar.GuiBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Horizontal Mek bars for one soul timer and the two native processing lanes. */
public final class GuiClibanoBar extends GuiBar<GuiBar.IBarInfoHandler> {
    private final Controller controller;
    private final int lane;
    public GuiClibanoBar(IGuiWrapper gui, Controller controller, int lane, int x, int y, int width) {
        super(BAR, gui, new IBarInfoHandler() {
            @Override public double getLevel() {
                if (controller.observed[0] < 0) return 0;
                if (lane < 0) return Math.clamp(controller.observed[0] / (double) Math.max(1, controller.soulDuration), 0, 1);
                return controller.processingInputs[lane].isEmpty() ? 0
                      : Math.clamp(controller.observed[3 + lane] / (double) Math.max(1, controller.observed[5 + lane]), 0, 1);
            }
            @Override public Component getTooltip() {
                if (controller.observed[0] < 0) return MachineScreen.text("unmeasured");
                if (lane < 0) return MachineScreen.text("soul_time", (controller.observed[0] + 19L) / 20, (controller.soulDuration + 19L) / 20);
                if (controller.processingInputs[lane].isEmpty()) return MachineScreen.text("status.0");
                return MachineScreen.text("processing_item", controller.processingInputs[lane].getHoverName(), Math.round(getLevel() * 100));
            }
        }, x, y, width, 8, true);
        this.controller = controller; this.lane = lane;
    }
    @Override protected void renderBarOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, double level) {
        int filled = calculateScaled(level, width - 2);
        int color = lane < 0 ? 0xFF62CBD2 : 0xFF70BA83;
        graphics.fill(relativeX + 1, relativeY + 1, relativeX + 1 + filled, relativeY + height - 1, color);
    }
    @Override public void tick() {
        super.tick();
        visible = lane != 1 || controller.observed[9] != 1;
    }
}
