package org.girino.frac.android.foss;

import android.graphics.Bitmap;

import org.girino.frac.palettes.PaletteProvider;

/** Builds preview strips from PaletteProvider.getColor (issue #16). */
public final class PaletteSwatch {
    /** Horizontal samples in the strip bitmap. */
    static final int STRIP_WIDTH = 128;
    static final int STRIP_HEIGHT = 28;

    private PaletteSwatch() {
    }

    /**
     * Samples the palette across escape values (inside the set stays black at
     * the ends; the colorful mid-range matches what users see in the fractal).
     */
    public static Bitmap createStrip(PaletteProvider palette) {
        Bitmap bitmap = Bitmap.createBitmap(STRIP_WIDTH, STRIP_HEIGHT, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < STRIP_WIDTH; x++) {
            double t = x / (double) (STRIP_WIDTH - 1);
            double value = 0.02 + t * 0.96;
            int color = palette.getColor(value);
            for (int y = 0; y < STRIP_HEIGHT; y++) {
                bitmap.setPixel(x, y, color);
            }
        }
        return bitmap;
    }
}
