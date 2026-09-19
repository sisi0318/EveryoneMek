package dev.everyonemek.factory.client;

import dev.everyonemek.factory.*;
import java.util.List;
import java.util.Locale;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.gauge.*;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

/** Read-only gauges over the menu's synchronized warehouse, never a second inventory. */
public final class WarehouseResourcesWindow extends GuiWindow {
    private final boolean chemicals;
    private final WarehouseMenu menu;
    public WarehouseResourcesWindow(IGuiWrapper gui, WarehouseMenu menu, boolean chemicals) {
        super(gui, 0, 20, 176, 118, WindowType.UNSPECIFIED);
        this.chemicals = chemicals; this.menu = menu;
        var type = GaugeType.MEDIUM.with(menu.output ? DataType.OUTPUT : DataType.INPUT);
        for (int i = 0; i < Buffers.TANKS; i++) {
            final int tankIndex = i;
            int x = relativeX + 12 + 40 * i, y = relativeY + 36;
            if (chemicals) {
                var tank = new ChemicalView(menu, i);
                addChild(new GuiChemicalGauge(() -> tank, List::of, type, gui, x, y) {
                    @Override public void onClick(double mx, double my, int button) { }
                    @Override public int getScaledLevel() { return Math.min(height - 2, super.getScaledLevel()); }
                    @Override public List<Component> getTooltipText() { return contents(tankIndex); }
                }.setLabel(Content.text("tank_number", i + 1)));
            } else {
                var tank = new FluidView(menu, i);
                addChild(new GuiFluidGauge(() -> tank, List::of, type, gui, x, y) {
                    @Override public void onClick(double mx, double my, int button) { }
                    @Override public int getScaledLevel() { return Math.min(height - 2, super.getScaledLevel()); }
                    @Override public List<Component> getTooltipText() { return contents(tankIndex); }
                }.setLabel(Content.text("tank_number", i + 1)));
            }
        }
    }
    private long amount(int i) { return chemicals ? menu.stock.chemicals[i].getAmount() : menu.stock.fluids[i].getAmount(); }
    private List<Component> contents(int i) {
        Component name;
        if (chemicals) { var stack = menu.stock.chemicals[i]; name = stack.isEmpty() ? Content.text("empty") : stack.getTextComponent(); }
        else { var stack = menu.stock.fluids[i]; name = stack.isEmpty() ? Content.text("empty") : Component.translatable(stack.getDescriptionId()); }
        return List.of(name, Content.text(chemicals ? "tank_chemical_amount" : "tank_fluid_amount", TextUtils.format(amount(i)), TextUtils.format(menu.tankCapacity)));
    }
    @Override public void renderForeground(GuiGraphics g, int x, int y) {
        super.renderForeground(g, x, y); drawTitleText(g, Content.text(chemicals ? "chemicals" : "fluids"), 5);
        for (int i = 0; i < Buffers.TANKS; i++) {
            drawScaledScrollingString(g, Content.text("tank_number", i + 1), relativeX + 12 + 40 * i, relativeY + 24,
                  relativeX + 46 + 40 * i, relativeY + 34, TextAlignment.CENTER, titleTextColor(), false, 0.8F, getTimeOpened());
            var percent = String.format(Locale.ROOT, "%.0f%%", amount(i) * 100D / Math.max(1, menu.tankCapacity));
            drawScaledScrollingString(g, Component.literal(percent), relativeX + 12 + 40 * i, relativeY + 99,
                  relativeX + 46 + 40 * i, relativeY + 111, TextAlignment.CENTER, titleTextColor(), false, 0.8F, getTimeOpened());
        }
    }
    private record FluidView(WarehouseMenu menu, int index) implements IExtendedFluidTank {
        public FluidStack getFluid() { return menu.stock.fluids[index]; }
        public int getCapacity() { return (int) Math.min(Integer.MAX_VALUE, menu.tankCapacity); }
        public boolean isFluidValid(FluidStack stack) { return false; }
        public FluidStack insert(FluidStack stack, Action action, AutomationType automation) { return stack; }
        public FluidStack extract(int amount, Action action, AutomationType automation) { return FluidStack.EMPTY; }
        public void setStack(FluidStack stack) { throw new UnsupportedOperationException("Display only"); }
        public void setStackUnchecked(FluidStack stack) { setStack(stack); }
        public void onContentsChanged() { }
    }
    private record ChemicalView(WarehouseMenu menu, int index) implements IChemicalTank {
        public ChemicalStack getStack() { return menu.stock.chemicals[index]; }
        public long getCapacity() { return menu.tankCapacity; }
        public boolean isValid(ChemicalStack stack) { return false; }
        public ChemicalStack insert(ChemicalStack stack, Action action, AutomationType automation) { return stack; }
        public ChemicalStack extract(long amount, Action action, AutomationType automation) { return ChemicalStack.EMPTY; }
        public void setStack(ChemicalStack stack) { throw new UnsupportedOperationException("Display only"); }
        public void setStackUnchecked(ChemicalStack stack) { setStack(stack); }
        public void onContentsChanged() { }
    }
}
