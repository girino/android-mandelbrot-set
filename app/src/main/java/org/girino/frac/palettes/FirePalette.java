package org.girino.frac.palettes;

/** Black to red, orange, and yellow (inferno-style). */
public class FirePalette implements PaletteProvider {

    private static final int[] STOPS = {
            Argb.BLACK,
            Argb.rgb(64, 0, 0),
            Argb.rgb(204, 0, 0),
            Argb.rgb(255, 102, 0),
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
