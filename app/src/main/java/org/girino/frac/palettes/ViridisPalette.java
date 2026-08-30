package org.girino.frac.palettes;

/** Viridis-inspired purple-teal-green-yellow gradient. */
public class ViridisPalette implements PaletteProvider {

    private static final int[] STOPS = {
            Argb.rgb(68, 1, 84),
            Argb.rgb(65, 68, 135),
            Argb.rgb(42, 120, 142),
            Argb.rgb(34, 168, 132),
            Argb.rgb(122, 209, 81),
            Argb.rgb(253, 231, 37),
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
