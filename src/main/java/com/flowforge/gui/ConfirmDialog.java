package com.flowforge.gui;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

/**
 * A themed yes/no confirmation dialog, used for destructive actions such as
 * deleting a workflow. Like the other pop-ups it is undecorated with a
 * custom {@link TitleBar} so it follows the active {@link FlowTheme}.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    /** @return {@code true} if the user confirmed, {@code false} otherwise. */
    public static boolean ask(Component parent, String title, String message) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        FlowTheme theme = FlowTheme.active();
        FlowTheme.Palette p = theme.palette();

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createLineBorder(p.danger(), 1));
        root.setBackground(p.background());
        dialog.setContentPane(root);

        TitleBar titleBar = new TitleBar(dialog, title);
        root.add(titleBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));
        JLabel icon = new JLabel("\u26A0");
        icon.setFont(icon.getFont().deriveFont(Font.BOLD, 24f));
        icon.setForeground(p.danger());
        body.add(icon, BorderLayout.WEST);
        JLabel text = new JLabel("<html><body style='width:260px'>" + message + "</body></html>");
        text.setForeground(p.foreground());
        text.setFont(theme.bodyFont(Font.PLAIN, 13f));
        body.add(text, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        final boolean[] result = {false};
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        bar.setOpaque(false);
        FlowButton cancel = new FlowButton("Cancel", FlowButton.Style.SECONDARY);
        FlowButton confirm = new FlowButton("Delete", FlowButton.Style.DANGER);
        cancel.applyPalette(p);
        confirm.applyPalette(p);
        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        bar.add(cancel);
        bar.add(confirm);
        root.add(bar, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(cancel);

        titleBar.applyPalette(p);
        titleBar.applyFont(theme.bodyFont(Font.BOLD, 14f));

        dialog.pack();
        dialog.setMinimumSize(new Dimension(360, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }
}
