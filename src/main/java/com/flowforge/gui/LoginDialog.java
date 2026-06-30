package com.flowforge.gui;

import com.flowforge.exception.AuthenticationException;
import com.flowforge.exception.PersistenceException;
import com.flowforge.model.User;
import com.flowforge.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/** Login/register dialog shown before the main multi-user app opens. */
public class LoginDialog {

    private final AuthService authService;
    private User user;

    public LoginDialog(AuthService authService) {
        this.authService = authService;
    }

    public User show(Component parent) {
        Window owner = parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "FlowForge Login", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        FlowTheme.Palette p = FlowTheme.active().palette();
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(p.background());
        root.setBorder(BorderFactory.createLineBorder(p.accent(), 1));
        dialog.setContentPane(root);

        TitleBar titleBar = new TitleBar(dialog, "FlowForge  -  Login");
        titleBar.applyPalette(p);
        titleBar.applyFont(FlowTheme.active().headingFont(Font.BOLD, 14f));
        root.add(titleBar, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 6, 7, 6);
        c.anchor = GridBagConstraints.WEST;

        JLabel hint = new JLabel(firstUserHint());
        hint.setForeground(p.foreground());
        hint.setFont(FlowTheme.active().bodyFont(Font.PLAIN, 12f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        form.add(hint, c);

        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        addRow(form, c, 1, "Username", username, p);
        addRow(form, c, 2, "Password", password, p);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actions.setOpaque(false);
        FlowButton register = new FlowButton("Register", FlowButton.Style.SECONDARY);
        FlowButton login = new FlowButton("Login", FlowButton.Style.PRIMARY);
        register.applyPalette(p);
        login.applyPalette(p);
        actions.add(register);
        actions.add(login);
        root.add(actions, BorderLayout.SOUTH);

        register.addActionListener(e -> tryRegister(dialog, username.getText(), password.getPassword()));
        login.addActionListener(e -> tryLogin(dialog, username.getText(), password.getPassword()));
        dialog.getRootPane().setDefaultButton(login);

        dialog.pack();
        dialog.setSize(Math.max(430, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return user;
    }

    private String firstUserHint() {
        try {
            return authService.hasUsers()
                    ? "Log in to your FlowForge account."
                    : "No users yet. Register the first account to start.";
        } catch (PersistenceException e) {
            return "Log in or register to continue.";
        }
    }

    private void tryLogin(JDialog dialog, String username, char[] password) {
        try {
            user = authService.login(username, password);
            dialog.dispose();
        } catch (AuthenticationException | PersistenceException e) {
            MessageDialog.show(dialog, "Login failed", e.getMessage(), MessageDialog.Kind.ERROR);
        }
    }

    private void tryRegister(JDialog dialog, String username, char[] password) {
        try {
            user = authService.register(username, password);
            dialog.dispose();
        } catch (AuthenticationException | PersistenceException e) {
            MessageDialog.show(dialog, "Registration failed", e.getMessage(), MessageDialog.Kind.ERROR);
        }
    }

    private static void addRow(JPanel form, GridBagConstraints c, int row, String label,
                               javax.swing.JComponent field, FlowTheme.Palette p) {
        c.gridwidth = 1;
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JLabel l = new JLabel(label);
        l.setForeground(p.foreground());
        l.setFont(FlowTheme.active().bodyFont(Font.BOLD, 12f));
        form.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }
}
