package com.flowforge.gui;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

/**
 * A themed replacement for {@link javax.swing.JOptionPane#showMessageDialog}.
 * It is an undecorated modal dialog with a custom {@link TitleBar}, so the
 * pop-up (including its title) follows the active {@link FlowTheme} instead
 * of the native OS window chrome.
 */
public final class MessageDialog {

    public enum Kind {INFO, SUCCESS, WARNING, ERROR}

    private MessageDialog() {
    }

    public static void show(Component parent, String title, String message, Kind kind) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        FlowTheme theme = FlowTheme.active();
        FlowTheme.Palette p = theme.palette();
        Color accent = switch (kind) {
            case ERROR -> p.danger();
            case WARNING -> p.danger();
            case SUCCESS -> p.success();
            case INFO -> p.accent();
        };

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createLineBorder(accent, 1));
        root.setBackground(p.background());
        dialog.setContentPane(root);

        TitleBar titleBar = new TitleBar(dialog, title);
        root.add(titleBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));
        JLabel icon = new JLabel(glyphFor(kind));
        icon.setFont(icon.getFont().deriveFont(Font.BOLD, 24f));
        icon.setForeground(accent);
        body.add(icon, BorderLayout.WEST);

        JLabel text = new JLabel("<html><body style='width:260px'>"
                + escapeHtml(message).replace("\n", "<br>") + "</body></html>");
        text.setForeground(p.foreground());
        text.setFont(theme.bodyFont(Font.PLAIN, 13f));
        body.add(text, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        bar.setOpaque(false);
        FlowButton ok = new FlowButton("OK", FlowButton.Style.PRIMARY);
        ok.applyPalette(p);
        ok.addActionListener(e -> dialog.dispose());
        bar.add(ok);
        root.add(bar, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);

        titleBar.applyPalette(p);
        titleBar.applyFont(theme.bodyFont(Font.BOLD, 14f));

        dialog.pack();
        dialog.setMinimumSize(new Dimension(340, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String glyphFor(Kind kind) {
        return switch (kind) {
            case ERROR -> "\u2716";
            case WARNING -> "\u26A0";
            case SUCCESS -> "\u2714";
            case INFO -> "\u2139";
        };
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
