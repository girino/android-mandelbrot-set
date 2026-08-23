package org.girino.frac.android.foss;

import android.graphics.Bitmap;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.HSBPalette;
import org.girino.frac.palettes.PaletteProvider;

/**
 * Mini offscreen fractal thumbnails for the formula picker (issue #30).
 * Fixed home-like viewport and the default RGB (HSB) palette so formulas
 * are comparable; not the live user palette/zoom.
 */
public final class FormulaPreview {

    static final int THUMB_WIDTH = 96;
    static final int THUMB_HEIGHT = 56;
    private static final int MAX_ITER = 40;
    /** Matches MandelbrotView home center. */
    private static final double CENTER_X = 0.0;
    private static final double CENTER_Y = 0.0;

    private FormulaPreview() {
    }

    /**
     * Scale so the thumbnail shows the same complex-plane extent as the
     * MandelbrotView home viewport (reset at width 320), not a deep zoom.
     * Using 100*300/THUMB_WIDTH alone would keep a full-screen scale on a
     * tiny bitmap and paint almost only the set interior (black).
     */
    static double referenceScale() {
        return 100.0 * 300.0 / 320.0 * THUMB_WIDTH / 320.0;
    }

    /**
     * Renders a small escape-time preview of operator. Caller owns the bitmap.
     */
    public static Bitmap createThumbnail(FractalOperator operator) {
        FractalOperator op = operator != null
                ? operator
                : FormulaCatalog.create(0);
        PaletteProvider palette = new HSBPalette();
        double scale = referenceScale();
        Bitmap bitmap = Bitmap.createBitmap(THUMB_WIDTH, THUMB_HEIGHT, Bitmap.Config.ARGB_8888);
        Complex point = new Complex();
        for (int y = 0; y < THUMB_HEIGHT; y++) {
            for (int x = 0; x < THUMB_WIDTH; x++) {
                point.set(
                        (x - THUMB_WIDTH / 2.0) / scale + CENTER_X,
                        (y - THUMB_HEIGHT / 2.0) / scale + CENTER_Y);
                double value = op.apply(point, MAX_ITER, false);
                bitmap.setPixel(x, y, palette.getColor(value));
            }
        }
        return bitmap;
    }
}
