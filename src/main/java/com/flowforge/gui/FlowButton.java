package com.flowforge.gui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A custom-painted, rounded, theme-aware button. It draws its own
 * background, hover and pressed states and an optional accent fill, so the
 * primary actions stand out against the themed chrome regardless of the
 * underlying Look&Feel.
 */
public class FlowButton extends JButton {

    /** PRIMARY = filled accent, SECONDARY = outlined, DANGER = filled danger. */
    public enum Style {PRIMARY, SECONDARY, DANGER}

    private final Style style;
    private FlowTheme.Palette palette = FlowTheme.active().palette();
    private boolean hovering;
    private boolean pressed;

    public FlowButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(foregroundFor());
        setFont(getFont().deriveFont(java.awt.Font.BOLD, 13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    public void applyPalette(FlowTheme.Palette palette) {
        this.palette = palette;
        setForeground(foregroundFor());
        repaint();
    }

    private Color baseColor() {
        return switch (style) {
            case PRIMARY -> palette.accent();
            case DANGER -> palette.danger();
            case SECONDARY -> palette.surface();
        };
    }

    private Color foregroundFor() {
        return switch (style) {
            case PRIMARY, DANGER -> palette.accentText();
            case SECONDARY -> palette.foreground();
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 12;

        Color base = baseColor();
        if (pressed) {
            base = FlowTheme.blend(base, Color.BLACK, 0.18f);
        } else if (hovering) {
            base = FlowTheme.blend(base, Color.WHITE, style == Style.SECONDARY ? 0.06f : 0.12f);
        }

        if (style == Style.SECONDARY) {
            g2.setColor(base);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(hovering ? palette.accent() : palette.border());
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        } else {
            // Subtle vertical gradient for depth on filled buttons.
            g2.setColor(base);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(FlowTheme.alpha(Color.WHITE, hovering ? 36 : 22));
            g2.fillRoundRect(0, 0, w, h / 2, arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 34);
        return d;
    }
}
