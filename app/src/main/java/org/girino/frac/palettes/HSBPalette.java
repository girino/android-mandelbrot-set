package org.girino.frac.palettes;

public class HSBPalette implements PaletteProvider {

    private static final int[] COLORS = PaletteLut.build(HSBPalette::colorAt);

    public int getColor(double value) {
        return PaletteLut.lookup(COLORS, value);
    }

    static int colorAt(double value) {
        float[] hsv = new float[] {0, 0.9f, 0.9f};
        double hue = 0.9 * value;
        hue -= Math.floor(hue);
        hue *= 360;
        hsv[0] = (float) hue;
        return Argb.hsvToColor(hsv);
    }
}
