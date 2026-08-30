package org.girino.frac.palettes;

/** Saturated multi-cycle hue (neon bands with smooth coloring). */
public class NeonPalette implements PaletteProvider {

    private static final float SATURATION = 1.0f;
    private static final float BRIGHTNESS = 1.0f;
    private static final double CYCLES = 2.5;

    private static final int[] COLORS = PaletteLut.build(NeonPalette::colorAt);

    @Override
    public int getColor(double value) {
        return PaletteLut.lookup(COLORS, value);
    }

    static int colorAt(double value) {
        float[] hsv = new float[] {0, SATURATION, BRIGHTNESS};
        double hue = CYCLES * value;
        hue -= Math.floor(hue);
        hsv[0] = (float) (hue * 360.0);
        return Argb.hsvToColor(hsv);
    }
}
