package dev.everyonemek.gravity.client;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import dev.everyonemek.gravity.Content;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.tooltip.TooltipUtils;

/** Editable draft; the adjacent value always comes from the synchronized menu. */
public final class PercentField extends GuiTextField {
    private final int maximum;
    private final IntSupplier confirmed;
    private final BooleanSupplier available;
    private final IntConsumer submit;
    private int lastConfirmed;

    public PercentField(IGuiWrapper gui, int x, int y, int width, int height, int maximum,
          IntSupplier confirmed, BooleanSupplier available, IntConsumer submit) {
        super(gui, x, y, width, height);
        this.maximum = maximum;
        this.confirmed = confirmed;
        this.available = available;
        this.submit = submit;
        lastConfirmed = confirmed.getAsInt();
        setMaxLength(3);
        setInputValidator(c -> c >= '0' && c <= '9');
        setText(Integer.toString(lastConfirmed));
        setEnterHandler(this::apply);
        addCheckmarkButton(this::apply);
        setResponder(ignored -> refresh());
        setTooltip(TooltipUtils.create(Content.text("percent_range", maximum)));
        refresh();
    }

    private int value() {
        try { return Integer.parseInt(getText()); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private void apply() {
        int value = value();
        if (available.getAsBoolean() && value >= 1 && value <= maximum) submit.accept(value);
    }

    private void refresh() {
        active = available.getAsBoolean();
        setEditable(active);
        int value = value();
        setTextColor(value >= 1 && value <= maximum ? 0xFFFFFF : 0xFF7777);
    }

    @Override public void tick() {
        super.tick();
        int next = confirmed.getAsInt();
        if (next != lastConfirmed) {
            // Do not erase a newer draft while a previous response is arriving.
            if (!isTextFieldFocused() || getText().equals(Integer.toString(lastConfirmed))) setText(Integer.toString(next));
            lastConfirmed = next;
        }
        refresh();
    }
}
