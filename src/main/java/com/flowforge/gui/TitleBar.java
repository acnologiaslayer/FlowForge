package com.flowforge.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;

/**
 * A custom, fully theme-able window title bar used by the undecorated main
 * frame and the themed dialogs. Because those windows are undecorated, this
 * bar (and the window border) is painted by us, so the entire window,
 * including its title, follows the active {@link FlowTheme} rather than the
 * native OS chrome.
 * <p>
 * For top-level frames it offers minimise / maximise / close and
 * drag-to-move; for dialogs it shows only a close button.
 */
public class TitleBar extends JPanel {

    private final Window window;
    private final JLabel titleLabel;
    private final ChromeButton minimiseButton; // null for dialogs
    private final ChromeButton maximiseButton; // null for dialogs
    private final ChromeButton closeButton;

    private Point dragOffset;
    private Rectangle normalBounds;

    public TitleBar(Window window, String title) {
        this.window = window;
        boolean isFrame = window instanceof Frame;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
        setPreferredSize(new Dimension(10, 40));

        JLabel mark = new JLabel("\u2699 ");
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 16f));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 9));
        left.setOpaque(false);
        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        left.add(mark);
        left.add(titleLabel);
        this.markLabel = mark;
        add(left, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 6));
        buttons.setOpaque(false);

        if (isFrame) {
            minimiseButton = new ChromeButton(ChromeButton.Glyph.MINIMISE);
            maximiseButton = new ChromeButton(ChromeButton.Glyph.MAXIMISE);
            minimiseButton.addActionListener(e -> ((Frame) window).setExtendedState(Frame.ICONIFIED));
            maximiseButton.addActionListener(e -> toggleMaximise());
            buttons.add(minimiseButton);
            buttons.add(maximiseButton);
        } else {
            minimiseButton = null;
            maximiseButton = null;
        }

        closeButton = new ChromeButton(ChromeButton.Glyph.CLOSE);
        closeButton.addActionListener(e -> window.dispatchEvent(
                new WindowEvent(window, WindowEvent.WINDOW_CLOSING)));
        buttons.add(closeButton);
        add(buttons, BorderLayout.EAST);

        installDragSupport(isFrame);
    }

    private final JLabel markLabel;

    /** Recolours the bar and its buttons for the given palette. */
    public void applyPalette(FlowTheme.Palette palette) {
        setBackground(palette.surface());
        titleLabel.setForeground(palette.foreground());
        markLabel.setForeground(palette.accent());
        if (minimiseButton != null) {
            minimiseButton.applyPalette(palette, false);
            maximiseButton.applyPalette(palette, false);
        }
        closeButton.applyPalette(palette, true);
        repaint();
    }

    public void applyFont(Font font) {
        titleLabel.setFont(font);
        repaint();
    }

    private void toggleMaximise() {
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        Rectangle screen = gc.getBounds();
        Insets si = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usable = new Rectangle(screen.x + si.left, screen.y + si.top,
                screen.width - si.left - si.right, screen.height - si.top - si.bottom);

        if (normalBounds == null) {
            normalBounds = window.getBounds();
            window.setBounds(usable);
        } else {
            window.setBounds(normalBounds);
            normalBounds = null;
        }
        window.revalidate();
    }

    private void installDragSupport(boolean allowMaximise) {
        MouseAdapter press = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (allowMaximise && e.getClickCount() == 2) {
                    toggleMaximise();
                }
            }
        };
        MouseMotionAdapter drag = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset == null || normalBounds != null) {
                    return;
                }
                Point onScreen = e.getLocationOnScreen();
                window.setLocation(onScreen.x - dragOffset.x, onScreen.y - dragOffset.y);
            }
        };
        addMouseListener(press);
        addMouseMotionListener(drag);
        titleLabel.addMouseListener(press);
        titleLabel.addMouseMotionListener(drag);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Color accent = SwingUtilities.isEventDispatchThread()
                ? FlowTheme.active().palette().accent() : Color.GRAY;
        g2.setColor(accent);
        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
        g2.dispose();
    }
}
