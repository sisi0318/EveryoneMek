package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.Controller;
import java.util.Locale;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.bar.GuiBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Uses the same narrow Mek bar frame as the Metallurgic Infuser. */
public final class GuiResourceBar extends GuiBar<GuiBar.IBarInfoHandler> {
    private static final int[] COLORS = {0xFFE7CB62, 0xFF82DDE3, 0xFFC7464B, 0xFF95CC5F};
    private final Controller controller;
    private final int resource;
    public GuiResourceBar(IGuiWrapper gui, Controller controller, int resource, int x, int y) {
        super(BAR, gui, new IBarInfoHandler() {
            @Override public double getLevel() { return controller.capacities[resource] <= 0 ? 0 : Math.clamp(controller.observed[resource] / (double) controller.capacities[resource], 0, 1); }
            @Override public Component getTooltip() {
                return MachineScreen.text("resource_storage", MachineScreen.text("resource." + resource), controller.observed[resource], controller.capacities[resource]).copy()
                      .append("\n").append(MachineScreen.text("production_rate", rate(controller.resourceRates[resource])));
            }
        }, x, y, 4, 52, false);
        this.controller = controller; this.resource = resource;
    }
    private static String rate(double value) { return String.format(Locale.ROOT, "%.1f", value).replaceFirst("\\.0$", ""); }
    @Override protected void renderBarOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, double level) {
        int filled = calculateScaled(level, height - 2);
        graphics.fill(relativeX + 1, relativeY + height - 1 - filled, relativeX + width - 1, relativeY + height - 1, COLORS[resource]);
    }
    @Override public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        graphics.pose().pushPose();
        graphics.pose().translate(relativeX + 8, relativeY + 8, 0);
        graphics.pose().scale(.75F, .75F, 1);
        graphics.drawString(font(), MachineScreen.text("resource." + resource), 0, 0, titleTextColor(), false);
        graphics.drawString(font(), MachineScreen.text("production_rate", rate(controller.resourceRates[resource])), 0, 18, titleTextColor(), false);
        graphics.pose().popPose();
    }
}
