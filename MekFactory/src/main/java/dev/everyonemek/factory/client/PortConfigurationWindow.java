package dev.everyonemek.factory.client;

import dev.everyonemek.factory.Content;
import dev.everyonemek.factory.Controller;
import mekanism.api.RelativeSide;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.BasicColorButton;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Uses native Mek widgets, while routing changes through the active factory menu and real shell ports. */
public final class PortConfigurationWindow extends GuiWindow {
    private final Controller tile;
    private final int menuId;

    public PortConfigurationWindow(IGuiWrapper gui, Controller tile, int menuId) {
        super(gui, 30, 18, 184, 152, WindowType.UNSPECIFIED);
        this.tile = tile;
        this.menuId = menuId;
        addChild(new MekanismButton(gui, relativeX + 8, relativeY + 25, 168, 16, ejectLabel(),
              (button, x, y) -> send(46)) {
            @Override public void tick() { super.tick(); setMessage(ejectLabel()); }
        });
        addSide(RelativeSide.TOP, 78, 47);
        addSide(RelativeSide.LEFT, 55, 70);
        addSide(RelativeSide.FRONT, 78, 70);
        addSide(RelativeSide.RIGHT, 101, 70);
        addSide(RelativeSide.BACK, 55, 93);
        addSide(RelativeSide.BOTTOM, 78, 93);
        addChild(new GuiInnerScreen(gui, relativeX + 8, relativeY + 125, 168, 16,
              () -> java.util.List.of(Content.text("port_face_hint"))));
    }

    private Component ejectLabel() { return Content.text(tile.autoEject ? "auto_eject_on" : "auto_eject_off"); }
    private boolean send(int action) { Minecraft.getInstance().gameMode.handleInventoryButtonClick(menuId, action); return true; }

    private void addSide(RelativeSide side, int x, int y) {
        int n = side.ordinal();
        addChild(new BasicColorButton(gui(), relativeX + x, relativeY + y, 22,
              () -> tile.portInputs[n] > 0 ? tile.portOutputs[n] > 0 ? EnumColor.PURPLE : EnumColor.DARK_RED
                    : tile.portOutputs[n] > 0 ? EnumColor.DARK_BLUE : tile.portConverters[n]>0?EnumColor.PURPLE:null,
              (button, mx, my) -> send(40 + n), (button, mx, my) -> send(40 + n)) {
            { refreshState(); }
            private void refreshState() { active = tile.portInputs[n] + tile.portOutputs[n] > 0; }
            @Override public void tick() { super.tick(); refreshState(); }
            @Override public boolean isMouseOver(double mx,double my) {
                // Fixed converters still have a tooltip; the inactive button cannot change their mode.
                return super.isMouseOver(mx,my)||visible&&tile.portConverters[n]>0&&mx>=getX()&&mx<getX()+getWidth()&&my>=getY()&&my<getY()+getHeight();
            }
            @Override public void updateTooltip(int mouseX, int mouseY) {
                var lines=new java.util.ArrayList<Component>();lines.add(Component.translatable(side.getTranslationKey()));
                lines.add(Content.text("port_counts",tile.portInputs[n],tile.portOutputs[n]));
                if(tile.portConverters[n]>0)lines.add(Content.text("converter_face",tile.portConverters[n]));
                lines.add(Content.text("port_corner_hint"));setTooltip(TooltipUtils.create(lines));
            }
            @Override public void drawBackground(GuiGraphics g, int mx, int my, float partialTicks) {
                super.drawBackground(g, mx, my, partialTicks);
                if (tile.portInputs[n] + tile.portOutputs[n] + tile.portConverters[n] > 0) {
                    GuiUtils.renderItem(g, new ItemStack((tile.portInputs[n]+tile.portOutputs[n]==0?Content.CONVERTERS:Content.PORTS).get(tile.grade())), getRelativeX() + 3, getRelativeY() + 3, 1, font(), null, true);
                }
            }
        });
    }

    @Override public void renderForeground(GuiGraphics g, int x, int y) {
        super.renderForeground(g, x, y);
        drawTitleText(g, Content.text("port_config"), 5);
    }
}
