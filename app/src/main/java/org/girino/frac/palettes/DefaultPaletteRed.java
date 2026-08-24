package org.girino.frac.palettes;

public class DefaultPaletteRed implements PaletteProvider {

    public static final double gammaCorrection = 0.9;

    private static final int[] COLORS = PaletteLut.build(DefaultPaletteRed::colorAt);

    public int getColor(double value) {
        return PaletteLut.lookup(COLORS, value);
    }

    static int colorAt(double value) {
        int ratio = (int) (Math.pow(value, gammaCorrection) * 255);
        int ratio2 = (int) (Math.pow(value, gammaCorrection / 2.0) * 255);
        int ival = (int) (value * 255);
        return Argb.rgb(ratio2, ival, ratio);
    }
}
