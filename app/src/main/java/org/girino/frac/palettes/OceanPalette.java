package org.girino.frac.palettes;

/** Deep navy through cyan to pale blue-white. */
public class OceanPalette implements PaletteProvider {

    private static final int[] STOPS = {
            Argb.BLACK,
            Argb.rgb(0, 17, 51),
            Argb.rgb(0, 68, 136),
            Argb.rgb(0, 136, 204),
            Argb.rgb(170, 239, 255),
            Argb.rgb(230, 248, 255),
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
