package org.girino.frac.palettes;

/** Dark teal through green to bright cyan. */
public class ElectricTealPalette implements PaletteProvider {

    private static final int[] STOPS = {
            Argb.BLACK,
            Argb.rgb(0, 32, 40),
            Argb.rgb(0, 120, 100),
            Argb.rgb(0, 220, 180),
            Argb.rgb(120, 255, 230),
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
