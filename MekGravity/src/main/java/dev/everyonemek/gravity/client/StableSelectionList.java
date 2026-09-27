package dev.everyonemek.gravity.client;

import java.util.List;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.scroll.GuiTextScrollList;

/** Keep selection by identity when a server refresh changes row order or labels. */
public final class StableSelectionList<K> extends GuiTextScrollList {
    private List<K> keys = List.of();
    private List<String> labels = List.of();

    public StableSelectionList(IGuiWrapper gui, int x, int y, int width, int height) {
        super(gui, x, y, width, height);
    }

    public void update(List<K> nextKeys, List<String> nextLabels, K current) {
        int index = getSelection();
        K selected = index >= 0 && index < keys.size() ? keys.get(index) : current;
        if (!keys.equals(nextKeys) || !labels.equals(nextLabels)) {
            keys = List.copyOf(nextKeys);
            labels = List.copyOf(nextLabels);
            setText(labels);
        }
        int next = selected == null ? -1 : keys.indexOf(selected);
        if (next < 0 && current != null) next = keys.indexOf(current);
        if (next >= 0) setSelected(next); else clearSelection();
    }
}
