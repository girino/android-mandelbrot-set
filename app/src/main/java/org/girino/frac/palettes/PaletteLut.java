package org.girino.frac.palettes;

/**
 * Precomputed escape-value to ARGB lookup (same indexing as SmoothFixedPalette).
 */
public final class PaletteLut {

    public static final int SIZE = 1024;

    public interface ColorFunction {
        int colorAt(double value);
    }

    private PaletteLut() {
    }

    public static int[] build(ColorFunction fn) {
        int[] lut = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            double value = i / (double) SIZE;
            // Interior (non-escaping) pixels use value -> 1.0; only that end is black.
            // Fast escapes at high maxIter map to small values and must use the palette low end.
            if ((1.0 - value) < PaletteProvider.epsilon) {
                lut[i] = Argb.BLACK;
            } else {
                lut[i] = fn.colorAt(value);
            }
        }
        return lut;
    }

    public static int lookup(int[] lut, double value) {
        if ((1.0 - value) < PaletteProvider.epsilon) {
            return Argb.BLACK;
        }
        return lut[(int) (lut.length * value)];
    }
}
