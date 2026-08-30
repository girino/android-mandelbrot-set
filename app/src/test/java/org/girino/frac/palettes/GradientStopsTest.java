package org.girino.frac.palettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class GradientStopsTest {

    @Test
    void sampleInterpolatesBetweenStops() {
        int[] stops = {Argb.rgb(0, 0, 0), Argb.rgb(100, 0, 0), Argb.rgb(200, 0, 0)};
        assertEquals(stops[0], GradientStops.sample(stops, 0.0));
        assertEquals(stops[2], GradientStops.sample(stops, 1.0));
        int mid = GradientStops.sample(stops, 0.5);
        assertEquals(100, Argb.red(mid));
    }

    @Test
    void lutMatchesDirectSample() {
        int[] stops = {Argb.BLACK, Argb.rgb(255, 128, 0), Argb.rgb(255, 255, 0)};
        int[] lut = GradientStops.buildLut(stops[0], stops[1], stops[2]);
        for (int i = 1; i < PaletteLut.SIZE - 1; i++) {
            double value = i / (double) PaletteLut.SIZE;
            assertEquals(GradientStops.sample(stops, value), PaletteLut.lookup(lut, value));
        }
    }

    @Test
    void fireAndOceanDifferAtMidRange() {
        assertNotEquals(FirePalette.colorAt(0.5), OceanPalette.colorAt(0.5));
    }
}
