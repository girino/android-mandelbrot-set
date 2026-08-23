package org.girino.frac.android.foss;

import org.girino.frac.palettes.PaletteProvider;

/**
 * Maps stored unnormalized escape counts onto a single palette max
 * (issue #28). Used when Adaptive raises the iteration limit or zoom
 * carries a higher pass-1 so all pixels share the same color scale.
 */
public final class PaletteNormalize {

    private PaletteNormalize() {
    }

    /**
     * Rewrites pixels in place. interior true → set-interior color;
     * otherwise value = rawCount / paletteMax (clamped to (0,1)).
     */
    public static void recolor(
            int[] pixels,
            double[] rawCount,
            boolean[] interior,
            int length,
            PaletteProvider palette,
            int paletteMax,
            boolean smooth) {
        if (pixels == null || rawCount == null || interior == null || palette == null) {
            return;
        }
        if (paletteMax < 1) {
            paletteMax = 1;
        }
        double denom = paletteMax;
        double interiorValue = smooth ? 0.0 : 1.0;
        int n = Math.min(length, Math.min(pixels.length, Math.min(rawCount.length, interior.length)));
        for (int i = 0; i < n; i++) {
            if (interior[i]) {
                pixels[i] = palette.getColor(interiorValue);
            } else {
                double value = rawCount[i] / denom;
                if (value <= 0) {
                    value = Double.MIN_VALUE;
                } else if (value >= 1.0) {
                    value = Math.nextDown(1.0);
                }
                pixels[i] = palette.getColor(value);
            }
        }
    }
}
