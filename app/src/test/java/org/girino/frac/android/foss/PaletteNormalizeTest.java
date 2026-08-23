package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.girino.frac.palettes.HSBPalette;
import org.junit.Test;

/** Palette remapping to a shared max (issue #28). */
public class PaletteNormalizeTest {

    @Test
    public void recolor_scalesEscapedPixelsToNewMax() {
        HSBPalette palette = new HSBPalette();
        int[] pixels = new int[2];
        double[] raw = {20.0, 0.0};
        boolean[] interior = {false, true};
        // Escaped at 20 with old max 40 → mid hue; remap to max 80 → darker/earlier band.
        int colorAt40 = palette.getColor(20.0 / 40.0);
        pixels[0] = colorAt40;
        PaletteNormalize.recolor(pixels, raw, interior, 2, palette, 80, false);
        assertEquals(palette.getColor(20.0 / 80.0), pixels[0]);
        assertEquals(palette.getColor(1.0), pixels[1]);
        assertNotEquals(colorAt40, pixels[0]);
    }
}
