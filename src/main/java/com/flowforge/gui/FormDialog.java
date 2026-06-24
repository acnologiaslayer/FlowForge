package com.flowforge.gui;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A reusable, themed, undecorated modal form dialog. Callers describe the
 * fields they want with {@link #addText}, {@link #addTextArea},
 * {@link #addCombo} and {@link #addCheckBox}; {@link #showDialog()} returns
 * the entered values keyed by field name, or {@code null} if cancelled.
 * <p>
 * Centralising the form layout here keeps the main GUI free of repetitive
 * dialog-building boilerplate and guarantees every pop-up shares the same
 * look.
 */
public class FormDialog {

    private interface Field {
        String key();

        String value();

        JComponent component();

        String label();
    }

    private final Component parent;
    private final String title;
    private final List<Field> fields = new ArrayList<>();
    private boolean confirmed;

    public FormDialog(Component parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    public FormDialog addText(String key, String label, String initial) {
        JTextField field = new JTextField(initial == null ? "" : initial, 22);
        fields.add(new Field() {
            public String key() {
                return key;
            }

            public String value() {
                return field.getText();
            }

            public JComponent component() {
                return field;
            }

            public String label() {
                return label;
            }
        });
        return this;
    }

    public FormDialog addTextArea(String key, String label, String initial) {
        JTextArea area = new JTextArea(initial == null ? "" : initial, 4, 22);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        fields.add(new Field() {
            public String key() {
                return key;
            }

            public String value() {
                return area.getText();
            }

            public JComponent component() {
                return scroll;
            }

            public String label() {
                return label;
            }
        });
        return this;
    }

    public FormDialog addCombo(String key, String label, String[] options, String initial) {
        JComboBox<String> combo = new JComboBox<>(options);
        if (initial != null) {
            combo.setSelectedItem(initial);
        }
        fields.add(new Field() {
            public String key() {
                return key;
            }

            public String value() {
                Object selected = combo.getSelectedItem();
                return selected == null ? "" : selected.toString();
            }

            public JComponent component() {
                return combo;
            }

            public String label() {
                return label;
            }
        });
        return this;
    }

    public FormDialog addCheckBox(String key, String label, boolean initial) {
        JCheckBox box = new JCheckBox();
        box.setSelected(initial);
        box.setOpaque(false);
        fields.add(new Field() {
            public String key() {
                return key;
            }

            public String value() {
                return String.valueOf(box.isSelected());
            }

            public JComponent component() {
                return box;
            }

            public String label() {
                return label;
            }
        });
        return this;
    }

    /**
     * Shows the dialog modally.
     *
     * @return a map of field key to entered value, or {@code null} if the
     * user cancelled
     */
    public Map<String, String> showDialog() {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        FlowTheme theme = FlowTheme.active();
        FlowTheme.Palette p = theme.palette();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(p.background());
        root.setBorder(BorderFactory.createLineBorder(p.accent(), 1));
        dialog.setContentPane(root);

        TitleBar titleBar = new TitleBar(dialog, title);
        root.add(titleBar, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        for (Field field : fields) {
            c.gridx = 0;
            c.gridy = row;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(field.label());
            label.setForeground(p.foreground());
            label.setFont(theme.bodyFont(Font.BOLD, 12f));
            form.add(label, c);

            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            form.add(field.component(), c);
            row++;
        }
        root.add(form, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        bar.setOpaque(false);
        FlowButton cancel = new FlowButton("Cancel", FlowButton.Style.SECONDARY);
        FlowButton ok = new FlowButton("OK", FlowButton.Style.PRIMARY);
        cancel.applyPalette(p);
        ok.applyPalette(p);
        cancel.addActionListener(e -> {
            confirmed = false;
            dialog.dispose();
        });
        ok.addActionListener(e -> {
            confirmed = true;
            dialog.dispose();
        });
        bar.add(cancel);
        bar.add(ok);
        root.add(bar, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);

        titleBar.applyPalette(p);
        titleBar.applyFont(theme.bodyFont(Font.BOLD, 14f));

        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        if (!confirmed) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : fields) {
            values.put(field.key(), field.value());
        }
        return values;
    }
}
