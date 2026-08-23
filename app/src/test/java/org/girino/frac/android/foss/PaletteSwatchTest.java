package org.girino.frac.android.foss;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Palette swatch previews (issue #16). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class PaletteSwatchTest {

    @Test
    public void createStrip_hasColorfulSamplesForEachCatalogPalette() {
        for (int i = 0; i < PaletteCatalog.size(); i++) {
            var bitmap = PaletteSwatch.createStrip(PaletteCatalog.get(i));
            assertTrue(bitmap.getWidth() > 0);
            assertTrue(bitmap.getHeight() > 0);
            int mid = bitmap.getPixel(PaletteSwatch.STRIP_WIDTH / 2, 0);
            assertNotEquals("palette " + i + " swatch should not be all black",
                    Color.BLACK, mid & 0x00FFFFFF);
            bitmap.recycle();
        }
    }
}
