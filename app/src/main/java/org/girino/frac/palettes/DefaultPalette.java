package org.girino.frac.palettes;

public class DefaultPalette implements PaletteProvider {

    public static final double gammaCorrection = 0.99;

    private static final int[] COLORS = PaletteLut.build(DefaultPalette::colorAt);

    public int getColor(double value) {
        return PaletteLut.lookup(COLORS, value);
    }

    static int colorAt(double value) {
        float ratio = (float) Math.pow(value, gammaCorrection);
        return Argb.rgb((int) ((1f - ratio) * 255), (int) (ratio * 255), (int) (value * 255));
    }
}
