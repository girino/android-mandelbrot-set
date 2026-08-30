package org.girino.frac.palettes;

/** Black to white monochrome. */
public class GrayscalePalette implements PaletteProvider {

    private static final int[] STOPS = {
            Argb.BLACK,
            Argb.rgb(64, 64, 64),
            Argb.rgb(160, 160, 160),
            Argb.rgb(255, 255, 255),
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
