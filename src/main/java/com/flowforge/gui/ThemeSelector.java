package com.flowforge.gui;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * A compact theme picker shown in the status bar. Clicking it pops up a
 * menu of the available {@link FlowTheme}s; choosing one invokes the
 * supplied callback so the whole application restyles immediately.
 */
public class ThemeSelector extends JButton {

    private final Consumer<FlowTheme> onSelect;

    public ThemeSelector(Consumer<FlowTheme> onSelect) {
        super("\u25C6 Theme");
        this.onSelect = onSelect;
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(110, 26));
        addActionListener(e -> showMenu());
    }

    public void applyPalette(FlowTheme.Palette palette) {
        setBackground(palette.surface());
        setForeground(palette.accent());
    }

    private void showMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (FlowTheme theme : FlowTheme.values()) {
            JMenuItem item = new JMenuItem(theme.getLabel()
                    + (theme == FlowTheme.active() ? "  \u2714" : ""));
            item.addActionListener(e -> onSelect.accept(theme));
            menu.add(item);
        }
        menu.show(this, 0, -menu.getPreferredSize().height);
    }
}
