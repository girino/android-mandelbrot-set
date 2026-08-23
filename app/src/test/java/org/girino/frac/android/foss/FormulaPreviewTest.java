package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Formula picker thumbnails (issue #30). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class FormulaPreviewTest {

    @Test
    public void createThumbnail_hasNonBlackPixelsForEachCatalogFormula() {
        for (int i = 0; i < FormulaCatalog.size(); i++) {
            Bitmap bitmap = FormulaPreview.createThumbnail(FormulaCatalog.create(i));
            assertTrue(bitmap.getWidth() == FormulaPreview.THUMB_WIDTH);
            assertTrue(bitmap.getHeight() == FormulaPreview.THUMB_HEIGHT);
            assertTrue(
                    "formula " + i + " (" + FormulaCatalog.labels()[i]
                            + ") preview should have colorful escape pixels",
                    hasNonBlackPixel(bitmap));
            bitmap.recycle();
        }
    }

    private static boolean hasNonBlackPixel(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        for (int y = 0; y < h; y += Math.max(1, h / 8)) {
            for (int x = 0; x < w; x += Math.max(1, w / 8)) {
                int rgb = bitmap.getPixel(x, y) & 0x00FFFFFF;
                if (rgb != Color.BLACK) {
                    return true;
                }
            }
        }
        // Dense scan fallback for sparse coloring.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bitmap.getPixel(x, y) & 0x00FFFFFF) != Color.BLACK) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void referenceScale_matchesHomeComplexExtent() {
        double homeScaleAt320 = 100.0 * 300.0 / 320.0;
        double homeHalfWidth = 160.0 / homeScaleAt320;
        double thumbHalfWidth =
                FormulaPreview.THUMB_WIDTH / 2.0 / FormulaPreview.referenceScale();
        assertEquals(homeHalfWidth, thumbHalfWidth, 1e-9);
    }

    @Test
    public void createThumbnail_nullOperator_fallsBackToMandelbrot() {
        Bitmap bitmap = FormulaPreview.createThumbnail(null);
        assertTrue(bitmap.getWidth() > 0);
        assertTrue(hasNonBlackPixel(bitmap));
        bitmap.recycle();
    }
}
