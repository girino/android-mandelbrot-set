package org.girino.frac.palettes;

/** Purple and magenta through orange to gold. */
public class SunsetPalette implements PaletteProvider {

    private static final int[] STOPS = {
            Argb.BLACK,
            Argb.rgb(26, 0, 51),
            Argb.rgb(102, 0, 102),
            Argb.rgb(255, 51, 153),
            Argb.rgb(255, 153, 51),
            Argb.rgb(255, 204, 0),
    };

    private static final int[] COLORS = GradientStops.buildLut(STOPS);

    @Override
    public int getColor(double value) {
        return PaletteLut.lookup(COLORS, value);
    }

    static int colorAt(double value) {
        return GradientStops.sample(STOPS, value);
    }
}
