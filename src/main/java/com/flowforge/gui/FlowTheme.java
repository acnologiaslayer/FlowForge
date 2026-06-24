package com.flowforge.gui;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Font;

/**
 * The visual themes FlowForge offers. Each theme bundles a {@link Palette}
 * that paints the custom chrome (title bar, sidebar, buttons, run log) and,
 * for the dark themes, overrides Swing's Nimbus colour keys so the standard
 * widgets (text fields, tables, scroll bars) match too.
 * <p>
 * Keeping the colours in one enum means the whole application restyles from
 * a single switch in the Theme menu, and adding a new theme is a one-line
 * change here.
 */
public enum FlowTheme {

    LIGHT("Light", new Palette(
            new Color(0xF4F5F7), new Color(0xFFFFFF), new Color(0x1B1F24),
            new Color(0x2D6CDF), Color.WHITE, new Color(0x18A957), new Color(0xD64545),
            new Color(0xC9CED6))),

    MIDNIGHT("Midnight", new Palette(
            new Color(0x10141C), new Color(0x1A2130), new Color(0xE6ECF5),
            new Color(0x4F8CFF), new Color(0x0B0F16), new Color(0x35D08A), new Color(0xFF5C6C),
            new Color(0x2C3647))),

    SYNTHWAVE("Synthwave", new Palette(
            new Color(0x140A22), new Color(0x21123A), new Color(0xF2E9FF),
            new Color(0x00E5FF), new Color(0x0B0418), new Color(0x6CFFB0), new Color(0xFF2A8D),
            new Color(0x3A1E5E))),

    FOREST("Forest", new Palette(
            new Color(0x0F1A14), new Color(0x16271D), new Color(0xE7F3EA),
            new Color(0x4ADE80), new Color(0x07120B), new Color(0x9BE15D), new Color(0xF0743A),
            new Color(0x24402F)));

    /**
     * The colour roles used across the UI.
     *
     * @param background main window background
     * @param surface    raised surfaces (title bar, sidebar, cards)
     * @param foreground primary text colour
     * @param accent     highlight colour (titles, primary buttons, selection)
     * @param accentText text drawn on top of the accent colour
     * @param success    success state (passed steps)
     * @param danger     destructive / failure state (delete, failed steps)
     * @param border     subtle separators and outlines
     */
    public record Palette(Color background, Color surface, Color foreground,
                          Color accent, Color accentText, Color success,
                          Color danger, Color border) {
    }

    private final String label;
    private final Palette palette;

    FlowTheme(String label, Palette palette) {
        this.label = label;
        this.palette = palette;
    }

    public String getLabel() {
        return label;
    }

    public Palette palette() {
        return palette;
    }

    public boolean isDark() {
        return this != LIGHT;
    }

    /** The display font for headings and the app title. */
    public Font headingFont(int style, float size) {
        return new Font("SansSerif", style, (int) size);
    }

    /** The font for body text and chrome. */
    public Font bodyFont(int style, float size) {
        return new Font("SansSerif", style, (int) size);
    }

    public static FlowTheme fromLabel(String label) {
        for (FlowTheme theme : values()) {
            if (theme.label.equals(label)) {
                return theme;
            }
        }
        return MIDNIGHT;
    }

    /** The theme currently installed application-wide. */
    private static FlowTheme active = MIDNIGHT;

    public static FlowTheme active() {
        return active;
    }

    /** Installs this theme's Look&Feel and colour overrides into the UIManager. */
    public void apply() throws Exception {
        UIManager.setLookAndFeel(nimbusClassName());
        installColors();
        active = this;
    }

    private void installColors() {
        Palette p = palette;
        // Core Nimbus keys.
        UIManager.put("control", new Color(p.surface().getRGB()));
        UIManager.put("info", p.surface());
        UIManager.put("nimbusBase", isDark() ? darken(p.surface(), 0.15f) : new Color(0xD6D9DF));
        UIManager.put("nimbusBlueGrey", p.surface());
        UIManager.put("nimbusLightBackground", p.background());
        UIManager.put("background", p.background());
        UIManager.put("text", p.foreground());
        UIManager.put("nimbusFocus", p.accent());
        UIManager.put("nimbusSelection", p.accent());
        UIManager.put("nimbusSelectionBackground", p.accent());
        UIManager.put("nimbusSelectedText", p.accentText());

        put("Panel.background", p.background());
        put("Label.foreground", p.foreground());
        put("TextField.background", isDark() ? darken(p.surface(), 0.1f) : Color.WHITE);
        put("TextField.foreground", p.foreground());
        put("TextArea.background", isDark() ? darken(p.surface(), 0.1f) : Color.WHITE);
        put("TextArea.foreground", p.foreground());
        put("ComboBox.background", p.surface());
        put("ComboBox.foreground", p.foreground());
        put("List.background", p.background());
        put("List.foreground", p.foreground());
        put("List.selectionBackground", p.accent());
        put("List.selectionForeground", p.accentText());
        put("ScrollPane.background", p.background());
        put("Viewport.background", p.background());
        put("ScrollBar.thumb", p.border());
        put("CheckBox.background", p.surface());
        put("CheckBox.foreground", p.foreground());
        put("ToolTip.background", p.surface());
        put("ToolTip.foreground", p.foreground());

        UIManager.put("TextField.caretForeground", p.foreground());
        UIManager.put("TextArea.caretForeground", p.foreground());
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    private static Color darken(Color c, float amount) {
        return new Color(
                Math.max(0, (int) (c.getRed() * (1 - amount))),
                Math.max(0, (int) (c.getGreen() * (1 - amount))),
                Math.max(0, (int) (c.getBlue() * (1 - amount))));
    }

    static Color lighten(Color c, float amount) {
        return new Color(
                Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * amount)),
                Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * amount)),
                Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * amount)));
    }

    /** Blends two colours; {@code ratio} 0 returns a, 1 returns b. */
    static Color blend(Color a, Color b, float ratio) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * ratio),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * ratio),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * ratio));
    }

    /** Returns a translucent copy of a colour. */
    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static String nimbusClassName() throws Exception {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                return info.getClassName();
            }
        }
        throw new Exception("Nimbus Look&Feel is not available on this JVM.");
    }
}
