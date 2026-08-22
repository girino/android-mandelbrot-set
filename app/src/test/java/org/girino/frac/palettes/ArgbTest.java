package org.girino.frac.palettes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArgbTest {

    @Test
    void rgbPacksOpaqueChannels() {
        assertEquals(0xFFFF8000, Argb.rgb(255, 128, 0));
        assertEquals(255, Argb.red(0xFFFF8000));
        assertEquals(128, Argb.green(0xFFFF8000));
        assertEquals(0, Argb.blue(0xFFFF8000));
    }

    @Test
    void hsvRedSectorMatchesPrimaryRed() {
        assertEquals(Argb.rgb(255, 0, 0), Argb.hsvToColor(new float[] {0f, 1f, 1f}));
    }

    @Test
    void hsvZeroSaturationIsGrey() {
        assertEquals(Argb.rgb(128, 128, 128), Argb.hsvToColor(new float[] {120f, 0f, 0.5f}));
    }
}
